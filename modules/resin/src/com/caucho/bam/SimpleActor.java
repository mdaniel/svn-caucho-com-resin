/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.bam;

import java.util.logging.*;
import java.util.concurrent.atomic.*;

import java.io.Serializable;

/**
 * Base class for implementing an Agent.
 */
public class SimpleActor extends SimpleActorStream
  implements Actor
{
  private static final Logger log
    = Logger.getLogger(SimpleActor.class.getName());

  private ActorStream _actorStream = this;

  public SimpleActor()
  {
    setActorClient(new ProxyActorClient(this));
  }
  
  //
  // basic Actor API
  //

  /**
   * Returns the custom {@link com.caucho.bam.ActorStream} to the
   * {@link com.caucho.bam.Broker}, so the Broker can send messages to
   * the agent.
   *
   * Developers will customize the ActorStream to receive messages from
   * the Broker.
   */
  public ActorStream getActorStream()
  {
    return _actorStream;
  }
  
  /**
   * Returns the stream to the actor for broker-forwarded messages.
   */
  public void setActorStream(ActorStream actorStream)
  {
    _actorStream = actorStream;
  }

  //
  // Actor children
  //

  /**
   * Requests that a client actor with the given jid be started.
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   */
  public boolean startChild(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " startChild(" + jid + ")");
    
    return false;
  }

  /**
   * Requests that a client actor with the given jid be stopped.
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   * 
   * @jid the jid of the child actor logging in.
   */
  public boolean stopChild(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " stopChild(" + jid + ")");
    
    return false;
  }

  /**
   * Called when a a child actor logs in
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   * 
   * @jid the jid of the child actor logging in.
   */
  public void onChildStart(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onChildStart(" + jid + ")");
  }

  /**
   * Called when a child actor logs out
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   * 
   * @jid the jid of the child actor logging in.
   */
  public void onChildStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onChildStop(" + jid + ")");
  }

  //
  // Filter API
  //

  /**
   * Creates an outbound filter.
   */
  public ActorStream getActorFilter(ActorStream stream)
  {
    return stream;
  }

  /**
   * Creates an inbound filter
   */
  public ActorStream getBrokerFilter(ActorStream stream)
  {
    return stream;
  }

  /**
   * Closes the actor stream.
   */
  @Override
  public void close()
  {
  }
}
