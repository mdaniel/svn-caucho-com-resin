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

package com.caucho.bam.broker;

import com.caucho.bam.actor.AbstractAgent;
import com.caucho.bam.actor.Agent;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MailboxType;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.mailbox.NonQueuedMailbox;
import com.caucho.bam.stream.ActorStream;


/**
 * Broker is the hub which routes messages to actors.
 */
abstract public class AbstractBroker 
  extends AbstractBrokerStream
  implements Broker
{
  /**
   * Returns the broker's jid, i.e. the virtual host domain name.
   */
  @Override
  public String getJid()
  {
    return getClass().getSimpleName() + ".localhost";
  }
  
  /**
   * Returns the stream to the broker
   */
  @Override
  public ActorStream getBrokerStream()
  {
    return this;
  }
  
  /**
   * Adds an actor.
   */
  @Override
  abstract public void addActor(ActorStream actorStream);
  
  /**
   * Removes an actor.
   */
  @Override
  abstract public void removeActor(ActorStream actorStream);
  
  /**
   * Creates an agent
   */
  @Override
  public Agent createAgent(ActorStream actorStream)
  {
    return createAgent(actorStream, MailboxType.DEFAULT);
  }
    
  /**
   * Creates an agent
   */
  @Override
  public Agent createAgent(ActorStream actorStream,
                           MailboxType mailboxType)
  {
    Mailbox mailbox = createMailbox(actorStream, mailboxType);
    
    Agent agent = new AbstractAgent(actorStream.getJid(),
                                    mailbox,
                                    this);
    
    addActor(agent.getMailbox());
    
    return agent;
  }
  
  protected Mailbox createMailbox(ActorStream actorStream,
                                  MailboxType mailboxType)
  {
    switch (mailboxType) {
    case NON_QUEUED:
      return new NonQueuedMailbox(actorStream);
      
    default:
      return new MultiworkerMailbox(actorStream, getBrokerStream(), 1);
    }
  }
  
  /**
   * Registers the client under a unique id. The
   * resource is only a suggestion; the broker may
   * return a different resource id.
   * 
   * @param clientStream the stream to the client
   * @param uid the client's uid
   * @param resource the suggested resource for the jid
   * @return the generated jid
   */
  @Override
  public String createClient(ActorStream clientStream,
                             String uid,
                             String resource)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Registers an listener for broker events, e.g. a missing actor.
   */
  @Override
  public void addBrokerListener(BrokerListener listener)
  {

  }
  
  /**
   * Returns true if the broker has been closed
   */
  @Override
  public boolean isClosed()
  {
    return false;
  }
}
