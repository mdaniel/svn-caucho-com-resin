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

import java.util.concurrent.ConcurrentHashMap;

import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.MessageStream;

/**
 * The abstract implementation of an {@link com.caucho.bam.stream.MessageStream}
 * returns query errors for RPC packets, and ignores unknown packets
 * for messages and presence announcement.
 *
 * Most developers will use {@link com.caucho.bam.actor.SkeletonActorFilter}
 * or {@link com.caucho.bam.actor.SimpleActor} because those classes use
 * introspection with {@link com.caucho.bam.Message @Message} annotations
 * to simplify Actor development.
 */
public class HashMapBroker extends AbstractManagedBroker
{
  private final String _address;
  
  private final ConcurrentHashMap<String,Mailbox> _mailboxMap
    = new ConcurrentHashMap<String,Mailbox>();

  public HashMapBroker(String address)
  {
    _address = address;
  }
  
  /**
   * Returns the address for the broker itself.
   */
  @Override
  public String getAddress()
  {
    return _address;
  }
  
  /**
   * Returns the actor stream for the given address.
   */
  @Override
  public Mailbox getMailbox(String address)
  {
    if (address == null)
      return null;

    return _mailboxMap.get(address);
  }
  
  /**
   * Adds a new actor to the broker.
   */
  @Override
  public void addMailbox(String address, Mailbox mailbox)
  {
    if (address == null)
      throw new NullPointerException(String.valueOf(mailbox) + " has a null address");
    
    _mailboxMap.put(address, mailbox);
  }

  /**
   * Removes an actor from the broker.
   */
  public void removeMailbox(MessageStream actor)
  {
    String address = actor.getAddress();
    
    _mailboxMap.remove(address);
  }
}
