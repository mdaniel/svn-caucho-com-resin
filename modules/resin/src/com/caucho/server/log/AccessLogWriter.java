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
public class AccessLogWriter implements Runnable {
  protected static final L10N L = new L10N(AccessLogWriter.class);
  protected static final Logger log = Log.open(AccessLogWriter.class);
  
  // Default maximum log size = 1G
  private static final long ROLLOVER_SIZE = 1024L * 1024L * 1024L;
  // Milliseconds in a day
  private static final long DAY = 24L * 3600L * 1000L;
  // How often to check size
  private static final long ROLLOVER_CHECK_TIME = 600L * 1000L;

  private static final int BUFFER_SIZE = 65536;
  private static final int BUFFER_GAP = 8 * 1024;

  private static final FreeList<AccessLogBuffer> _freeBuffers
    = new FreeList<AccessLogBuffer>(4);

  private QDate _calendar = QDate.createLocal();
  
  private final AccessLog _log;
 
  // the write queue
  private int _maxQueueLength = 32;
  private final ArrayList<AccessLogBuffer> _writeQueue
    = new ArrayList<AccessLogBuffer>();
  
  private WriteStream _os;

  private String _format;

  // prefix for the rollover
  private String _rolloverPrefix;

  // The time of the next period-based rollover
  private long _nextPeriodEnd = -1;
  private long _nextRolloverCheckTime = -1;

  private final byte []_buffer = new byte[BUFFER_SIZE];
  private int _length;

  private boolean _isActive;

  AccessLogWriter(AccessLog log)
  {
    _log = log;
  }

  public String getPathFormat()
  {
    return _log.getPathFormat();
  }

  public String getArchiveFormat()
  {
    return _log.getArchiveFormat();
  }

  public Path getPath()
  {
    return _log.getPath();
  }

  public long getRolloverSize()
  {
    return _log.getRolloverSize();
  }

  public long getRolloverPeriod()
  {
    return _log.getRolloverPeriod();
  }

  public long getRolloverCheckTime()
  {
    return _log.getRolloverCheckTime();
  }
  
  /**
   * Initialize the log.
   */
  public void init()
    throws ServletException, IOException
  {
    _isActive = true;
    
    long now = Alarm.getCurrentTime();
    
    _nextRolloverCheckTime = now + getRolloverCheckTime();

    Path path = getPath();

    if (path != null) {
      path.getParent().mkdirs();
    
      _rolloverPrefix = path.getTail();

      long lastModified = path.getLastModified();
      if (lastModified <= 0)
	lastModified = now;
    
      _calendar.setGMTTime(lastModified);
      long zone = _calendar.getZoneOffset();

      _nextPeriodEnd = Period.periodEnd(lastModified, getRolloverPeriod());
    }
    else
      _nextPeriodEnd = Period.periodEnd(now, getRolloverPeriod());

    if (_nextPeriodEnd < _nextRolloverCheckTime && _nextPeriodEnd > 0)
      _nextRolloverCheckTime = _nextPeriodEnd;

    rolloverLog(now);

    new Thread(this).start();
  }

