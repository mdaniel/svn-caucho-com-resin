/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.websocket.plain;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.RemoteEndpoint;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;
import com.caucho.v5.websocket.common.ContainerWebSocketBase;
import com.caucho.v5.websocket.common.EndpointConnection;
import com.caucho.v5.websocket.common.SessionWebSocketBase;
import com.caucho.v5.websocket.io.WebSocketConstants;

/**
 * websocket server container
 */
@Module
public class SessionWebSocketPlain extends SessionWebSocketBase
{
  private static final L10N L = new L10N(SessionWebSocketPlain.class);
  
  private static final Logger log
    = Logger.getLogger(SessionWebSocketPlain.class.getName());
  
  private String _id;
  
  private EndpointConnection _conn;
  private RemoteEndpointBasicPlain _remoteEndpointBasic;
  private RemoteEndpointAsyncPlain _remoteEndpointAsync;
  
  private String _protocolVersion;
  
  private ReadPlain _textReader = new ReadPlainNull();
  private ReadPlain _binaryReader = new ReadPlainNull();
  private ReadPing _pingReader = new ReadPing();

  private AtomicBoolean _isWriteClosed = new AtomicBoolean();
  private AtomicBoolean _isReadClosed = new AtomicBoolean();
  
  private boolean _isSecure;
  private boolean _isMasked;
  private String _subprotocol = "";
  private Endpoint _endpoint;

  private EndpointExt _endpointExt;

  private EndpointConfig _config;
  private ConnectionPlain _connPlain;

  private ClassLoader _classLoader;
  
  public SessionWebSocketPlain(long id,
                               ContainerWebSocketBase container, 
                               String uri,
                               Endpoint endpoint,
                               EndpointConfig config,
                               ConnectionPlain connPlain)
  {
    super(container, uri);
    
    // _id = String.valueOf(id) + "." + _debug.incrementAndGet();
    _id = String.valueOf(id);

    _isSecure = false;
    
    if (container != null) {
      _isMasked = container.isMasked();
    }

    _protocolVersion = WebSocketConstants.VERSION;
    
    _endpoint = endpoint;
    
    if (endpoint instanceof EndpointExt) {
      _endpointExt = (EndpointExt) endpoint;
    }
    
    _config = config;
    
    _connPlain = connPlain;
    
    _classLoader = Thread.currentThread().getContextClassLoader();

    if (config != null && config.getDecoders() != null) {
      ArrayList<Decoder> decoders = new ArrayList<>();
      
      for (Class<? extends Decoder> cl : config.getDecoders()) {
        try {
          decoders.add(cl.newInstance());
        } catch (Exception e) {
          throw new DeploymentException(e);
        }
      }
      
      setDecoders(decoders);
    }

    if (config != null && config.getEncoders() != null) {
      for (Class<? extends Encoder> cl : config.getEncoders()) {
        try {
          addEncoder(cl.newInstance());
        } catch (Exception e) {
          throw new DeploymentException(e);
        }
      }
    }
  }
  

  @Override
  public String getId()
  {
    return _id;
  }
  
  boolean isMasked()
  {
    return _isMasked;
  }
  
  @Override
  protected EndpointConfig getEndpointConfig()
  {
    return _config;
  }
  
  void setSubprotocol(String subprotocol)
  {
    if (subprotocol == null) {
      subprotocol = "";
    }
    
    _subprotocol = subprotocol;
  }

