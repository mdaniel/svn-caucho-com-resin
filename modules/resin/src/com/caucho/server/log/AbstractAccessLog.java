/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.config.ConfigException;
import com.caucho.util.*;
import com.caucho.log.Log;
import com.caucho.config.types.InitProgram;
import com.caucho.vfs.*;

/**
 * Represents an log of every top-level request to the server.
 */
abstract public class AbstractAccessLog {
  protected static final Logger log = Log.open(AbstractAccessLog.class);

  protected Path _path;

  protected String _pathFormat;

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
    throws ConfigException
  {
    _pathFormat = pathFormat;
  }

  /**
   * Sets the access-log's path (backwards compatibility).
   */
  public void setId(Path path)
  {
    setPath(path);
  }

  public void addInit(InitProgram init)
    throws Throwable
  {
    init.init(this);
  }

  /**
   * Initialize the log.
   */
  public void init()
    throws ServletException, IOException
  {
  }

  /**
   * Logs a request using the current format.
   *
   * @param request the servlet request.
   * @param response the servlet response.
   */
  public abstract void log(HttpServletRequest request,
                           HttpServletResponse response,
                           ServletContext application)
    throws IOException;

  /**
   * Flushes the log.
   */
  public void flush()
  {
  }

  /**
   * Cleanup the log.
   */
  public void destroy()
    throws IOException
  {
  }
}
