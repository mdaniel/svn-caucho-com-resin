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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.marshal.AmqpMessageDecoder;
import com.caucho.message.DistributionMode;
import com.caucho.message.SettleMode;
import com.caucho.message.common.AbstractMessageReceiver;

/**
 * AMQP client
 */
class AmqpClientReceiver<T> extends AbstractMessageReceiver<T>
  implements AmqpReceiver<T> 
{
  private static final long TIMEOUT_INFINITY = Long.MAX_VALUE / 2;
  
  private final AmqpClientConnectionImpl _client;
  
  private final String _address;
  private final AmqpClientReceiverLink _link;
  
  private final SettleMode _settleMode;
  
  private int _prefetch;
  private final AmqpMessageDecoder<T> _decoder;
  
  private final Map<String,Object> _attachProperties;
  private final Map<String,Object> _sourceProperties;
  private final Map<String,Object> _targetProperties;

  private int _linkCredit;

  private LinkedBlockingQueue<ValueNode<T>> _valueQueue
    = new LinkedBlockingQueue<ValueNode<T>>();

  private long _lastMessageId;
  
  AmqpClientReceiver(AmqpClientConnectionImpl client,
                     AmqpSession session,
                     AmqpClientReceiverFactory builder)
  {
    _client = client;
    _address = builder.getAddress();
    
    _settleMode = builder.getSettleMode();
    _decoder = (AmqpMessageDecoder) builder.getDecoder();
    
    if (builder.getAttachProperties() != null)
      _attachProperties = new HashMap<String,Object>(builder.getAttachProperties());
    else
      _attachProperties = null;
    
    if (builder.getSourceProperties() != null)
      _sourceProperties = new HashMap<String,Object>(builder.getSourceProperties());
    else
      _sourceProperties = null;
    
    if (builder.getTargetProperties() != null)
      _targetProperties = new HashMap<String,Object>(builder.getTargetProperties());
    else
      _targetProperties = null;

    _prefetch = builder.getPrefetch();
    
    _link = new AmqpClientReceiverLink("client-" + _address, _address, this);
    
    DistributionMode distMode = builder.getDistributionMode();
    session.addReceiverLink(_link, distMode, _settleMode);

    if (_prefetch > 0) {
      _linkCredit = _prefetch;
      _link.updatePrefetch(_prefetch);
    }
  }
  
  Map<String,Object> getAttachProperties()
  {
    return _attachProperties;
  }
  
  Map<String,Object> getSourceProperties()
  {
    return _sourceProperties;
  }
  
  Map<String,Object> getTargetProperties()
  {
    return _targetProperties;
  }
  
  public int getPrefetchQueueSize()
  {
    return _valueQueue.size();
  }
  
  @Override
  protected T pollMicros(long timeoutMicros)
  {
    ValueNode<T> value = _valueQueue.poll();
    
    if (value == null) {
      if (_linkCredit > 0 || _prefetch > 0) {
        return null;
      }
      
      if (_prefetch == 0) {
        _link.updatePrefetch(1);
      }
      try {
        value = _valueQueue.poll(1000, TimeUnit.MILLISECONDS);
        
        if (value == null) {
          return null;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
        return null;
      } finally {
        if (_prefetch == 0) {
          // drain
          _link.updatePrefetch(0);
        }
      }
    }

    _link.updateTake();
    
    _lastMessageId = value.getMessageId();
    
    return value.getValue();
  }
  
  @Override
  public long getLastMessageId()
  {
    return _lastMessageId;
  }

  @Override
  public void accepted(long mid)
  {
    _link.accepted(mid);
  }
  
  @Override
  public void rejected(long mid, String errorMessage)
  {
    _link.rejected(mid, errorMessage);
  }
  
  @Override
  public void released(long mid)
  {
    _link.released(mid);
  }
  
  @Override
  public void modified(long mid,
                       boolean isFailed, 
                       boolean isUndeliverableHere)
  {
    _link.modified(mid, isFailed, isUndeliverableHere);
  }
  
  /**
   * @param ain
   */
  void receive(long mid, AmqpReader ain)
    throws IOException
  {
    T value = _decoder.decode(ain, null);
    
    _valueQueue.add(new ValueNode<T>(value, mid));
  }
  
  public void close()
  {
    _link.detach();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _link + "]";
  }
  
  static class ValueNode<T> {
    private final T _value;
    private final long _mid;
  
    ValueNode(T value, long mid)
    {
      _value = value;
      _mid = mid;
    }
    
    public T getValue()
    {
      return _value;
    }
    
    public long getMessageId()
    {
      return _mid;
    }
  }
}
