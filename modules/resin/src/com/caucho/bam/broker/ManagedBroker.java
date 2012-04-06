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

import com.caucho.bam.actor.Agent;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MailboxType;
import com.caucho.bam.stream.MessageStream;


/**
 * Broker is the hub which routes messages to mailboxes.
 */
public interface ManagedBroker extends Broker
{
  /**
   * Adds a mailbox
   */
  public void addMailbox(String address, Mailbox mailbox);
  
  /**
   * Removes a mailbox
   */
  public void removeMailbox(Mailbox mailbox);
  
  /**
   * Creates an agent
   */
  public Agent createAgent(MessageStream actorStream);
    
  /**
   * Creates an agent
   */
  public Agent createAgent(MessageStream actorStream,
                           MailboxType mailboxType);

  /**
   * @param actorStream
   * @param uid
   * @param resource
   * @return
   */
  public Mailbox createClient(Mailbox next,
                              String uid,
                              String resource);
}
