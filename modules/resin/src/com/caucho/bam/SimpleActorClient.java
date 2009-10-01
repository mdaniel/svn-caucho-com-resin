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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import com.caucho.util.*;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's JID as the "from" parameter.
 */
public class SimpleActorClient implements ActorClient {
  private String _jid;
  
  private ActorStream _actorStream;
  private ActorStream _brokerStream;

  private final QueryMap _queryMap = new QueryMap();
    
  private final AtomicLong _qId = new AtomicLong();

  protected SimpleActorClient()
  {
  }
  
  /**
   * Returns the Actor's jid used for all "from" parameters.
   */
  public String getJid()
  {
    return _jid;
  }
  
  //
  // streams
  //

  /**
   * Registers a callback {@link com.caucho.bam.ActorStream} with the client
   */
  public void setActorStream(ActorStream actorStream)
  {
    if (actorStream instanceof SimpleActorStream) {
      ((SimpleActorStream) actorStream).setActorClient(this);
    }
	
    _actorStream = actorStream;
  }

  /**
   * Returns the registered callback {@link com.caucho.bam.ActorStream}.
   */
  public ActorStream getActorStream()
  {
    return _actorStream;
  }
  
  /**
   * Returns the underlying, low-level stream to the broker
   */
  public ActorStream getBrokerStream()
  {
    return _brokerStream;
  }

