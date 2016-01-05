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

package com.caucho.remote.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketEncoder;

/**
 * User facade for http requests.
 */
public class WebSocketContextStreamImpl
  implements WebSocketContext, WebSocketConstants
{
  private static final L10N L = new L10N(WebSocketContextStreamImpl.class);
  private static final Logger log
    = Logger.getLogger(WebSocketContextStreamImpl.class.getName());
  
  private WriteStream _out;
  
  private WebSocketOutputStream _binaryOut;
  
  private WebSocketWriter _textOut;
  private PrintWriter _textWriter;
  
  private AtomicBoolean _isWriteClosed = new AtomicBoolean();

  public WebSocketContextStreamImpl(WriteStream out)
  {     
    _out = out;
  }

  @Override
  public void setTimeout(long timeout)
  {
  }

  @Override
  public long getTimeout()
  {
    throw new UnsupportedOperationException(getClass().getName());
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
      _binaryOut = new WebSocketOutputStream(getWriteStream(),
                                             TempBuffer.allocate().getBuffer());
    
    _binaryOut.init();
    
    return _binaryOut;
  }

  @Override
  public PrintWriter startTextMessage()
    throws IOException
  {
    if (_textOut == null) {
      WriteStream os = getWriteStream();
      
      if (os == null) {
        throw new IllegalStateException(L.l("{0} is closed for writing."));
      }
      
      _textOut = new WebSocketWriter(os,
                                     TempBuffer.allocate().getBuffer());
      
      _textWriter = new WebSocketPrintWriter(_textOut);
    }
    
    _textOut.init();
    
    return _textWriter;
  }
  
  @Override
  public void pong(byte []bytes)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public void close()
  {
    close(1000, "ok");
  }
  
  @Override
  public void close(int code, String msg)
  {
    if (_isWriteClosed.getAndSet(true))
      return;

    try {
      WriteStream out = getWriteStream();
    
      out.write(0x88);
      out.write(0x00);
      out.flush();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      disconnect();
    }
  }
  
  protected WriteStream getWriteStream()
  {
    return _out;
  }

  @Override
  public void disconnect()
  {
    _out = null;
  }
  
  //
  // duplex callbacks
  //
  
  void onStart()
    throws IOException
  {
  }
  
  @Override
  public void flush()
    throws IOException
  {
    WriteStream out = getWriteStream();
    
    out.flush();
  }

  protected WebSocketInputStream createWebSocketInputStream(FrameInputStream is)
    throws IOException
  {
    return new WebSocketInputStream(is);
  }

  @Override
  public void onClose(int closeCode, String closeMessage)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getWriteStream() + "]";
  }

  /* (non-Javadoc)
   * @see com.caucho.websocket.WebSocketContext#createOutputQueue(com.caucho.websocket.WebSocketEncoder)
   */
  @Override
  public <T> BlockingQueue<T> createOutputQueue(WebSocketEncoder<T> encoder)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.websocket.WebSocketContext#setAutoFlush(boolean)
   */
  @Override
  public void setAutoFlush(boolean isAutoFlush)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.websocket.WebSocketContext#isAutoFlush()
   */
  @Override
  public boolean isAutoFlush()
  {
    // TODO Auto-generated method stub
    return false;
  }
}
