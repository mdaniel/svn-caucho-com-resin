/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.actor.AbstractActorSender;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.SimpleActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.client.LinkConnection;
import com.caucho.bam.client.LinkConnectionFactory;
import com.caucho.bam.stream.ActorStream;
import com.caucho.cloud.security.SecurityService;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.websocket.WebSocketListener;

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
  public ActorStream getOutboundStream()
  {
    return _hmtpListener.getOutboundStream();
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }
}
