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

package com.caucho.jms.amq;

import com.caucho.server.connection.Connection;
import com.caucho.server.port.ServerRequest;
import com.caucho.util.ByteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Protocol specific information for each request.  ServerRequest
 * is reused to reduce memory allocations.
 *
 * <p>ServerRequests are created by Server.createRequest()
 */
public class AmqRequest extends AmqConnection implements ServerRequest
{
  private static final Logger log =
    Logger.getLogger(AmqRequest.class.getName());

  private static final byte []AMQP_HEADER = new byte[] {
    (byte) 'A', (byte) 'M', (byte) 'Q', (byte) 'P',
    1, 1, 9, 1
  };

  private Connection _conn;
  private ClassLoader _loader;

  private ByteBuffer _packet = new ByteBuffer();
  
  AmqRequest(Connection conn, ClassLoader loader)
  {
    _conn = conn;
    _loader = loader;
  }
  
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  public boolean isWaitForRead()
  {
    return false;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   *
   * @param conn Information about the connection, including buffered
   * read and write streams.
   */
  public boolean handleRequest() throws IOException
  {
    _is = _conn.getReadStream();
    _os = _conn.getWriteStream();

    return doRequest();
  }

  protected boolean doHello()
    throws IOException
  {
    if (_is.read() != 'M'
	|| _is.read() != 'Q'
	|| _is.read() != 'P'
	|| _is.read() != 1
	|| _is.read() != 1
	|| _is.read() != 9
	|| _is.read() != 1) {
      _os.write(AMQP_HEADER, 0, AMQP_HEADER.length);
      _os.flush();

      return false;
    }

    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_START);
    _packet.add(9); // major
    _packet.add(1); // minor
    addTable(_packet, null); // system properties
    addLongString(_packet, "PLAIN"); // security mechanisms
    addLongString(_packet, "en_US"); // locales

    writePacket(FRAME_METHOD, 0, _packet);

    return true;
  }

  protected boolean doConnectionStartOk(InputStream is)
    throws IOException
  {
    HashMap<String,String> props = readTable(is);

    String auth = readShortString(is);
    String credentials = readLongString(is);
    String locale = readShortString(is);

    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_TUNE);
    _packet.addShort(256); // max # of channels
    _packet.addInt(MAX_FRAME); // frame max
    _packet.addShort(HEARTBEAT); // minor

    writePacket(FRAME_METHOD, 0, _packet);
    
    return true;
  }

  protected boolean doConnectionTuneOk(InputStream is)
    throws IOException
  {
    int channelMax = readShort(is);
    int frameMax = readInt(is);

    if (frameMax < 4096 || MAX_FRAME < frameMax)
      return fatalProtocolError(frameMax + " is an invalid frame size");

    int heartbeat = readShort(is);
    
    return true;
  }

  protected boolean doConnectionOpen(InputStream is)
    throws IOException
  {
    String host = readShortString(is);

    System.out.println("VHOST: " + host);

    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_OPEN_OK);
    addShortString(_packet, "");

    writePacket(FRAME_METHOD, 0, _packet);
    
    return true;
  }

  protected boolean doChannelOpen(int id, InputStream is)
    throws IOException
  {
    int prefetch = readInt(is);
    String oob = readShortString(is);
    
    synchronized (_channels) {
      if (_channels[id] != null)
	return fatalProtocolError(id + " is an existing channel");

      _channels[id] = new AmqServerChannel(this, id);

      // set prefetch
    }

    _packet.clear();
    _packet.addShort(CLASS_CHANNEL);
    _packet.addShort(ID_CHANNEL_OPEN_OK);

    writePacket(FRAME_METHOD, id, _packet);
    
    return true;
  }
  
  /**
   * Handles a connection resume.
   */
  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }
  
  public void protocolCloseEvent()
  {
  }
}
