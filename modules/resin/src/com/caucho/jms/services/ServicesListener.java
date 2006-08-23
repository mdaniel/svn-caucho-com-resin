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

import java.io.IOException;

import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenContext;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import javax.naming.Context;
import javax.naming.InitialContext;

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.DirectSkeleton;

import com.caucho.vfs.StringStream;
import com.caucho.vfs.NullWriteStream;

public class ServicesListener {
  private static final Logger log =
    Logger.getLogger(ServicesListener.class.getName());

  private Destination _destination;
  private ConnectionFactory _connectionFactory;
  private Session _jmsSession;
  private Connection _jmsConnection;

  private int _listenerMax = 5;
  private Object _object;
  private Class _class;
  private DirectSkeleton _skeleton;

  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  public void setService(Object service)
  {
    _object = service;

    if (_class == null)
      _class = service.getClass();
  }

  public void setInterface(Class cl)
  {
    _class = cl;
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
  }

  public void start() throws Throwable
  {
    for (int i = 0; i < _listenerMax; i++) {
      Session session = 
        _jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer consumer = session.createConsumer(_destination);

      consumer.setMessageListener(new ServicesListenerMDB());
    }

    _jmsConnection.start();
  }

  public void stop() throws JMSException
  {
    _jmsConnection.stop();
  }

  private class ServicesListenerMDB implements MessageListener {
    private MessageDrivenContext _messageDrivenContext = null;

    public 
    void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
      throws EJBException
    {
      _messageDrivenContext = messageDrivenContext;
    }

    public void onMessage(Message message)
    {
      try {
        if (message instanceof TextMessage) {
          String text = ((TextMessage) message).getText();
          
          // ignore return value
          NullWriteStream ws = new NullWriteStream();

          try {
            XMLInputFactory factory = XMLInputFactory.newInstance();

            XMLStreamReader xmlReader =
              factory.createXMLStreamReader(StringStream.open(text));

            getSkeleton().invoke(_object, xmlReader, ws);
          } catch (XMLStreamException e) {
            log.info(e.toString());
          } catch (IOException e) {
            log.info(e.toString());
          }
        }    
      } catch (JMSException e) {
        log.info("JMS exception: " + e);
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
}
