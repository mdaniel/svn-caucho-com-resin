/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.server.watchdog;

import com.caucho.cli.resin.ProgramInfoResin;


/**
 * Process responsible for watching a backend watchdog.
 */
public class WatchdogResin extends WatchdogMain
{
  protected WatchdogResin(String []argv)
    throws Exception
  {
    super(argv);
  }

  @Override
  protected ArgsWatchdog parseArgs()
  {
    ArgsWatchdog args = new ArgsWatchdog(getArgv(), new ProgramInfoResin());

    args.parse();

    return args;
  }

  /**
   * The launching program for the watchdog manager, generally called
   * from ResinBoot.
   */
  public static void main(String []argv)
    throws Exception
  {
    WatchdogResin main = new WatchdogResin(argv);
    
    main.start();
  }
}
