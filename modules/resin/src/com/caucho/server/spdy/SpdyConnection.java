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

package com.caucho.server.spdy;

import java.io.IOException;

import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.AmqpReceiverFactory;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * SPDY connection
 */
class SpdyConnection extends AbstractProtocolConnection {
  private static final L10N L = new L10N(SpdyConnection.class);
  
  public static final int CONTROL_FLAG = 0x80;
  
  private SpdyServerProtocol _spdy;
  private TcpSocketLink _link;
  
  SpdyConnection(SpdyServerProtocol spdy, SocketLink link)
  {
    _spdy = spdy;
    _link = (TcpSocketLink) link;
  }
  
  @Override
  public boolean isWaitForRead()
  {
    return true;
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    ReadStream rs = _link.getReadStream();
    
    int ch = rs.read();
    
    if (ch < 0) {
      return false;
    }

    if ((ch & CONTROL_FLAG) != 0) {
      parseControlFrame(rs, ch);
    }
    else {
      parseDataFrame(rs, ch);
    }
    
    _link.getWriteStream().println("HELLO");
    System.out.println("REQ!");
    
    return false;
  }
  
  private void parseControlFrame(ReadStream rs, int ch)
    throws IOException
  {
    int version = ((ch & 0x7f) << 8) + rs.read();
    
    System.out.println("CONT: V:" + version);
    
    if (version != 3) {
      throw new IOException(L.l("Unknown SPDY version %d\n", version));
    }

  }
  
  private void parseDataFrame(ReadStream rs, int ch)
    throws IOException
  {
    
  }
}
