/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.ActorStream;

/**
 * Base class for implementing an Agent.
 */
public class SimpleActor extends SimpleActorStream
  implements Actor
{
  private ActorStream _actorStream;
  
  private final SimpleActorClient _sender;

  public SimpleActor()
  {
    _sender = new SimpleActorClient(this);
    
    setActorStream(_sender.getActorStream());
  }

  public SimpleActor(Broker broker)
  {
    setBroker(broker);
    
    _sender = new SimpleActorClient(this, broker);
    
    setActorStream(_sender.getActorStream());
  }
  
  //
  // basic Actor API
  //

  /**
   * Returns the custom {@link com.caucho.bam.stream.ActorStream} to the
   * {@link com.caucho.bam.broker.Broker}, so the Broker can send messages to
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
  

  /**
   * Sets the Actor's jid so the {@link com.caucho.bam.broker.Broker} can
   * register it.
   */
  @Override
  public void setJid(String jid)
  {
    super.setJid(jid);
  }

  /**
   * Returns the ActorClient to the link for convenient message calls.
   */
  public ActorSender getSender()
  {
    return _sender;
  }
}
