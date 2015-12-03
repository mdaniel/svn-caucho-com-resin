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
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpConnection;
import com.caucho.amqp.AmqpException;
import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.common.AmqpConnectionHandler;
import com.caucho.amqp.common.AmqpLink;
import com.caucho.amqp.common.AmqpLinkFactory;
import com.caucho.amqp.common.AmqpReceiverLink;
import com.caucho.amqp.common.AmqpSenderLink;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.env.thread.ThreadPool;
import com.caucho.message.DistributionMode;
import com.caucho.message.SettleMode;
import com.caucho.vfs.QSocketWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
public class AmqpClientConnectionImpl implements AmqpConnection, AmqpLinkFactory
{
  private static final Logger log
    = Logger.getLogger(AmqpClientConnectionImpl.class.getName());
  
  private String _hostname;
  private int _port;
  
  private QSocketWrapper _s;
  private ReadStream _is;
  private WriteStream _os;
  
  private AmqpConnectionHandler _handler;
  private AmqpSession _session;
  
  private AmqpClientFrameReader _reader;
  
  private final AtomicInteger _linkId = new AtomicInteger();
  
  private final AtomicBoolean _isClosed = new AtomicBoolean();
  private boolean _isDisconnected;
  
  public AmqpClientConnectionImpl()
  {
  }
  
  public AmqpClientConnectionImpl(String hostname, int port)
  {
    _hostname = hostname;
    _port = port;
  }
  
  public void setHostname(String hostname)
  {
    _hostname = hostname;
  }
  
  public void setPort(int port)
  {
    _port = port;
  }

  public boolean isDisconnected()
  {
    return _isDisconnected;
  }
  
  public void connect()
  {
    boolean isValid = false;
    
    try {
      connectSocket();
      
      _handler = new AmqpConnectionHandler(this, _is, _os);
      
      _handler.getWriter().writeVersion(0);
      _handler.getWriter().writeOpen();
      
      _session = _handler.beginSession();
      
      _handler.getWriter().flush();
      
      _handler.getReader().readVersion();
      
      _handler.getReader().readOpen();
      
      _reader = new AmqpClientFrameReader(this, _handler.getReader());
      
      ThreadPool.getCurrent().schedule(_reader);
      
      isValid = true;
    } catch (IOException e) {
      throw new AmqpException(e);
    } finally {
      if (! isValid) {
        disconnect();
      }
    }
  }
  
  public AmqpClientSenderFactory createSenderFactory()
  {
    return new AmqpClientSenderFactory(this);
  }
  
  @SuppressWarnings("unchecked")
  public AmqpClientSender<?> createSender(String address)
  {
    return (AmqpClientSender) createSenderFactory().setAddress(address).build();
  }
  
  AmqpClientSender<?> buildSender(AmqpClientSenderFactory factory)
  {
    return new AmqpClientSender(this, _session, factory);
  }

  @Override
  public AmqpClientReceiverFactory createReceiverFactory()
  {
    return new AmqpClientReceiverFactory(this);
  }
  
  @Override
  public AmqpReceiver<?> createReceiver(String address)
  {
    AmqpClientReceiverFactory factory = createReceiverFactory();
    factory.setAddress(address);
    
    return factory.build();
  }
  
  AmqpClientReceiver<?> buildReceiver(AmqpClientReceiverFactory factory)
  {
    return new AmqpClientReceiver(this, _session, factory);
  }

  void closeSender(AmqpClientSenderLink link)
  {
    _handler.closeSender(link);
  }
  
  void closeReceiver(AmqpClientSenderLink link)
  {
    closeSender(link);
  }

  @Override
  public AmqpReceiverLink createReceiverLink(String name, 
                                             String address,
                                             Map<String,Object> nodeProperties)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public AmqpSenderLink createSenderLink(String name, 
                                         String address,
                                         DistributionMode distMode,
                                         SettleMode settleMode,
                                         Map<String,Object> nodeProperties)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private void connectSocket()
    throws IOException
  {
    Socket socket = new Socket(_hostname, _port);
    
    _s = new QSocketWrapper(socket);
    
    _is = new ReadStream(_s.getStream());
    _os = new WriteStream(_s.getStream());
  }

  /**
   * @return
   */
  public int nextLinkId()
  {
    return _linkId.incrementAndGet();
  }

  public void onClose()
  {
    close();
  }

  public void close()
  {
    if (! _isClosed.compareAndSet(false, true)) {
      return;
    }
    
    try {
      _handler.closeSessions();
      
      _handler.closeConnection();
    } finally {
      disconnect();
    }
  }
  
  
  public void disconnect()
  {
    try {
      disconnectImpl();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  private void disconnectImpl()
    throws IOException
  {
    _isDisconnected = true;
    
    QSocketWrapper s = _s;
    _s = null;
    
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;
   
    if (s != null) {
      s.close();
    }
    
    if (is != null) {
      is.close();
    }
    
    if (os != null) {
      os.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _hostname + ":" + _port + "]";
  }
}
