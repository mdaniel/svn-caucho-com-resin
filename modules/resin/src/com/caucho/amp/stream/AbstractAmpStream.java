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

package com.caucho.amp.stream;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amp.actor.AmpActorRef;

/**
 * Primary stream handling all message packets.
 *
 * {@link com.caucho.bam.actor.ActorHolder Actors} send packets to the 
 * {@link com.caucho.bam.broker.Broker} for delivery to other Actors.
 *
 * Messages are divided into two groups:
 * <ul>
 * <li>message - unidirectional messages
 * <li>query - RPC call/reply packets
 * </ul>
 */
public class AbstractAmpStream implements AmpStream
{
  private static final Logger log
    = Logger.getLogger(AbstractAmpStream.class.getName());
  
  @Override
  public void send(AmpActorRef to, 
                   AmpActorRef from,
                   AmpEncoder encoder,
                   String methodName,
                   Object ...args)
  {
    if (log.isLoggable(Level.FINER)) { 
      log.finer(this + " send '" + methodName + "' from=" + from + " is ignored");
    }
    
    from.error(to, NullEncoder.ENCODER, new AmpError());
  }

  @Override
  public void query(long id, 
                    AmpActorRef to, 
                    AmpActorRef from,
                    AmpEncoder encoder,
                    String methodName,
                    Object ...args)
  {
    if (log.isLoggable(Level.FINER)) { 
      log.finer(this + " query-result from=" + from + " is ignored");
    }
    
    from.queryError(id, to, NullEncoder.ENCODER, new AmpError());
  }

  @Override
  public void queryResult(long id, 
                          AmpActorRef to,
                          AmpActorRef from,
                          AmpEncoder encoder,
                          Object result)
  {
    if (log.isLoggable(Level.FINER)) { 
      log.finer(this + " query-result from=" + from + " is ignored");
    }
  }

  @Override
  public void queryError(long id,
                         AmpActorRef to,
                         AmpActorRef from,
                         AmpEncoder encoder,
                         AmpError error)
  {
    if (log.isLoggable(Level.FINER)) { 
      log.finer(this + " query-error from=" + from + " is ignored");
    }
  }

  @Override
  public void error(AmpActorRef to,
                    AmpActorRef from,
                    AmpEncoder encoder,
                    AmpError error)
  {
    if (log.isLoggable(Level.FINER)) { 
      log.finer(this + " error from=" + from + " is ignored");
    }
  }
}
