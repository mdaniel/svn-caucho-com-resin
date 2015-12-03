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

import java.util.logging.Logger;

import com.caucho.bam.client.LinkConnection;
import com.caucho.bam.stream.MessageStream;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.util.L10N;

/**
 * HMTP client protocol
 */
class HmtpLinkConnection implements LinkConnection {
  private static final L10N L = new L10N(HmtpLinkConnection.class);
  
  private static final Logger log
    = Logger.getLogger(HmtpLinkConnection.class.getName());
  
  private WebSocketClient _webSocketClient;
  private HmtpWebSocketListener _hmtpListener;

  HmtpLinkConnection(WebSocketClient webSocketClient,
                     HmtpWebSocketListener hmtpListener)
  {
    _webSocketClient = webSocketClient;
    _hmtpListener = hmtpListener;
  }
  
  @Override
  public String getAddress()
  {
    return _hmtpListener.getAddress();
  }
  
  @Override
  public MessageStream getOutboundStream()
  {
    MessageStream stream = _hmtpListener.getOutboundStream();

    return stream;
  }

  @Override
  public boolean isClosed()
  {
    return _hmtpListener.isClosed();
  }
}
