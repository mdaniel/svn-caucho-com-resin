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

  private final AccessLog _log;
  
  // AccessStream
  private WriteStream _os;
  private Object _streamLock = new Object();

  private String _format;

  // prefix for the rollover
  private String _rolloverPrefix;

  // template for the archived files
  private String _archiveFormat;
  
  // How often the logs are rolled over.
  private long _rolloverPeriod = -1;

  // Maximum size of the log.
  private long _rolloverSize = ROLLOVER_SIZE;

  // How often the rolloverSize should be checked
  private long _rolloverCheckTime = ROLLOVER_CHECK_TIME;

  // The time of the next period-based rollover
  private long _nextPeriodEnd = -1;
  private long _nextRolloverCheckTime = -1;

  private final byte []_buffer = new byte[BUFFER_SIZE];
  private int _length;

  private Object _bufferLock = new Object();

  private boolean _isActive;

  AccessLogWriter(AccessLog log)
  {
    _log = log;
  }
  
  /**
   * Initialize the log.
   */
  public void init()
    throws ServletException, IOException
  {
    _isActive = true;
    
    long now = Alarm.getCurrentTime();
    
    _nextRolloverCheckTime = now + _rolloverCheckTime;

    if (getPath() != null) {
      _path.getParent().mkdirs();
    
      _rolloverPrefix = _path.getTail();

      long lastModified = _path.getLastModified();
      if (lastModified <= 0)
	lastModified = now;
    
      _calendar.setGMTTime(lastModified);
      long zone = _calendar.getZoneOffset();

      if (_rolloverPeriod > 0)
	_nextPeriodEnd = Period.periodEnd(lastModified, _rolloverPeriod);
    }
    else
      _nextPeriodEnd = Period.periodEnd(now, _rolloverPeriod);

    if (_nextPeriodEnd < _nextRolloverCheckTime && _nextPeriodEnd > 0)
      _nextRolloverCheckTime = _nextPeriodEnd;

    rolloverLog(now);

    new Thread(this).start();
  }

  /**
   * Check to see if we need to rollover the log.
   *
   * @param now current time in milliseconds.
   */
  private void rolloverLog(long now)
  {
    flush();
    
    synchronized (_streamLock) {
      _nextRolloverCheckTime = now + _rolloverCheckTime;
      
      if (_nextPeriodEnd > 0 && _nextPeriodEnd < now) {
	if (getPathFormat() == null) {
	  String savedName = getArchiveName(_nextPeriodEnd - 1);
	  movePathToArchive(savedName);
	}
	
	_nextPeriodEnd = Period.periodEnd(now, _rolloverPeriod);
      
	if (log.isLoggable(Level.FINE))
	  log.fine(_path + ": next rollover at " +
		   QDate.formatLocal(_nextPeriodEnd));
      }
      else if (_path != null && _rolloverSize <= _path.getLength()) {
	if (getPathFormat() == null) {
	  String savedName = getArchiveName(_nextRolloverCheckTime - 1);
	  movePathToArchive(savedName);
	}
      }

      long nextPeriodEnd = _nextPeriodEnd;
      if (_nextPeriodEnd < _nextRolloverCheckTime && _nextPeriodEnd > 0)
	_nextRolloverCheckTime = _nextPeriodEnd;
    }
    
    openLog();
  }

  /**
   * Tries to open the log.
   */
  private void openLog()
  {
    try {
      synchronized (_streamLock) {
	WriteStream os = _os;
	_os = null;

	if (os != null)
	  os.close();
      }
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
    
    synchronized (_streamLock) {
      for (int i = 0; i < 3 && _os == null; i++) {
	try {
	  _os = path.openAppend();
	} catch (IOException e) {
	  log.log(Level.INFO, e.toString(), e);
	}
      }

      if (_os == null)
	log.warning(L.l("Can't open access log file '{0}'.",
			_path));
    }
  }

  private void movePathToArchive(String savedName)
  {
    log.info(L.l("Archiving access log to {0}.", savedName));
	     
    synchronized (_streamLock) {
      try {
	WriteStream os = _os;
	_os = null;
	if (os != null)
	  os.close();
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
	
      String tail = _path.getTail();
      Path newPath = _path.getParent().lookup(savedName);
        
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
	  _path.writeToStream(out);
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
	  
	  _path.remove();
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
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
    return getFormatName(_archiveFormat, time);
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
    else if (_rolloverPeriod % (24 * 3600 * 1000L) == 0)
      return _rolloverPrefix + "." + _calendar.formatLocal(time, "%Y%m%d");
    else
      return _rolloverPrefix + "." + _calendar.formatLocal(time, "%Y%m%d.%H");
  }

  /**
   * Prints a CharSegment to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param cb the new char segment to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, CharSegment cb)
  {
    char []charBuffer = cb.getBuffer();
    int cbOffset = cb.getOffset();
    int length = cb.getLength();

    // truncate for hacker attacks
    if (buffer.length - offset - 256 < length)
      length =  buffer.length - offset - 256;

    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) charBuffer[cbOffset + i];

    return offset + length;
  }

  /**
   * Prints a String to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param s the new string to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, String s)
  {
    int length = s.length();

    _cb.ensureCapacity(length);
    char []cBuf = _cb.getBuffer();

    s.getChars(0, length, cBuf, 0);

    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) cBuf[i];

    return offset + length;
  }

  /**
   * Prints a String to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param s the new string to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset,
                    char []cb, int cbOff, int length)
  {
    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) cb[cbOff + i];

    return offset + length;
  }

  /**
   * Prints an integer to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param v the new integer to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, int v)
  {
    if (v == 0) {
      buffer[offset] = (byte) '0';
      return offset + 1;
    }

    if (v < 0) {
      buffer[offset++] = (byte) '-';
      v = -v;
    }

    int length = 0;
    int exp = 10;
    
    for (; v >= exp; length++)
      exp = 10 * exp;

    offset += length;
    for (int i = 0; i <= length; i++) {
      buffer[offset - i] = (byte) (v % 10 + '0');
      v = v / 10;
    }

    return offset + 1;
  }

  /**
   * Flushes the log.
   */
  public void flush()
  {
    synchronized (_bufferLock) {
      if (_length == 0)
	return;

      flushImpl();
    }
  }

  /**
   * Flushes the log.
   *
   * Called with a _bufferLock
   */
  private void flushImpl()
  {
    if (_os == null)
      openLog();
      
    try {
      synchronized (_streamLock) {
	WriteStream os = _os;

	if (os != null) {
	  os.write(_buffer, 0, _length);
	  _length = 0;
	  os.flush();
	}
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
    _isActive = false;
    
    Alarm alarm = _alarm;;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    /*
    synchronized (_streamLock) {
      flush();
      WriteStream os = _os;
      _os = null;
      if (os != null)
	os.close();
    }
    */
  }

  public void run()
  {
    do {
      try {
	Thread.wait(_bufferList);
      } catch (Throwable e) {
      }
    } while (_isActive);
  }
}
