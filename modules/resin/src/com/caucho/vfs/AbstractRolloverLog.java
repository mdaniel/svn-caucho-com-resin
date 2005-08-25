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

package com.caucho.vfs;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;

import java.io.OutputStream;
import java.io.IOException;

import com.caucho.util.Log;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;

import com.caucho.loader.Environment;
import com.caucho.loader.CloseListener;

import com.caucho.config.types.Period;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.InitProgram;
import com.caucho.config.ConfigException;


/**
 * Abstract class for a log that rolls over based on size or period.
 */
public class AbstractRolloverLog {
  protected static final Logger log = Log.open(AbstractRolloverLog.class);
  protected static final L10N L = new L10N(AbstractRolloverLog.class);

  // Milliseconds in a day
  private static final long DAY = 24L * 3600L * 1000L;
  
  // Default maximum log size = 2G
  private static final long DEFAULT_ROLLOVER_SIZE = Bytes.INFINITE;
  // How often to check size
  private static final long DEFAULT_ROLLOVER_CHECK_PERIOD = 600L * 1000L;

  // prefix for the rollover
  private String _rolloverPrefix;

  // template for the archived files
  private String _archiveFormat;
  
  // How often the logs are rolled over.
  private long _rolloverPeriod = Period.INFINITE;

  // Maximum size of the log.
  private long _rolloverSize = DEFAULT_ROLLOVER_SIZE;

  // How often the rolloverSize should be checked
  private long _rolloverCheckPeriod = DEFAULT_ROLLOVER_CHECK_PERIOD;

  private QDate _calendar = QDate.createLocal();

  private Path _pwd = Vfs.lookup();
  
  protected Path _path;

  protected String _pathFormat;

  private String _format;

  // The time of the next period-based rollover
  private long _nextPeriodEnd = -1;
  private long _nextRolloverCheckTime = -1;

  private WriteStream _os;

  /**
   * Returns the access-log's path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the access-log's path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Returns the pwd for the rollover log
   */
  public Path getPwd()
  {
    return _pwd;
  }

  /**
   * Returns the formatted path
   */
  public String getPathFormat()
  {
    return _pathFormat;
  }

  /**
   * Sets the formatted path.
   */
  public void setPathFormat(String pathFormat)
  {
    _pathFormat = pathFormat;
  }

  /**
   * Sets the archive name format
   */
  public void setArchiveFormat(String format)
  {
    _archiveFormat = format;
  }

  /**
   * Sets the archive name format
   */
  public String getArchiveFormat()
  {
    return _archiveFormat;
  }

  /**
   * Sets the log rollover period, rounded up to the nearest hour.
   *
   * @param period the new rollover period in milliseconds.
   */
  public void setRolloverPeriod(Period period)
  {
    _rolloverPeriod = period.getPeriod();
    
    if (_rolloverPeriod > 0) {
      _rolloverPeriod += 3600000L - 1;
      _rolloverPeriod -= _rolloverPeriod % 3600000L;
    }
    else
      _rolloverPeriod = Period.INFINITE;
  }

  /**
   * Sets the log rollover period, rounded up to the nearest hour.
   *
   * @param period the new rollover period in milliseconds.
   */
  public long getRolloverPeriod()
  {
    return _rolloverPeriod;
  }

  /**
   * Sets the log rollover size, rounded up to the megabyte.
   *
   * @param size maximum size of the log file
   */
  public void setRolloverSize(Bytes bytes)
  {
    long size = bytes.getBytes();
    
    if (size < 0)
      _rolloverSize = Bytes.INFINITE;
    else
      _rolloverSize = size;
  }

  /**
   * Sets the log rollover size, rounded up to the megabyte.
   *
   * @param size maximum size of the log file
   */
  public long getRolloverSize()
  {
    return _rolloverSize;
  }

  /**
   * Sets how often the log rollover will be checked.
   *
   * @param period how often the log rollover will be checked.
   */
  public void setRolloverCheckPeriod(long period)
  {
    if (period > 1000)
      _rolloverCheckPeriod = period;
    else if (period > 0)
      _rolloverCheckPeriod = 1000;
  }

