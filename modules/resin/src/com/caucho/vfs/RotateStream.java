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

import java.lang.ref.SoftReference;

import java.io.OutputStream;
import java.io.IOException;

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;

import com.caucho.util.AlarmListener;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;

import com.caucho.log.Log;

import com.caucho.config.types.Period;

/**
 * Automatically-rotating streams.  Normally, clients will call
 * getStream instead of using the StreamImpl interface.
 */
public class RotateStream extends StreamImpl {
  private static final Logger log = Log.open(RotateStream.class);
  
  // Milliseconds in an hour
  private static final long HOUR = 3600L * 1000L;
  // Milliseconds in a day
  private static final long DAY = 24L * HOUR;
  
  private static final long ROLLOVER_SIZE = 100 * 1024 * 1024;
  
  private static HashMap<Path,SoftReference<RotateStream>> _streams
    = new HashMap<Path,SoftReference<RotateStream>>();
  
  private static HashMap<String,SoftReference<RotateStream>> _formatStreams
    = new HashMap<String,SoftReference<RotateStream>>();

  private Path _path;
  private String _formatPath;
  
  private String _archiveFormat;

  // How often the logs are rolled over.
  private long _rolloverPeriod = -1;

  // Maximum size of the log.
  private long _rolloverSize = ROLLOVER_SIZE;
  
  private long _updateInterval = HOUR;
  
  private int _maxRolloverCount;

  // currently open stream
  private StreamImpl _source;

  // calendar using the local timezone
  private QDate _calendar = new QDate(true);
    
  // When the log will next be rolled over for the period check
  private long _nextPeriodEnd = -1;
  private long _nextCheckTime = -1;
  
  private long _lastTime = -1; // time of the last check

  private volatile boolean _isInit;

  /**
   * Create rotate stream.
   *
   * @param path underlying log path
   */
  private RotateStream(Path path)
  {
    _path = path;
    _rolloverSize = ROLLOVER_SIZE;
    _maxRolloverCount = 100;
  }

  /**
   * Create rotate stream.
   *
   * @param path underlying log path
   */
  private RotateStream(String formatPath)
  {
    _formatPath = formatPath;
    _rolloverSize = ROLLOVER_SIZE;
    _maxRolloverCount = 100;
  }

  /**
   * Returns the rotate stream corresponding to this path
   */
  public static RotateStream create(Path path)
  {
    synchronized (_streams) {
      SoftReference<RotateStream> ref = _streams.get(path);
      RotateStream stream = ref != null ? ref.get() : null;

      if (stream == null) {
        stream = new RotateStream(path);

        _streams.put(path, new SoftReference<RotateStream>(stream));
      }

      return stream;
    }
  }

  /**
   * Returns the rotate stream corresponding to this path
   */
  public static RotateStream create(String path)
  {
    synchronized (_formatStreams) {
      SoftReference<RotateStream> ref = _formatStreams.get(path);
      RotateStream stream = ref != null ? ref.get() : null;

      if (stream == null) {
        stream = new RotateStream(path);

        _formatStreams.put(path, new SoftReference<RotateStream>(stream));
      }

      return stream;
    }
  }

  /**
   * Clears the streams.
   */
  public static void clear()
  {
    synchronized (_streams) {
      _streams.clear();
    }
    
    synchronized (_formatStreams) {
      _formatStreams.clear();
    }
  }

  /**
   * Sets the maximum number of rolled logs.
   */
  public void setMaxRolloverCount(int count)
  {
    if (count < 0)
      count = 1;

    _maxRolloverCount = count;
  }

  /**
   * Sets the log rollover period, rounded up to the nearest hour.
   *
   * @param period the new rollover period in milliseconds.
   */
  public void setRolloverPeriod(long period)
  {
    if (period > 0) {
      _rolloverPeriod = period;
      _rolloverPeriod += 3600000L - 1;
      _rolloverPeriod -= _rolloverPeriod % 3600000L;
    }
    else
      _rolloverPeriod = Long.MAX_VALUE / 2;
  }

  /**
   * Sets the archive format.
   *
   * @param format the archive format.
   */
  public void setArchiveFormat(String format)
  {
    if (format == null)
      throw new NullPointerException();
    
    _archiveFormat = format;
  }

  /**
   * Sets the log rollover size, rounded up to the megabyte.
   *
   * @param size maximum size of the log file, rolled up to the nearest meg.
   */
  public void setRolloverSize(long size)
  {
    _rolloverSize = size;
  }

  /**
   * Initialize the stream, setting any logStream, System.out and System.err
   * as necessary.
   */
  public void init()
  {
    synchronized (this) {
      if (_isInit)
	return;
      _isInit = true;
    }

    if (_rolloverPeriod < 0 && _rolloverSize < 0)
      _rolloverPeriod = 30L * DAY;

    if (_rolloverSize <= 0)
      _rolloverSize = Long.MAX_VALUE / 2;

    if (_formatPath != null) {
    }
    else if (_archiveFormat != null) {
    }
    else if (_rolloverPeriod % DAY != 0)
      _archiveFormat = _path.getTail() + ".%Y%m%d.%H";
    else
      _archiveFormat = _path.getTail() + ".%Y%m%d";

    handleAlarm();
    
    new RotateAlarm(this);
  }

