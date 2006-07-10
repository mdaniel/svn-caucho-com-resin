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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.vfs.*;
import com.caucho.util.*;

/**
 * AMQ client.
 */
public class AmqClient extends AmqConnection {
  private static final Logger log
    = Logger.getLogger(AmqClient.class.getName());

  private static final byte []AMQP_HEADER = new byte[] {
    (byte) 'A', (byte) 'M', (byte) 'Q', (byte) 'P',
    1, 1, 9, 1
  };

  private static final int MAX_PREFETCH = 0;

  private ClassLoader _loader = Thread.currentThread().getContextClassLoader();
  
  private String _host;
  private int _port;
  private Path _path;

  private ByteBuffer _packet = new ByteBuffer();

  private ReadThread _readThread;

  AmqClient(String host, int port)
  {
    _host = host;
    _port = port;

    _path = Vfs.lookup("tcp://" + host + ":" + port);
  }
  
  /**
   * Opens a new channel.
   */
  public AmqClientChannel openChannel()
    throws IOException
  {
    connect();

    ByteBuffer packet = new ByteBuffer();
    packet.addShort(CLASS_CHANNEL);
    packet.addShort(ID_CHANNEL_OPEN);
    packet.addInt(MAX_PREFETCH);
    addShortString(packet, "");
    
    AmqClientChannel channel = new AmqClientChannel(this);

    int channelId = addChannel(channel);
    System.out.println("I: " + channelId);

    writePacket(FRAME_METHOD, channelId, packet);

    channel.waitOpen();

    return channel;
  }

  private void connect()
    throws IOException
  {
    synchronized (this) {
      if (_readThread != null)
	return;

      log.fine("AMQ client connecting to " + _path);
    
      ReadWritePair pair = _path.openReadWrite();

      _is = pair.getReadStream();
      _os = pair.getWriteStream();

      _os.write(AMQP_HEADER, 0, AMQP_HEADER.length);

      doRequest();
    }
  }

  // type=byte,empty=byte,channel=short,size=int   data  frame-end=byte

  protected boolean doConnectionStart(InputStream is)
    throws IOException
  {
    int major = is.read();
    int minor = is.read();

    HashMap<String,String> props = readTable(is);

    String security = readLongString(is);
    String locales = readLongString(is);
    
    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_START_OK);

    addTable(_packet, null);
    addShortString(_packet, "PLAIN"); // auth method
    addLongString(_packet, "harry:quidditch"); // auth credentials
    addShortString(_packet, "en_US"); // locale

    writePacket(FRAME_METHOD, 0, _packet);

    return doRequest();
  }

  protected boolean doConnectionTune(InputStream is)
    throws IOException
  {
    int channelMax = readShort(is);
    int frameMax = readInt(is);
    int heartbeat = readShort(is);
    
    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_TUNE_OK);

    _packet.addShort(256);
    _packet.addInt(MAX_FRAME);
    _packet.addShort(HEARTBEAT);

    writePacket(FRAME_METHOD, 0, _packet);
    
    _packet.clear();
    _packet.addShort(CLASS_CONNECTION);
    _packet.addShort(ID_CONNECTION_OPEN);

    addShortString(_packet, "/" + _host + ":" + _port); // virtual host
    addShortString(_packet, ""); // capabilities
    _packet.add(0); // insist

    writePacket(FRAME_METHOD, 0, _packet);

    return doRequest();
  }

  protected boolean doConnectionOpenOk(InputStream is)
    throws IOException
  {
    String hosts = readShortString(is);

    _readThread = new ReadThread();

    log.fine("AMQ: openOk(" + _host + ":" + _port + ")");
    System.out.println("OPEN:");
    // also the heartbeak
    
    ThreadPool.schedule(_readThread);

    return true;
  }

  public void close()
  {
    try {
      WriteStream os = _os;
      _os = null;
      
      if (os != null)
	os.close();
    } catch (Throwable e) {
    }
    
    try {
      ReadStream is = _is;
      _is = null;
      
      if (is != null)
	is.close();
    } catch (Throwable e) {
    }

    ReadThread readThread = _readThread;
    _readThread = null;
    
    if (readThread != null)
      readThread.close();
  }

  class ReadThread implements Runnable {
    private Thread _thread;
    
    public void run()
    {
      _thread = Thread.currentThread();
      try {
	while (_is != null && doRequest()) {
	}
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      } finally {
	_thread = null;
      }
    }

    void close()
    {
      Thread thread = _thread;

      if (thread != null)
	thread.interrupt();
    }
  }
}
