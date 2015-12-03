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

import com.caucho.amp.actor.AmpActorRef;

/**
 * Primary stream handling all messages.
 *
 * Messages are divided into two groups:
 * <ul>
 * <li>message - unidirectional messages
 * <li>query - RPC call/reply packets
 * </ul>
 */
public interface AmpStream
{
  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.actor.ActorHolder},
   * addressed by the Actor's address.
   * 
   * @param to the target actor's address
   * @param from the source actor's address
   * @param payload the message payload
   */
  public void send(AmpActorRef to, 
                   AmpActorRef from,
                   AmpEncoder encoder,
                   String methodName,
                   Object ...args);
  
  /**
   * Sends a message error to an {@link com.caucho.bam.actor.ActorHolder},
   * addressed by the Actor's address.  Actor protocols may choose to send
   * error messages if a message fails for some reason.
   *
   * In general, Actors should not rely on the delivery of error messages.
   * If an error return is required, use an RPC query instead.
   * 
   * @param to the target actor's address
   * @param from the source actor's address
   * @param payload the message payload
   * @param error the message error
   */
  public void error(AmpActorRef to,
                    AmpActorRef from,
                    AmpEncoder encoder,
                    AmpError error);

  //
  // queries (iq)
  //
  
  /**
   * Sends a query/RPCinformation call
   *
   * The receiver of a <code>query</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The stream MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's address
   * @param from the client actor's address
   * @param payload the query payload
   */
  public void query(long id,
                    AmpActorRef to,
                    AmpActorRef from,
                    AmpEncoder encoder,
                    String methodName,
                    Object ...args);

  /**
   * Sends a query response for a query
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param payload the result payload
   */
  public void queryResult(long id,
                    AmpActorRef to,
                    AmpActorRef from,
                    AmpEncoder encoder,
                    Object result);
  
  /**
   * Sends a query error from a failed query.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param methodName the name of the called method
   * @param error additional error information
   */
  public void queryError(long id,
                         AmpActorRef to,
                         AmpActorRef from,
                         AmpEncoder encoder,
                         AmpError error);
}
