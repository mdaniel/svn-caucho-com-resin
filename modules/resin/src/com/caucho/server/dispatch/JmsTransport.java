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

package com.caucho.server.dispatch;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.caucho.jms.services.ServicesListener;

import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.StartListener;

/**
 * A child of &lt;web-service&gt; that describes a published interface to
 * the service.
 */
public abstract class JmsTransport {
  private ConnectionFactory _connectionFactory;
  private String _connectionFactoryName = "jms/ConnectionFactory";
  private String _destinationName;
  private WebService _webService;

  public JmsTransport(WebService webService)
  {
    _webService = webService;
  }

  protected WebService getWebService()
  {
    return _webService;
  }

  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  protected ConnectionFactory getConnectionFactory()
  {
    return _connectionFactory;
  }

  public void setQueue(String queueName)
  {
    _destinationName = queueName;
  }

  public void setTopic(String topicName)
  {
    _destinationName = topicName;
  }

  public void init()
  {
    try {
      Context context = (Context) new InitialContext().lookup("java:comp/env");

      if (_connectionFactory == null) {
        _connectionFactory = 
          (ConnectionFactory) context.lookup(_connectionFactoryName);
      }

      Destination destination = (Destination) context.lookup(_destinationName);

      initService(destination);
    } catch (NamingException e) {
    }
  }

  protected abstract void initService(Destination destination);
}
