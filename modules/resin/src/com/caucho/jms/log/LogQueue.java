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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.log;

import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.JMSException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import com.caucho.log.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.TempBuffer;

import com.caucho.config.ConfigException;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.selector.Selector;

/**
 * A log queue.
 */
public class LogQueue extends AbstractDestination
  implements Queue {
  static final Logger log = Log.open(LogQueue.class);
  static final L10N L = new L10N(LogQueue.class);

  private static final byte []RESIN = new byte[] { 'R', 'E', 'S', 'I', 'N' };

  ArrayList<Message> _queue = new ArrayList<Message>();

  private String _queueName;
  private Selector _selector;

  private Path _basePath;
  private long _logFileSize = 10 * 1024 * 1024;

  private Path _pathA;
  private long _lengthA;
  private TempOutputStream _tempA = new TempOutputStream();
  private WriteStream _writeA;
  private TempStream _outA = new TempStream();

  public LogQueue()
  {
  }

  /**
   * Sets the queue's base log file.
   */
  public void setPath(Path path)
  {
    _basePath = path;
  }

  /**
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return _queueName;
  }

  /**
   * Sets the queue's name.
   */
  public void setQueueName(String name)
  {
    _queueName = name;
  }

  /**
   * Sets the queue's selector.
   */
  public void setSelector(Selector selector)
  {
    _selector = selector;
  }

  /**
   * Gets the queue's selector.
   */
  public Selector getSelector()
  {
    return _selector;
  }

  /**
   * Initialize the queue.
   */
  public void init()
    throws ConfigException, IOException
  {
    if (_basePath == null)
      throw new ConfigException(L.l("LogQueue requires a <path> element."));

    if (_basePath.isDirectory())
      throw new ConfigException(L.l("<path> must be a file prefix, not a directory."));

    _basePath.getParent().mkdirs();

    String tail = _basePath.getTail();

    _pathA = _basePath.getParent().lookup(tail + "_a");
    _lengthA = _pathA.getLength();
    _writeA = _pathA.openAppend();
  }

  /**
   * Sends the message to the queue.
   */
  public void send(Message message)
    throws JMSException
  {
    if (_selector != null && ! _selector.isMatch(message))
      return;

    long sequenceId = nextConsumerSequenceId();

    if (log.isLoggable(Level.FINE))
      log.fine("jms log queue:" + _queueName + " send message " + sequenceId);

    try {
      synchronized (_tempA) {
	_tempA.clearWrite();
	_tempA.write('S');
	int offset = _tempA.getLength();
	writeInt(_tempA, 0);
	writeLong(_tempA, message.getJMSExpiration());

	ObjectOutputStream oos = new ObjectOutputStream(_tempA);

	oos.writeObject(message);

	oos.close();

	int length = _tempA.getLength() - offset;
	writeInt(_tempA, length);
	_tempA.write(RESIN, 0, RESIN.length);

	TempBuffer ptr = _tempA.getHead();
	if (ptr != null) {
	  byte []buffer = ptr.getBuffer();
	  buffer[1] = (byte) (length >> 24);
	  buffer[2] = (byte) (length >> 16);
	  buffer[3] = (byte) (length >> 8);
	  buffer[4] = (byte) (length);
	  
	  for (; ptr != null; ptr = ptr.getNext()) {
	    _writeA.write(ptr.getBuffer(), 0, ptr.getLength());
	  }
	  // XXX: eventually needs more complicated flush.
	  _writeA.flush();
	  
	  _tempA.clearWrite();
	  
	}
      }
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    messageAvailable();
  }

  /**
   * Removes the first message matching the selector.
   */
  public Message receive(Selector selector)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Message message = _queue.get(i);

	if (selector == null || selector.isMatch(message)) {
	  _queue.remove(i);
	  return message;
	}
      }
    }

    return null;
  }

  /**
   * Returns an enumeration of the matching messages.
   */
  public Enumeration getEnumeration(Selector selector)
  {
    return new BrowserEnumeration(this, selector);
  }

  /**
   * Removes the first message matching the selector.
   */
  private boolean hasMessage(Selector selector)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Message message = _queue.get(i);

	if (selector == null || selector.isMatch(message))
	  return true;
      }
    }

    return false;
  }

  /**
   * Writes an integer.
   */
  private void writeInt(OutputStream os, int value)
    throws IOException
  {
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }

  /**
   * Writes a long
   */
  private void writeLong(OutputStream os, long value)
    throws IOException
  {
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) value);
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "MemoryQueue[" + _queueName + "]";
  }

  static class BrowserEnumeration implements Enumeration {
    private LogQueue _queue;
    private Selector _selector;

    BrowserEnumeration(LogQueue queue, Selector selector)
    {
      _queue = queue;
      _selector = selector;
    }

    public boolean hasMoreElements()
    {
      try {
	return _queue.hasMessage(_selector);
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    public Object nextElement()
    {
      try {
	return _queue.receive(_selector);
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  static class TempOutputStream extends OutputStream {
    private TempStream _tempStream = new TempStream();
    private byte []_oneBuf = new byte[1];
    private int _length;

    void clearWrite()
    {
      _tempStream.clearWrite();
    }

    TempBuffer getHead()
    {
      return _tempStream.getHead();
    }

    /**
     * Writes a byte to the temp stream.
     */
    public void write(int ch)
      throws IOException
    {
      _length++;
      _oneBuf[0] = (byte) ch;
      _tempStream.write(_oneBuf, 0, 1, false);
    }

    /**
     * Writes a buffer to the temp stream.
     */
    public void write(byte []buffer, int offset, int length)
      throws IOException
    {
      _length += length;
      _tempStream.write(buffer, offset, length, false);
    }

    /**
     * Returns the current length.
     */
    int getLength()
    {
      return _length;
    }
  }
}

