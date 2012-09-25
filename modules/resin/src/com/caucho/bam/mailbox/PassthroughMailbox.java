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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;

/**
 * mailbox for BAM messages waiting to be sent to the Actor.
 */
public class PassthroughMailbox implements Mailbox
{
  private static final Logger log
    = Logger.getLogger(PassthroughMailbox.class.getName());
  
  private final String _address;
  private final Broker _broker;
  private final MessageStream _actorStream;

  public PassthroughMailbox(String address,
                            MessageStream actorStream,
                            Broker broker)
  {
    _address = address;
    
    if (broker == null)
      throw new NullPointerException();
    
    _broker = broker;
    
    if (actorStream == null)
      throw new NullPointerException();
    
    _actorStream = actorStream;
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
  public int getSize()
  {
    return 0;
  }

  @Override
  public boolean isClosed()
  {
    return _actorStream.isClosed();
  }
  
  @Override
  public MessageStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * Sends a message
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    try {
      _actorStream.message(to, from, payload);
    } catch (Throwable e) {
      // Throwable caught because the Mailbox conceptually operates in
      // a new thread and the caller would never receive the exception.
      
      getBroker().messageError(from, to, payload,
                               BamError.create(e));
      
      log.log(Level.WARNING, e.toString(), e);
    }
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
    try {
      _actorStream.messageError(to, from, payload, error);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
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
    try {
      _actorStream.query(id, to, from, payload);
    } catch (Throwable e) {
      // Throwable caught because the Mailbox conceptually operates in
      // a new thread and the caller would never receive the exception.
    
      getBroker().queryError(id, from, to, payload, BamError.create(e));
    
      log.log(Level.WARNING, e.toString(), e);
    }
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
    try {
      _actorStream.queryResult(id, to, from, payload);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
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
    try {
      _actorStream.queryError(id, to, from, payload, error);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public void close()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
