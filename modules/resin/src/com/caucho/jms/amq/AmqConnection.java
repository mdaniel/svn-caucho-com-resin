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

import com.caucho.util.ByteBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Common for client and server
 */
public class AmqConnection implements AmqConstants {
  private static final Logger log =
    Logger.getLogger(AmqConnection.class.getName());

  // denial-of-service sleep time
  private static final long DOS_TIME = 5000L;
  protected static final int MAX_FRAME = 65536;
  protected static final int HEARTBEAT = 10;

  private static final byte []AMQP_HEADER = new byte[] {
    (byte) 'A', (byte) 'M', (byte) 'Q', (byte) 'P',
    1, 1, 9, 1
  };

  private static final int CONNECTION_START
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_START;
  private static final int CONNECTION_START_OK
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_START_OK;
  private static final int CONNECTION_TUNE
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_TUNE;
  private static final int CONNECTION_TUNE_OK
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_TUNE_OK;
  private static final int CONNECTION_OPEN
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_OPEN;
  private static final int CONNECTION_OPEN_OK
    = (CLASS_CONNECTION << 16) + ID_CONNECTION_OPEN_OK;

  protected AmqChannel []_channels = new AmqChannel[256];

  protected ReadStream _is;
  protected WriteStream _os;

  private int _maxFrameSize = MAX_FRAME;

  private ByteBuffer _packet = new ByteBuffer();

  /**
   * Sets a channel callback.
   */
  protected int addChannel(AmqChannel channel)
  {
    int id;

    synchronized (_channels) {
      for (int i = 1; i < _channels.length; i++) {
	if (_channels[i] == null) {
	  channel.setId(i);
	  _channels[i] = channel;

	  return i;
	}
      }
    }

    return 0;
  }
  
  /**
   * Handles a new request.  The input stream and output stream are
   * already initialized.
   */
  protected boolean doRequest() throws IOException
  {
    ReadStream is = _is;
    
    int code = is.read();

    switch (code) {
    case -1:
      close();
      return false;
      
    case 'A':
      return doHello();

    case FRAME_METHOD:
      {
	int cycle = is.read();
	int channel = 256 * is.read() + is.read();
	int size = ((is.read() << 24)
		    + (is.read() << 16)
		    + (is.read() << 8)
		    + (is.read()));
	int methodClass = 256 * is.read() + is.read();
	int methodId = 256 * is.read() + is.read();

	if (_maxFrameSize < size)
	  return fatalProtocolError("Frame size too large at " + size);

	_packet.clear();
	_packet.ensureCapacity(size - 4);
	is.read(_packet.getBuffer(), 0, size - 4);
	_packet.setLength(size - 4);
	int end = is.read();

	if (end != FRAME_END) {
	  return fatalProtocolError("Bad packet end at 0x" + Integer.toHexString(end));
	}

	return doMethod(channel, methodClass, methodId, _packet);
      }

    case FRAME_HEADER:
      {
	int cycle = is.read();
	int channelId = 256 * is.read() + is.read();
	int size = ((is.read() << 24)
		    + (is.read() << 16)
		    + (is.read() << 8)
		    + (is.read()));
	int classId = 256 * is.read() + is.read();
	int weight = 256 * is.read() + is.read();
	int bodySize = ((is.read() << 56)
			+ (is.read() << 48)
			+ (is.read() << 40)
			+ (is.read() << 32)
			+ (is.read() << 24)
			+ (is.read() << 16)
			+ (is.read() << 8)
			+ (is.read()));
	
	_packet.clear();
	_packet.ensureCapacity(size - 12);
	is.read(_packet.getBuffer(), 0, size - 12);
	_packet.setLength(size - 12);
	int end = is.read();

	if (end != FRAME_END) {
	  return fatalProtocolError("Bad packet end at 0x" + Integer.toHexString(end));
	}

	if (channelId <= 0 || _channels.length <= channelId)
	  return fatalProtocolError("header illegal channel at " + channelId);

	AmqChannel channel = _channels[channelId];
	if (_channels[channelId] == null)
	  return fatalProtocolError("header  illegal channel at " + channelId);

	return channel.doHeader(classId, weight, bodySize, _packet.createInputStream());
      }

    case FRAME_BODY:
      {
	int cycle = is.read();
	int channelId = 256 * is.read() + is.read();
	int size = ((is.read() << 24)
		    + (is.read() << 16)
		    + (is.read() << 8)
		    + (is.read()));

	if (channelId <= 0 || _channels.length <= channelId)
	  return fatalProtocolError("header illegal channel at " + channelId);

	AmqChannel channel = _channels[channelId];
	if (_channels[channelId] == null)
	  return fatalProtocolError("header  illegal channel at " + channelId);

	while (size > 0) {
	  Chunk chunk = new Chunk(size);

	  byte []buffer = chunk.getBuffer();
	  int offset = chunk.getOffset();

	  int sublen = is.read(buffer, offset, buffer.length - offset);

	  if (sublen < 0)
	    return false;

	  chunk.setOffset(offset + sublen);

	  channel.addChunk(chunk, offset, sublen);

	  size -= sublen;
	}
	
	int end = is.read();

	if (end != FRAME_END) {
	  return fatalProtocolError("Bad packet end at 0x" + Integer.toHexString(end));
	}

	channel.endContentFrame();

	return true;
      }
      
    default:
      System.out.println("BOGUS:" + code);
      return false;
    }
  }

