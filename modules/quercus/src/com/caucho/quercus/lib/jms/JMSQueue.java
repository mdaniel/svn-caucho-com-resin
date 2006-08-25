/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.quercus.lib.jms;

import java.io.Serializable;

import java.util.*;

import java.util.logging.Logger;

import javax.jms.*;
import javax.naming.*;

import com.caucho.util.L10N;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ResourceValue;

import com.caucho.quercus.program.JavaClassDef;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.AbstractQuercusModule;

/**
 * JMS functions
 */
public class JMSQueue {
  private static final Logger log = Logger.getLogger(JMSQueue.class.getName());

  private Connection _connection;
  private Session _session;
  private MessageConsumer _consumer;
  private MessageProducer _producer;
  private Destination _destination;

  /**
   * Connects to a named queue.
   */
  public JMSQueue(Context context, ConnectionFactory connectionFactory,
                  String queueName)
    throws Exception
  {
    _connection = connectionFactory.createConnection();

    _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    if (queueName == null || queueName.length() == 0)
      _destination = _session.createTemporaryQueue();
    else
      _destination = (Destination) context.lookup(queueName);

    _consumer = _session.createConsumer(_destination);
    _producer = _session.createProducer(_destination);

    _connection.start();
  }

  public static Value __construct(Env env, @Optional String queueName)
  {
    JMSQueue queue = JMSModule.message_get_queue(env, queueName, null);

    return new JavaValue(env, queue, 
                         env.getJavaClassDefinition(JMSQueue.class.getName()));
  }

  public boolean send(@NotNull Value value, @Optional JMSQueue replyTo)
    throws JMSException
  {
    Message message = null;

    if (value.isArray()) {
      message = _session.createMapMessage();

      ArrayValue array = (ArrayValue) value;

      Set<Map.Entry<Value,Value>> entrySet = array.entrySet();

      for (Map.Entry<Value,Value> entry : entrySet) {
        if (entry.getValue() instanceof BinaryValue) {
          byte []bytes = ((BinaryValue) entry.getValue()).toBytes();

          ((MapMessage) message).setBytes(entry.getKey().toString(), bytes);
        } else {
          // every primitive except for bytes can be translated from a string
          ((MapMessage) message).setString(entry.getKey().toString(), 
                                           entry.getValue().toString());
        }
      }
    } else if (value instanceof BinaryValue) {
      message = _session.createBytesMessage();


      byte []bytes = ((BinaryValue) value).toBytes();

      ((BytesMessage) message).writeBytes(bytes);
    } else if (value.isLongConvertible()) {
      message = _session.createStreamMessage();

      ((StreamMessage) message).writeLong(value.toLong());
    } else if (value.isDoubleConvertible()) {
      message = _session.createStreamMessage();

      ((StreamMessage) message).writeDouble(value.toDouble());
    } else if (value.toJavaObject() instanceof String) {
      message = _session.createTextMessage();

      ((TextMessage) message).setText(value.toString());
    } else if (value.toJavaObject() instanceof Serializable) {
      message = _session.createObjectMessage();

      ((ObjectMessage) message).setObject((Serializable) value.toJavaObject());
    } else {
      return false;
    }

    if (replyTo != null)
      message.setJMSReplyTo(replyTo._destination);

    _producer.send(message);

    return true;
  }

  public Value receive(Env env, @Optional("1") long timeout)
    throws JMSException
  {
    try {
      java.io.FileWriter out = new java.io.FileWriter("/tmp/x.log", true);
      out.write("timeout = " + timeout + "\n");
      out.close();
    } catch (java.io.IOException e) {}
    Message message = _consumer.receive(timeout);

    if (message == null)
      return BooleanValue.FALSE;

    if (message instanceof ObjectMessage) {
      Object object = ((ObjectMessage) message).getObject();

      return objectToValue(object, env);
    } else if (message instanceof TextMessage) {
      return new StringValueImpl(((TextMessage) message).getText());
    } else if (message instanceof StreamMessage) {
      Object object = ((StreamMessage) message).readObject();

      return objectToValue(object, env);
    } else if (message instanceof BytesMessage) {
      BytesMessage bytesMessage = (BytesMessage) message;

      BinaryBuilderValue bb = 
        new BinaryBuilderValue((int) bytesMessage.getBodyLength());

      bytesMessage.readBytes(bb.getBuffer());
      bb.setOffset((int) bytesMessage.getBodyLength());

      return bb;
    } else if (message instanceof MapMessage) {
      MapMessage mapMessage = (MapMessage) message;

      Enumeration mapNames = mapMessage.getMapNames();

      ArrayValue array = new ArrayValueImpl();

      while (mapNames.hasMoreElements()) {
        String name = mapNames.nextElement().toString();

        Object object = mapMessage.getObject(name);

        array.put(new StringValueImpl(name), objectToValue(object, env));
      }

      return array;
    } else {
      return BooleanValue.FALSE;
    }
  }

  protected void finalize()
  {
    try {
      _connection.close();
    } catch (JMSException e) {}
  }

  private static Value objectToValue(Object object, Env env)
  {
    JavaClassDef def = 
      env.getQuercus().getJavaClassDefinition(object.getClass().getName());

    if (object instanceof Long) {
      return LongValue.create(((Long) object).longValue());
    } else if (object instanceof Integer) {
      return LongValue.create(((Integer) object).intValue());
    } else if (object instanceof Double) {
      return DoubleValue.create(((Double) object).doubleValue());
    } else {
      return new JavaValue(env, object, def);
    }
  }
}

