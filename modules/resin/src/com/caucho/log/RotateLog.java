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

package com.caucho.log;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.vfs.Path;
import com.caucho.vfs.RotateStream;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.config.types.Bytes;

/**
 * Configuration for a rotating log
 */
public class RotateLog {
  private final static L10N L = new L10N(RotateLog.class);
  
  private static final long ROLLOVER_SIZE = 100 * 1024 * 1024;
  
  private Path _path;
  private RotateStream _rotateStream;
  private long _rolloverPeriod = -1;
  private long _rolloverSize = ROLLOVER_SIZE;
  private int _rolloverCount = 10;
  private String _timestamp;
  private String _archiveFormat;

  /**
   * Gets the output path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the output path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Sets the output path (backward compat).
   */
  public void setHref(Path path)
  {
    setPath(path);
  }

  /**
   * Sets the rollover period
   */
  public long getRolloverPeriod()
  {
    return _rolloverPeriod;
  }

  /**
   * Sets the rollover period.
   */
  public void setRolloverPeriod(Period period)
  {
    _rolloverPeriod = period.getPeriod();
  }

  /**
   * Sets the rollover size
   */
  public long getRolloverSize()
  {
    return _rolloverSize;
  }

  /**
   * Sets the rollover size.
   */
  public void setRolloverSize(Bytes bytes)
  {
    _rolloverSize = bytes.getBytes();
  }

  /**
   * Sets the rollover count
   */
  public int getRolloverCount()
  {
    return _rolloverCount;
  }

  /**
   * Sets the rollover count.
   */
  public void setRolloverCount(int count)
  {
    _rolloverCount = count;
  }

  /**
   * Sets the timestamp
   */
  public String getTimestamp()
  {
    return _timestamp;
  }

  /**
   * Sets the timestamp.
   */
  /*
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }
  */

  /**
   * Gets the archive format
   */
  public String getArchiveFormat()
  {
    return _archiveFormat;
  }

  /**
   * Sets the archive format.
   */
  public void setArchiveFormat(String format)
  {
    _archiveFormat = format;
  }

  /**
   * Returns the rotated stream.
   */
  public RotateStream getRotateStream()
  {
    return _rotateStream;
  }

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "rotate-log";
  }

  /**
   * Initialize the log.
   */
  public void init()
    throws ConfigException
  {
    if (_path == null)
      throw new ConfigException(L.l("`path' is a required attribute of <{0}>.  Each <{0}> must configure the destination stream.", getTagName()));
    _rotateStream = RotateStream.create(_path);

    _rotateStream.setRolloverPeriod(_rolloverPeriod);
    _rotateStream.setRolloverSize(_rolloverSize);
    _rotateStream.setMaxRolloverCount(_rolloverCount);
    if (_archiveFormat != null)
      _rotateStream.setArchiveFormat(_archiveFormat);

    _rotateStream.init();
  }
}
