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
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.query.QueryActorFilter;
import com.caucho.bam.stream.MessageStream;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's address as the "from" parameter.
 */
public class SimpleActorSender extends AbstractActorSender implements ActorSender {
  private Actor _actor;
  private Broker _broker;
  private String _clientAddress;

  public SimpleActorSender(String address, Broker broker)
  {
    this(address, (Actor) null, broker);
    
    _clientAddress = address;
  }
  
  public SimpleActorSender(Actor next)
  {
    this(next.getAddress(), next, next.getBroker());
  }
  
  public SimpleActorSender(String address, Actor next, Broker broker)
  {
    super(address);
    
    if (next == null)
      next = new DefaultActor();
    
    _actor = new QueryActorFilter(next, getQueryManager());
    _broker = broker;
    
    _clientAddress  = next.getAddress();
  }
  
  public SimpleActorSender(Actor next,
                           ManagedBroker broker,
                           String uid, 
                           String resource)
  {
    this(uid + "/" + resource, next, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(next.getAddress(),
                                             _actor,
                                             broker, 
                                             1);

    // MessageStream stream = broker.createClient(mailbox, uid, resource);
    // _clientAddress = stream.getAddress();
    broker.addMailbox(next.getAddress(), mailbox);
  }
  
  public SimpleActorSender(ManagedBroker broker,
                           String uid)
  {
    this(broker, uid, null);
  }
  
  public SimpleActorSender(ManagedBroker broker,
                           String uid, 
                           String resource)
  {
    this(uid + "/" + resource, (Actor) null, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(null,
                                             _actor,
                                             broker, 
                                             1);

    MessageStream stream = broker.createClient(mailbox, uid, resource);
    _clientAddress = stream.getAddress();
  }

  /**
   * Returns the Actor's address used for all "from" parameters.
   */
  @Override
  public String getAddress()
  {
    if (_actor != null)
      return _actor.getAddress();
    else
      return _clientAddress;
  }

  //
  // streams
  //

  public Actor getActor()
  {
    return _actor;
  }
  /**
   * The underlying, low-level stream to the link
   */
  @Override
  public Broker getBroker()
  {
    return _broker;
  }
  
  public void setBroker(Broker broker)
  {
    _broker = broker;
  }
  
  protected ManagedBroker getManagedBroker()
  {
    return (ManagedBroker) getBroker();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getActor() + "]";
  }
  
  class DefaultActor extends AbstractActor {
    @Override
    public String getAddress()
    {
      return _clientAddress;
    }
    
    @Override
    public Broker getBroker()
    {
      return SimpleActorSender.this.getBroker();
    }
  }
}
