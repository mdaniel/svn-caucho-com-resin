/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.*;
import com.caucho.vfs.Path;

import java.lang.reflect.*;
import java.net.*;
import java.util.Date;
import java.util.logging.*;

/**
 * Thread responsible for watching a backend server.
 */
class WatchdogTask implements Runnable
{
  private static final L10N L = new L10N(WatchdogTask.class);
  private static final Logger log
    = Logger.getLogger(WatchdogTask.class.getName());

  private final Watchdog _watchdog;
  
  private final Lifecycle _lifecycle = new Lifecycle();

  private WatchdogProcess _process;

  WatchdogTask(Watchdog watchdog)
  {
    _watchdog = watchdog;
  }

  boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns the state name.
   */
  String getState()
  {
    return _lifecycle.getStateName();
  }

  public void start()
  {
    if (! _lifecycle.toActive())
      return;

    Thread thread = new Thread(this, "watchdog-" + _watchdog.getId());
    thread.setDaemon(false);

    thread.start();
  }

  public void stop()
  {
    if (! _lifecycle.toDestroy())
      return;

    WatchdogProcess process = _process;
    if (process != null)
      process.stop();
  }

  public void run()
  {
    try {
      int i = 0;
      long retry = Long.MAX_VALUE;
    
      while (_lifecycle.isActive() && i++ < retry) {
	String id = String.valueOf(i);

	_watchdog.notifyTaskStarted();
	
	_process = new WatchdogProcess(id, _watchdog);
	try {
	  _process.run();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	} finally {
	  _process.destroy();
	}
      }
    } finally {
      _lifecycle.toDestroy();

      _watchdog.completeTask(this);
    }
  }
  
  /**
   * kills the task
   */
  void kill()
  {
    WatchdogProcess process = _process;
    _process = null;
    
    if (process != null)
      process.destroy();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _watchdog + "]";
  }
}
