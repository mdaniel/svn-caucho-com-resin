/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.log;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.jmx.server.LoggerMXBean;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-specific java.util.logging.Logger configuration.
 */
public class LoggerAdmin extends ManagedObjectBase implements LoggerMXBean
{
  private static final L10N L = new L10N(LoggerAdmin.class);

  private final Logger _logger;
  private final ClassLoader _loader;

  LoggerAdmin(Logger logger)
  {
    _logger = logger;
    _loader = Thread.currentThread().getContextClassLoader();
  }
  
  /**
   * Sets the name of the logger to configure.
   */
  public String getName()
  {
    return _logger.getName();
  }
  
  /**
   * Sets the output level.
   */
  public void setLevel(String level)
  {
    try {
      _logger.setLevel(Level.parse(level.toUpperCase()));
    } catch (Exception e) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging",
                                             level));
    }

  }

  public String getLevel()
  {
    return _logger.getLevel().toString();
  }
}
