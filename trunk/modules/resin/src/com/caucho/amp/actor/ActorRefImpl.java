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

package com.caucho.amp.actor;

import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;

/**
 * Handles the context for an actor, primarily including its
 * query map.
 */
public final class ActorRefImpl implements AmpActorRef
{
  private final String _address;
  private final AmpMailbox _mailbox;
  private final AmpActorContext _actor;

  public ActorRefImpl(String address,
                      AmpMailbox mailbox,
                      AmpActorContext actor)
  {
    _address = address;
    _mailbox = mailbox;
    _actor = actor;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  public AmpMethodRef getMethod(String methodName, 
                                AmpEncoder encoder)
  {
    return _actor.getMethod(methodName, encoder);
  }

  @Override
  public void send(AmpActorRef from, 
                   AmpEncoder encoder, 
                   String methodName,
                   Object... args)
  {
    getMailbox().send(this, from, encoder, methodName, args);
  }

  @Override
  public void query(long id, 
                    AmpActorRef from, 
                    AmpEncoder encoder,
                    String methodName, 
                    Object... args)
  {
    getMailbox().query(id, this, from, encoder, methodName, args);
  }

  @Override
  public void reply(long id, 
                    AmpActorRef from, 
                    AmpEncoder encoder, 
                    Object result)
  {
    getMailbox().queryResult(id, this, from, encoder, result);
  }

  @Override
  public void queryError(long id, 
                         AmpActorRef from, 
                         AmpEncoder encoder,
                         AmpError error)
  {
    getMailbox().queryError(id, this, from, encoder, error);
  }

  @Override
  public void error(AmpActorRef from, 
                    AmpEncoder encoder, 
                    AmpError error)
  {
  }
  
  private AmpMailbox getMailbox()
  {
    return _mailbox;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
