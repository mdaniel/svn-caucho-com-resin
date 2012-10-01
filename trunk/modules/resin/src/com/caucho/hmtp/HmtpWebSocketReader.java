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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.bam.stream.MessageStream;
import com.caucho.remote.websocket.UnmaskedFrameInputStream;
import com.caucho.remote.websocket.WebSocketInputStream;
import com.caucho.websocket.WebSocketContext;

/**
 * HmtpReader stream handles client packets received from the server.
 */
public class HmtpWebSocketReader {
  private InputStream _is;
  private WebSocketInputStream _wsIs;
  private HmtpReader _hIn;

  public HmtpWebSocketReader(InputStream is)
    throws IOException
  {
    _hIn = new HmtpReader();
    UnmaskedFrameInputStream fIs = new UnmaskedFrameInputStream();
    
    WebSocketContext cxt = null;
    
    fIs.init(cxt, is);
    _wsIs = new WebSocketInputStream(fIs);
  }

  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  public boolean readPacket(MessageStream actorStream)
    throws IOException
  {
    if (actorStream == null)
      throw new IllegalStateException("HmtpReader.readPacket requires a valid ActorStream for callbacks");

    if (_wsIs.startBinaryMessage()) {
      boolean isPacket =_hIn.readPacket(_wsIs, actorStream);
      
      _wsIs.close();
      
      return isPacket;
    }
    
    return false;
  }

  /**
   * @return
   */
  public boolean isDataAvailable()
    throws IOException
  {
    InputStream is = _is;
    
    return is != null && is.available() > 0;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _is + "]";
  }

  /**
   * 
   */
  public void close()
  {
  }
}
