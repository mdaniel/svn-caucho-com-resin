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

package com.caucho.amqp.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.transform.AmqpMessageDecoder;

/**
 * AMQP client
 */
class AmqpClientReceiver<T> implements AmqpReceiver<T> {
  private static final long TIMEOUT_INFINITY = Long.MAX_VALUE / 2;
  
  private final AmqpConnectionImpl _client;
  
  private final String _address;
  private final int _handle;
  
  private final boolean _isAutoAck;
  
  private final AmqpMessageDecoder<T> _decoder;
  
  private long _deliveryCount = -1;
  private int _prefetch;

  private ConcurrentLinkedQueue<T> _valueQueue
    = new ConcurrentLinkedQueue<T>();
  
  AmqpClientReceiver(AmqpConnectionImpl client,
                     AmqpClientReceiverFactory builder,
                     int handle)
  {
    _client = client;
    _address = builder.getAddress();
    _handle = handle;
    
    _isAutoAck = builder.getAckMode();
    _decoder = (AmqpMessageDecoder) builder.getDecoder(); 
    
    _prefetch = builder.getPrefetch();
    
    if (_prefetch > 0) {
      _client.flow(_handle, _deliveryCount, _prefetch);
    }
    
  }
  
  public int getPrefetchQueueSize()
  {
    return _valueQueue.size();
  }
  
  @Override
  public T take()
  {
    return poll(TIMEOUT_INFINITY);
  }
  
  @Override
  public T poll()
  {
    return poll(0);
  }

  @Override
  public T poll(long timeout, TimeUnit unit)
  {
    return poll(unit.toMicros(timeout));
  }
  
  private T poll(long timeoutMicros)
  {
    T value = _valueQueue.poll();
    
    if (value == null) {
      return null;
    }
    
    _client.flow(_handle, _deliveryCount, _prefetch - _valueQueue.size());
    
    if (_isAutoAck) {
      _client.dispositionAccept(_handle);
    }
    
    return value;
  }

  @Override
  public T element()
  {
    return poll();
  }

  @Override
  public T peek()
  {
    return _valueQueue.peek();
  }

  @Override
  public T remove()
  {
    return poll();
  }

  @Override
  public void accepted()
  {
    _client.dispositionAccept(_handle);
  }
  
  @Override
  public void rejected(String errorMessage)
  {
    _client.dispositionReject(_handle, errorMessage);
  }
  
  @Override
  public void released()
  {
    _client.dispositionRelease(_handle);
  }
  
  @Override
  public void modified(boolean isFailed, boolean isUndeliverableHere)
  {
    _client.dispositionModified(_handle, isFailed, isUndeliverableHere);
  }

  void setDeliveryCount(long deliveryCount)
  {
    _deliveryCount = deliveryCount;
  }
  
  /**
   * @param ain
   */
  void receive(AmqpReader ain)
    throws IOException
  {
    _deliveryCount++;
    
    T value = _decoder.decode(ain, null);
    _valueQueue.add(value);
  }
  
  public void close()
  {
    _client.closeReceiver(_handle);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _address + "]";
  }
  
  static class ValueNode {
    private ValueNode _next;
    
    private Object _value;
    private long _count;
  
    ValueNode(Object value)
    {
      _value = value;
    }
    
    public Object getValue()
    {
      return _value;
    }
    
    public ValueNode getNext()
    {
      return _next;
    }
    
    public void setNext(ValueNode next)
    {
      _next = next;
    }
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
   */
  @Override
  public boolean add(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
   */
  @Override
  public int drainTo(Collection arg0)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection, int)
   */
  @Override
  public int drainTo(Collection arg0, int arg1)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
   */
  @Override
  public boolean offer(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
   */
  @Override
  public boolean offer(Object arg0, long arg1, TimeUnit arg2)
    throws InterruptedException
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
   */
  @Override
  public void put(Object arg0) throws InterruptedException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#remainingCapacity()
   */
  @Override
  public int remainingCapacity()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.BlockingQueue#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#clear()
   */
  @Override
  public void clear()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  @Override
  public boolean containsAll(Collection arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#isEmpty()
   */
  @Override
  public boolean isEmpty()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#iterator()
   */
  @Override
  public Iterator iterator()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#size()
   */
  @Override
  public int size()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#toArray()
   */
  @Override
  public Object[] toArray()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see java.util.Collection#toArray(T[])
   */
  @Override
  public Object[] toArray(Object[] arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
