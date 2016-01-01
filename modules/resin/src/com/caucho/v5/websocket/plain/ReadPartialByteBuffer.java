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
import java.nio.ByteBuffer;

import javax.websocket.MessageHandler;

import com.caucho.v5.vfs.TempBuffer;
import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketInputStream;

/**
 * Callback for reads.
 */
class ReadPartialByteBuffer extends ReadPlain {
  private final MessageHandler.Partial<ByteBuffer> _handler;
  
  ReadPartialByteBuffer(MessageHandler.Partial<ByteBuffer> handler)
  {
    _handler = handler;
  }
  
  @Override
  void onRead(FrameInputStream is)
    throws IOException
  {
    WebSocketInputStream wsIn = is.initBinary();

    TempBuffer tBuf = TempBuffer.allocate();

    try {
      byte []buffer = tBuf.buffer();
      int length = buffer.length;

      while (true) {
        int sublen = wsIn.read(buffer, 0, length);
        
        if (sublen < 0) {
          ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[0]);
          _handler.onMessage(byteBuffer, true);
          break;
        }
        
        boolean isLast = wsIn.available() < 0;
        
        byte []newBuffer = new byte[sublen];
        System.arraycopy(buffer, 0, newBuffer, 0, sublen);
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(newBuffer);
        _handler.onMessage(byteBuffer, isLast);
        
        if (isLast) {
          break;
        }
      }
    } finally {
      wsIn.close();
    }

    tBuf.freeSelf();
  }
}
