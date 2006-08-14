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

package com.caucho.jms.services;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.jms.*;
import javax.ejb.*;
import javax.xml.soap.*;
import javax.xml.stream.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.caucho.soap.reflect.WebServiceIntrospector;

import com.caucho.soap.skeleton.DirectSkeleton;

import com.caucho.vfs.StringStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

public class ServicesListener implements MessageListener 
{
  private static final Logger log =
    Logger.getLogger(ServicesListener.class.getName());

  private String _outboundQueueName = "jms/OutboundQueue";
  private String _connectionFactoryName;
  private MessageProducer _producer;
  private Session _jmsSession;
  private Connection _jmsConnection;
  private Object _object;
  private Class _class;
  private DirectSkeleton _skeleton;
  private boolean _initialized = false;
  private transient MessageDrivenContext _messageDrivenContext = null;

  public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
    throws EJBException
  {
    _messageDrivenContext = messageDrivenContext;
  }

  public void setOutboundQueue(String outboundQueueName)
  {
    _outboundQueueName = outboundQueueName;
  }

  public void setConnectionFactory(String connectionFactoryName)
  {
    _connectionFactoryName = connectionFactoryName;
  }

  public void setService(Object o)
    throws Exception
  {
    _object = o;

    if (_class == null)
      _class = o.getClass();
  }

  public void init()
  {
    _initialized = true;

    if (_outboundQueueName == null) {
      log.fine("OutboundQueue not set, aborting.");
      return;
    }

    if (_connectionFactoryName == null) {
      log.fine("ConnectionFactory not set, assuming jms/ConnectionFactory.");
      _connectionFactoryName = "jms/ConnectionFactory";
    }

    try {
      Context context = (Context) new InitialContext().lookup("java:comp/env");

      ConnectionFactory connectionFactory = 
        (ConnectionFactory) context.lookup(_connectionFactoryName);

      Destination destination = 
        (Destination) context.lookup(_outboundQueueName);

      _jmsConnection = connectionFactory.createConnection();
      _jmsSession = 
        _jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      _producer = _jmsSession.createProducer(destination);
    } catch (Exception e) {
      log.fine(e.toString());
    }
  }

  public void onMessage(Message message)
  {
    if (!_initialized)
      init();

    if (_producer == null)
      return;

    try {
      if (message instanceof TextMessage) {
        String text = ((TextMessage) message).getText();
        TempStream tempStream = new TempStream();
        WriteStream ws = new WriteStream(tempStream);

        try {
          XMLInputFactory factory = XMLInputFactory.newInstance();

          XMLStreamReader xmlReader =
            factory.createXMLStreamReader(StringStream.open(text));

          getSkeleton().invoke(_object, xmlReader, ws);
          ws.flush();
          tempStream.flush();
        } catch (XMLStreamException e) {
          log.info(e.toString());
        } catch (IOException e) {
          log.info(e.toString());
        }

        BytesMessage bytesMessage = _jmsSession.createBytesMessage();

        for (TempBuffer buffer = tempStream.getHead(); 
             buffer != null; 
             buffer = buffer.getNext())
          bytesMessage.writeBytes(buffer.getBuffer(), 0, buffer.getLength());

        _producer.send(bytesMessage);

      }    
    } catch (JMSException e) {
      log.info("jms exception: " + e);
      _messageDrivenContext.setRollbackOnly();
    }
  }

  private DirectSkeleton getSkeleton()
  {
    if (_skeleton == null)
      _skeleton = new WebServiceIntrospector().introspect(_class);
    
    return _skeleton;
  }
}
