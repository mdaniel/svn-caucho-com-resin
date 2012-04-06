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

package com.caucho.hmtp.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.actor.BamSkeleton;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.HashMapBroker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.PassthroughMailbox;
import com.caucho.bam.stream.MessageStream;
import com.caucho.bam.stream.FallbackMessageStream;
import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
@SuppressWarnings("serial")
public class HmtpServlet extends HttpServlet implements ActorHolder, Actor
{
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());
  
  private final AtomicInteger _gId = new AtomicInteger();

  private String _address;
  private ManagedBroker _servletBroker;
  private Actor _servletActorStream = this;

  private BamSkeleton _skeleton;
  private MessageStream _servletFallbackStream;
  private ActorHolder _actor;
  
  private HashMapBroker _broker;
  private Mailbox _mailbox;
  
  public HmtpServlet()
  {
    _address = getClass().getSimpleName() + "@localhost";
  }

  @Override
  public String getAddress()
  {
    return _address;
  }

  @Override
  public void setAddress(String address)
  {
    _address = address;
  }
  
  public String getBrokerAddress()
  {
    return getClass().getSimpleName() + ".broker.localhost";
  }

  @Override
  public void init()
  {
    _broker = new HashMapBroker(getBrokerAddress());
    
    _skeleton = BamSkeleton.getSkeleton(getClass());
    
    Mailbox servletMailbox = createServletMailbox();
    String address = servletMailbox.getAddress();
    
    _broker.addMailbox(address, servletMailbox);
  }
  
  @Override
  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    WebSocketServletRequest req = (WebSocketServletRequest) request;
    
    WebSocketListener listener = createWebSocketListener();
    
    req.startWebSocket(listener);
  }
  
  protected WebSocketListener createWebSocketListener()
  {
    return new HmtpClientWebSocketListener(this);
  }
  
  /**
   * Creates and returns the actor for the client link
   */
  
  protected ClientLinkActor createClientLinkActor(String uid,
                                                  MessageStream hmtpStream)
  {
    if (uid == null)
      uid = "anon";
    
    int resource = _gId.incrementAndGet();
    
    String address = uid + "@" + getBrokerAddress() + "/" + resource;
    
    return new ClientLinkActor(address, _broker, hmtpStream);
  }

  public void addClientLinkActor(ClientLinkActor linkActor)
  {
    _broker.createAgent(linkActor.getActorStream());
  }  

  public void removeClientLinkActor(ActorHolder linkActor)
  {
  }

  public void destroyClientLinkActor(ActorHolder linkActor)
  {
  }
  
  protected Mailbox createServletMailbox()
  {
    _servletFallbackStream = new FallbackMessageStream(this);
    
    return new PassthroughMailbox(getAddress(), this, _broker);
  }

  //
  // servlet Actor methods
  //
  
  @Override
  public Actor getActor()
  {
    return _servletActorStream;
  }

  public void setActorStream(Actor actorStream)
  {
    _servletActorStream = actorStream;
  }
  
  @Override
  public ManagedBroker getBroker()
  {
    return _broker;
  }
  
  public void setBroker(Broker broker)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public Mailbox getMailbox()
  {
    return _mailbox;
  }
  
  public void setMailbox(Mailbox mailbox)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected MessageStream getFallbackStream()
  {
    return _servletFallbackStream;
  }
  
  //
  // ActorStream methods
  //

  @Override
  public void message(String to, String from, Serializable payload)
  {
    _skeleton.message(this, _servletFallbackStream, to, from, payload);
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#messageError(java.lang.String, java.lang.String, java.io.Serializable, com.caucho.bam.ActorError)
   */
  @Override
  public void messageError(String to,
                           String from, 
                           Serializable payload,
                           BamError error)
  {
    _skeleton.messageError(this, 
                           getFallbackStream(),
                           to, 
                           from, 
                           payload, 
                           error);
  }

  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    _skeleton.query(this,
                    getFallbackStream(),
                    getBroker(),
                    id,
                    to, 
                    from, 
                    payload);
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    _skeleton.queryResult(this,
                          getFallbackStream(),
                          id,
                          to, 
                          from, 
                          payload);
  }

  @Override
  public void queryError(long id, 
                         String to, 
                         String from, 
                         Serializable payload,
                         BamError error)
  {
    _skeleton.queryError(this,
                         getFallbackStream(),
                         id,
                         to, 
                         from, 
                         payload,
                         error);
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }

}
