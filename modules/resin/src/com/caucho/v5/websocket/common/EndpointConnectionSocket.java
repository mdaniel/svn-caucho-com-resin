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

package com.caucho.v5.websocket.common;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.io.IoUtil;
import com.caucho.v5.vfs.SocketBar;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * Low level interface to the connection.
 */
public class EndpointConnectionSocket implements EndpointConnection {
  private static final Logger log
    = Logger.getLogger(EndpointConnectionSocket.class.getName());
  
  private Socket _s;
  private ReadStream _is;
  private WriteStream _os;
  private long _idleTimeout = 600 * 1000;
  
  public EndpointConnectionSocket(Socket s) throws IOException
  {
    _s = s;
    _is = Vfs.openRead(_s.getInputStream());
    _os = Vfs.openWrite(_s.getOutputStream());
  }

  public Socket getSocket()
  {
    return _s;
  }
  
  public SocketBar getQSocket()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public ReadStream getInputStream()
  {
    return _is;
  }
  
  @Override
  public WriteStream getOutputStream()
  {
    return _os;
  }
  
  @Override
  public long getIdleReadTimeout()
  {
    return _idleTimeout;
  }
  
  @Override
  public void setIdleReadTimeout(long timeout)
  {
    _idleTimeout = timeout;
  }

  @Override
  public void disconnect()
  {
    Socket s = _s;
    _s = null;
    
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;

    IoUtil.close(os);
    IoUtil.close(is);
    
    try {
      if (s != null) {
        s.close();
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  @Override
  public void closeWrite()
  {
    Socket s = _s;
    
    WriteStream os = _os;
    _os = null;

    IoUtil.close(os);
    
    try {
      if (s != null) {
        s.shutdownOutput();
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}
