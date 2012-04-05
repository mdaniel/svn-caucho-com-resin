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

package com.caucho.amp.mailbox;

import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpStream;

/**
 * Mailbox for an actor
 */
public class SimpleAmpMailbox implements AmpMailbox
{
  private final AmpActorContext _actor;
  
  public SimpleAmpMailbox(AmpActorContext actor)
  {
    _actor = actor;
  }
  
  /**
   * Returns the delegated actor stream for the actor itself.
   */
  @Override
  public AmpStream getActorStream()
  {
    return _actor.getStream();
  }

  @Override
  public void send(final AmpActorRef to, 
                   final AmpActorRef from,
                   final AmpEncoder encoder, 
                   final String methodName, 
                   final Object... args)
  {
    _actor.runAs(new Runnable() {
      public void run() {
        getActorStream().send(to, from, encoder, methodName, args);
      }
    });
  }

  @Override
  public void query(final long id, 
                    final AmpActorRef to, 
                    final AmpActorRef from,
                    final AmpEncoder encoder, 
                    final String methodName, 
                    final Object... args)
  {
    _actor.runAs(new Runnable() {
      public void run() {
        getActorStream().query(id, to, from, encoder,
                               methodName, args);
      }
    });
  }

  @Override
  public void queryResult(final long id, 
                          final AmpActorRef to, 
                          final AmpActorRef from,
                          final AmpEncoder encoder, 
                          final Object result)
  {
    _actor.runAs(new Runnable() {
      public void run() {
        getActorStream().queryResult(id, to, from, encoder, result);
      }
    });
  }

  @Override
  public void queryError(final long id, 
                         final AmpActorRef to, 
                         final AmpActorRef from, 
                         final AmpEncoder encoder, 
                         final AmpError error)
  {
    _actor.runAs(new Runnable() {
      public void run() {
        getActorStream().queryError(id, to, from, encoder, error);
      }
    });
  }

  @Override
  public void error(final AmpActorRef to, 
                    final AmpActorRef from,
                    final AmpEncoder encoder, 
                    final AmpError error)
  {
    _actor.runAs(new Runnable() {
      public void run() {
        getActorStream().error(to, from, encoder, error);
      }
    });
  }

  /**
   * Closes the mailbox
   */
  @Override
  public void close()
  {
    
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _actor + "]";
  }
}
