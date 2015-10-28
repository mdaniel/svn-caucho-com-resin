/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import com.caucho.v5.websocket.io.WebSocketContext;


/**
 * Application handler for a WebSocket tcp stream
 *
 * The read stream should only be read by the <code>onRead</code> thread.
 *
 * The write stream must be synchronized if it's every written by a thread
 * other than the <code>serviceRead</code>
 */
abstract public class AbstractWebSocketListener implements WebSocketListener
{
  /**
   * Called when the connection is established
   */
  @Override
  public void onStart(WebSocketContext context)
    throws IOException
  {
  }

  /**
   * Called when a binary message is available
   */
  @Override
  public void onReadBinary(WebSocketContext context, InputStream is)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Called when a text message is available
   */
  @Override
  public void onReadText(WebSocketContext context, Reader is)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Called when the peer closes the connection gracefully.
   */
  @Override
  public void onClose(WebSocketContext context)
    throws IOException
  {
  }

  /**
   * Called when the connection terminates.
   */
  @Override
  public void onDisconnect(WebSocketContext context)
    throws IOException
  {
  }

  /**
   * Called when the connection times out
   */
  @Override
  public void onTimeout(WebSocketContext context)
    throws IOException
  {
    
  }
}