  boolean isRollover()
  {
    return false;
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
	if (_writeQueue.size() > 0)
	  System.out.println("WRITE: " + _writeQueue.size());
	
	if (_writeQueue.size() < _maxQueueLength) {
	  _writeQueue.add(logBuffer);
	  _writeQueue.notifyAll();
	  break;
	}
	else {
	  try {
	    System.out.println("FULL-QUEUE:");
	    long start = System.currentTimeMillis();
	    _writeQueue.wait();
	    long end = System.currentTimeMillis();
	    System.out.println("FULL-QUEUE:" + (end - start));
	  } catch (Throwable e) {
	    log.log(Level.WARNING, e.toString(), e);
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

    rolloverLog(now);

    if (_os != null) {
      _os.write(buffer.getBuffer(), 0, buffer.getLength());
      _os.flush();
    }

    _freeBuffers.free(buffer);
  }

  /**
   * Check to see if we need to rollover the log.
   *
   * @param now current time in milliseconds.
   */
  private void rolloverLog(long now)
  {
    _nextRolloverCheckTime = now + getRolloverCheckTime();

    Path path = getPath();
      
    if (_nextPeriodEnd < now) {
      if (getPathFormat() == null) {
	String savedName = getArchiveName(_nextPeriodEnd - 1);
	movePathToArchive(savedName);
      }
	
      _nextPeriodEnd = Period.periodEnd(now, getRolloverPeriod());
      
      if (log.isLoggable(Level.FINE))
	log.fine(getPath() + ": next rollover at " +
		 QDate.formatLocal(_nextPeriodEnd));
    }
    else if (path != null && getRolloverSize() <= path.getLength()) {
      if (getPathFormat() == null) {
	String savedName = getArchiveName(_nextRolloverCheckTime - 1);
	movePathToArchive(savedName);
      }
    }

    long nextPeriodEnd = _nextPeriodEnd;
    if (_nextPeriodEnd < _nextRolloverCheckTime && _nextPeriodEnd > 0)
      _nextRolloverCheckTime = _nextPeriodEnd;

    if (_os == null)
      openLog();
  }

  /**
   * Tries to open the log.
   */
  private void openLog()
  {
    try {
      WriteStream os = _os;
      _os = null;

      if (os != null)
	os.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
      
    Path path = getPath();

    if (path == null) {
      path = getPath(Alarm.getCurrentTime());
    }
    
    try {
      if (! path.getParent().isDirectory())
	path.getParent().mkdirs();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    for (int i = 0; i < 3 && _os == null; i++) {
      try {
	_os = path.openAppend();
      } catch (IOException e) {
	log.log(Level.INFO, e.toString(), e);
      }
    }

    if (_os == null)
      log.warning(L.l("Can't open access log file '{0}'.",
		      getPath()));
  }

  private void movePathToArchive(String savedName)
  {
    log.info(L.l("Archiving access log to {0}.", savedName));
	     
    try {
      WriteStream os = _os;
      _os = null;
      if (os != null)
	os.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    Path path = getPath();
    String tail = path.getTail();
    Path newPath = path.getParent().lookup(savedName);
        
    try {
      newPath.getParent().mkdirs();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
        
    try {
      WriteStream os = newPath.openWrite();
      OutputStream out;

      if (savedName.endsWith(".gz"))
	out = new GZIPOutputStream(os);
      else if (savedName.endsWith(".zip")) 
	out = new ZipOutputStream(os);
      else
	out = os;

      try {
	path.writeToStream(out);
      } finally {
	try {
	  out.close();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}

	try {
	  if (out != os)
	    os.close();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
	  
	path.remove();
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the path of the format file
   *
   * @param time the archive date
   */
  protected Path getPath(long time)
  {
    String formatString = getPathFormat();

    if (formatString == null)
      throw new IllegalStateException(L.l("getPath requires a format path"));
    
    String pathString = getFormatName(formatString, time);

    return Vfs.lookup().lookup(pathString);
  }

  /**
   * Returns the name of the archived file
   *
   * @param time the archive date
   */
  protected String getArchiveName(long time)
  {
    return getFormatName(getArchiveFormat(), time);
  }

  /**
   * Returns the name of the archived file
   *
   * @param time the archive date
   */
  protected String getFormatName(String format, long time)
  {
    if (time <= 0)
      time = Alarm.getCurrentTime();
    
    if (format != null)
      return _calendar.formatLocal(time, format);
    else if (getRolloverPeriod() % (24 * 3600 * 1000L) == 0)
      return _rolloverPrefix + "." + _calendar.formatLocal(time, "%Y%m%d");
    else
      return _rolloverPrefix + "." + _calendar.formatLocal(time, "%Y%m%d.%H");
  }

  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
    _isActive = false;
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
	  else if (! _isActive) {
	    break;
	  }
	  else {
	    // timeout so _isActive will always be called eventually
	    _writeQueue.wait(15000);
	  }
	}

	if (buffer != null) {
	  writeBuffer(buffer);
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    try {
      WriteStream os = _os;
      _os = null;
      if (os != null)
	os.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
