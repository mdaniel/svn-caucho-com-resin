/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus.common;

import com.caucho.nautilus.ReceiverMode;
import com.caucho.nautilus.ReceiverController;
import com.caucho.nautilus.ReceiverListener;
import com.caucho.nautilus.DecoderMessage;
import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.ReceiverQueue;
import com.caucho.nautilus.SettleMode;
import com.caucho.nautilus.SettleTime;
import com.caucho.nautilus.encode.NautilusDecoder;

/**
 * local connection to the message store
 */
abstract public class ReceiverBuilderBase<M>
  implements ReceiverBuilder<M>
{
  private String _address;
  private int _prefetch;
  private SettleMode _settleMode = SettleMode.ALWAYS;
  private SettleTime _settleTime = SettleTime.QUEUE_REMOVE;
  private ReceiverMode _distributionMode;
  private ReceiverListener<M> _listener;
  private DecoderMessage<?> _decoder = NautilusDecoder.DECODER;

  @Override
  public ReceiverBuilder<M> address(String address)
  {
    _address = address;
    
    return this;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  /*
  @Override
  public MessageReceiverFactory setListener(MessageConsumer<?> listener)
  {
    _listener = listener;
    
    return this;
  }
  */
  
  @Override
  public ReceiverListener<M> getListener()
  {
    return _listener;
  }
  
  @Override
  public ReceiverMode getReceiverMode()
  {
    return _distributionMode;
  }
  
  @Override
  public ReceiverBuilder<M> subscribe()
  {
    _distributionMode = ReceiverMode.SUBSCRIBE;
    
    return this;
  }
  
  @Override
  public ReceiverBuilder<M> consume()
  {
    _distributionMode = ReceiverMode.CONSUME;
    
    return this;
  }

  @Override
  public int getPrefetch()
  {
    return _prefetch;
  }

  @Override
  public ReceiverBuilder<M> prefetch(int prefetch)
  {
    _prefetch = prefetch;

    return this;
  }

  @Override
  public ReceiverBuilder<M> setSettleMode(SettleMode settleMode)
  {
    _settleMode = settleMode;
    
    return this;
  }

  @Override
  public SettleMode getSettleMode()
  {
    return _settleMode;
  }

  @Override
  public ReceiverBuilder<M> setSettleTime(SettleTime settleTime)
  {
    _settleTime = settleTime;
    
    return this;
  }

  @Override
  public SettleTime getSettleTime()
  {
    return _settleTime;
  }
  
  /*
  @Override
  public MessageReceiverFactory setMessageDecoder(MessageDecoder<?> decoder)
  {
    _decoder = decoder;
    
    return this;
  }
  */
  
  @Override
  public DecoderMessage<M> getMessageDecoder()
  {
    return (DecoderMessage) _decoder;
  }
  
  @Override
  public ReceiverBuilder<M> setListener(ReceiverListener listener)
  {
    _listener = listener;
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public ReceiverBuilder<M> setMessageDecoder(DecoderMessage<M> decoder)
  {
    // TODO Auto-generated method stub
    return this;
  }

  public ReceiverQueue<M> build()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public ReceiverController build(ReceiverListener<M> receiver)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override 
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "]";
  }
}
