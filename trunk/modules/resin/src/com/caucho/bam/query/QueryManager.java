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

package com.caucho.bam.query;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.ErrorPacketException;
import com.caucho.bam.TimeoutException;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.WeakAlarm;

/**
 * QueryCallbackManager is used to generate query ids and to wait
 * for query callbacks.
 */
public class QueryManager {
  private final String _id;
  
  private final AtomicLong _qId = new AtomicLong();

  private final QueryMap _queryMap = new QueryMap();
  
  private AlarmListener _listener = new TimeoutAlarmListener();
  private Alarm _alarm = new WeakAlarm(_listener);
  
  private long _timeout = 15 * 60 * 1000L;

  public QueryManager(String id)
  {
    _id = id;
  }

  public QueryManager(String id, long seed)
  {
    this(id);
    
    _qId.set(seed);
  }
  
  public boolean isEmpty()
  {
    return _queryMap.isEmpty();
  }
  
  public long getTimeout()
  {
    return _timeout;
  }
  
  public void setTimeout(long timeout)
  {
    _timeout = timeout;
  }

  /**
   * Generates a new unique query identifier.
   */
  public final long nextQueryId()
  {
    return _qId.incrementAndGet();
  }

  /**
   * Adds a query callback to handle a later message.
   *
   * @param id the unique query identifier
   * @param callback the application's callback for the result
   */
  public void addQueryCallback(long id, 
                               QueryCallback callback,
                               long timeout)
  {
    _queryMap.add(id, callback, timeout);

    Alarm alarm = _alarm;

    long expireTime = timeout + CurrentTime.getCurrentTime();

    if (alarm != null 
        && (! alarm.isQueued() 
            || expireTime < alarm.getWakeTime())) {
      alarm.queueAt(expireTime);
    }
  }

  /**
   * Registers a callback future.
   */
  public QueryFuture addQueryFuture(long id, 
                                    String to,
                                    String from,
                                    Serializable payload,
                                    long timeout)
  {
    QueryFutureImpl future
      = new QueryFutureImpl(id, to, from, payload, timeout);
    
    addQueryCallback(id, future, timeout);

    return future;
  }
  
  /**
   * Queries through to a stream.
   */
  public void query(MessageStream stream,
                    String to,
                    String from,
                    Serializable payload,
                    QueryCallback cb,
                    long timeout)
  {
    long id = nextQueryId();

    addQueryCallback(id, cb, timeout);
    
    stream.query(id, to, from, payload);
  }
  
  /**
   * Queries through to a stream.
   */
  public Serializable query(MessageStream stream,
                            String to,
                            String from,
                            Serializable payload,
                            long timeout)
  {
    long id = nextQueryId();
    
    QueryFuture future = addQueryFuture(id, to, from, payload, timeout);
    
    stream.query(id, to, from, payload);
    
    return future.get();
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
                                    BamError error)
  {
    QueryItem item = _queryMap.remove(id);
    
    if (item != null) {
      item.onQueryError(to, from, payload, error);

      return true;
    }
    else
      return false;
  }

  /**
   * 
   */
  void checkTimeout(long now)
  {
    _queryMap.checkTimeout(now);
  }

  public void close()
  {
    Alarm alarm = _alarm;
    _alarm = null;
    
    if (alarm != null)
      alarm.dequeue();
  }

  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  static final class QueryMap {
    private final QueryItem []_entries = new QueryItem[128];
    private final int _mask = _entries.length - 1;

    boolean isEmpty()
    {
      for (QueryItem item : _entries) {
        if (item != null)
          return false;
      }
      
      return true;
    }

    void checkTimeout(long now)
    {
      for (QueryItem item : _entries) {
        QueryItem next;
        
        while (item != null) {
          next = item.getNext();
          
          if (item._expires < now) {
            item = remove(item.getId());
            
            if (item != null) {
              QueryCallback cb = item._callback;
              
              Exception exn = new TimeoutException(item.toString());
              BamError error = BamError.create(exn);
              
              cb.onQueryError(null, null, null, error);
            }
          }
          
          item = next;
        }
      }
    }
    
    void add(long id, QueryCallback callback, long timeout)
    {
      long expires = timeout + CurrentTime.getCurrentTime();
      
      int hash = (int) (id & _mask);
      
      synchronized (_entries) {
        _entries[hash] = new QueryItem(id, callback, expires, _entries[hash]);
      }
    }

    QueryItem remove(long id)
    {
      int hash = (int) (id & _mask);
      
      synchronized (_entries) {
        QueryItem prev = null;
        QueryItem next = null;

        for (QueryItem ptr = _entries[hash];
             ptr != null;
             ptr = next) {
          next = ptr.getNext();
          
          if (id == ptr.getId()) {
            if (prev != null)
              prev.setNext(next);
            else
              _entries[hash] = next;

            return ptr;
          }

          prev = ptr;
          ptr = next;
        }

        return null;
      }
    }
  }

  static final class QueryItem {
    private final long _id;
    private final QueryCallback _callback;
    private final long _expires;

    private QueryItem _next;

    QueryItem(long id, QueryCallback callback, long expires, QueryItem next)
    {
      _id = id;
      _callback = callback;
      _expires = expires;
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
    
    final long getExpires()
    {
      return _expires;
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

  static final class QueryFutureImpl implements QueryCallback, QueryFuture {
    private final long _id;
    private final String _to;
    private final String _from;
    private final Serializable _payload;
    private final long _timeout;

    private volatile Serializable _result;
    private volatile BamError _error;
    private final AtomicBoolean _isResult = new AtomicBoolean();
    private volatile Thread _thread;

    QueryFutureImpl(long id,
                    String to,
                    String from,
                    Serializable payload,
                    long timeout)
    {
      _id = id;
      _to = to;
      _from = from;
      _payload = payload;
      _timeout = timeout;
    }

    public Serializable getResult()
    {
      return _result;
    }

    @Override
    public Serializable get()
      throws TimeoutException, BamException
    {
      if (! waitFor(_timeout)) {
        throw new TimeoutException(this + " query timeout " + _timeout + "ms for " + _payload
                                   + " {to:" + _to + "}");
      }
      else if (getError() != null) {
        ErrorPacketException exn = getError().createException();
        
        if (exn.getSourceException() instanceof RuntimeException)
          throw (RuntimeException) exn.getSourceException();
        else
          throw exn;
      }
      else
        return getResult();
    }

    public BamError getError()
    {
      return _error;
    }

    boolean waitFor(long timeout)
    {
      _thread = Thread.currentThread();
      long now = CurrentTime.getCurrentTimeActual();
      long expires = now + timeout;

      while (! _isResult.get() && CurrentTime.getCurrentTimeActual() <= expires) {
        try {
          Thread.interrupted();
          LockSupport.parkUntil(expires);
        } catch (Exception e) {
        }
      }
      
      _thread = null;

      return _isResult.get();
    }

    @Override
    public void onQueryResult(String fromAddress,
                              String toAddress,
                              Serializable payload)
    {
      _result = payload;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }

    @Override
    public void onQueryError(String fromAddress, String toAddress,
                             Serializable payload, BamError error)
    {
      _error = error;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }
    
    @Override
    public String toString()
    {
      return (getClass().getSimpleName()
              + "[to=" + _to 
              + ",from=" + _from
              + ",payload=" + _payload + "]");
    }
  }
  
  class TimeoutAlarmListener implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        long now = CurrentTime.getCurrentTime();

        checkTimeout(now);
      } finally {
        if (_alarm == alarm && ! isEmpty()) {
          alarm.queue(getTimeout());
        }
      }
    }
    
  }
}
