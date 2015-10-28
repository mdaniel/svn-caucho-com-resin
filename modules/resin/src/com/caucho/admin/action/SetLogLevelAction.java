/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

import com.caucho.v5.util.*;

public class SetLogLevelAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(SetLogLevelAction.class.getName());

  private static final L10N L = new L10N(SetLogLevelAction.class);
  
  private static ClassLoader _systemClassLoader = 
    ClassLoader.getSystemClassLoader();

  public String execute(final String[] loggerNames, 
                        final Level newLevel, 
                        final long time)
  {
    if (time > 0) {
      final Map<String, Level> oldLevels = getLoggerLevels(loggerNames);
  
      AlarmListener listener = new AlarmListener()
      {
        @Override
        public void handleAlarm(Alarm alarm)
        {
          setLoggerLevels(oldLevels);
        }
      };
  
      new Alarm("log-level", listener, time);
    }

    setLoggerLevels(loggerNames, newLevel);
    
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i<loggerNames.length; i++) {
      if (loggerNames[i].length() == 0)
        sb.append("{root}");
      else
        sb.append(loggerNames[i]);
      
      if (i < (loggerNames.length-1))
        sb.append(", ");
    }

    if (time > 0) {
      return L.l("Logger '{2}' level is set to '{0}', active time {1} seconds",
                 newLevel,
                 (time / 1000),
                 sb.toString());
    } else {
      return L.l("Logger '{1}' level is set to '{0}'",
                 newLevel,
                 sb.toString());
    }
  }
  
  private static void setLoggerLevels(final String[] loggerNames, final Level level)
  {
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(_systemClassLoader);
      }
      
      for (String loggerName : loggerNames) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  private static void setLoggerLevels(final Map<String, Level> levelsMap)
  {
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(_systemClassLoader);
      }
      
      for (Map.Entry<String, Level> entry : levelsMap.entrySet()) {
        Logger logger = Logger.getLogger(entry.getKey());
        logger.setLevel(entry.getValue());
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  

  private static Map<String, Level> getLoggerLevels(String[] loggerNames)
  {
    Map<String, Level> oldLevels = new HashMap<String, Level>();
    
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_systemClassLoader);
      
      for (String loggerName : loggerNames) {
        Logger logger = Logger.getLogger(loggerName);
        Level level = logger.getLevel();
        oldLevels.put(loggerName, level);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
    
    return oldLevels;
  }
}
