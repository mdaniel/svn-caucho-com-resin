/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.remote.websocket.WebSocketConstants;
import com.caucho.remote.websocket.WebSocketInputStream;
import com.caucho.remote.websocket.WebSocketOutputStream;
import com.caucho.remote.websocket.WebSocketPrintWriter;
import com.caucho.remote.websocket.WebSocketReader;
import com.caucho.remote.websocket.WebSocketWriter;
import com.caucho.remote.websocket.FrameInputStream;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketListener;

/**
 * User facade for http requests.
 */
class WebSocketContextImpl
  implements WebSocketContext, WebSocketConstants, SocketLinkDuplexListener
{
  private static final L10N L = new L10N(WebSocketContextImpl.class);
  private static final Logger log
    = Logger.getLogger(WebSocketContextImpl.class.getName());
  
  private final HttpServletRequestImpl _request;
  private final WebSocketListener _listener;

  private SocketLinkDuplexController _controller;
  
  private FrameInputStream _is;
  
  private WebSocketOutputStream _binaryOut;
  private WebSocketInputStream _binaryIn;
  
  private WebSocketWriter _textOut;
  private PrintWriter _textWriter;
  private WebSocketReader _textIn;
  
  private boolean _isReadClosed;
  private AtomicBoolean _isWriteClosed = new AtomicBoolean();

  WebSocketContextImpl(HttpServletRequestImpl request,
                       HttpServletResponseImpl response,
                       WebSocketListener listener,
                       FrameInputStream is)
  {     
    _request = request;
    _listener = listener;
    _is = is;
  }

  public void setController(SocketLinkDuplexController controller)
  {
    _controller = controller;
    
    _is.init(controller.getReadStream());
  }

  @Override
  public void setTimeout(long timeout)
  {
    _controller.setIdleTimeMax(timeout);
  }

  @Override
  public long getTimeout()
  {
    return _controller.getIdleTimeMax();
  }

  /*
  @Override
  public InputStream getInputStream()
  throws IOException
  {
    return _controller.getReadStream();
  }
  */

  @Override
  public OutputStream startBinaryMessage()
  throws IOException
  {
    if (_isWriteClosed.get())
      throw new IllegalStateException(L.l("{0} is closed for writing.",
                                          this));
    
    if (_binaryOut == null)
      _binaryOut = new WebSocketOutputStream(_controller.getWriteStream(),
                                             TempBuffer.allocate().getBuffer());
    
    _binaryOut.init();
    
    return _binaryOut;
  }

  @Override
  public PrintWriter startTextMessage()
    throws IOException
  {
    if (_textOut == null) {
      _textOut = new WebSocketWriter(_controller.getWriteStream(),
                                     TempBuffer.allocate().getBuffer());
      _textWriter = new WebSocketPrintWriter(_textOut);
    }
    
    _textOut.init();
    
    return _textWriter;
  }
  
  @Override
  public void close()
  {
    if (_isWriteClosed.getAndSet(true))
      return;

    try {
      WriteStream out = _controller.getWriteStream();
    
      out.write(0x81);
      out.write(0x00);
      out.flush();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      disconnect();
    }
  }

  @Override
  public void disconnect()
  {
    _controller.complete();
  }
  
  //
  // duplex callbacks
  //
  
  void onStart()
    throws IOException
  {
    _listener.onStart(this);
  }
  
  void flush()
    throws IOException
  {
    WriteStream out = _controller.getWriteStream();
    
    out.flush();
  }

  @Override
  public void onStart(SocketLinkDuplexController context) 
    throws IOException
  {
  }

  @Override
  public void onRead(SocketLinkDuplexController duplex)
  throws IOException
  {
    do {
      if (! readFrame())
        return;
    } while (_request.getAvailable() > 0);
  }
  
  private boolean readFrame()
    throws IOException
  {  
    if (! _is.readFrameHeader()) {
      return false;
    }

    int opcode = _is.getOpcode();

    switch (opcode) {
    case OP_BINARY:
      if (_binaryIn == null)
        _binaryIn = createWebSocketInputStream(_is);

      _binaryIn.init();

      _listener.onReadBinary(this, _binaryIn);
      break;

    case OP_TEXT:
      if (_textIn == null)
        _textIn = new WebSocketReader(_is);

      _textIn.init();

      _listener.onReadText(this, _textIn);
      break;
      
    case OP_CLOSE:
      _isReadClosed = true;
      try {
        _listener.onClose(this);
      } finally {
        close();
      }
      break;

    default:
      // XXX:
      disconnect();
      break;
    }
    
    return true;
  }
  
  protected WebSocketInputStream createWebSocketInputStream(FrameInputStream is)
    throws IOException
  {
    return new WebSocketInputStream(is);
  }

  @Override
  public void onDisconnect(SocketLinkDuplexController duplex)
    throws IOException
  {
    _listener.onDisconnect(this);
  }

  @Override
  public void onTimeout(SocketLinkDuplexController duplex)
    throws IOException
  {
    _listener.onTimeout(this);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listener + "]";
  }
}
