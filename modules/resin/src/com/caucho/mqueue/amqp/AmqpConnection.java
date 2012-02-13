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

package com.caucho.mqueue.amqp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.net.ProgressSource.State;

import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.HashKey;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class AmqpConnection implements ProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(AmqpConnection.class.getName());
  
  private AmqpProtocol _amqp;
  private SocketLink _link;
  
  private State _state = State.NEW;
  
  AmqpConnection(AmqpProtocol amqp, SocketLink link)
  {
    _amqp = amqp;
    _link = link;
  }
  
  @Override
  public String getProtocolRequestURL()
  {
    return "stomp:";
  }
  
  @Override
  public void init()
  {
  }
  
  SocketLink getLink()
  {
    return _link;
  }
  
  ReadStream getReadStream()
  {
    return _link.getReadStream();
  }
  
  WriteStream getWriteStream()
  {
    return _link.getWriteStream();
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    ReadStream is = _link.getReadStream();
    
    switch (_state) {
    case NEW:
      readVersion(is);
      break;
    default:
      System.out.println("UNKNOWN STATE: " + _state);
      break;
    }
    System.out.println("REQ:");

    return false;
  }
  
  private boolean readVersion(ReadStream is)
    throws IOException
  {
    if (is.read() != 'A'
        || is.read() != 'M'
        || is.read() != 'Q'
        || is.read() != 'P'
        || is.read() != 0) {
      System.out.println("ILLEGAL_HEADER:");
      throw new IOException();
    }
    
    int major = is.read() & 0xff;
    int minor = is.read() & 0xff;
    int version = is.read() & 0xff;
    
    if (major != 0x01 || minor != 0x00 || version != 0x00) {
      System.out.println("UNKNOWN_VERSION");
      throw new IOException();
    }
    
    WriteStream os = _link.getWriteStream();
    
    os.write('A');
    os.write('M');
    os.write('Q');
    os.write('P');
    os.write(0);
    os.write(0x01); // major
    os.write(0x00); // minor
    os.write(0x00); // version
    os.flush();
    
    return true;
  }
  
  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }

  @Override
  public boolean isWaitForRead()
  {
    return false;
  }

  @Override
  public void onCloseConnection()
  {
    System.out.println("CLOSE");
  }

  @Override
  public void onStartConnection()
  {
    _state = State.NEW;
  }
  
  private enum State {
    NEW,
    VERSION;
  }
}
