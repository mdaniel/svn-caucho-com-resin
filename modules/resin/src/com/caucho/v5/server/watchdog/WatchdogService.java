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

import io.baratine.core.Result;

import com.caucho.v5.amp.spi.ShutdownModeAmp;

/**
 * Service API for communicating with the watchdog.
 */
public interface WatchdogService
{
  String getHomeDirectory();
  
  String startAll(String[] argv);
  void startAll(String[] argv, Result<String> result);
  
  String start(int serverPort, String[] argv);
  void start(int serverPort, String[] argv, Result<String> result);
  
  String waitForStart(int serverPort);
  void waitForStart(int serverPort, Result<String> result);

  String stop(int serverPort, ShutdownModeAmp mode);

  String kill(int serverPort);

  String restart(int serverPort, String[] argv);

  String shutdown(ShutdownModeAmp mode);

  String status();

  boolean ping();
}
