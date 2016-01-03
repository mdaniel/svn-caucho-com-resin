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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import com.caucho.v5.io.IoUtil;
import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.WriteStream;
import com.caucho.v5.websocket.common.RemoteAsyncWebSocketBase;
import com.caucho.v5.websocket.io.OutWebSocket;
import com.caucho.websocket.io.WebSocketWriter;

/**
 * websocket writing endpoint.
 */
@ModulePrivate
public class RemoteEndpointAsyncPlain extends RemoteAsyncWebSocketBase
{
  private static final L10N L = new L10N(RemoteEndpointAsyncPlain.class);
  private static final Logger log
    = Logger.getLogger(RemoteEndpointAsyncPlain.class.getName());

  private WriteStream _os;
  private OutWebSocket _binaryOut;

  private WebSocketWriter _textOut;
  private PrintWriter _textWriter;
  
  private boolean _isBatching;
  
  private SessionWebSocketPlain _session;

  RemoteEndpointAsyncPlain(SessionWebSocketPlain session,
                           WriteStream os)
  {
    _session = session;
    _os = os;
  }

  @Override
  public void setBatchingAllowed(boolean isBatching)
  {
    _isBatching = isBatching;
    
    if (_textOut != null) {
      _textOut.setBatching(isBatching);
    }
    
    if (_binaryOut != null) {
      _binaryOut.setBatching(isBatching);
    }
  }

  @Override
  public boolean getBatchingAllowed()
  {
    return _isBatching;
  }

  public OutputStream startBinaryMessage()
    throws IOException
  {
    if (isWriteClosed()) {
      throw new IllegalStateException(L.l("{0} is closed for writing.",
                                          this));
    }
    
    if (_binaryOut == null)
      _binaryOut = new OutWebSocket(_os,
                                             TempBuffer.allocate().buffer());
    
    _binaryOut.init();
    
    //return _binaryOut;
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Writer startTextMessage()
    throws IOException
  {
    WriteStream out = _os;
    
    if (out == null) {
      throw new IllegalStateException(L.l("{0} is closed for writing.",
                                          this));
    }
    
    if (_textOut == null) {
      _textOut = new WebSocketWriter(out,
                                     TempBuffer.allocate().buffer());
      
      _textOut.setBatching(getBatchingAllowed());
    }
    
    _textOut.init();
    
    return _textOut;
  }
  
  public void pong(byte []value)
    throws IOException
  {
    WriteStream out = _os;
    
    byte []bytes = value;
        
    out.write(0x8a);
    out.write(bytes.length);
    out.write(bytes);
    out.flush();
  }
  
  public boolean isClosed()
  {
    return isWriteClosed();
  }
  
  public boolean isWriteClosed()
  {
    return _session.isWriteClosed();
  }
    
  void closeImpl(CloseReason reason)
  {
    _os = null;
  }

  public void disconnect()
    // _isWriteClosed.set(true);
  {
  }
  
  @Override
  public void flushBatch()
    throws IOException
  {
    WriteStream os = _os;
    
    if (os != null) {
      os.flush();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
