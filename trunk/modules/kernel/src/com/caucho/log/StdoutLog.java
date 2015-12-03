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

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.vfs.WriteStream;

/**
 * Configuration for the standard output log
 */
public class StdoutLog extends RotateLog {
  private boolean _isSkipInit;
  
  private String _timestamp;
  
  /**
   * Creates the StdoutLog
   */
  public StdoutLog()
  {
    this(false);
    // setTimestamp("[%Y/%m/%d %H:%M:%S.%s] ");
  }
  
  public StdoutLog(boolean isSkipInit)
  {
    _isSkipInit = isSkipInit;
  }

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "stdout-log";
  }

  /**
   * Sets the timestamp.
   */
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }
  
  /**
   * Initialize the log.
   */
  @PostConstruct
  public void init()
      throws ConfigException, IOException
  {
    super.init();
    
    if (! _isSkipInit) {
      initImpl();
    }
  }
  
  public void initImpl()
    throws ConfigException, IOException
  {
    WriteStream out = getRotateStream().getStream();

    if (_timestamp != null) {
      out = new WriteStream(new TimestampFilter(out, _timestamp));
    }

    EnvironmentStream.setStdout(out);
  }
}


