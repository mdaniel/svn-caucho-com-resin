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

package com.caucho.server.log;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.types.Bytes;
import com.caucho.env.actor.AbstractWorkerQueue;
import com.caucho.log.AbstractRolloverLog;
import com.caucho.server.httpcache.TempFileService;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeRing;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempStreamApi;

/**
 * Represents an log of every top-level request to the server.
 */
public class AccessLogWriter extends AbstractRolloverLog
{
  protected static final L10N L = new L10N(AccessLogWriter.class);
  protected static final Logger log
    = Logger.getLogger(AccessLogWriter.class.getName());

  private final AccessLog _log;

  private boolean _isAutoFlush;
  private final Object _bufferLock = new Object();
  
  private int _logBufferSize = 1024;

  private final FreeRing<LogBuffer> _freeList
    = new FreeRing<LogBuffer>(512);

  private final LogWriterTask _logWriterTask = new LogWriterTask();
  
  private TempFileService _tempService;

  AccessLogWriter(AccessLog log)
  {
    _log = log;

    /*
    _logBuffer = getLogBuffer();
    _buffer = _logBuffer.getBuffer();
    _length = 0;
    */

    // _semaphoreProbe = MeterService.createSemaphoreMeter("Resin|Log|Semaphore");
    
    _tempService = TempFileService.getCurrent();
    
    if (_tempService == null)
      throw new IllegalStateException(L.l("'{0}' is required for AccessLog",
                                          TempFileService.class.getSimpleName()));
  }

  void setBufferSize(Bytes bytes)
  {
    _logBufferSize = (int) bytes.getBytes();
  }

  int getBufferSize()
  {
    return _logBufferSize;
  }

  @Override
  public void init()
    throws IOException
  {
    super.init();

    _isAutoFlush = _log.isAutoFlush();
    
    for (int i = 0; i < 64; i++) {
      _freeList.free(new LogBuffer(_logBufferSize));
    }
  }
  
  boolean isBufferAvailable()
  {
    return _freeList.getSize() >= 16;
  }

  void writeThrough(byte []buffer, int offset, int length)
    throws IOException
  {
    synchronized (_bufferLock) {
      write(buffer, offset, length);
    }

    flushStream();
  }

  void writeBuffer(LogBuffer buffer)
  {
    _logWriterTask.offer(buffer);
    _logWriterTask.wake();
  }

  // must be synchronized by _bufferLock.
  @Override
  protected void flush()
  {
    // server/021g
    _logWriterTask.wake();
    
    waitForFlush(10);
    
    try {
      super.flush();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  protected void wake()
  {
    _logWriterTask.wake();
  }

  protected void waitForFlush(long timeout)
  {
    long expire;

    expire = CurrentTime.getCurrentTimeActual() + timeout;

    while (true) {
      if (_logWriterTask.isEmpty()) {
        return;
      }

      long delta;
      delta = expire - CurrentTime.getCurrentTimeActual();

      if (delta < 0)
        return;

      if (delta > 50) {
        delta = 50;
      }

      try {
        _logWriterTask.wake();

        Thread.sleep(delta);
      } catch (Exception e) {
      }
    }
  }

  LogBuffer allocateBuffer()
  {
    LogBuffer buffer = _freeList.allocate();

    if (buffer == null) {
      buffer = new LogBuffer(_logBufferSize);
    }

    return buffer;
  }

  void freeBuffer(LogBuffer logBuffer)
  {
    logBuffer.clear();
    
    if (! logBuffer.isPrivate()) {
      _freeList.free(logBuffer);
    }
  }

  @Override
  protected TempStreamApi createTempStream()
  {
    return _tempService.getManager().createTempStream();
  }

  @Override
  public void close()
    throws IOException
  {
    try {
      flush();
    } finally {
      super.close();
    }
  }
  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
    _logWriterTask.close();
  }

  class LogWriterTask extends AbstractWorkerQueue<LogBuffer> {
    private final String _threadName;
    
    LogWriterTask()
    {
      super(16 * 1024);
      
      _threadName = toString();
    }
    
    @Override
    public String getThreadName()
    {
      return _threadName;
    }

    @Override
    public void process(LogBuffer value)
    {
      if (value == null) {
        return;
        
      }
      try {
        write(value.getBuffer(), 0, value.getLength());
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        freeBuffer(value);
      }
    }

    @Override
    public void onProcessComplete()
    {
      try {
        flushStream();
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    public void close()
    {
      wake();
    }
    
    @Override
    public String toString()
    {
      Path path = getPath();
      
      if (path != null)
        return getClass().getSimpleName() + "[" + path.getTail() + "]";
      else
        return getClass().getSimpleName() + "[" + path + "]";
    }
  }
}
