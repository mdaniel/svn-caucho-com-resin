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

package com.caucho.v5.nautilus.common;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.thread.WorkerThreadPoolBase;
import com.caucho.v5.nautilus.ReceiverConfig;
import com.caucho.v5.nautilus.ReceiverListener;

/**
 * basic message receiver includes a linked queue.
 */
public class ReceiverQueueImpl<T> extends ReceiverQueueBase<T> {
  private static final Logger log
    = Logger.getLogger(ReceiverQueueImpl.class.getName());
  
  private final String _address;
  
  private final ReceiverListener<T> _listener;
  
  private final LinkedBlockingQueue<QueueEntry<T>> _queue
    = new LinkedBlockingQueue<QueueEntry<T>>();
  
  private final ReceiverQueueCredit _credit = new ReceiverQueueCredit();
  
  private final MessageListenerWorker _worker;
  
  private long _lastMessageId;
  
  private boolean _isClosed;
  
  protected ReceiverQueueImpl(String address,
                                 ReceiverConfig<T> config,
                                 ReceiverListener<T> listener)
  {
    Objects.requireNonNull(address);
    Objects.requireNonNull(listener);
    
    _address = address;
    
    _listener = listener;
    
    _credit.setPrefetch(config.getPrefetch());
    
    _worker = new MessageListenerWorker();
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  public int getPrefetch()
  {
    return _credit.getPrefetch();
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  protected void initCredits()
  {
    _credit.init();
  }

  //
  // runtime values
  //
  
  @Override
  public long getLastMessageId()
  {
    return _lastMessageId;
  }
  
  @Override
  public void onBuild()
  {
    wake();
  }

  @Override
  protected T pollMicros(long timeoutMicros)
  {
    boolean isFlow = false;
    
    try {
      QueueEntry<T> entry = _queue.poll(timeoutMicros, TimeUnit.MICROSECONDS);
      
      if (entry == null) {
        return null;
      }
      
      isFlow = true;
      
      _credit.receiveClient();
      
      T value = entry.getValue();
      
      _lastMessageId = entry.getMessageId();
      
      clientReceiveAck(_lastMessageId);

      return value;
    } catch (InterruptedException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } finally {
      if (isFlow && _credit.isFlowRequired()) {
        updateCredit();
      }
    }
  }
  
  protected void clientReceiveAck(long messageId)
  {
    
  }
  
  protected void updateCredit()
  {
    if (_credit.updateCredit()) {
      long credit = _credit.getCreditSequence();
  
      // _credit.updateCredit(credit);
  
      // updateFlow(credit, endpointSequence);
      updateFlow(credit);
    }
  }
  
  protected void updateFlow(long creditSequence)
  {
    
  }
  
  public void receiveEntry(T value)
  {
    long mid = _credit.receiveEndpoint();
    
    _queue.add(new QueueEntry<T>(mid, value));
    
    wake();
  }
  
  protected void wake()
  {
    MessageListenerWorker worker = _worker;
    
    if (worker != null) {
      worker.wake();
    }
  }
  
  private void receiveMessages()
  {
    while (receiveMessage()) {
    }
  }
  
  private boolean receiveMessage()
  {
    boolean isFlow = false;
      
    try {
      QueueEntry<T> entry = _queue.poll();
        
      if (entry == null) {
        return false;
      }
        
      isFlow = true;
        
      _credit.receiveClient();
        
      long mid = entry.getMessageId();
      T value = entry.getValue();
        
      _lastMessageId = mid;

      ReceiverListener<T> listener = _listener;
      
      clientReceiveAck(mid);
        
      listener.onMessage(mid, value, this);
        
      return true;
    } finally {
      if (isFlow && _credit.isFlowRequired()) {
        _credit.updateCredit();
          
        updateFlow(_credit.getCreditSequence());
      }
    }
  }
  
  protected void clearQueue()
  {
    _queue.clear();
  }
  
  public void close()
  {
    sendClose();
    
    _isClosed = true;
  }
  
  protected void sendClose()
  {
    
  }
 
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
  
  private static class QueueEntry<T> {
    private long _mid;
    private T _value;
    
    QueueEntry(long mid, T value)
    {
      _mid = mid;
      _value = value;
    }
    
    public long getMessageId()
    {
      return _mid;
    }
    
    public T getValue()
    {
      return _value;
    }
  }
  
  private class MessageListenerWorker extends WorkerThreadPoolBase {
    @Override
    public long runTask()
    {
      receiveMessages();
      
      return 0;
    }
    
  }
}
