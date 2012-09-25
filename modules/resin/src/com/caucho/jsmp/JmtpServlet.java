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

package com.caucho.jsmp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.broker.AbstractBroker;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.AbstractMessageStream;
import com.caucho.bam.stream.MessageStream;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * JmtpReader stream handles client packets received from the server.
 */
public class JmtpServlet extends GenericServlet {
  private static final L10N L = new L10N(JmtpServlet.class);

  private static final Logger log
    = Logger.getLogger(JmtpServlet.class.getName());

  private Class<?> _actorClass;

  public void setActorClass(Class<?> actorClass)
  {
    _actorClass = actorClass;
  }

  public void init()
  {
    if (_actorClass == null)
      throw new ConfigException(L.l("JmtpServlet requires an actor"));
  }

  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    WebSocketServletRequest wsRequest = (WebSocketServletRequest) request;

    ActorHolder actor;

    try {
      actor = (ActorHolder) _actorClass.newInstance();
    } catch (Exception e) {
      throw new ServletException(e);
    }

    Listener listener = new Listener(actor);

    wsRequest.startWebSocket(listener);
  }

  static class Listener extends AbstractWebSocketListener {
    private ActorHolder _actor;
    private MessageStream _actorStream;

    private InputStream _is;
    private OutputStream _os;

    private JsmpReader _jmtpReader;
    private JsmpWriter _jmtpWriter;
    
    private JmtpMailbox _jmtpMailbox;
    private JmtpBroker _jmtpBroker;

    Listener(ActorHolder actor)
    {
      _actor = actor;
      
      if (_actor == null)
        throw new NullPointerException();
    }

    @Override
    public void onStart(WebSocketContext context)
      throws IOException
    {
      _jmtpMailbox = new JmtpMailbox(this);
      
      _actor.setBroker(new JmtpBroker(this));
      _actorStream = _actor.getActor();
    }

    @Override
    public void onReadBinary(WebSocketContext context, InputStream is)
      throws IOException
    {
      JsmpReader reader = new JsmpReader(is);
      
      reader.readPacket(_actorStream);
    }

    public void onReadText(WebSocketContext context,
                           Reader is)
    throws IOException
    {
      /*
      JmtpReader reader = new JmtpReader(is);

      reader.readPacket(_actorStream);
       */
    }
  }
  
  private static class JmtpBroker extends AbstractBroker {
    private Listener _listener;
    
    JmtpBroker(Listener listener)
    {
      _listener = listener;
    }
    
    public Mailbox getMailbox(String address)
    {
      return _listener._jmtpMailbox;
    }
  }
  
  private static class JmtpMailbox extends AbstractMessageStream implements Mailbox {
    private Listener _listener;
    
    JmtpMailbox(Listener listener)
    {
      _listener = listener;
    }
    
    public Broker getBroker()
    {
      return _listener._jmtpBroker;
    }
    
    public int getSize()
    {
      return -1;
    }
    
    @Override
    public MessageStream getActorStream()
    {
      return null;
    }

    @Override
    public void close()
    {
    }
  }
}
