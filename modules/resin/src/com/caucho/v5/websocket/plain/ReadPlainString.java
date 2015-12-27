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

import javax.websocket.MessageHandler;

import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketReader;

/**
 * Callback for reads.
 */
class ReadPlainString extends ReadPlain {
  private final MessageHandler.Whole<String> _handler;
  
  private final StringBuilder _sb = new StringBuilder();
  private final char []_charBuffer = new char[128];
  
  ReadPlainString(MessageHandler.Whole<String> handler)
  {
    _handler = handler;
  }
  
  @Override
  void onRead(FrameInputStream is)
    throws IOException
  {
    WebSocketReader textIn = is.initReader();

    try {
      final char []charBuffer = _charBuffer;
      final int bufferLength = charBuffer.length;
      
      int len;
      
      while ((len = textIn.read(charBuffer, 0, bufferLength)) >= 0) {
        _sb.append(charBuffer, 0, len);
      }

      _handler.onMessage(_sb.toString());
      _sb.setLength(0);
    } finally {
      textIn.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handler + "]";
  }
}
