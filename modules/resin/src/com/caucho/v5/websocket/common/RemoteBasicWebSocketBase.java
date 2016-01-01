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

package com.caucho.v5.websocket.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint;

import com.caucho.v5.inject.Module;
import com.caucho.v5.vfs.TempBuffer;

/**
 * stub/adapter for RemoteEndpoint.Basic
 */
@Module
public class RemoteBasicWebSocketBase 
  extends RemoteWebSocketBase
  implements RemoteEndpoint.Basic
{
  private OutputStream _os;
  
  @Override
  public OutputStream getSendStream() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Writer getSendWriter() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void sendBinary(ByteBuffer data) throws IOException
  {
    try (OutputStream os = getSendStream()) {
      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.buffer();
      
      int len;
      
      while ((len = data.remaining()) > 0) {
        len = Math.min(len, buffer.length);
        
        data.get(buffer, 0, len);

        os.write(buffer, 0, len);
      }

      TempBuffer.free(tBuf);
    }
  }

  @Override
  public void sendBinary(ByteBuffer data, boolean isLast) throws IOException
  {
    OutputStream os = _os;
    _os = null;
    
    if (os == null) {
      os = getSendStream();
    }
    
    TempBuffer tBuf = TempBuffer.allocate();
    byte []buffer = tBuf.buffer();
    
    int len;
    
    while ((len = data.remaining()) > 0) {
      len = Math.min(len, buffer.length);
      
      data.get(buffer, 0, len);

      os.write(buffer, 0, len);
    }

    TempBuffer.free(tBuf);
    
    if (isLast) {
      os.close();
    }
    else {
      os.flush();
      _os = os;
    }
  }

  @Override
  public void sendObject(Object data) throws IOException, EncodeException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void sendText(String text) throws IOException
  {
    try (Writer out = getSendWriter()) {
      out.append(text);
    }
  }

  @Override
  public void sendText(String partial, boolean isLast) throws IOException
  {
    try (Writer out = getSendWriter()) {
      out.append(partial);
    }
  }
}
