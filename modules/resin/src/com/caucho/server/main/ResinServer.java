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

package com.caucho.server.main;

import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.loader.Environment;
import com.caucho.server.container.ArgsServerBase;
import com.caucho.server.resin.ServerArgsResin;
import com.caucho.util.CompileException;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class ResinServer
{
  private static final Logger log = Logger.getLogger(ResinServer.class.getName());
  
  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml   : alternate configuration file
   * -port port        : set the server's portt
   * <pre>
   */
  public static void main(String []argv)
  {
    ArgsServerBase args = null;
    
    try {
      Environment.init();
      
      // Resin.validateEnvironment();
      
      args = new ServerArgsResin(argv);
      //args.parse();

      args.doCommand();
      /*
      Command command = args.getCommand();
      
      command.doCommand(args);
      */
      
      /*
      final Resin resin = new Resin(args);

      resin.initMain();

      resin.getServer();

      resin.waitForExit();

      if (! resin.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
                                       "Resin shutdown from unknown reason");
      }
      */
    } catch (Throwable e) {
      Throwable cause;

      if (args != null && args.isVerbose()) {
        e.printStackTrace();
      }
      
      for (cause = e;
           cause != null && cause.getCause() != null;
           cause = cause.getCause()) {
        if (cause instanceof CompileException) {
          break;
        }
      }

      if (cause instanceof BindException) {
        System.err.println(e.getMessage());

        log.severe(e.toString());

        log.log(Level.FINE, e.toString(), e);

        System.exit(ExitCode.BIND.ordinal());
      }
      else if (e instanceof ConfigException) {
        System.err.println(e.getMessage());

        log.log(Level.CONFIG, e.toString(), e);
        
        System.exit(ExitCode.BAD_CONFIG.ordinal());
      }
      else {
        System.err.println(e.getMessage());

        log.log(Level.WARNING, e.toString(), e);
        
        e.printStackTrace(System.err);
      }
    } finally {
      System.exit(ExitCode.UNKNOWN.ordinal());
    }
  }
}
