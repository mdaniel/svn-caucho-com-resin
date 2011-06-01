/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 */

package com.caucho.admin.action;

import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;

public class SetLogLevelAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(SetLogLevelAction.class.getName());

  private static final L10N L = new L10N(SetLogLevelAction.class);
  
  private static Map<String, Level> _defaultLevels = new HashMap<String, Level>();
  private static ClassLoader _systemClassLoader = ClassLoader.getSystemClassLoader();

  public String execute(final String logger, final Level newLevel, final long time)
  {
    try {
      final Level oldLevel = getLoggerLevel(logger);

      AlarmListener listener = new AlarmListener()
      {
        @Override
        public void handleAlarm(Alarm alarm)
        {
          setLoggerLevel(logger, oldLevel);
        }
      };

      new Alarm("log-level", listener, time);

      setLoggerLevel(logger, newLevel);

      return L.l("Log {0}.level is set to `{1}'. Active time {2} seconds.",
                 (logger.isEmpty() ? "{root}" : logger),
                 newLevel,
                 (time / 1000));
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage(), e);
      return e.toString();
    }
  }
  
  private void setLoggerLevel(final String name, final Level level)
  {
    final Logger logger = Logger.getLogger(name);
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      thread.setContextClassLoader(_systemClassLoader);
      logger.setLevel(level);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  private synchronized Level getLoggerLevel(String name)
  {
    Level level = _defaultLevels.get(name);
    if (level != null)
      return level;

    final Logger logger = Logger.getLogger(name);
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      thread.setContextClassLoader(_systemClassLoader);

      level = logger.getLevel();
      _defaultLevels.put(name, level);

      return level;
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
}
