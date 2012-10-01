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

package com.caucho.bam.actor;

import java.io.Serializable;

import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.stream.MessageStream;

/**
 * ActorProxy is a convenience API for sending messages to a specific Actors,
 * which always using the actor's address as the "from" parameter.
 */
public interface ActorProxy {
  /**
   * Returns the proxy's address used for all "from" parameters.
   */
  public String getAddress();

  /**
   * Returns the target actor's address used for all "to" parameters.
   */
  public String getTo();

  //
  // message handling
  //

  /**
   * Sends a unidirectional message to the target {@link com.caucho.bam.actor.ActorHolder},
   *
   * @param payload the message payload
   */
  public void message(Serializable payload);

  //
  // query handling
  //

  /**
   * Sends a query information call (get) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param payload the query payload
   */
  public Serializable query(Serializable payload);

  /**
   * Sends a query information call (get) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param payload the query payload
   * @param timeout time spent waiting for the query to return
   */
  public Serializable query(Serializable payload,
                            long timeout);

  /**
   * Sends a query information call (get) to an actor,
   * providing a callback to receive the result or error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's address
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void query(Serializable payload,
                    QueryCallback callback);

  /**
   * Registers a callback {@link com.caucho.bam.stream.MessageStream} with the client
   */
  public void setClientStream(MessageStream clientStream);

  /**
   * Returns the registered callback {@link com.caucho.bam.stream.MessageStream}.
   */
  public MessageStream getClientStream();

  /**
   * Returns the stream to this client.
   */
  public MessageStream getActorStream();

  /**
   * The ActorStream to the link.
   */
  public MessageStream getLinkStream();

  /**
   * Sets the ActorStream to the link.
   */
  public void setLinkStream(MessageStream linkStream);
  
  //
  // lifecycle
  //

  /**
   * Returns true if the proxy is closed
   */
  public boolean isClosed();

  /**
   * Closes the proxy
   */
  public void close();
}
