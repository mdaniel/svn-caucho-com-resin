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

package com.caucho.amp.actor;

import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.mailbox.AmpMailboxBuilder;
import com.caucho.amp.mailbox.AmpMailboxFactory;
import com.caucho.amp.spi.AmpSpi;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpHeaders;
import com.caucho.amp.stream.AmpStream;
import com.caucho.amp.stream.NullEncoder;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.ExpandableArray;
import com.caucho.util.WeakAlarm;

/**
 * Handles the context for an actor, primarily including its
 * query map.
 */
public final class ActorContextImpl extends AmpActorContext 
  implements AmpActor, AlarmListener
{
  private final String _address;
  private final AmpActor _actor;
  private final AmpMailbox _mailbox;
  private final AmpActorRef _from;
  
  private long _timeout = 15 * 60 * 1000L;
  
  private long _qId;

  private final ExpandableArray<QueryItem> _queryMap
    = new ExpandableArray<QueryItem>(QueryItem.class);
  
  private Alarm _alarm = new WeakAlarm(this);

  public ActorContextImpl(String address,
                          AmpActor actorStream, 
                          AmpMailboxFactory mailboxFactory)
  {
    _address = address;
    _actor = actorStream;
    _mailbox = mailboxFactory.createMailbox(this);
    
    _from = new ActorRefImpl(address, _mailbox, this);
    
    _qId = CurrentTime.getCurrentTime() << 16;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  public AmpMailbox getMailbox()
  {
    return _mailbox;
  }
  
  public AmpActorRef getActorRef()
  {
    return _from;
  }
  
  @Override
  public AmpStream getStream()
  {
    return this;
  }
  
  @Override
  public AmpMethodRef getMethod(String methodName, AmpEncoder encoder)
  {
    return _actor.getMethod(methodName, encoder);
  }
  
  public long getQueryTimeout()
  {
    return _timeout;
  }
  
  public void setQueryTimeout(long timeout)
  {
    _timeout = timeout;
  }
  
  //
  // message filters
  //

  @Override
  public final void send(AmpActorRef to,
                         AmpActorRef from,
                         AmpEncoder encoder, 
                         String methodName, 
                         Object... args)
  {
    _actor.send(to, from, encoder, methodName, args);
  }

  @Override
  public void error(AmpActorRef to, 
                    AmpActorRef from,
                    AmpEncoder encoder, 
                    AmpError error)
  {
    _actor.error(to, from, encoder, error);
  }

  @Override
  public void query(long id, 
                    AmpActorRef to, 
                    AmpActorRef from,
                    AmpEncoder encoder, 
                    String methodName, 
                    Object... args)
  {
    _actor.query(id, to, from, encoder, methodName, args);
  }
  
  @Override
  public void queryResult(long id, 
                          AmpActorRef to, 
                          AmpActorRef from,
                          AmpEncoder encoder, 
                          Object result)
  {
    QueryItem queryItem = extractQuery(id);

    if (queryItem != null) {
      queryItem.onQueryResult(to, from, encoder, result);
    }
    else {
      _actor.queryResult(id, to, from, encoder, result);
    }
  }

  @Override
  public void queryError(long id, 
                         AmpActorRef to, 
                         AmpActorRef from,
                         AmpEncoder encoder, 
                         AmpError error)
  {
    QueryItem queryItem = extractQuery(id);

    if (queryItem != null) {
      queryItem.onQueryError(to, from, error);
    }
    else {
      _actor.queryError(id, to, from, encoder, error);
    }
  }
  
  //
  // query/sender methods
  //

  @Override
  public void query(AmpMethodRef methodRef,
                    Object[] args,
                    AmpQueryCallback cb, 
                    long timeout)
  {
    long id = addQueryCallback(cb, timeout);

    methodRef.query(id, _from, args);
  }

  /**
   * Adds a query callback to handle a later message.
   *
   * @param id the unique query identifier
   * @param callback the application's callback for the result
   */
  long addQueryCallback(AmpQueryCallback callback,
                        long timeout)
  {
    long id = _qId++;
    
    QueryItem item = new QueryItem(id, callback, timeout);
    
    _queryMap.add(item);

    Alarm alarm = _alarm;

    long expireTime = timeout + CurrentTime.getCurrentTime();
    
    if (alarm != null 
        && (! alarm.isQueued() 
            || expireTime < alarm.getWakeTime())) {
      alarm.queueAt(expireTime);
    }
    
    return id;
  }
  
  private QueryItem extractQuery(long id)
  {
    QueryItem []queries = _queryMap.getArray();
    int size = _queryMap.getSize();

    for (int i = 0; i < size; i++) {
      QueryItem query = queries[i];
      
      if (query.getId() == id) {
        _queryMap.remove(i);
        
        return query;
      }
    }
    
    return null;
  }

  /**
   * Registers a callback future.
   */
  /*
  QueryFuture addQueryFuture(long id, 
                             String to,
                             String from,
                             Serializable payload,
                             long timeout)
  {
    QueryFutureImpl future
      = new QueryFutureImpl(id, to, from, payload, timeout);

    _queryMap.add(id, future, timeout);

    return future;
  }
  */

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
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }

  static final class QueryItem {
    private final long _id;
    private final AmpQueryCallback _callback;
    private final long _expires;

    QueryItem(long id, AmpQueryCallback callback, long expires)
    {
      _id = id;
      _callback = callback;
      _expires = expires;
    }

    final long getId()
    {
      return _id;
    }
    
    final long getExpires()
    {
      return _expires;
    }

    void onQueryResult(AmpActorRef to, 
                       AmpActorRef from,
                       AmpEncoder encoder,
                       Object value)
    {
      _callback.onQueryResult(to, from, value);
    }

    void onQueryError(AmpActorRef to,
                      AmpActorRef from,
                      AmpError error)
    {
      _callback.onQueryError(to, from, error);
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _id + "," + _callback + "]";
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.util.AlarmListener#handleAlarm(com.caucho.util.Alarm)
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    // TODO Auto-generated method stub
    
  }
}