  /**
   * Sets how often the log rollover will be checked.
   *
   * @param period how often the log rollover will be checked.
   */
  public long getRolloverCheckPeriod()
  {
    return _rolloverCheckPeriod;
  }
  
  /**
   * Initialize the log.
   */
  public void init()
    throws IOException
  {
    long now = Alarm.getCurrentTime();
    
    _nextRolloverCheckTime = now + _rolloverCheckPeriod;

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
  }

  public long getNextRolloverCheckTime()
  {
    if (_nextPeriodEnd < _nextRolloverCheckTime)
      return _nextPeriodEnd;
    else
      return _nextRolloverCheckTime;
  }

  public boolean isRollover()
  {
    long now = Alarm.getCurrentTime();
    
    return _nextPeriodEnd <= now || _nextRolloverCheckTime <= now;
  }

  public boolean rollover()
  {
    long now = Alarm.getCurrentTime();

    if (_nextPeriodEnd <= now || _nextRolloverCheckTime <= now) {
      rolloverLog(now);
      return true;
    }
    else
      return false;
  }

  /**
   * Writes to the underlying log.
   */
  protected void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_os == null)
      openLog();

    if (_os != null)
      _os.write(buffer, offset, length);
  }

  /**
   * Writes to the underlying log.
   */
  protected void flush()
    throws IOException
  {
    if (_os != null)
      _os.flush();
  }

  /**
   * Check to see if we need to rollover the log.
   *
   * @param now current time in milliseconds.
   */
  protected void rolloverLog(long now)
  {
    _nextRolloverCheckTime = now + _rolloverCheckPeriod;

    long lastPeriodEnd = _nextPeriodEnd;
    _nextPeriodEnd = Period.periodEnd(now, getRolloverPeriod());

    Path path = getPath();
      
    if (lastPeriodEnd < now) {
      closeLogStream();
      
      if (getPathFormat() == null) {
	Path savedPath = getArchivePath(lastPeriodEnd - 1);

	movePathToArchive(savedPath);
      }

      /*
      if (log.isLoggable(Level.FINE))
	log.fine(getPath() + ": next rollover at " +
		 QDate.formatLocal(_nextPeriodEnd));
      */
    }
    else if (path != null && getRolloverSize() <= path.getLength()) {
      closeLogStream();
      
      if (getPathFormat() == null) {
	Path savedPath = getArchivePath(_nextRolloverCheckTime - 1);
	movePathToArchive(savedPath);
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
    closeLogStream();
    
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

  /**
   * Tries to open the log.
   */
  private void closeLogStream()
  {
    try {
      WriteStream os = _os;
      _os = null;

      if (os != null)
	os.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  private void movePathToArchive(Path savedPath)
  {
    Path path = getPath();
    
    String savedName = savedPath.getTail();
    
    log.info(L.l("Archiving access log to {0}.", savedName));
	     
    try {
      WriteStream os = _os;
      _os = null;
      if (os != null)
	os.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      savedPath.getParent().mkdirs();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
        
    try {
      WriteStream os = savedPath.openWrite();
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

    return getPwd().lookup(pathString);
  }

  /**
   * Returns the name of the archived file
   *
   * @param time the archive date
   */
  protected Path getArchivePath(long time)
  {
    Path path = getPath();

    String archiveFormat = getArchiveFormat();
    
    String name = getFormatName(archiveFormat, time);

    Path newPath = path.getParent().lookup(name);

    if (newPath.exists()) {
      if (archiveFormat == null)
	archiveFormat = _rolloverPrefix + ".%Y%m%d.%H%M";
      else if (! archiveFormat.contains("%H"))
	archiveFormat = archiveFormat + ".%H%M";
      else if (! archiveFormat.contains("%M"))
	archiveFormat = archiveFormat + ".%M";
      
      name = getFormatName(archiveFormat, time);

      newPath = path.getParent().lookup(name);
    }

    return newPath;
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
  public void close()
    throws IOException
  {
    if (_os != null) {
      WriteStream os = _os;
      _os = null;
      
      os.close();
    }
  }
}
