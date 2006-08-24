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

package com.caucho.esb.transport;

import java.io.OutputStream;

import java.util.logging.Logger;

import javax.ejb.MessageDrivenContext;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import com.caucho.esb.WebService;
import com.caucho.esb.encoding.ServiceEncoding;

import com.caucho.jms.util.BytesMessageInputStream;
import com.caucho.jms.util.BytesMessageOutputStream;

import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.StartListener;

import com.caucho.util.NullOutputStream;

public class JmsTransport implements ServiceTransport {
  private static final Logger log =
    Logger.getLogger(JmsTransport.class.getName());

  private ServiceEncoding _encoding;
  private boolean _sendResponse = true;

  private Destination _destination;
  private ConnectionFactory _connectionFactory;
  private Session _jmsSession;
  private Connection _jmsConnection;

  private int _listenerMax = 5;
  private Object _object;
  private Class _class;

  private WebService _webService;

  public void setWebService(WebService webService)
  {
    _webService = webService;
  }

  public void setSendResponse(boolean sendResponse)
  {
    _sendResponse = sendResponse;
  }

  public void setEncoding(ServiceEncoding encoding)
  {
    _encoding = encoding;
  }

  public void setQueue(Destination queue)
  {
    _destination = queue;
  }

  public void setTopic(Destination topic)
  {
    _destination = topic;
  }

  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  public void init()
  {
    try {
      _jmsConnection = _connectionFactory.createConnection();
    } catch (Exception e) {
      log.fine(e.toString());
    }
    
    if (_destination instanceof Topic)
      _listenerMax = 1;

    Environment.addEnvironmentListener(new StartListener(this));
    Environment.addClassLoaderListener(new CloseListener(this));
  }

  public void start() throws Throwable
  {
    for (int i = 0; i < _listenerMax; i++) {
      Session session = 
        _jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer consumer = session.createConsumer(_destination);

      consumer.setMessageListener(new ServicesListenerMDB(session));
    }

    _jmsConnection.start();
  }

  public void stop() throws JMSException
  {
    _jmsConnection.stop();
  }

  private class ServicesListenerMDB implements MessageListener {
    private Session _jmsSession;
    private MessageDrivenContext _messageDrivenContext = null;

    public ServicesListenerMDB(Session jmsSession)
    {
      _jmsSession = jmsSession;
    }

    public 
    void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
    {
      _messageDrivenContext = messageDrivenContext;
    }

    public void onMessage(Message message)
    {
      try {
        if (message instanceof BytesMessage) {
          BytesMessageInputStream is = 
            new BytesMessageInputStream((BytesMessage) message);

          BytesMessage outMessage = null;
          MessageProducer producer = null;
          OutputStream os = null;

          if (_sendResponse && message.getJMSReplyTo() != null) {
            producer = _jmsSession.createProducer(message.getJMSReplyTo());

            outMessage = _jmsSession.createBytesMessage();

            os = new BytesMessageOutputStream(outMessage);
          } 
          else 
            os = new NullOutputStream();

          _encoding.invoke(is, os);

          if (_sendResponse && message.getJMSReplyTo() != null)
            producer.send(outMessage);
        }    
      } catch (JMSException e) {
        log.fine("JMS exception: " + e);
      }
    }
  }

}
