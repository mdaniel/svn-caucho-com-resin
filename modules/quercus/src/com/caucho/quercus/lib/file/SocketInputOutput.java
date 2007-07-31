/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.Env;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents read/write stream
 */
public class SocketInputOutput
  extends AbstractBinaryInputOutput
{
  private static final Logger log
    = Logger.getLogger(SocketInputOutput.class.getName());
  
  public enum Domain { AF_INET, AF_INET6, AF_UNIX };

  private int _lastError;
  private Domain _domain;
  private Socket _socket;

  public SocketInputOutput(Env env, Socket socket, Domain domain)
  {
    super(env);
    
    env.addClose(this);
    
    _socket = socket;
    _domain = domain;
  }

  public void bind(SocketAddress address)
    throws IOException
  {
    _socket.bind(address);
  }

  public void connect(SocketAddress address)
    throws IOException
  {
    _socket.connect(address);

    init();
  }

  public void setError(int error)
  {
    _lastError = error;
  }

  public void init()
  {
    SocketStream sock = new SocketStream(_socket);

    WriteStream os = new WriteStream(sock);
    ReadStream is = new ReadStream(sock, os);

    init(is, os);
  }

  public void setTimeout(long timeout)
  {
    try {
      if (_socket != null)
	_socket.setSoTimeout((int) timeout);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Closes the stream.
   */
  public void close()
  {
    super.close();
    
    Socket s = _socket;
    _socket = null;

    try {
      if (s != null)
        s.close();
    } catch (IOException e) {
    }
  }

  public String toString()
  {
    if (_socket != null)
      return "SocketReadWrite[" + _socket.getInetAddress() + "," + _socket.getPort() + "]";
    else
      return "SocketReadWrite[closed]";
  }
}

