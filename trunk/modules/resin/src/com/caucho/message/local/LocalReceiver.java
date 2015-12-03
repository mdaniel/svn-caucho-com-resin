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

package com.caucho.message.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.marshal.AmqpMessageDecoder;
import com.caucho.message.DistributionMode;
import com.caucho.message.MessageDecoder;
import com.caucho.message.MessageReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.message.common.AbstractMessageReceiver;
import com.caucho.util.L10N;

/**
 * local connection to the message store
 */
public class LocalReceiver<T> extends AbstractMessageReceiver<T> {
  private static final L10N L = new L10N(LocalReceiver.class);
  
  private static final Logger log
    = Logger.getLogger(LocalReceiver.class.getName());
  
  private String _address;
  
  private int _prefetch;
  private int _linkCredit;
  
  private MessageDecoder<T> _decoder;
  
  private LinkedBlockingQueue<QueueEntry> _queue
    = new LinkedBlockingQueue<QueueEntry>();
  
  private BrokerReceiver _sub;
  
  LocalReceiver(LocalReceiverFactory factory)
  {
    _address = factory.getAddress();
    _prefetch = factory.getPrefetch();

    _decoder = (MessageDecoder) factory.getMessageDecoder();
    
    EnvironmentMessageBroker broker = EnvironmentMessageBroker.getCurrent();
    
    ReceiverMessageHandler handler = new LocalMessageHandler();
    
    DistributionMode distMode = factory.getDistributionMode();
    
    Map<String,Object> nodeProperties = null;
    _sub = broker.createReceiver(_address, distMode, nodeProperties, handler);
    
    if (_sub == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown queue",
                                             _address));
    }
    
    _linkCredit = _prefetch;
    if (_prefetch > 0) {
      _sub.flow(-1, _prefetch);
    }
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  protected T pollMicros(long timeoutMicros)
  {
    boolean isFlow = false;
    
    try {
      QueueEntry entry = _queue.poll(timeoutMicros, TimeUnit.MICROSECONDS);
      
      if (entry == null) {
        return null;
      }
      
      isFlow = true;
      
      InputStream is = entry.getInputStream();
      
      T value = _decoder.decode(is);

      return value;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } catch (InterruptedException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } finally {
      if (isFlow) {
        _linkCredit--;
        
        int delta = _prefetch - _linkCredit;
        
        if (_linkCredit <= (_prefetch >> 2)) {
          _sub.flow(-1, _prefetch);
          _linkCredit = _prefetch;
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
  
  class LocalMessageHandler implements ReceiverMessageHandler {
    @Override
    public void onMessage(long messageId, 
                          InputStream bodyIs, 
                          long contentLength)
        throws IOException
    {
      _queue.add(new QueueEntry(messageId, bodyIs));
    }
  }
  
  static class QueueEntry {
    private long _mid;
    private InputStream _is;
    
    QueueEntry(long mid, InputStream is)
    {
      _mid = mid;
      _is = is;
    }
    
    public InputStream getInputStream()
    {
      return _is;
    }
  }
}
