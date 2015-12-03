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

package com.caucho.amp.broker;

import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpStream;

/**
 * AmpRouter routes messages to mailboxes.
 */
public interface AmpBroker
{
  /**
   * Returns the mailbox to the router itself.
   */
  public AmpActorRef getBrokerActor();
  
  /**
   * Returns a mailbox for the given address, 
   * or null if the mailbox does not exist.
   * 
   * @param address the address of the mailbox
   * 
   * @return the mailbox with the given address or null
   */
  public AmpActorRef getActorRef(String address);
  
  //
  // gateway methods to send directly.
  //
  
  
  public void send(String to, String from, AmpEncoder encoder,
                   String methodName, Object ...args);
  
  public void query(long id, String to, String from, AmpEncoder encoder,
                    String methodName, Object ...args);
  
  public void reply(long id, 
                    String to, 
                    String from, 
                    AmpEncoder encoder,
                    Object result);
  
  /**
   * Adds a mailbox (optional operation).
   */
  public AmpActorRef addMailbox(String address, AmpMailbox mailbox);
  
  /**
   * Removes a mailbox (optional operation).
   */
  public void removeMailbox(String address, AmpMailbox mailbox);
  
  /**
   * Close the broker.
   */
  public void close();
}
