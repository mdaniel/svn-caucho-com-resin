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

package com.caucho.bam.stream;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;

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
public interface MessageStream
{
  /**
   * Returns the address of the {@link com.caucho.bam.actor.ActorHolder} at the end
   * of the stream.
   */
  public String getAddress();

  /**
   * Tests if the stream is closed.
   */
  public boolean isClosed();
  
  /**
   * Returns the owning broker
   */
  public Broker getBroker();
  
  //
  // messages
  //
  
  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.actor.ActorHolder},
   * addressed by the Actor's address.
   * 
   * @param to the target actor's address
   * @param from the source actor's address
   * @param payload the message payload
   */
  public void message(String to, String from, Serializable payload);
  
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
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error);

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
                    String to,
                    String from,
                    Serializable payload);

  /**
   * Sends a query response for a query
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param payload the result payload
   */
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload);
  
  /**
   * Sends a query error from a failed query.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's address
   * @param from the service actor's address
   * @param payload the query payload
   * @param error additional error information
   */
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error);
}
