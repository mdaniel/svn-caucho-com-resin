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

package com.caucho.message.common;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.message.MessageReceiverFactory;
import com.caucho.message.MessageReceiverListener;

/**
 * basic message receiver includes a linked queue.
 */
public class BasicMessageReceiver<T> extends AbstractMessageReceiver<T> {
  private static final Logger log
    = Logger.getLogger(BasicMessageReceiver.class.getName());
  
  private final String _address;
  
  private final MessageReceiverListener<T> _listener;
  
  private final LinkedBlockingQueue<QueueEntry<T>> _queue
    = new LinkedBlockingQueue<QueueEntry<T>>();
  
  private final MessageReceiverCredit _credit = new MessageReceiverCredit();
  
  private final MessageListenerWorker _worker;
  
  private long _lastMessageId;

  protected BasicMessageReceiver(MessageReceiverFactory factory)
  {
    _address = factory.getAddress();
    
    _listener = (MessageReceiverListener) factory.getListener();
    
    _credit.setPrefetch(factory.getPrefetch());
    
    if (_listener != null) {
      _worker = new MessageListenerWorker();
    }
    else {
      _worker = null;
    }
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  public int getPrefetch()
  {
    return _credit.getPrefetch();
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

      return value;
    } catch (InterruptedException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } finally {
      if (isFlow && _credit.isFlowRequired()) {
        int credit = _credit.getCredit();
        long endpointSequence = _credit.getEndpointSequence();
        _credit.updateCredit(credit);
        
        updateFlow(credit, endpointSequence);
      }
    }
  }
  
  protected void updateFlow(int credit, long endpointSequence)
  {
    
  }
  
  public void updateCredit(int credit)
  {
    _credit.updateCredit(credit);
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

      MessageReceiverListener<T> listener = _listener;
        
      listener.onMessage(mid, value, this);
        
      return true;
    } finally {
      if (isFlow && _credit.isFlowRequired()) {
        int credit = _credit.getCredit();
        long endpointSequence = _credit.getEndpointSequence();
        _credit.updateCredit(credit);
          
        updateFlow(credit, endpointSequence);
      }
    }
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
  
  private class MessageListenerWorker extends AbstractTaskWorker {
    @Override
    public long runTask()
    {
      receiveMessages();
      
      return 0;
    }
    
  }
}
