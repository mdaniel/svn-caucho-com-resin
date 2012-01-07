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

import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.query.QueryActorFilter;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryFuture;
import com.caucho.bam.query.QueryManager;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.WeakAlarm;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's address as the "from" parameter.
 */
public class SimpleActorSender implements ActorSender {
  private Actor _actor;
  private Broker _broker;
  private String _clientAddress;

  private final QueryManager _queryManager = new QueryManager();
  
  private long _timeout = 120000L;

  public SimpleActorSender(String address, Broker broker)
  {
    this((Actor) null, broker);
    
    _clientAddress = address;
  }
  
  public SimpleActorSender(Actor next)
  {
    this(next, next.getBroker());
  }
  
  public SimpleActorSender(Actor next, Broker broker)
  {
    if (next == null)
      next = new DefaultActor();
    
    _actor = new QueryActorFilter(next, _queryManager);
    _broker = broker;
    
    _clientAddress  = next.getAddress();
  }
  
  public SimpleActorSender(Actor next,
                           ManagedBroker broker,
                           String uid, 
                           String resource)
  {
    this(next, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(next.getAddress(),
                                             _actor,
                                             broker, 
                                             1);

    MessageStream stream = broker.createClient(mailbox, uid, resource);
    _clientAddress = stream.getAddress();
  }
  
  public SimpleActorSender(ManagedBroker broker,
                           String uid)
  {
    this(broker, uid, null);
  }
  
  public SimpleActorSender(ManagedBroker broker,
                           String uid, 
                           String resource)
  {
    this((Actor) null, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(null,
                                             _actor,
                                             broker, 
                                             1);

    MessageStream stream = broker.createClient(mailbox, uid, resource);
    _clientAddress = stream.getAddress();
  }

  /**
   * Returns the Actor's address used for all "from" parameters.
   */
  @Override
  public String getAddress()
  {
    if (_actor != null)
      return _actor.getAddress();
    else
      return _clientAddress;
  }

  //
  // streams
  //

  public Actor getActor()
  {
    return _actor;
  }
  /**
   * The underlying, low-level stream to the link
   */
  @Override
  public Broker getBroker()
  {
    return _broker;
  }
  
  public void setBroker(Broker broker)
  {
    _broker = broker;
  }
  
  protected ManagedBroker getManagedBroker()
  {
    return (ManagedBroker) getBroker();
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
    MessageStream broker = getBroker();

    if (broker == null)
      throw new IllegalStateException(this + " can't send a message because the link is closed.");

    broker.message(to, getAddress(), payload);
  }

  //
  // query handling
  //

  @Override
  public long nextQueryId()
  {
    return _queryManager.nextQueryId();
  }
  
  
  @Override
  public QueryManager getQueryManager()
  {
    return _queryManager;
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
    return query(to, payload, _timeout);
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
                            Serializable payload,
                            long timeout)
  {
    MessageStream linkStream = getBroker();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.nextQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getAddress(), payload, timeout);

    linkStream.query(id, to, getAddress(), payload);
    
    return future.get();
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
    query(to, payload, callback, _timeout);
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
  public void query(String to,
                    Serializable payload,
                    QueryCallback callback,
                    long timeout)
  {
    MessageStream linkStream = getBroker();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.nextQueryId();
    
    _queryManager.addQueryCallback(id, callback, timeout);
    
    linkStream.query(id, to, getAddress(), payload);
  }

  /**
   * Returns true if the client is closed
   */
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Closes the client
   */
  @Override
  public void close()
  {
    _queryManager.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getActor() + "]";
  }
  
  class DefaultActor extends AbstractActor {
    @Override
    public String getAddress()
    {
      return _clientAddress;
    }
    
    @Override
    public Broker getBroker()
    {
      return SimpleActorSender.this.getBroker();
    }
  }
}
