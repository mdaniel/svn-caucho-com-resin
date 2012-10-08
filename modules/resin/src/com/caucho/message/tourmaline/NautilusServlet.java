/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.message.tourmaline;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.message.broker.MessageBroker;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * Custom serialization for the cache
 */
public class NautilusServlet extends GenericServlet
{
  private MessageBroker _broker;
  
  public void setBroker(MessageBroker broker)
  {
    _broker = broker;
  }
  
  @PostConstruct
  public void init()
  {
    if (_broker == null) {
      _broker = EnvironmentMessageBroker.create();
    }
  }
  
  public MessageBroker getBroker()
  {
    return _broker;
  }
  
  @Override
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    WebSocketServletRequest wsReq = (WebSocketServletRequest) req;
    
    wsReq.startWebSocket(new NautilusServerEndpoint(getBroker()));
  }
}
