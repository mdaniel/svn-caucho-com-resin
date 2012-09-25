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

package com.caucho.bam.mailbox;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.BamLargePayload;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;

/**
 * Mailbox which filters large messages to a separate queue, so large messages
 * don't block small messages.
 */
public class DualSizeMailbox implements Mailbox
{
  private final String _address;
  private final Broker _broker;
  
  private final Mailbox _largeMailbox;
  private final Mailbox _smallMailbox;

  public DualSizeMailbox(String address,
                         Broker broker,
                         Mailbox smallMailbox,
                         Mailbox largeMailbox)
  {
    _address = address;
    
    if (broker == null)
      throw new NullPointerException();
    
    _broker = broker;
    
    if (largeMailbox == null)
      throw new NullPointerException();
    
    _largeMailbox = largeMailbox;
    
    if (smallMailbox == null)
      throw new NullPointerException();
    
    _smallMailbox = smallMailbox;
  }
  
  public int getSize()
  {
    return getSmallQueueSize() + getLargeQueueSize();
  }
  
  public int getSmallQueueSize()
  {
    return _smallMailbox.getSize();
  }
  
  public int getLargeQueueSize()
  {
    return _largeMailbox.getSize();
  }

  /**
   * Returns the actor's address
   */
  @Override
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public boolean isClosed()
  {
    return _smallMailbox.isClosed();
  }
  
  @Override
  public MessageStream getActorStream()
  {
    return _smallMailbox.getActorStream();
  }

  /**
   * Sends a message
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    if (payload instanceof BamLargePayload)
      _largeMailbox.message(to, from, payload);
    else
      _smallMailbox.message(to, from, payload);
  }

  /**
   * Sends a message
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    if (payload instanceof BamLargePayload)
      _largeMailbox.messageError(to, from, payload, error);
    else
      _smallMailbox.messageError(to, from, payload, error);
  }

  /**
   * Query an entity
   */
  @Override
  public void query(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    if (payload instanceof BamLargePayload)
      _largeMailbox.query(id, to, from, payload);
    else
      _smallMailbox.query(id, to, from, payload);
  }

  /**
   * Query an entity
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (payload instanceof BamLargePayload)
      _largeMailbox.queryResult(id, to, from, payload);
    else
      _smallMailbox.queryResult(id, to, from, payload);
  }

  /**
   * Query an entity
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    if (payload instanceof BamLargePayload)
      _largeMailbox.queryError(id, to, from, payload, error);
    else
      _smallMailbox.queryError(id, to, from, payload, error);
  }
  
  @Override
  public void close()
  {
    _smallMailbox.close();
    _largeMailbox.close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _smallMailbox + "," + _largeMailbox + "]";
  }
}