  private boolean doMethod(int channel,
			   int methodClass, int methodId,
			   ByteBuffer packet)
    throws IOException
  {
    System.out.println("METHOD: " + methodClass + "." + methodId);
    
    switch (methodClass) {
    case CLASS_CONNECTION:
      if (channel != 0)
	return fatalProtocolError("Connection requires channel 0 at " +
				  channel);
      
      switch (methodId) {
      case ID_CONNECTION_START:
	return doConnectionStart(packet.createInputStream());
      
      case ID_CONNECTION_START_OK:
	return doConnectionStartOk(packet.createInputStream());
      
      case ID_CONNECTION_TUNE:
	return doConnectionTune(packet.createInputStream());
      
      case ID_CONNECTION_TUNE_OK:
	return doConnectionTuneOk(packet.createInputStream());
      
      case ID_CONNECTION_OPEN:
	return doConnectionOpen(packet.createInputStream());
      
      case ID_CONNECTION_OPEN_OK:
	return doConnectionOpenOk(packet.createInputStream());
      }
      break;
      
    case CLASS_CHANNEL:
      {
	if (channel <= 0 || _channels.length <= channel)
	  return fatalProtocolError(methodClass + "." + methodId + " illegal channel at " + channel);

	if (methodId == ID_CHANNEL_OPEN)
	  return doChannelOpen(channel, packet.createInputStream());
      
	AmqChannel channelCallback = _channels[channel];
	if (channelCallback == null)
	  return fatalProtocolError(methodClass + "." + methodId + " illegal channel at " + channel);
      
	switch (methodId) {
	case ID_CHANNEL_OPEN_OK:
	  return channelCallback.doOpenOk(packet.createInputStream());
	}
      }
      break;

    case CLASS_QUEUE:
      {
	if (channel <= 0 || _channels.length <= channel)
	  return fatalProtocolError(methodClass + '.' + methodId + " illegal channel at " + channel);
      
	AmqChannel channelCallback = _channels[channel];
	if (channelCallback == null)
	  return fatalProtocolError(methodClass + '.' + methodId + " illegal channel at " + channel);

	switch (methodId) {
	case ID_QUEUE_DECLARE:
	  return channelCallback.doQueueDeclare(packet.createInputStream());
	  
	case ID_QUEUE_DECLARE_OK:
	  return channelCallback.doQueueDeclareOk(packet.createInputStream());
	}
      }
      break;

    case CLASS_BASIC:
      {
	if (channel <= 0 || _channels.length <= channel)
	  return fatalProtocolError(methodClass + '.' + methodId + " illegal channel at " + channel);
      
	AmqChannel channelCallback = _channels[channel];
	if (channelCallback == null)
	  return fatalProtocolError(methodClass + '.' + methodId + " illegal channel at " + channel);

	switch (methodId) {
	case ID_BASIC_PUBLISH:
	  return channelCallback.doBasicPublish(packet.createInputStream());
	}
      }
      break;
    }

    System.out.println("UNKNOWN METHOD: " + methodClass + "." + methodId);
      
    return fatalProtocolError("Unknown method " + methodClass + "." + methodId);
  }

  protected boolean doHello()
    throws IOException
  {
    return fatalProtocolError("doHello() should not be called");
  }

  protected boolean doConnectionStart(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionStart() should not be called");
  }

  protected boolean doConnectionStartOk(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionStartOk() should not be called");
  }

  protected boolean doConnectionTune(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionTune() should not be called");
  }

  protected boolean doConnectionTuneOk(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionTuneOk() should not be called");
  }

  protected boolean doConnectionOpen(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionOpen() should not be called");
  }

