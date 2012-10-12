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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.AmqpReceiverFactory;
import com.caucho.env.actor.ActorQueue;
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
  private static final Logger log
    = Logger.getLogger(SpdyConnection.class.getName());
  
  private static final L10N L = new L10N(SpdyConnection.class);
  
  public static final int CONTROL_FLAG = 0x80;
  
  public static final int SYN_STREAM = 1; 
  public static final int SYN_REPLY = 2; 
  public static final int RST_STREAM = 3;
  public static final int SETTINGS = 4;
  public static final int PING = 6;
  public static final int GOAWAY = 7;
  public static final int HEADERS = 8;
  public static final int WINDOW_UPDATE = 9;
  
  private SpdyServerProtocol _spdy;
  private TcpSocketLink _link;
  
  private SpdyWriteActor _actor;
  
  private int _lastServerId;
  
  SpdyConnection(SpdyServerProtocol spdy, SocketLink link)
  {
    _spdy = spdy;
    _link = (TcpSocketLink) link;
    
    _actor = new SpdyWriteActor(this);
  }
  
  WriteStream getWriteStream()
  {
    return _link.getWriteStream();
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
      return parseControlFrame(rs, ch);
    }
    else {
      return parseDataFrame(rs, ch);
    }
  }
  
  private boolean parseControlFrame(ReadStream rs, int ch)
    throws IOException
  {
    int version = ((ch & 0x7f) << 8) + rs.read();
    
    if (version != 3) {
      throw new IOException(L.l("Unknown SPDY version {0}\n", version));
    }
    
    int type = readShort(rs);
    int flags = rs.read();
    int length = readInt24(rs);
    
    long pos = rs.getPosition();
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " type=0x" + Integer.toHexString(type)
                + " flags=0x" + Integer.toHexString(flags)
                + " len=" + length + " version=" + version);
    }
    
    boolean isValid = false;
    
    switch (type) {
    case SYN_STREAM:
      isValid = synStream(rs, flags, length);
      break;
      
    default:
      throw new IOException(L.l("Unknown SPDY code {0}\n", type));
    }

    long tailPos = rs.getPosition();
    long endPos = pos + length;
      
    if (endPos < tailPos) {
      throw new IOException(L.l("Malformed length {0}\n", length));
    }
    else if (tailPos < endPos) {
      rs.skip((int) (endPos - tailPos));
    }
    
    return isValid;
  }
  
  private boolean synStream(ReadStream is, int flags, int length)
    throws IOException
  {
    int clientId = readInt(is);
    int assocStreamId = readInt(is);
    
    int pri = is.read() >> 5;
    int credentialSlot = is.read();
    
    System.out.println("SYN-ST: " + clientId);
    
    int serverId = _lastServerId + 2;
    _lastServerId = serverId;
    
    SpdyStream stream = new SpdyStream(this, clientId, serverId,
                                       is, _link.getWriteStream());
    
    int headerLen = stream.readHeaderInt();
    
    for (int i = 0; i < headerLen; i++) {
      int keyLen = stream.readHeaderInt();
      String key = stream.readHeaderString(keyLen);
      
      int valueLen = stream.readHeaderInt();
      String value = stream.readHeaderString(valueLen);
      
      System.out.println("HEADER: " + key + ": " + value);
    }
    
    _actor.writeStreamReply(stream);
    
    return true;
  }
  
  private int readShort(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();
      
    if (ch2 < 0)
      return -1;
    
    return (ch1 << 8) + ch2;
  }
  
  private int readInt24(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
      
    if (ch3 < 0)
      return -1;
    
    return (ch1 << 16) + (ch2 << 8) + ch3;
  }
  
  private int readInt(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
    int ch4 = is.read();
      
    if (ch4 < 0)
      return -1;
    
    return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
  }
  
  private boolean parseDataFrame(ReadStream rs, int ch)
    throws IOException
  {
    return false;
  }

  /**
   * 
   */
  public void close()
  {
    // TODO Auto-generated method stub
    
  }
}
