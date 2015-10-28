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

package com.caucho.v5.cli.resin;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.spi.Command;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.util.L10N;

/**
 * ResinBoot is the main bootstrap class for Resin.  It parses the
 * resin.xml and looks for the &lt;server> block matching the -server
 * argument.
 *
 * <h3>Start Modes:</h3>
 *
 * The start modes are STATUS, DIRECT, START, STOP, KILL, RESTART, SHUTDOWN.
 *
 * <ul>
 * <li>DIRECT starts a <server> from the command line
 * <li>START starts a <server> with a Watchdog in the background
 * <li>STOP stop the <server> Resin in the background
 * </ul>
 */
public class ResinCommandLine
{
  private static L10N _L;
  private static final Logger log = Logger.getLogger(ResinCommandLine.class.getName());
  
  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml  : alternate configuration file
   * -server web-a    : &lt;server> to start
   * <pre>
   */
  public static void main(String []argv)
  {
    ArgsResinCli args = new ArgsResinCli(argv);
    
    args.doMain();

    /*
    try {
      final String jvmVersion = System.getProperty("java.runtime.version");

      if ("1.7".compareTo(jvmVersion) > 0) {
        throw new ConfigException(L().l("Resin requires Java 1.7 or later but was started with {0}",
                                        jvmVersion));
      }

      args.initHomeClassPath();
      
      ExitCode exitCode = args.doCommand();

      System.exit(exitCode.ordinal());
    } catch (BootArgumentException e) {
      printException(e, args);

      if (args.getCommand() != null) {
        System.err.println(args.getCommand().usage(args));
      }

      System.exit(ExitCode.UNKNOWN_ARGUMENT.ordinal());
    } catch (ConfigException e) {
      printException(e, args);

      System.exit(ExitCode.BAD_CONFIG.ordinal());
    } catch (Exception e) {
      printException(e, args);

      System.exit(ExitCode.UNKNOWN.ordinal());
    }
    */
  }

  private static void printException(Throwable e,
                                     ArgsResinCli args)
  {
    System.err.println(e.getMessage());

    if (e.getMessage() == null
        || args.isVerbose()
        || log.isLoggable(Level.FINE)) {
      e.printStackTrace();
    }
  }

  private static L10N L()
  {
    if (_L == null) {
      _L = new L10N(ResinCommandLine.class);
    }
    
    return _L;
  }
}
