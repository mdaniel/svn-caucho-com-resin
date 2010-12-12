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

package com.caucho.bam.mailbox;

import java.io.Serializable;

import com.caucho.bam.ActorError;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.ActorStream;

/**
 * mailbox for BAM messages waiting to be sent to the Actor.
 */
public class NonQueuedMailbox implements Mailbox
{
  private final Broker _broker;
  private final ActorStream _actorStream;

  public NonQueuedMailbox(Broker broker,
                          ActorStream actorStream)
  {
    if (broker == null)
      throw new NullPointerException();
    
    _broker = broker;
    
    if (actorStream == null)
      throw new NullPointerException();
    
    _actorStream = actorStream;
  }

  /**
   * Returns the actor's jid
   */
  @Override
  public String getJid()
  {
    return _actorStream.getJid();
  }
  
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public boolean isClosed()
  {
    return _actorStream.isClosed();
  }
  
  @Override
  public ActorStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * Sends a message
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    _actorStream.message(to, from, payload);
  }

  /**
   * Sends a message
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    _actorStream.messageError(to, from, payload, error);
  }

  /**
   * Query an entity
   */
  @Override
  public void query(long id,
                       String to,
                       String from,
                       Serializable query)
  {
    _actorStream.query(id, to, from, query);
  }

  /**
   * Query an entity
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable value)
  {
    _actorStream.queryResult(id, to, from, value);
  }

  /**
   * Query an entity
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable query,
                         ActorError error)
  {
    _actorStream.queryError(id, to, from, query, error);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
