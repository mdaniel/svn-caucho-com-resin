/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import com.caucho.bam.stream.ActorStream;
import com.caucho.util.Alarm;

/**
 * Broker is the hub which routes messages to actors.
 */
abstract public class AbstractManagedBroker 
  extends AbstractBroker
  implements ManagedBroker
{
  private final AtomicLong _sequence = new AtomicLong(Alarm.getCurrentTime());
  
  /**
   * Adds a mailbox.
   */
  @Override
  public void addMailbox(Mailbox mailbox)
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
  public Agent createAgent(ActorStream actorStream)
  {
    return createAgent(actorStream, MailboxType.DEFAULT);
  }
    
  /**
   * Creates an agent
   */
  // @Override
  public Agent createAgent(ActorStream actorStream,
                           MailboxType mailboxType)
  {
    Mailbox mailbox = createMailbox(actorStream.getJid(),
                                    actorStream, 
                                    mailboxType);
    
    Agent agent = new AbstractAgent(actorStream.getJid(),
                                    mailbox,
                                    this);
    
    addMailbox(mailbox);
    
    return agent;
  }
  
  protected Mailbox createMailbox(ActorStream actorStream,
                                  MailboxType mailboxType)
  {
    return createMailbox(actorStream.getJid(), actorStream, mailboxType);
  }
  
  protected Mailbox createMailbox(String jid,
                                  ActorStream actorStream,
                                  MailboxType mailboxType)
  {
    switch (mailboxType) {
    case NON_QUEUED:
      return new PassthroughMailbox(jid, actorStream, this);
      
    default:
      return new MultiworkerMailbox(jid, actorStream, this, 5);
    }
  }
  
  @Override
  public Mailbox createClient(Mailbox next,
                              String uid,
                              String resource)
  {
    String jid = null;
    
    if (uid == null)
      uid = Long.toHexString(_sequence.incrementAndGet());
    
    if (uid.indexOf('@') < 0)
      uid = uid + '@' + getJid();
    
    if (resource != null) {
      jid = uid + "/" + resource;
      
      Mailbox mailbox = getMailbox(jid);
      
      if (mailbox != null)
        jid = uid + "/" + resource + "-" + Long.toHexString(_sequence.incrementAndGet());
    }
    else {
      jid = uid + "/" + Long.toHexString(_sequence.incrementAndGet());
    }
   
    Mailbox mailbox = new PassthroughMailbox(jid, next, this);
    
    addMailbox(mailbox);
    
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
