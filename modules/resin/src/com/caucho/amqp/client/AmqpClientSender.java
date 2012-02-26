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

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.AmqpSender;
import com.caucho.amqp.io.AmqpAbstractComposite;
import com.caucho.amqp.io.AmqpAbstractPacket;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.amqp.transform.AmqpMessageEncoder;
import com.caucho.amqp.transform.AmqpStringEncoder;
import com.caucho.util.L10N;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.QSocketWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
class AmqpClientSender<T> implements AmqpSender<T> {
  private static final long TIMEOUT_INFINITY = Long.MAX_VALUE / 2;
  
  private static final Logger log
    = Logger.getLogger(AmqpClientSender.class.getName());
  
  private AmqpConnectionImpl _client;
  
  private String _address;
  private int _handle;
  
  private AmqpMessageEncoder<T> _encoder;
  
  AmqpClientSender(AmqpConnectionImpl client,
                   AmqpClientSenderFactory factory,
                   int handle)
  {
    _client = client;
    _address = factory.getAddress();
    _handle = handle;
    _encoder = (AmqpMessageEncoder) factory.getEncoder();
  }

  @Override
  public boolean add(T value)
  {
    return offer(value);
  }

  @Override
  public boolean addAll(Collection<? extends T> values)
  {
    for (T value : values) {
      if (! add(value)) {
        return false;
      }
    }
    
    return true;
  }

  @Override
  public boolean offer(T value)
  {
    return offer(value, TIMEOUT_INFINITY);
  }

  @Override
  public void put(T value) throws InterruptedException
  {
    offer(value, TIMEOUT_INFINITY);
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }

  @Override
  public boolean offer(T value, long timeout, TimeUnit timeUnit)
  {
    return offer(value, timeUnit.toMicros(timeout));
  }

  private boolean offer(T value, long timeoutMicros)
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
      
      _client.transmit(_handle, tOut.getInputStream());
      
      return true;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  public void close()
  {
    _client.closeSender(_handle);
  }

  //
  // consumer operations are unsupported
  //

  @Override
  public boolean contains(Object value)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public int drainTo(Collection values)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public int drainTo(Collection arg0, int arg1)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T poll(long timeout, TimeUnit unit) throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean remove(Object arg0)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T take() throws InterruptedException
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T element()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T peek()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T poll()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public T remove()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean containsAll(Collection<?> arg0)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean isEmpty()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public Iterator iterator()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean removeAll(Collection arg0)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean retainAll(Collection arg0)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public int size()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public Object[] toArray(Object[] arg0)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _address + "]";
  }
}
