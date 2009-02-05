/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.bam;

import java.io.Serializable;

import com.caucho.util.*;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's JID as the "from" parameter.
 */
public interface ActorClient {
  /**
   * Returns the Actor's jid used for all "from" parameters.
   */
  public String getJid();

  //
  // lifecycle
  //
  
  /**
   * Returns true if the client is closed
   */
  public boolean isClosed();

  /**
   * Closes the client
   */
  public void close();
  
  //
  // handlers
  //

  /**
   * Registers a callback {@link com.caucho.bam.ActorStream} with the client
   */
  public void setActorStream(ActorStream handler);

  /**
   * Returns the registered callback {@link com.caucho.bam.ActorStream}.
   */
  public ActorStream getActorStream();

  //
  // message handling
  //

  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.Actor},
   * addressed by the Actor's JID.
   * 
   * @param to the target actor's JID
   * @param payload the message payload
   */
  public void message(String to, Serializable payload);

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
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable queryGet(String to,
			       Serializable payload);

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
   * @param to the target actor's JID
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void queryGet(String to,
		       Serializable payload,
		       QueryCallback callback);

  /**
   * Sends a query update call (set) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>querySet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable querySet(String to,
			       Serializable payload);

  /**
   * Sends a query update call (set) to an actor,
   * providing a callback to receive the result or error.
   *
   * The target actor of a <code>querySet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void querySet(String to,
		       Serializable payload,
		       QueryCallback callback);

  //
  // presence handling
  //

  /**
   * Announces a subscribing actor's presence, like an IM user logging on, or
   * a subscriber logging on.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presence(String to,
		       Serializable payload);
  
  /**
   * Announces a subscribing actor's logout, like an IM user logging out,
   * or subscriber logging out.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnavailable(String to,
				  Serializable payload);
  
  /**
   * Presence probing packet from a publisher actor to a
   * subscriber actor, used to query subscriber capabilities.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceProbe(String to,
			    Serializable payload);
  
  /**
   * A subscription request from a subscriber to a publisher.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribe(String to,
				Serializable payload);

  /**
   * A subscription acceptance from a publisher to a subscriber.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribed(String to,
				 Serializable payload);
  
  /**
   * A unsubscription request from a subscriber to a publisher.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribe(String to,
				  Serializable payload);

  /**
   * A unsubscription acceptance from a publisher to a subscriber.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribed(String to,
				   Serializable payload);

  /**
   * A presence error message.
   *
   * @param to the originator actor's JID
   * @param payload the presence payload
   * @param error the error information
   */
  public void presenceError(String to,
			    Serializable payload,
			    ActorError error);

  //
  // callbacks and low-level routines
  //

  /**
   * Returns the underlying ActorStream to the broker.
   */
  public ActorStream getBrokerStream();

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public boolean onQueryResult(long id,
			       String to,
			       String from,
			       Serializable payload);

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public boolean onQueryError(long id,
			      String to,
			      String from,
			      Serializable payload,
			      ActorError error);
}
