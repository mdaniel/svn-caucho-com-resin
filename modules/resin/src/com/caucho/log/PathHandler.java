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
 *   Free SoftwareFoundation, Inc.
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
import com.caucho.vfs.WriteStream;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;

/**
 * Resin's rotating path-based log.
 */
public class PathHandler extends Handler {
  private static L10N L = new L10N(PathHandler.class);
  
  private Path _path;

  public PathHandler(Path path)
  {
    _path = path;
  }

  /**
   * Publishes the record.
   */
  public void publish(LogRecord record)
  {
    if (record.getLevel().intValue() < getLevel().intValue())
      return;

    WriteStream os = null;
    synchronized (_path) {
      try {
	try {
	  os = _path.openAppend();
	} catch (Throwable e) {
	  _path.getParent().mkdirs();
	  os = _path.openAppend();
	}

	String msg = record.getMessage();
	os.println(msg);
      } catch (Throwable e) {
	e.printStackTrace();
      } finally {
	try {
	  if (os != null)
	    os.close();
	} catch (IOException e) {
	}
      }
    }
  }

  /**
   * Flushes the buffer.
   */
  public void flush()
  {
  }

  /**
   * Closes the handler.
   */
  public void close()
  {
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return _path.hashCode();
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof PathHandler))
      return false;

    PathHandler handler = (PathHandler) o;

    return _path.equals(handler._path);
  }
}