  public void setBrokerStream(ActorStream brokerStream)
  {
    if (_actorStream instanceof SimpleActorStream)
      ((SimpleActorStream) _actorStream).setBrokerStream(brokerStream);

    _brokerStream = brokerStream;
  }

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
  public void message(String to, Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send a message because the client is closed.");

    stream.message(to, getJid(), payload);
  }

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
			       Serializable payload)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    queryGet(to, payload, callback);

    if (! callback.waitFor()) {
      throw new TimeoutException(this + " queryGet timeout " + payload
				 + " {to:" + to + "}");
    }
    else if (callback.getError() != null)
      throw callback.getError().createException();
    else
      return callback.getResult();
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
  public void queryGet(String to,
		       Serializable payload,
		       QueryCallback callback)
  {
    long id = _qId.incrementAndGet();
      
    _queryMap.add(id, callback);

    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send a queryGet because the client is closed.");

    stream.queryGet(id, to, getJid(), payload);
  }

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
			       Serializable payload)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    querySet(to, payload, callback);

    if (! callback.waitFor()) {
      throw new TimeoutException(this + " querySet timeout " + payload
				 + " {to:" + to + "}");
    }
    else if (callback.getError() != null)
      throw callback.getError().createException();
    else
      return callback.getResult();
  }


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
		       QueryCallback callback)
  {
    long id = _qId.incrementAndGet();
      
    _queryMap.add(id, callback);

    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send a querySet because the client is closed.");

    stream.querySet(id, to, getJid(), payload);
  }

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
		       Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presence because the client is closed.");

    stream.presence(to, getJid(), payload);
  }
  
  /**
   * Announces a subscribing actor's logout, like an IM user logging out,
   * or subscriber logging out.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnavailable(String to,
				  Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceUnavailable because the client is closed.");

    stream.presenceUnavailable(to, getJid(), payload);
  }
  
  /**
   * Presence probing packet from a publisher actor to a
   * subscriber actor, used to query subscriber capabilities.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceProbe(String to,
			    Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceProbe because the client is closed.");

    stream.presenceProbe(to, getJid(), payload);
  }
  
  /**
   * A subscription request from a subscriber to a publisher.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribe(String to,
				Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceSubscribe because the client is closed.");

    stream.presenceSubscribe(to, getJid(), payload);
  }

  /**
   * A subscription acceptance from a publisher to a subscriber.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceSubscribed(String to,
				 Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceSubscribed because the client is closed.");

    stream.presenceSubscribed(to, getJid(), payload);
  }
  
  /**
   * A unsubscription request from a subscriber to a publisher.
   *
   * @param to the publisher actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribe(String to,
				  Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceUnsubscribe because the client is closed.");

    stream.presenceUnsubscribe(to, getJid(), payload);
  }

  /**
   * A unsubscription acceptance from a publisher to a subscriber.
   *
   * @param to the subscriber actor's JID
   * @param payload the presence payload
   */
  public void presenceUnsubscribed(String to,
				   Serializable payload)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceUnsubscribed because the client is closed.");

    stream.presenceUnsubscribed(to, getJid(), payload);
  }

  /**
   * A presence error message.
   *
   * @param to the originator actor's JID
   * @param payload the presence payload
   * @param error the error information
   */
  public void presenceError(String to,
			    Serializable payload,
			    ActorError error)
  {
    ActorStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException(this + " can't send presenceError because the client is closed.");

    stream.presenceError(to, getJid(), payload, error);
  }

  //
  // callbacks and low-level routines
  //

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public final boolean onQueryResult(long id,
				     String to,
				     String from,
				     Serializable payload)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null) {
      item.onQueryResult(to, from, payload);
      
      return true;
    }
    else
      return false;
  }

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public final boolean onQueryError(long id,
				    String to,
				    String from,
				    Serializable payload,
				    ActorError error)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null) {
      item.onQueryError(to, from, payload, error);
      
      return true;
    }
    else
      return false;
  }

  //
  // lifecycle
  //
  
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
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }

  static final class QueryMap {
    private final QueryItem []_entries = new QueryItem[128];
    private final int _mask = _entries.length - 1;
    
    void add(long id, QueryCallback callback)
    {
      int hash = (int) (id & _mask);

      synchronized (_entries) {
	_entries[hash] = new QueryItem(id, callback, _entries[hash]);
      }
    }

    QueryItem remove(long id)
    {
      int hash = (int) (id & _mask);

      synchronized (_entries) {
	QueryItem prev = null;

	for (QueryItem ptr = _entries[hash];
	     ptr != null;
	     ptr = ptr.getNext()) {
	  if (id == ptr.getId()) {
	    if (prev != null)
	      prev.setNext(ptr.getNext());
	    else
	      _entries[hash] = ptr.getNext();

	    return ptr;
	  }

	  prev = ptr;
	}

	return null;
      }
    }
  }

  static final class QueryItem {
    private final long _id;
    private final QueryCallback _callback;

    private QueryItem _next;

    QueryItem(long id, QueryCallback callback, QueryItem next)
    {
      _id = id;
      _callback = callback;
      _next = next;
    }

    final long getId()
    {
      return _id;
    }

    final QueryItem getNext()
    {
      return _next;
    }

    final void setNext(QueryItem next)
    {
      _next = next;
    }

    void onQueryResult(String to, String from, Serializable value)
    {
      if (_callback != null)
	_callback.onQueryResult(to, from, value);
    }

    void onQueryError(String to,
		      String from,
		      Serializable value,
		      ActorError error)
    {
      if (_callback != null)
	_callback.onQueryError(to, from, value, error);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _id + "," + _callback + "]";
    }
  }

  static final class WaitQueryCallback implements QueryCallback {
    private volatile Serializable _result;
    private volatile ActorError _error;
    private final AtomicBoolean _isResult = new AtomicBoolean();
    private volatile Thread _thread;

    public Serializable getResult()
    {
      return _result;
    }
    
    public ActorError getError()
    {
      return _error;
    }

    boolean waitFor()
    {
      _thread = Thread.currentThread();
      long now = Alarm.getCurrentTimeActual();
      long expires = now + 10000L;

      while (! _isResult.get() && Alarm.getCurrentTimeActual() < expires) {
        try {
          Thread.interrupted();
          LockSupport.parkUntil(expires);
        } catch (Exception e) {
        }
      }

      _thread = null;

      return _isResult.get();
    }
    
    public void onQueryResult(String fromJid, String toJid,
			      Serializable payload)
    {
      _result = payload;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }
  
    public void onQueryError(String fromJid, String toJid,
			     Serializable payload, ActorError error)
    {
      _error = error;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }
  }
}
