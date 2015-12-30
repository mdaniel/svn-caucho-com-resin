/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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

package com.caucho.v5.server.watchdog;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.server.ProgramInfoDaemon;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.jni.JniCauchoSystem;
import com.caucho.v5.loader.DynamicClassLoader;

/**
 * Process responsible for watching a backend watchdog.
 */
public class WatchdogMain {
  private static final Logger log
    = Logger.getLogger(WatchdogMain.class.getName());
  
  private final String []_argv;
  
  protected WatchdogMain(String []argv)
    throws Exception
  {
    _argv = argv;
  }
  
  protected String []getArgv()
  {
    return _argv;
  }
  
  protected void start()
  {
    
    // boolean isValid = false;

    ArgsWatchdog args = null;
    
    try {
      DynamicClassLoader.setJarCacheEnabled(false);
      DynamicClassLoader.setGlobalDependencyCheckInterval(-1);

      JniCauchoSystem.create().initJniBackground();

      args = parseArgs();

      ExitCode code = args.doCommand();
      
      System.exit(code.ordinal());
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      System.err.println(e);
      
      if (args != null && args.isVerbose()) {
        e.printStackTrace();
      }
    } finally {
      System.exit(ExitCode.UNKNOWN.ordinal());
    }
  }
  
  protected ArgsWatchdog parseArgs()
  {
    ArgsWatchdog args = new ArgsWatchdog(getArgv(), new ProgramInfoDaemon());
    
    args.envCli().initLogging();

    args.parse();

    return args;
  }
}