  /**
   * Initialize the stream, setting any logStream, System.out and System.err
   * as necessary.
   */
  private void open()
  {
    if (_source != null)
      return;
    
    try {
      _path.getParent().mkdirs();
    } catch (IOException e) {
      // e.printStackTrace();
    }
    
    try {
      _source = _path.openAppendImpl();
    } catch (IOException e) {
      // e.printStackTrace();
    }
  }

  /**
   * True if the stream can write
   */
  public boolean canWrite()
  {
    return _path != null;
  }

  /**
   * Writes to the stream
   */
  public void write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    synchronized (this) {
      if (_source == null) {
	init();
	open();
      }

      if (_source != null) {
	_source.write(buffer, offset, length, isEnd);
      }
    }
  }

  /**
   * Gets the current write stream
   */
  public WriteStream getStream()
  {
    return new WriteStream(this);
  }    

  void handleAlarm()
  {
    synchronized (this) {
      rotateLog();
    }
  }

  /**
   * Rotate the logs.  If the file is too big, close the stream and
   * save the file to the rotated log.
   */
  private void rotateLog()
  {
    long now = Alarm.getCurrentTime();
    
    long lastTime = _lastTime;
    _lastTime = Alarm.getCurrentTime();
    
    if (now < _nextCheckTime)
      return;

    _nextCheckTime = now + _updateInterval;

    closeImpl();

    try {
      Path savedPath = null;

      _calendar.setGMTTime(now);

      if (lastTime < 0) {
	lastTime = _path.getLastModified();

	if (lastTime < 0)
	  lastTime = Alarm.getCurrentTime();
      }

      boolean isOverflow = _rolloverSize < _path.getLength();

      if (_nextPeriodEnd < 0 && _rolloverPeriod > 0) {
	long modifiedTime = _path.getLastModified() ;

	if (modifiedTime <= 0) {
	  _nextPeriodEnd = Period.periodEnd(now, _rolloverPeriod);
	  lastTime = now;
	}
	else {
	  _nextPeriodEnd = Period.periodEnd(modifiedTime, _rolloverPeriod);
	  lastTime = _nextPeriodEnd - 1;
	}

	if (_nextPeriodEnd < _nextCheckTime)
	  _nextCheckTime = _nextPeriodEnd;

	if (now < _nextPeriodEnd && ! isOverflow)
	  return;
      }

      Path parent = _path.getParent();

      if (_rolloverPeriod > 0 && _nextPeriodEnd < now) {
	lastTime = _nextPeriodEnd - 1;
	_nextPeriodEnd = Period.periodEnd(now, _rolloverPeriod);
      }
      else if (! isOverflow) {
	return;
      }

      if (isOverflow)
	lastTime = Alarm.getCurrentTime();

      String date = _calendar.formatLocal(lastTime, _archiveFormat);
      savedPath = parent.lookup(date);

      if (savedPath.exists()) {
	String extArchive = _archiveFormat;
	if (extArchive.indexOf("%H") < 0)
	  extArchive += ".%H";

	date = _calendar.formatLocal(lastTime, extArchive);
	savedPath = parent.lookup(date);

	if (savedPath.exists()) {
	  for (int i = 1; i < _maxRolloverCount; i++) {
	    savedPath = parent.lookup(date + '-' + i);
	    if (! savedPath.exists())
	      break;
	  }
	}
      }
        
      try {
	String savedName = savedPath.getTail();

	try {
	  savedPath.getParent().mkdirs();
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}
	
	WriteStream os = savedPath.openWrite();
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
	    
	  os.close();
	}
	  
	_path.remove();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    } finally {
      init();
      open();
    }
  }

  /**
   * Closes the underlying stream.
   */
  public void flush()
  {
    StreamImpl source = _source;

    try {
      if (source != null)
	source.flush();
    } catch (IOException e) {
    }
  }

  /**
   * Closes the underlying stream.
   */
  private void closeImpl()
  {
    try {
      StreamImpl source = _source;
      _source = null;

      if (source != null)
	source.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * finalize.
   */
  public void finalize()
  {
    closeImpl();
  }

  static class RotateAlarm implements AlarmListener {
    SoftReference<RotateStream> _ref;

    RotateAlarm(RotateStream stream)
    {
      _ref = new SoftReference<RotateStream>(stream);

      handleAlarm(new Alarm("rotate-alarm", this));
    }

    public void handleAlarm(Alarm alarm)
    {
      RotateStream stream = _ref.get();

      if (stream != null) {
	try {
	  stream.handleAlarm();
	} finally {
	  long now = Alarm.getCurrentTime();
	  long nextTime = now + HOUR;

	  nextTime = nextTime - nextTime % HOUR + 1000L;
	  
	  alarm.queue(nextTime - now);
	}
      }
    }
  }
}
