/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import com.caucho.bam.query.QueryActorStreamFilter;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryFuture;
import com.caucho.bam.query.QueryManager;
import com.caucho.bam.stream.AbstractActorStream;
import com.caucho.bam.stream.ActorStream;
import com.caucho.bam.stream.NullActorStream;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's JID as the "from" parameter.
 */
public class SimpleActorSender implements ActorSender {
  private ActorStream _actorStream;
  private Broker _broker;
  private String _clientJid;

  private final QueryManager _queryManager = new QueryManager();
  
  private long _timeout = 120000L;

  public SimpleActorSender(String jid, Broker broker)
  {
    this((ActorStream) null, broker);
    
    _clientJid = jid;
  }
  
  public SimpleActorSender(ActorStream next)
  {
    this(next, next.getBroker());
  }
  
  public SimpleActorSender(ActorStream next, Broker broker)
  {
    if (next == null)
      next = new DefaultActorStream();
    
    _actorStream = new QueryActorStreamFilter(next, _queryManager);
    _broker = broker;
    
    _clientJid  = next.getJid();
  }
  
  public SimpleActorSender(ActorStream next,
                           ManagedBroker broker,
                           String uid, 
                           String resource)
  {
    this(next, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(next.getJid(),
                                             _actorStream,
                                             broker, 
                                             1);

    _actorStream = broker.createClient(mailbox, uid, resource);
    _clientJid = _actorStream.getJid();
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
    this((ActorStream) null, broker);
    
    Mailbox mailbox = new MultiworkerMailbox(null,
                                             _actorStream,
                                             broker, 
                                             1);

    _actorStream = broker.createClient(mailbox, uid, resource);
    _clientJid = _actorStream.getJid();
  }

  /**
   * Returns the Actor's jid used for all "from" parameters.
   */
  @Override
  public String getJid()
  {
    return getActorStream().getJid();
  }

  //
  // streams
  //

  public ActorStream getActorStream()
  {
    return _actorStream;
    
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
   * Sends a unidirectional message to an {@link com.caucho.bam.actor.Actor},
   * addressed by the Actor's JID.
   *
   * @param to the target actor's JID
   * @param payload the message payload
   */
  @Override
  public void message(String to, Serializable payload)
  {
    ActorStream broker = getBroker();

    if (broker == null)
      throw new IllegalStateException(this + " can't send a message because the link is closed.");

    broker.message(to, getJid(), payload);
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
   * @param to the target actor's JID
   * @param payload the query payload
   */
  @Override
  public Serializable query(String to,
                            Serializable payload)
  {
    Broker broker = getBroker();

    if (broker == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.nextQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, _timeout);

    broker.query(id, to, getJid(), payload);

    return future.get();
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
   * @param to the target actor's JID
   * @param payload the query payload
   */
  @Override
  public Serializable query(String to,
                            Serializable payload,
                            long timeout)
  {
    ActorStream linkStream = getBroker();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.nextQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, timeout);

    linkStream.query(id, to, getJid(), payload);

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
   * @param to the target actor's JID
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  @Override
  public void query(String to,
                    Serializable payload,
                    QueryCallback callback)
  {
    ActorStream linkStream = getBroker();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.nextQueryId();
    
    _queryManager.addQueryCallback(id, callback);

    linkStream.query(id, to, getJid(), payload);
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
  public void close()
  {
    // _queryMap.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getActorStream() + "]";
  }
  
  class DefaultActorStream extends AbstractActorStream {
    @Override
    public String getJid()
    {
      return _clientJid;
    }
    
    @Override
    public Broker getBroker()
    {
      return SimpleActorSender.this.getBroker();
    }
  }
}
