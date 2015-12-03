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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.QSocketWrapper;

/**
 * Standard TCP network system.
 */
public class TcpNetworkSystem extends NetworkSystem {
  @Override
  public QServerSocket openServerSocket(InetAddress address,
                                        int port,
                                        int backlog,
                                        boolean isJni)
    throws IOException
  {
    return QJniServerSocket.create(address, port, backlog, isJni);
  }
  
  @Override
  public QSocket connect(InetSocketAddress addr,
                         long connectTimeout)
    throws IOException
  {
    Socket s = new Socket();

    if (connectTimeout > 0)
      s.connect(addr, (int) connectTimeout);
    else
      s.connect(addr);

    if (! s.isConnected())
      throw new IOException("connection timeout");
    
    return new QSocketWrapper(s);
  }
}

