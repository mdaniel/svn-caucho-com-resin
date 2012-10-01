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

package com.caucho.bam.broker;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.bam.actor.AbstractAgent;
import com.caucho.bam.actor.Agent;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MailboxType;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.mailbox.PassthroughMailbox;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.CurrentTime;

/**
 * Broker is the hub which routes messages to actors.
 */
abstract public class AbstractManagedBroker 
  extends AbstractBroker
  implements ManagedBroker
{
  private final AtomicLong _sequence
    = new AtomicLong(CurrentTime.getCurrentTime());
  
  /**
   * Adds a mailbox.
   */
  @Override
  public void addMailbox(String address, Mailbox mailbox)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Removes a mailbox.
   */
  @Override
  public void removeMailbox(Mailbox mailbox)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Creates an agent
   */
  // @Override
  public Agent createAgent(MessageStream actorStream)
  {
    return createAgent(actorStream, MailboxType.DEFAULT);
  }
    
  /**
   * Creates an agent
   */
  // @Override
  public Agent createAgent(MessageStream actorStream,
                           MailboxType mailboxType)
  {
    String address = actorStream.getAddress();
    
    Mailbox mailbox = createMailbox(address,
                                    actorStream, 
                                    mailboxType);
    
    Agent agent = new AbstractAgent(actorStream.getAddress(),
                                    mailbox,
                                    this);

    addMailbox(address, mailbox);
    
    return agent;
  }
  
  protected Mailbox createMailbox(MessageStream actorStream,
                                  MailboxType mailboxType)
  {
    return createMailbox(actorStream.getAddress(), actorStream, mailboxType);
  }
  
  protected Mailbox createMailbox(String address,
                                  MessageStream actorStream,
                                  MailboxType mailboxType)
  {
    switch (mailboxType) {
    case NON_QUEUED:
      return new PassthroughMailbox(address, actorStream, this);
      
    default:
      return new MultiworkerMailbox(address, actorStream, this, 5);
    }
  }
  
  @Override
  public Mailbox createClient(Mailbox next,
                              String uid,
                              String resource)
  {
    String address = null;
    
    if (uid == null)
      uid = Long.toHexString(_sequence.incrementAndGet());
    
    if (uid.indexOf('@') < 0)
      uid = uid + '@' + getAddress();
    
    if (resource != null) {
      address = uid + "/" + resource;
      
      Mailbox mailbox = getMailbox(address);
      
      if (mailbox != null)
        address = uid + "/" + resource + "-" + Long.toHexString(_sequence.incrementAndGet());
    }
    else {
      address = uid + "/" + Long.toHexString(_sequence.incrementAndGet());
    }
   
    Mailbox mailbox = new PassthroughMailbox(address, next, this);
    
    addMailbox(address, mailbox);
    
    return mailbox;
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
