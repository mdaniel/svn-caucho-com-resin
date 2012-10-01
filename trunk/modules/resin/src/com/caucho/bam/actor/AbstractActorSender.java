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

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryFuture;
import com.caucho.bam.query.QueryManager;

/**
 * ActorSender is a convenience API for sending messages to other Actors,
 * which always using the actor's address as the "from" parameter and manages
 * query ids.
 */
abstract public class AbstractActorSender implements ActorSender {
  private final QueryManager _queryManager;
  private long _timeout = 120000;
  
  protected AbstractActorSender(String id)
  {
    _queryManager = new QueryManager(id);
  }
  
  protected AbstractActorSender()
  {
    _queryManager = new QueryManager(toString());
  }

  /**
   * The default query timeout.
   */
  public long getTimeout()
  {
    return _timeout;
  }
  
  /**
   * The default query timeout.
   */
  public void setTimeout(long timeout)
  {
    _timeout = timeout;
  }

  //
  // lifecycle
  //

  /**
   * Returns true if the sender is closed
   */
  @Override
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Closes the sender.
   */
  @Override
  public void close()
  {
  }

  /**
   * The underlying broker.
   */
  abstract public Broker getBroker();
  
  @Override
  public QueryManager getQueryManager()
  {
    return _queryManager;
  }
  
  //
  // message handling
  //

  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.actor.ActorHolder},
   * addressed by the Actor's address.
   *
   * @param to the target actor's address
   * @param payload the message payload
   */
  @Override
  public void message(String to, Serializable payload)
  {
    getBroker().message(to, getAddress(), payload);
  }

  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.actor.ActorHolder},
   * addressed by the Actor's address.
   *
   * @param to the target actor's address
   * @param payload the message payload
   */
  @Override
  public void message(BamActorRef to, Serializable payload)
  {
    to.message(getAddress(), payload);
  }

  //
  // query handling
  //

  /**
   * Returns the next query identifier.
   */
  @Override
  public long nextQueryId()
  {
    return getQueryManager().nextQueryId();
  }
  
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
   * @param to the target actor's address
   * @param payload the query payload
   */
  @Override
  public Serializable query(String to,
                            Serializable payload)
  {
    return query(to, payload, getTimeout());
  }

  /**
   * Sends a query information call to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>query</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's address
   * @param payload the query payload
   * @param timeout time spent waiting for the query to return
   */
  @Override
  public Serializable query(String to,
                            Serializable payload,
                            long timeout)
  {
    return queryFuture(to, payload, timeout).get();
  }

  /**
   * Sends a query information call to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>query</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's address
   * @param payload the query payload
   * @param timeout time spent waiting for the query to return
   */
  public QueryFuture queryFuture(String to,
                                 Serializable payload,
                                 long timeout)
  {
    long qId = getQueryManager().nextQueryId();
    
    QueryFuture future
      = getQueryManager().addQueryFuture(qId, to, getAddress(), payload, timeout);
    
    try {
      getBroker().query(qId, to, getAddress(), payload);
    } catch (RuntimeException e) {
      getQueryManager().onQueryError(qId, getAddress(), to, payload,
                                     BamError.create(e));
    }
                      
    return future;
  }

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
  @Override
  public void query(String to,
                    Serializable payload,
                    QueryCallback callback)
  {
    query(to, payload, callback, getTimeout());
  }

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
  @Override
  public void query(String to,
                    Serializable payload,
                    QueryCallback callback,
                    long timeout)
  {
    long qId = getQueryManager().nextQueryId();
    
    getQueryManager().addQueryCallback(qId, callback, timeout);

    try {
      getBroker().query(qId, to, getAddress(), payload);
    } catch (RuntimeException e) {
      getQueryManager().onQueryError(qId, getAddress(), to, payload,
                                     BamError.create(e));
    }
  }

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
  @Override
  public void query(BamActorRef to,
                    Serializable payload,
                    QueryCallback callback)
  {
    query(to, payload, callback, getTimeout());
  }

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
  @Override
  public void query(BamActorRef to,
                    Serializable payload,
                    QueryCallback callback,
                    long timeout)
  {
    long qId = getQueryManager().nextQueryId();
    
    getQueryManager().addQueryCallback(qId, callback, timeout);

    try {
      to.query(qId, getAddress(), payload);
    } catch (RuntimeException e) {
      getQueryManager().onQueryError(qId, getAddress(), to.getAddress(),
                                     payload,
                                     BamError.create(e));
    }
  }
}
