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

import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.MessageStream;
import com.caucho.cloud.bam.BamQueueFullHandler;


/**
 * Broker is the hub which routes messages to mailboxes.
 */
public interface Broker extends MessageStream
{
  /**
   * Returns the mailbox to the broker itself.
   */
  public Mailbox getBrokerMailbox();
  
  public BamQueueFullHandler getQueueFullHandler();
  
  /**
   * Returns a mailbox for the given address, or null if the mailbox does not exist.
   * 
   * @param address the address of the mailbox
   * 
   * @return the mailbox with the given address or null
   */
  public Mailbox getMailbox(String address);
  
  /**
   * Adds a mailbox (optional operation).
   */
  public void addMailbox(String address, Mailbox mailbox);
  
  /**
   * Removes a mailbox (optional operation).
   */
  public void removeMailbox(Mailbox mailbox);
  
  /**
   * Close the Broker
   */
  public void close();

}
