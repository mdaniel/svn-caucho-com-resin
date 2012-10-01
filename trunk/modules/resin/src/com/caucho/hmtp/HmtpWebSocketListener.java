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

import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.broker.Broker;
import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;

/**
 * HmtpReader stream handles client packets received from the server.
 */
public class HmtpWebSocketListener extends AbstractWebSocketListener {
  private Broker _broker;
  
  private HmtpReader _hIn;
  private HmtpWebSocketContextWriter _hOut;

  public HmtpWebSocketListener(Broker broker)
  {
    if (broker == null)
      throw new IllegalArgumentException();
    
    _broker = broker;
  }

  HmtpWebSocketContextWriter getOutboundStream()
  {
    return _hOut;
  }
  
  String getAddress()
  {
    return "hmtp@" + _broker.getAddress();
  }
  
  @Override
  public void onStart(WebSocketContext context)
  {
    _hOut = new HmtpWebSocketContextWriter(context);
    _hIn = new HmtpReader();
  }
  
  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  @Override
  public void onReadBinary(WebSocketContext context, InputStream is)
    throws IOException
  {
    _hIn.readPacket(is, _broker);
  }
  
  @Override
  public void onDisconnect(WebSocketContext context)
  {
    _hOut = null;
    _hIn = null;
  }
  
  boolean isClosed()
  {
    return _hOut == null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _broker + "]";
  }
}
