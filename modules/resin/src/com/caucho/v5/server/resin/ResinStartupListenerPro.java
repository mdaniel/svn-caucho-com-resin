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

import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.web.server.StartInfoListener;

/**
 * Callback when the Resin instance gets its startup information from
 * the watchdog.
 */
public class ResinStartupListenerPro implements StartInfoListener
{
  private static final String LOG_STARTUP_TYPE = "Resin|Startup";
  
  @Override
  public void setStartInfo(boolean isRestart, 
                           String startMessage,
                           ExitCode exitCode)
  {
    LogSystem logSystem = LogSystem.getCurrent();
    
    if (logSystem == null)
      return;
    
    String startupType = logSystem.createFullType(LOG_STARTUP_TYPE);
    
    String msg;
    
    if (isRestart)
      msg = "Restart: " + startMessage;
    else
      msg = "Start: " + startMessage;
    
    logSystem.log(startupType, msg); 
  }
}
