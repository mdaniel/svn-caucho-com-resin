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

package com.caucho.bam.actor;

import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.AbstractMessageStream;
import com.caucho.bam.stream.MessageStream;

/**
 * Base class for implementing an Agent.
 */
public class SimpleActor extends AbstractMessageStream
  implements ManagedActor, ActorHolder
{
  private final SkeletonActorFilter<?> _skeleton;
  private final SimpleActorSender _sender;
  
  private String _address;
  private Broker _broker;
  private Mailbox _mailbox;

  public SimpleActor()
  {
    _address = getClass().getSimpleName() + "@localhost";
    
    _skeleton = new SkeletonActorFilter(this, this);
    _sender = new SimpleActorSender(_skeleton);
  }

  public SimpleActor(String address, Broker broker)
  {
    this();
    
    _address = address;
    
    setBroker(broker);
    
    if (broker == null)
      throw new IllegalArgumentException("broker must not be null");
  }
  
  //
  // basic Actor API
  //

  /* (non-Javadoc)
   * @see com.caucho.bam.actor.Actor#setAddress(java.lang.String)
   */
  @Override
  public String getAddress()
  {
    return _address;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.actor.Actor#setAddress(java.lang.String)
   */
  @Override
  public void setAddress(String address)
  {
    _address = address;
  }

  /**
   * Returns the custom {@link com.caucho.bam.stream.MessageStream} to the
   * {@link com.caucho.bam.broker.Broker}, so the Broker can send messages to
   * the agent.
   *
   * Developers will customize the ActorStream to receive messages from
   * the Broker.
   */
  @Override
  public Actor getActor()
  {
    return _sender.getActor();
  }
  
  /**
   * Returns the stream to the actor for broker-forwarded messages.
   */
  /*
  public void setActor(Actor actorStream)
  {
    // _actor = actorStream;
  }
  */
  

  /**
   * Returns the ActorClient to the link for convenient message calls.
   */
  public ActorSender getSender()
  {
    return _sender;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#getMailbox()
   */
  @Override
  public Mailbox getMailbox()
  {
    return _mailbox;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#setMailbox(com.caucho.bam.mailbox.Mailbox)
   */
  @Override
  public void setMailbox(Mailbox mailbox)
  {
    _mailbox = mailbox;
  }

  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public void setBroker(Broker broker)
  {
    if (broker == null)
      throw new NullPointerException();
    
    _broker = broker;
    _sender.setBroker(broker);
  }
}  
