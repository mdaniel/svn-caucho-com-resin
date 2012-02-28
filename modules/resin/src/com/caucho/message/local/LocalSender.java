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
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.amqp.transform.AmqpMessageEncoder;
import com.caucho.message.MessageSender;
import com.caucho.message.broker.BrokerPublisher;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.util.L10N;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * local connection to the message store
 */
public class LocalSender<T> implements MessageSender<T> {
  private static final L10N L = new L10N(LocalSender.class);
  
  private String _address;
  private AmqpMessageEncoder<T> _encoder;
  
  private BrokerPublisher _publisher;
  
  LocalSender(LocalSenderFactory factory)
  {
    _address = factory.getAddress();
    _encoder = (AmqpMessageEncoder) factory.getEncoder();
    
    EnvironmentMessageBroker broker = EnvironmentMessageBroker.getCurrent();
        
    _publisher = broker.createSender(_address);
    
    if (_publisher == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown queue",
                                             _address));
    }
  }
  
  public String getAddress()
  {
    return _address;
  }

  @Override
  public boolean add(T value)
  {
    return offer(value);
  }

  @Override
  public boolean offer(T value)
  {
    return offerMicros(value, 0);
  }

  @Override
  public boolean offer(T value, long timeout, TimeUnit timeUnit)
  {
    return offerMicros(value, timeUnit.toMicros(timeout));
  }
  
  private boolean offerMicros(T value, long timeoutMicros)
  {
    try {
      TempOutputStream tOut = new TempOutputStream();
      WriteStream os = Vfs.openWrite(tOut);
      AmqpStreamWriter sout = new AmqpStreamWriter(os);
      AmqpWriter aout = new AmqpWriter();
      aout.initBase(sout);
      
      String contentType = _encoder.getContentType(value);
      
      if (contentType != null) {
        MessageProperties properties = new MessageProperties();
        
        properties.setContentType(contentType);
        
        properties.write(aout);
      }
    
      _encoder.encode(aout, value);
      
      sout.flush();
      os.flush();
      
      tOut.flush();
      tOut.close();

      long xid = 0;
      
      _publisher.messageComplete(xid, tOut.getHead(), tOut.getLength(), null);
      
      return true;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  @Override
  public void put(T value) throws InterruptedException
  {
    offerMicros(value, 0);
  }

  @Override
  public boolean addAll(Collection<? extends T> arg0)
  {
    return false;
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }
  
  @Override
  public void close()
  {
    
  }

  //
  // receiver side
  //

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
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean containsAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean isEmpty()
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
  public int size()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <X> X[] toArray(X[] arg0)
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
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
