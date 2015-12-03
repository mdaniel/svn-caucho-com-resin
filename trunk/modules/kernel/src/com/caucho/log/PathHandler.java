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

package com.caucho.log;

import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Configures a log handler
 */
@Configurable
public class PathHandler extends AbstractLogHandler {
  private final RotateLog _pathLog = new RotateLog();

  private Formatter _formatter;
  private String _timestamp;

  private WriteStream _os;

  public PathHandler()
  {
    _timestamp = "[%Y/%m/%d %H:%M:%S.%s] ";
  }

  /**
   * Convenience method to create a path.  Calls init() automatically.
   */
  public PathHandler(Path path)
  {
    this();

    setPath(path);

    init();
  }

  /**
   * Convenience method to create a path.  Calls init() automatically.
   */
  public PathHandler(String path)
  {
    this(Vfs.lookup(path));
  }

  /**
   * Sets the path
   */
  public void setPath(Path path)
  {
    _pathLog.setPath(path);
  }

  /**
   * Sets the path-format
   */
  public void setPathFormat(String pathFormat)
  {
    _pathLog.setPathFormat(pathFormat);
  }

  /**
   * Sets the archive-format
   */
  public void setArchiveFormat(String archiveFormat)
  {
    _pathLog.setArchiveFormat(archiveFormat);
  }

  /**
   * Sets the rollover-period
   */
  public void setRolloverPeriod(Period rolloverPeriod)
  {
    _pathLog.setRolloverPeriod(rolloverPeriod);
  }

  /**
   * Sets the rollover-size
   */
  public void setRolloverSize(Bytes size)
  {
    _pathLog.setRolloverSize(size);
  }

  /**
   * Sets the rollover-count
   */
  public void setRolloverCount(int count)
  {
    _pathLog.setRolloverCount(count);
  }

  /**
   * Sets the timestamp.
   */
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }

  /**
   * Sets the formatter.
   */
  @Override
  public void setFormatter(Formatter formatter)
  {
    _formatter = formatter;
  }

  /**
   * Initialize the log.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _pathLog.init();

      WriteStream os = _pathLog.getRotateStream().getStream();

      if (_timestamp != null) {
        TimestampFilter filter = new TimestampFilter();
        filter.setTimestamp(_timestamp);
        filter.setStream(os);
        os = new WriteStream(filter);
      }

      String encoding = System.getProperty("file.encoding");

      if (encoding != null)
        os.setEncoding(encoding);

      os.setDisableClose(true);

      _os = os;
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }
    
  @Override
  protected void processPublish(LogRecord record)
  {
    synchronized (_os) {
      try {
        if (_formatter != null) {
          String value = _formatter.format(record);
  
          _os.println(value);
  
          return;
        }
  
        String message = record.getMessage();
        Throwable thrown = record.getThrown();
        
        if (thrown != null) {
          if (message != null
              && ! message.equals(thrown.toString())
              && ! message.equals(thrown.getMessage())) {
            printMessage(_os, message, record.getParameters());
          }
  
          record.getThrown().printStackTrace(_os.getPrintWriter());
        }
        else {
          printMessage(_os, message, record.getParameters());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      }
  }
  
  @Override
  protected void processFlush()
  {
    synchronized (_os) {
      try {
        _os.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    if (_os == null || _os.getPath() == null)
      return super.hashCode();
    else
      return _os.getPath().hashCode();
  }

  /**
   * Test for equality.
   */
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (getClass() != o.getClass())
      return false;

    PathHandler handler = (PathHandler) o;

    if (_os == null || handler._os == null)
      return false;
    else
      return _os.getPath().equals(handler._os.getPath());
  }

  @Override
  public String toString()
  {
    if (_os == null)
      return getClass().getSimpleName() + "[" + _pathLog + "]";
    else
      return getClass().getSimpleName() + "[" + _os.getPath() + "]";
  }
}