  protected boolean doConnectionOpenOk(InputStream is)
    throws IOException
  {
    return fatalProtocolError("doConnectionOpenOk() should not be called");
  }

  protected boolean doChannelOpen(int id, InputStream is)
    throws IOException
  {
    return fatalProtocolError("doChannelOpen() should not be called");
  }

  protected boolean fatalProtocolError(String msg)
    throws IOException
  {
    System.out.println("AMQ: " + msg);
    log.warning("AMQ: " + msg);

    try {
      Thread.sleep(DOS_TIME);
    } catch (InterruptedException e) {
    }
    
    close();

    return false;
  }

  protected final void writePacket(int frame, int channel, ByteBuffer packet)
    throws IOException
  {
    WriteStream os = _os;

    if (os == null)
      return;

    synchronized (os) {
      os.write(frame);
      os.write(CYCLE_TBD);
      os.write(channel >> 8);
      os.write(channel);
      writeInt(os, packet.size());
      os.write(packet.getBuffer(), 0, packet.getLength());
      os.write(FRAME_END);
    }

    Thread.yield();

    synchronized (os) {
      os.flush(); // XXX: batch?
    }
  }

  protected final void writeData(int channel, long length, InputStream is)
    throws IOException
  {
    WriteStream os = _os;

    while (length > 0) {
      synchronized (os) {
	os.write(FRAME_BODY);
	os.write(CYCLE_TBD);
	os.write(channel >> 8);
	os.write(channel);

	int offset = os.getBufferOffset() + 4;
	byte []buffer = os.getBuffer();

	int sublen = buffer.length - offset - 1;
	if (sublen <= 0) {
	  os.flush();
	  offset = os.getBufferOffset() + 4;
	  buffer = os.getBuffer();
	  sublen = buffer.length - offset - 1;
	}
	if (length < sublen)
	  sublen = (int) length;

	sublen = is.read(buffer, offset, sublen);

	if (sublen <= 0)
	  throw new IOException("unexpected EOF");

	buffer[offset - 4] = (byte) (sublen >> 24);
	buffer[offset - 3] = (byte) (sublen >> 16);
	buffer[offset - 2] = (byte) (sublen >> 8);
	buffer[offset - 1] = (byte) (sublen);
	buffer[offset + sublen] = (byte) FRAME_END;

	os.setBufferOffset(offset + sublen + 1);

	length -= sublen;
      }

      Thread.yield();
    }

    synchronized (os) {
      os.flush(); // XXX: batch?
    }
  }

  protected final void addTable(ByteBuffer packet,
				HashMap<String,String> props)
  {
    packet.addShort(0);
  }

  protected final void addShort(ByteBuffer packet, String v)
  {
    packet.add(v.length());
    packet.addString(v);
  }

  protected final void addLongString(ByteBuffer packet, String v)
  {
    packet.addInt(v.length());
    packet.addString(v);
  }

  protected final void addShortString(ByteBuffer packet, String v)
  {
    packet.add(v.length());
    packet.addString(v);
  }

  protected final HashMap<String,String> readTable(InputStream is)
    throws IOException
  {
    int length = 256 * is.read() + is.read();

    return null;
  }
  
  protected final int readShort(InputStream is)
    throws IOException
  {
    return 256 * is.read() + is.read();
  }
  
  protected final int readInt(InputStream is)
    throws IOException
  {
    return ((is.read() << 24)
	    + (is.read() << 16)
	    + (is.read() << 8)
	    + (is.read()));
  }

  protected final String readLongString(InputStream is)
    throws IOException
  {
    int length = ((is.read() << 24)
		  + (is.read() << 16)
		  + (is.read() << 8)
		  + (is.read()));

    char []buf = new char[length];

    for (int i = 0; i < length; i++)
      buf[i] = (char) is.read();

    return new String(buf, 0, length);
  }

  protected final String readShortString(InputStream is)
    throws IOException
  {
    int length = is.read();

    char []buf = new char[length];

    for (int i = 0; i < length; i++)
      buf[i] = (char) is.read();

    return new String(buf, 0, length);
  }

  protected final void writeInt(WriteStream os, int v)
    throws IOException
  {
    os.write(v >> 24);
    os.write(v >> 16);
    os.write(v >> 8);
    os.write(v);
  }

  private final void writeShort(WriteStream os, int v)
    throws IOException
  {
    os.write(v >> 8);
    os.write(v);
  }
  
  public void close()
  {
    try {
      WriteStream os = _os;
      _os = null;

      if (os != null)
	os.close();
    } catch (IOException e) {
    }
    
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();
  }
}