  @Override
  public boolean isSecure()
  {
    return _isSecure;
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  @Override
  public Map<String,String> getPathParameters()
  {
    return _connPlain.getPathParameters();
  }

  @Override
  public String getProtocolVersion()
  {
    return _protocolVersion;
  }

  @Override
  public String getNegotiatedSubprotocol()
  {
    return _subprotocol;
  }

  @Override
  public RemoteEndpoint.Basic getBasicRemote()
  {
    testActivity();
    
    return _remoteEndpointBasic;
  }

  @Override
  public RemoteEndpoint.Async getAsyncRemote()
  {
    testActivity();
    
    return _remoteEndpointAsync;
  }
  
  @Override
  public long getMaxIdleTimeout()
  {
    return _conn.getIdleReadTimeout();
  }
  
  @Override
  public void setMaxIdleTimeout(long timeout)
  {
    _conn.setIdleReadTimeout(timeout);
  }

  @Override
  public void addBasicHandler(MessageHandler.Whole<?> handler,
                              Class<?> type)
  {
    if (String.class.equals(type)) {
      setTextReader(new ReadPlainString((MessageHandler.Whole<String>) handler));
    }
    else if (ByteBuffer.class.equals(type)) {
      setBinaryReader(new ReadWholeByteBuffer((MessageHandler.Whole<ByteBuffer>) handler));
    }
    else if (Reader.class.equals(type)) {
      setTextReader(new ReadPlainReader((MessageHandler.Whole<Reader>) handler));
    }
    else if (InputStream.class.equals(type)) {
      setBinaryReader(new ReadPlainInputStream((MessageHandler.Whole<InputStream>) handler));
    }
    else if (PongMessage.class.equals(type)) {
      _pingReader = new ReadPingHandler((MessageHandler.Whole<PongMessage>) handler);
    }
    else {
      super.addBasicHandler(handler, type);
    }
  }
  
  @Override
  protected void addAsyncHandler(MessageHandler.Partial<?> handler, Class<?> type)
  {
    if (type.equals(String.class)) {
      setTextReader(new ReadPartialString((MessageHandler.Partial<String>) handler));
    }
    else if (type.equals(ByteBuffer.class)) {
      setBinaryReader(new ReadPartialByteBuffer((MessageHandler.Partial<ByteBuffer>) handler));
    }
    else {
      super.addAsyncHandler(handler, type);
    }
  }
  
  private void setTextReader(ReadPlain textReader)
  {
    if (! (_textReader instanceof ReadPlainNull)) {
      throw new IllegalStateException(L.l("multiple text MessageHandlers are forbidden old={0}, new={1}",
                                          _textReader,
                                          textReader));
    }
    
    _textReader = textReader;
  }
  
  private void setBinaryReader(ReadPlain binaryReader)
  {
    if (! (_binaryReader instanceof ReadPlainNull)) {
      throw new IllegalStateException(L.l("multiple binaryMessageHandlers are forbidden old={0}, new={1}",
                                          _binaryReader,
                                          binaryReader));
    }
    
    _binaryReader = binaryReader;
  }

  ReadPlain getBinaryHandler()
  {
    onActivity();
    
    return _binaryReader;
  }

  ReadPlain getTextHandler()
  {
    onActivity();
    
    return _textReader;
  }

  ReadPing getPingHandler()
  {
    onActivity();
    
    return _pingReader;
  }
  
  //
  // lifecycle
  //

  @Override
  public boolean isOpen()
  {
    /*
    try {
      testActivity();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    }
    */
    
    return ! _isWriteClosed.get() && ! _isReadClosed.get();
  }

  public boolean isOpenRead()
  {
    return ! _isReadClosed.get();
  }
  
  /**
   * Called on connection, before any handshake.
   * 
   * Called on the init/handshake thread.
   */
  public void init(EndpointConnection conn)
    throws IOException
  {
    _conn = conn;
    
    WriteStream os = _conn.getOutputStream();
    
    _remoteEndpointBasic = new RemoteEndpointBasicPlain(this, os);
    _remoteEndpointAsync = new RemoteEndpointAsyncPlain(this, os);
  }
  
  /**
   * Called after the handshake completes, before any messages.
   * 
   * Called on the init/handshake thread.
   */

  public void open()
  {
    try {
      _endpoint.onOpen(this, _config);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  boolean isWriteClosed()
  {
    return _isWriteClosed.get();
  }

  @Override
  public void close()
    throws IOException
  {
    close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "ok"));
  }
  
  @Override
  public void close(CloseReason reason)
    throws IOException
  {
    closeWrite(reason);
    
    super.close(reason);
    
    //long timeout = 5000L;
    //waitForReadClose(timeout);
  }
  
  private boolean closeWrite(CloseReason reason)
  {
    if (! _isWriteClosed.compareAndSet(false, true)) {
      return false;
    }
    
    try {
      RemoteEndpointBasicPlain remoteEndpoint = _remoteEndpointBasic;
      _remoteEndpointBasic = null;

      if (remoteEndpoint != null) {
        remoteEndpoint.closeWrite(reason);
      }
    
      _connPlain.closeWrite();
    } finally {
      synchronized (_isWriteClosed) {
        _isWriteClosed.notifyAll();
      }
    }
    
    return true;
  }
  
  void writeDisconnect()
  {
    if (_isWriteClosed.compareAndSet(false, true)) {
      synchronized (_isWriteClosed) {
        _isWriteClosed.notifyAll();
      }
      
      _connPlain.closeWrite();
    }
  }
  
  /**
   * Called when a ping is received by the read thread.
   */
  @Override
  public void onPing(byte []buffer, int offset, int length)
  {
    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
    
    _pingReader.onPing(byteBuffer);
  }
  
  /**
   * Called when a pong is received by the read thread.
   */
  @Override
  public void onPong(byte []buffer, int offset, int length)
  {
    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
    
    _pingReader.onPong(byteBuffer);
  }

  /**
   * Called when close received by the read thread.
   * 
   * Called on read thread.
   */
  @Override
  public void onClose(CloseReason reason)
  {
    if (_isReadClosed.compareAndSet(false, true)) {
      try {
        _endpoint.onClose(this, reason);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
        
        try {
          _endpoint.onError(this, e);
        } catch (Exception e1) {
          log.log(Level.FINER, e1.toString(), e1);
        }
      }
    }

    // websocket/00da
    // TCK expects the close to be automatic
    // read close does not automatically call write close
    // closeImpl(reason);
    // waitForWriteClose(10000);
    try {
      close(reason);
    } catch (IOException e) {
      e.printStackTrace();
      log.log(Level.FINER, e.toString(), e);
    }
    
    synchronized (_isReadClosed) {
      _isReadClosed.notifyAll();
    }
  }
  
  void onReadClose(CloseReason reason)
  {
    onClose(reason);
  }
  
  void waitForWriteClose(long timeout)
  {
    synchronized (_isWriteClosed) {
      if (! _isWriteClosed.get()) {
        try {
          _isWriteClosed.wait(timeout);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
  
  void waitForReadClose(long timeout)
  {
    synchronized (_isReadClosed) {
      if (! _isReadClosed.get()) {
        try {
          _isReadClosed.wait(timeout);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public void beforeBatch()
  {
    EndpointExt endpointExt = _endpointExt;
    
    if (endpointExt != null) {
      endpointExt.beforeBatch();
    }
  }

  public void afterBatch()
  {
    EndpointExt endpointExt = _endpointExt;
    
    if (endpointExt != null) {
      endpointExt.afterBatch();
    }
  }

  @Override
  public void error(Throwable exn)
  {
    _endpoint.onError(this, exn);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + getRequestURI() + "]";
  }
  
  class ReadPing {
    void onPing(ByteBuffer bBuf)
    {
    }
    
    void onPong(ByteBuffer bBuf)
    {
    }
  }
  
  class ReadPingHandler extends ReadPing {
    private MessageHandler.Whole<PongMessage> _handler;
    
    ReadPingHandler(MessageHandler.Whole<PongMessage> handler)
    {
      _handler = handler;
    }
    
    @Override
    void onPing(ByteBuffer bBuf)
    {
      _handler.onMessage(new PongMessageImpl(bBuf));
      
      try {
        getBasicRemote().sendPong(bBuf.duplicate());
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    @Override
    void onPong(ByteBuffer bBuf)
    {
      _handler.onMessage(new PongMessageImpl(bBuf));
    }
  }
  
  static class PongMessageImpl implements PongMessage {
    private ByteBuffer _bBuf;
    
    PongMessageImpl(ByteBuffer bBuf)
    {
      _bBuf = bBuf;
    }
    
    @Override
    public ByteBuffer getApplicationData()
    {
      return _bBuf;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _bBuf + "]";
    }
  }
}
