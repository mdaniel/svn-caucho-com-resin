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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.websocket.common.ConnectionWebSocketBase;
import com.caucho.v5.websocket.common.ContainerWebSocketBase;
import com.caucho.v5.websocket.common.EndpointConnection;
import com.caucho.v5.websocket.io.FrameInputStream;

/**
 * websocket server container
 */
@ModulePrivate
public class ConnectionPlain extends ConnectionWebSocketBase
{
  private static final Logger log
    = Logger.getLogger(ConnectionPlain.class.getName());
  
  private EndpointConnection _conn;
  private EndpointReaderPlain _endpointReader;
  
  private SessionWebSocketPlain _session;
  private Map<String, String> _pathParameters;

  public ConnectionPlain(long id,
                         ContainerWebSocketBase container, 
                         String uri,
                         Endpoint endpoint,
                         EndpointConfig config, 
                         FrameInputStream fIs)
  {
    super(container);
    
    Objects.requireNonNull(fIs);
    
    _session = new SessionWebSocketPlain(id, container, uri, endpoint, config,
                                         this);
    
    _endpointReader = new EndpointReaderPlain(endpoint, _session, fIs);
  }
  
  @Override
  public void setSubprotocol(String subprotocol)
  {
    _session.setSubprotocol(subprotocol);
  }


  @Override
  public void setPathParameters(ArrayList<String> names, String[] paths)
  {
    _pathParameters = new HashMap<>();

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      String path = paths[i];
      _pathParameters.put(name, path);
    }
  }

  public Map<String,String> getPathParameters()
  {
    return _pathParameters;
  }

  /**
   * Called on connection, before any handshake.
   */
  @Override
  public void initConnection(EndpointConnection conn)
    throws IOException
  {
    _conn = conn;
    
    _endpointReader.init(conn.getInputStream());

    _session.init(conn);
  }

  /**
   * Called when the handshake completes, before any message processing.
   * 
   * Open is called on the initial handshake thread.
   */
  @Override
  public void open()
  {
    _session.open();
  }

  @Override
  public boolean isActive()
  {
    Session session = _session;

    return session != null && session.isOpen();
  }
  
  @Override
  public Session getSession()
  {
    return _session;
  }
  
  @Override
  public EndpointReaderPlain getEndpointReader()
  {
    return _endpointReader;
  }
  
  @Override
  public void close()
  {
    try {
      _session.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  @Override
  public void onDisconnectRead()
  {
    /*
    try {
      _session.error(new WebSocketDisconnectException());
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    */

    try {
      _session.onClose(new CloseReason(CloseCodes.CLOSED_ABNORMALLY, "disconnect"));
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _session.waitForWriteClose(10000);
  }
  
  void closeWrite()
  {
    try {
      EndpointConnection conn = _conn;
      _conn = null;
      if (conn != null) {
        conn.closeWrite();
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _session.getRequestURI() + "]";
  }
}
