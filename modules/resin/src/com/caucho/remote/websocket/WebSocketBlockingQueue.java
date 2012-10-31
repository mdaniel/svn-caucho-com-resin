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

package com.caucho.remote.websocket;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.env.actor.ValueActorQueue;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketEncoder;

/**
 * WebSocketBlockingQueue blocks and encodes.
 */
public class WebSocketBlockingQueue<T> implements BlockingQueue<T>
{
  private final WebSocketContext _ws;
  private final WebSocketEncoder<T> _encoder;
  private final ValueActorQueue<T> _queue;
  
  public WebSocketBlockingQueue(WebSocketContext ws,
                                WebSocketEncoder<T> encoder,
                                int capacity)
  {
    if (ws == null)
      throw new NullPointerException();
    
    if (encoder == null)
      throw new NullPointerException();
    
    _ws = ws;
    _encoder = encoder;
    
    _queue = new ValueActorQueue(capacity, 
                                 new WebSocketWriterActor(_ws, _encoder));
  }

  @Override
  public boolean isEmpty()
  {
    return _queue.isEmpty();
  }

  @Override
  public int size()
  {
    return _queue.getSize();
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }

  @Override
  public boolean offer(T value)
  {
    return _queue.offer(value, false);
  }

  @Override
  public boolean offer(T value, long timeout, TimeUnit unit)
    throws InterruptedException
  {
      return _queue.offer(value,  true);
  }

  @Override
  public boolean add(T value)
  {
    return _queue.offer(value,  true);
  }

  @Override
  public void put(T value) throws InterruptedException
  {
    _queue.offer(value,  true);
  }

  @Override
  public boolean addAll(Collection<? extends T> collection)
  {
    for (T value : collection) {
      if (! add(value)) {
        return false;
      }
    }
    
    return true;
  }
  
  //
  // getter operations are unsupported
  //

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T element()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T poll()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T remove()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean containsAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Iterator<T> iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean removeAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean retainAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <T> T[] toArray(T[] arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean contains(Object arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public int drainTo(Collection<? super T> arg0, int arg1)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T poll(long arg0, TimeUnit arg1) throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean remove(Object arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public T take() throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  static class WebSocketWriterActor<T> extends AbstractActorProcessor<T> {
    private final WebSocketContext _ws;
    private final WebSocketEncoder<T> _encoder;
    
    WebSocketWriterActor(WebSocketContext ws, WebSocketEncoder<T> encoder)
    {
      _ws = ws;
      _encoder = encoder;
    }
    
    @Override
    public String getThreadName()
    {
      return toString();
    }

    @Override
    public void process(T value) throws Exception
    {
      _encoder.encode(_ws, value);
    }

    @Override
    public void onProcessComplete() throws Exception
    {
      _encoder.flush(_ws);
    }
  }
}
