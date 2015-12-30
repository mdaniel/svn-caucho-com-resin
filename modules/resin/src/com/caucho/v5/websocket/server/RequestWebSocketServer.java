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

package com.caucho.v5.websocket.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.caucho.v5.inject.Module;
import com.caucho.v5.network.port.ConnectionProtocol;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.network.port.StateConnection;
import com.caucho.v5.websocket.io.InWebSocket;
import com.caucho.v5.websocket.io.WebSocketBaratine;

/**
 * User facade for http requests.
 */
@Module
public class RequestWebSocketServer implements ConnectionProtocol
{
  private static final Logger log = Logger.getLogger(RequestWebSocketServer.class.getName());
  
  //private final HttpServletRequestImpl _request;

  //private DuplexController _controller;
  //private final Endpoint _endpoint;

  //private FrameInputStream _is;

  private WebSocketBaratine _wsConn;

  private AtomicBoolean _isWriteClosed = new AtomicBoolean();
  private InWebSocket _reader;

  //private Connection _conn;
  private ConnectionTcp _connTcp;

  public RequestWebSocketServer(WebSocketBaratine wsConn,
                                ConnectionTcp connTcp)
  {
    /*
    _request = request;
    _is = is;
    _endpoint = endpoint;

    URI uri;

    try {
      uri = new URI("ws://" + _request.getServerName() + _request.getRequestURI());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    new PlainConnection(container, uri, endpoint, _is);

    */

    _wsConn = wsConn;
    _connTcp = connTcp;
    
    //_conn = new Connection(duplex);
    //_wsConn.initConnection(_conn);

    /*
    _session.setDecoders(config.getDecoders());
    _session.setEncoders(config.getEncoders());
    */

    //_reader = _wsConn.getEndpointReader();
  }

  // @Override
  public void onStart()
    throws IOException
  {
    //_conn = new Connection(_connTcp);
/*
    _wsConn.initConnection(_conn);

    _reader = _wsConn.getEndpointReader();
    */
  }

  public void setTimeout(long timeout)
  {
    // _controller.setIdleTimeMax(timeout);
  }

  public long getTimeout()
  {
      // return _controller.getIdleTimeMax();
    
    return 0;
  }

  public boolean isClosed()
  {
    return _isWriteClosed.get();
  }

  public void disconnect()
  {
    _isWriteClosed.set(true);

    // _controller.complete();

    //_wsConn.onDisconnectRead();
  }

  //
  // duplex callbacks
  //

  /*
  void onStart()
    throws IOException
  {
    _endpoint.onOpen(_session);
    // _listener.onStart(this);
  }
  */

  /*
  public void setController(DuplexController controller)
    throws IOException
  {
    // _controller = controller;
  }
  */

  @Override
  public StateConnection service()
    throws IOException
  {
    if (_reader.onRead()) {
      return StateConnection.READ;
    }
    else {
      return StateConnection.CLOSE;
    }
  }

  @Override
  public void onCloseRead()
  {
    InWebSocket reader = _reader;
    _reader = null;
/*
    if (reader != null) {
      reader.onDisconnectRead();
    }
    
    _wsConn.onDisconnectRead();
    */
  }

  @Override
  public void onClose()
  {
    InWebSocket reader = _reader;
    _reader = null;
    
    /*
    if (reader != null) {
      reader.onDisconnectRead();
    }

    _wsConn.onDisconnectRead();
    */
  }

  @Override
  public void onTimeout()
  {
    //_reader.onReadTimeout();

    // _wsConn.onDisconnectRead();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  /*
  private static class Connection implements EndpointConnection
  {
    private ConnectionSocket _connTcp;

    Connection(ConnectionSocket connTcp)
    {
      _connTcp = connTcp;
    }

    @Override
    public ReadStream getInputStream() throws IOException
    {
      return _connTcp.getReadStream();
    }

    @Override
    public WriteStream getOutputStream() throws IOException
    {
      return _connTcp.getWriteStream();
    }

    @Override
    public long getIdleReadTimeout()
    {
      return _connTcp.getIdleTimeout();
    }

    @Override
    public void setIdleReadTimeout(long timeout)
    {
      _connTcp.setIdleTimeout(timeout);
    }

    @Override
    public void disconnect()
    {
      //_controller.close();
    }

    @Override
    public void closeWrite()
    {
      try {
        _connTcp.getWriteStream().close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    @Override
    public QSocket getQSocket()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  */
}
