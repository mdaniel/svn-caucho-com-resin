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
 * @author Scott Ferguson
 */

package com.caucho.v5.server.resin;

import java.io.IOException;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.log.impl.LogConfig;
import com.caucho.v5.log.impl.LogHandlerConfig;
import com.caucho.v5.log.impl.LoggerConfig;
import com.caucho.v5.log.impl.StdoutLog;
import com.caucho.v5.util.CurrentTime;

/**
 * The ResinConfig class represents configuration for 
 * the top-level <resin> system.
 */
abstract public class ServerBaseConfig implements EnvironmentBean
{
  @Configurable
  public LoggerConfig createLogger()
  {
    return new LoggerConfig(true);
  }
  
  /**
   * logger: Overrides standard <logger> configuration to change to 
   * system-class-loader.
   */
  @Configurable
  public void addLogger(LoggerConfig logger)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      if (! CurrentTime.isTest())
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

      logger.initImpl();
      
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  /**
   * log: configures a java.util logger.
   */
  @Configurable
  public LogConfig createLog()
  {
    return new LogConfig(true);
  }
  
  /**
   * Overrides standard <log> configuration to change to 
   * system-class-loader.
   */
  @Configurable
  public void addLog(LogConfig log)
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      }

      log.initImpl();
      
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  /**
   * Overrides standard <log-handler> configuration to change to 
   * system-class-loader.
   */
  @Configurable
  public LogHandlerConfig createLogHandler()
  {
    return new LogHandlerConfig(true);
  }

  /**
   * Overrides standard <log-handler> configuration to change to 
   * system-class-loader.
   */
  @Configurable
  public void addLogHandler(LogHandlerConfig logHandler)
  {
    // env/02sf
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      }

      logHandler.initImpl();
      
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  @Configurable
  public StdoutLog createStdoutLog()
  {
    return new StdoutLog(true);
  }

  /**
   * Overrides standard <stdout-log> configuration to change to 
   * system-class-loader.
   */
  @Configurable
  public void addStdoutLog(StdoutLog log)
      throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      }

      log.initImpl();

    } finally {
      thread.setContextClassLoader(loader);
    }
  }
}

