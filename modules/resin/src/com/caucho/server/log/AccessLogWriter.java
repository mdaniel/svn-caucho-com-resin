/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;

import java.io.OutputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.*;

import com.caucho.vfs.*;
import com.caucho.vfs.AbstractRolloverLog;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.CloseListener;

import com.caucho.config.types.Period;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.InitProgram;
import com.caucho.config.ConfigException;

import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.AbstractHttpResponse;

import com.caucho.server.dispatch.ServletConfigException;

/**
 * Represents an log of every top-level request to the server.
 */
public class AccessLogWriter extends AbstractRolloverLog implements Runnable {
  protected static final L10N L = new L10N(AccessLogWriter.class);
  protected static final Logger log = Log.open(AccessLogWriter.class);

  private static final int BUFFER_SIZE = 65536;
  private static final int BUFFER_GAP = 8 * 1024;

  private static final FreeList<AccessLogBuffer> _freeBuffers
    = new FreeList<AccessLogBuffer>(4);

  private final AccessLog _log;

  private boolean _hasThread;
  private boolean _isFlushing;
 
  // the write queue
  private int _maxQueueLength = 32;
  private final ArrayList<AccessLogBuffer> _writeQueue
    = new ArrayList<AccessLogBuffer>();
  
  AccessLogWriter(AccessLog log)
  {
    _log = log;
  }
  
  AccessLogBuffer getLogBuffer()
  {
    AccessLogBuffer buffer = _freeBuffers.allocate();

    if (buffer == null)
      buffer = new AccessLogBuffer();

    return buffer;
  }

  AccessLogBuffer write(AccessLogBuffer logBuffer)
  {
    while (true) {
      synchronized (_writeQueue) {
	if (_writeQueue.size() < _maxQueueLength) {
	  _writeQueue.add(logBuffer);

	  if (! _hasThread) {
	    _hasThread = true;
	    ThreadPool.schedule(this);
	  }

	  break;
	}
	else if (! _isFlushing) {
	  try {
	    _isFlushing = true;
	    // If the queue is full, call the flush code directly
	    // since the thread pool may be out of threads for
	    // a schedule
	    log.fine("AccessLogWriter flushing log directly.");
	      
	    run();
	  } catch (Throwable e) {
	    log.log(Level.WARNING, e.toString(), e);
	  } finally {
	    _isFlushing = false;
	  }
	}
      }
    }
    
    AccessLogBuffer buffer = _freeBuffers.allocate();

    if (buffer == null)
      buffer = new AccessLogBuffer();

    return buffer;
  }

  /**
   * Writes the buffer data to the output stream.
   */
  private void writeBuffer(AccessLogBuffer buffer)
    throws IOException
  {
    long now = Alarm.getCurrentTime();

    write(buffer.getBuffer(), 0, buffer.getLength());
    flush();
    
    _freeBuffers.free(buffer);
    
    rolloverLog(now);
  }

  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
  }

  public void run()
  {
    while (true) {
      try {
	AccessLogBuffer buffer = null;
	
	synchronized (_writeQueue) {
	  if (_writeQueue.size() > 0) {
	    buffer = _writeQueue.remove(0);
	    _writeQueue.notifyAll();
	  }
	  else {
	    _hasThread = false;
	    return;
	  }
	}

	if (buffer != null) {
	  writeBuffer(buffer);
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
