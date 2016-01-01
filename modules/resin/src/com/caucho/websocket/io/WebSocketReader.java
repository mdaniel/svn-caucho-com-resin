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

package com.caucho.websocket.io;

import java.io.IOException;
import java.io.Reader;

import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketConstants;

/**
 * WebSocketReader reads a single WebSocket packet.
 *
 * <code><pre>
 * 0x00 utf-8 data 0xff
 * </pre></code>
 */
public final class WebSocketReader extends Reader
  implements WebSocketConstants
{
  private final FrameInputStream _is;

  public WebSocketReader(FrameInputStream is)
    throws IOException
  {
    _is = is;
  }

  public void init()
  {
  }

  public long getLength()
  {
    return _is.getLength();
  }

  @Override
  public int read()
    throws IOException
  {
    return _is.readText();
  }
  
  @Override
  public int read(char []charBuffer, int charOffset, int charLength)
    throws IOException
  {
    return _is.readText(charBuffer, charOffset, charLength);
  }

  @Override
  public void close()
  throws IOException
  {
    _is.skipToFrameEnd();
  }
}
