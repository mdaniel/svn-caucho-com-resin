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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * Abstract bam connection
 */
abstract public class AbstractBamConnection implements BamConnection
{
  private static final Logger log
    = Logger.getLogger(AbstractBamConnection.class.getName());

  private BamStream _agentStream;

  private Map<Long,QueryItem> _queryMap
    = Collections.synchronizedMap(new HashMap<Long,QueryItem>());
    
  private final AtomicLong _qId = new AtomicLong();

  protected AbstractBamConnection()
  {
  }
  
  //
  // handlers
  //

  /**
   * The agent stream is the stream which receives messages
   * sent to the BamConnection.
   */
  public void setAgentStream(BamStream agentStream)
  {
    _agentStream = agentStream;

    if (agentStream instanceof SimpleBamConnectionStream) {
      ((SimpleBamConnectionStream) agentStream).setBamConnection(this);
    }
  }

  /**
   * The agent stream is the stream which receives messages
   * sent to the BamConnection.
   */
  public BamStream getAgentStream()
  {
    return _agentStream;
  }

  //
  // bam messages
  //

  /**
   * Sends a message to a given jid
   */
  public void message(String to, Serializable value)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.message(to, null, value);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presence(null, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presence(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presence(to, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceUnavailable(null, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceUnavailable(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceUnavailable(to, null, data);
  }

  /**
   * Sends a presence probe packet to the server
   */
  public void presenceProbe(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceProbe(to, null, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceSubscribe(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceSubscribe(to, null, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceSubscribed(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceSubscribed(to, null, data);
  }

  /**
   * Sends a presence subscribe packet to the server
   */
  public void presenceUnsubscribe(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceUnsubscribe(to, null, data);
  }

  /**
   * Sends a presence subscribed packet to the server
   */
  public void presenceUnsubscribed(String to, Serializable data)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceUnsubscribed(to, null, data);
  }

  /**
   * Sends a presence packet to the server
   */
  public void presenceError(String to, Serializable data, BamError error)
  {
    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.presenceError(to, null, data, error);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable queryGet(String to,
			       Serializable query)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    queryGet(to, query, callback);

    if (! callback.waitFor())
      throw new BamTimeoutException(this + " queryGet timeout to=" + to + " query=" + query);
    else if (callback.getError() != null)
      throw callback.getError().createException();
    else
      return callback.getResult();
  }

  /**
   * Sends a query-get packet to the server
   */
  public void queryGet(String to,
		       Serializable value,
		       BamQueryCallback callback)
  {
    long id = _qId.getAndIncrement();
      
    _queryMap.put(id, new QueryItem(id, callback));

    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");

    if (getJid() == null)
      Thread.dumpStack();
    
    stream.queryGet(id, to, getJid(), value);
  }

  /**
   * Sends a query-set packet to the server
   */
  public Serializable querySet(String to,
			       Serializable query)
  {
    WaitQueryCallback callback = new WaitQueryCallback();

    querySet(to, query, callback);


    if (! callback.waitFor())
      throw new RuntimeException(this + " queryGet timeout to=" + to + " query=" + query);
    else if (callback.getError() != null)
      throw callback.getError().createException();
    else
      return callback.getResult();
  }

  /**
   * Sends a query-set packet to the server
   */
  public void querySet(String to,
		       Serializable value,
		       BamQueryCallback callback)
  {
    long id = _qId.getAndIncrement();
      
    _queryMap.put(id, new QueryItem(id, callback));

    BamStream stream = getBrokerStream();

    if (stream == null)
      throw new IllegalStateException("connection is closed");
    
    stream.querySet(id, to, getJid(), value);
  }

  //
  // callbacks
  //

  /**
   * Callback for the response
   */
  public void onQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null)
      item.onQueryResult(to, from, value);
  }

  /**
   * Callback for the response
   */
  public void onQueryError(long id,
		    String to,
		    String from,
		    Serializable value,
		    BamError error)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null)
      item.onQueryError(to, from, value, error);
  }

  //
  // lifecycle (close)
  //

  /**
   * Closes the connection
   */
  public void close()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getBrokerStream() + "]";
  }

  static class QueryItem {
    private final long _id;
    private final BamQueryCallback _callback;

    QueryItem(long id, BamQueryCallback callback)
    {
      _id = id;
      _callback = callback;
    }

    void onQueryResult(String to, String from, Serializable value)
    {
      if (_callback != null)
	_callback.onQueryResult(to, from, value);
    }

    void onQueryError(String to,
		      String from,
		      Serializable value,
		      BamError error)
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

  static class WaitQueryCallback implements BamQueryCallback {
    private final Semaphore _resultSemaphore = new Semaphore(0);
    
    private volatile Serializable _result;
    private volatile BamError _error;

    public Serializable getResult()
    {
      return _result;
    }
    
    public BamError getError()
    {
      return _error;
    }

    boolean waitFor()
    {
      try {
	return _resultSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
	log.log(Level.FINEST, e.toString(), e);
	
	return false;
      }
    }
    
    public void onQueryResult(String fromJid, String toJid,
			      Serializable value)
    {
      _result = value;

      _resultSemaphore.release();
    }
  
    public void onQueryError(String fromJid, String toJid,
			     Serializable value, BamError error)
    {
      _error = error;

      _resultSemaphore.release();
    }
  }
}
