/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.log;

import com.caucho.log.AbstractRolloverLog;
import com.caucho.util.Alarm;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an log of every top-level request to the server.
 */
public class AccessLogWriter extends AbstractRolloverLog implements Runnable
{
  protected static final L10N L = new L10N(AccessLogWriter.class);
  protected static final Logger log
    = Logger.getLogger(AccessLogWriter.class.getName());

  private static final int BUFFER_SIZE = 65536;
  private static final int BUFFER_GAP = 8 * 1024;

  private static final FreeList<AccessLogBuffer> _freeBuffers
    = new FreeList<AccessLogBuffer>(16);

  private final AccessLog _log;

  private final Object _bufferLock = new Object();
  private AccessLogBuffer _logBuffer;
  private byte []_buffer;
  private int _length;

  private boolean _hasThread;
  private boolean _isFlushing;

  // the write queue
  private int _maxQueueLength = 32;
  private final ArrayList<AccessLogBuffer> _writeQueue
    = new ArrayList<AccessLogBuffer>();

  private Thread _flushThread;
  
  AccessLogWriter(AccessLog log)
  {
    _log = log;

    _logBuffer = getLogBuffer();
    _buffer = _logBuffer.getBuffer();
    _length = 0;
  }
  
  private AccessLogBuffer getLogBuffer()
  {
    AccessLogBuffer buffer = _freeBuffers.allocate();

    if (buffer == null)
      buffer = new AccessLogBuffer();

    return buffer;
  }

  Object getBufferLock()
  {
    return _bufferLock;
  }

  /**
   * Returns the current buffer for shared-buffer.  _bufferLock
   * must be synchronized.
   */
  byte []getBuffer(int requiredLength)
  {
    if (_buffer.length - _length < requiredLength || isRollover()) {
      flush();
    }

    return _buffer;
  }

  /**
   * Returns the current buffer length for shared-buffer.  _bufferLock
   * must be synchronized.
   */
  int getLength()
  {
    return _length;
  }

  /**
   * Returns the current buffer length for shared-buffer.  _bufferLock
   * must be synchronized.
   */
  void setLength(int length)
  {
    _length = length;
  }

  void writeThrough(byte []buffer, int offset, int length)
  {
    writeBuffer(buffer, offset, length);
    flush();
  }

  void writeBuffer(byte []buffer, int offset, int length)
  {
    boolean isFlush = false;
    
    synchronized (_bufferLock) {
      byte []logBuffer = _buffer;
      int logLength = _length;
      
      if (buffer.length - logLength < length) {
	isFlush = flushBuffer();
	
	logBuffer = _buffer;
	logLength = _length;
      }

      if (logBuffer.length < length)
	length = logBuffer.length;

      System.arraycopy(buffer, offset, logBuffer, logLength, length);
      _length = logLength + length;
    }

    if (isFlush)
      flush();
  }

  // must be synchronized by _bufferLock.
  @Override
  protected void flush()
  {
    boolean isFlush = false;
    
    synchronized (_bufferLock) {
      isFlush = flushBuffer();
    }

    if (isFlush) {
      scheduleThread();
    }
  }

  /**
   * Must be called from inside _bufferLock
   */
  private boolean flushBuffer()
  {
    if (_length > 0) {
      _logBuffer.setLength(_length);
      _logBuffer = write(_logBuffer);
      _buffer = _logBuffer.getBuffer();
      _length = 0;

      return true;
    }
    else
      return false;
  }

  private AccessLogBuffer write(AccessLogBuffer logBuffer)
  {
    boolean isScheduleThread = false;
    
    while (true) {
      boolean isFlush = false;
      
      synchronized (_writeQueue) {
	if (_writeQueue.size() < _maxQueueLength) {
	  _writeQueue.add(logBuffer);

	  break;
	}
	else if (! _isFlushing) {
	  _isFlushing = true;
	  isFlush = true;
	}
	else {
	  try {
	    _writeQueue.wait();
	  } catch (Exception e) {
	  }
	}
      }

      if (isFlush) {
	try {
	  // If the queue is full, call the flush code directly
	  // since the thread pool may be out of threads for
	  // a schedule
	  log.fine("AccessLogWriter flushing log directly.");
	      
	  runImpl();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	} finally {
	  _isFlushing = false;
	}
      }
    }
    
    AccessLogBuffer buffer = _freeBuffers.allocate();

    if (buffer == null)
      buffer = new AccessLogBuffer();

    return buffer;
  }

  private void scheduleThread()
  {
    boolean isScheduleThread = false;
    
    synchronized (_writeQueue) {
      if (_hasThread)
	wake();
      else {
	_hasThread = true;
	isScheduleThread = true;
      }
    }

    if (isScheduleThread) {
      ThreadPool.getThreadPool().schedulePriority(this);
    }
  }

  protected void waitForFlush(long timeout)
  {
    long expire;

    if (! Alarm.isTest())
      expire = Alarm.getCurrentTime() + timeout;
    else
      expire = System.currentTimeMillis() + timeout;

    synchronized (_writeQueue) {
      while (true) {
        if (_writeQueue.size() == 0)
          return;

        long delta;
        if (! Alarm.isTest())
          delta = expire - Alarm.getCurrentTime();
        else
          delta = expire - System.currentTimeMillis();

        if (delta < 0)
          return;

        try {
          _writeQueue.wait(delta);
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Writes the buffer data to the output stream.
   */
  private void writeBuffer(AccessLogBuffer buffer)
    throws IOException
  {
    write(buffer.getBuffer(), 0, buffer.getLength());
    
    super.flush();
    
    _freeBuffers.free(buffer);
    
    rolloverLog();
  }

  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
    long expire = Alarm.getCurrentTime() + 5000;
    
    synchronized (_writeQueue) {
      while (_writeQueue.size() > 0 && Alarm.getCurrentTime() < expire) {
	try {
	  _writeQueue.wait(expire - Alarm.getCurrentTime());
	} catch (Exception e) {
	}
      }
    }
  }

  void wake()
  {
    Thread flushThread = _flushThread;

    if (flushThread != null)
      LockSupport.unpark(flushThread);
  }

  public void run()
  {
    _flushThread = Thread.currentThread();
    int idleMax = 5;
    int idleCount = idleMax;

    while (true) {
      _isFlushing = true;
      
      if (runImpl()) {
	idleCount = idleMax;
      }
      else if (idleCount > 0) {
	idleCount--;
	LockSupport.parkNanos(10L * 1000000000L);
      }
      else {
	synchronized (_writeQueue) {
	  if (_writeQueue.size() == 0) {
	    _isFlushing = false;
	    _hasThread = false;
	    _flushThread = null;
	    return;
	  }
	}
      }
    }
  }

  private boolean runImpl()
  {
    boolean hasData = false;
    
    while (true) {
      try {
	AccessLogBuffer buffer = null;
	
	synchronized (_writeQueue) {
	  if (_writeQueue.size() == 0)
	    return hasData;
	  
	  buffer = _writeQueue.remove(0);
	  _writeQueue.notifyAll();
	  hasData = true;
	}

	writeBuffer(buffer);
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
