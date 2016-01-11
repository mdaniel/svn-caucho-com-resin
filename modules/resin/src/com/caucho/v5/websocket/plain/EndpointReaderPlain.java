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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;

import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.websocket.common.EndpointReaderWebSocket;
import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketConstants;

/**
 * User facade for http requests.
 */
@ModulePrivate
public class EndpointReaderPlain
  implements EndpointReaderWebSocket
{
  private static final Logger log
    = Logger.getLogger(EndpointReaderPlain.class.getName());
  
  private final Endpoint _endpoint;
  private final SessionWebSocketPlain _session;
  private final FrameInputStream _is;
  
  private int _op;
  
  public EndpointReaderPlain(Endpoint endpoint,
                             SessionWebSocketPlain session,
                             FrameInputStream is)
  {
    Objects.requireNonNull(is);
    Objects.requireNonNull(session);
    
    _endpoint = endpoint;
    _session = session;
    _is = is;
  }
  
  //
  // duplex callbacks
  //

  @Override
  public void init(ReadStream is)
    throws IOException
  {
    Objects.requireNonNull(is);

    //_is.init(_session, is);
  }
  
  @Override
  public boolean isReadAvailable()
  {
    try {
      return _is.available() > 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean onRead()
    throws IOException
  {
    boolean isValid = false;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_session.getClassLoader());
      
      _session.beforeBatch();
      
      do {
        if (! _session.isOpenRead() || ! readFrame()) {
          return false;
        }
        
        isValid = true;
      } while (_is.available() > 0);
    } catch (IOException e) {
      if (! _session.isOpen()) {
        log.log(Level.FINEST, e.toString(), e);
        return false;
      }
      else {
        throw e;
      }
    } finally {
      _session.afterBatch();
      
      thread.setContextClassLoader(oldLoader);
    }
    
    return isValid;
  }
  
  private boolean readFrame()
    throws IOException
  {  
    if (! _is.readFrameHeader()) {
      // disconnect();
      
      return false;
    }

    int opcode = _is.getOpcode();

    switch (opcode) {
    case WebSocketConstants.OP_BINARY:
      _session.getBinaryHandler().onRead(_is);
      _op = opcode;
      break;

    case WebSocketConstants.OP_TEXT:
      _session.getTextHandler().onRead(_is);
      _op = opcode;
      break;
      
    case WebSocketConstants.OP_CONT:
      if (_op == WebSocketConstants.OP_BINARY) {
        _session.getBinaryHandler().onRead(_is);
      }
      else if (_op == WebSocketConstants.OP_TEXT) {
        _session.getTextHandler().onRead(_is);
      }
      else {
        log.fine(this + " unexpected opcode " + opcode);

        CloseReason reason
          = new CloseReason(CloseCodes.PROTOCOL_ERROR, "unexpected opcode");
          
        _session.onClose(reason);
        _session.close(reason);
      }
      break;
      
    case -1:
    {
      log.fine(this + " unexpected disconnect");

      CloseReason reason
        = new CloseReason(CloseCodes.GOING_AWAY, "disconnect");
        
      _session.onClose(reason);
      _session.close(reason);
      return false;
    }

    default:
    {
      log.fine(this + " unexpected opcode " + opcode);

      CloseReason reason
        = new CloseReason(CloseCodes.PROTOCOL_ERROR, "unexpected opcode");
        
      _session.onClose(reason);
      _session.close(reason);
      return false;
    }
    }
    
    if (_is.isFinal()) {
      _op = -1;
    }
    
    return true;
  }

  @Override
  public void onReadTimeout()
  {
    // _listener.onTimeout(this);
  }

  @Override
  public void onDisconnectRead()
  {
    CloseReason reason = new CloseReason(CloseCodes.CLOSED_ABNORMALLY, "disconnect");
    
    _session.writeDisconnect();
    _session.onClose(reason);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
