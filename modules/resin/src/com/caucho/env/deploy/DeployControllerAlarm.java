/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.env.deploy;

import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.WeakAlarm;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
class DeployControllerAlarm<C extends DeployController<?>>
  implements AlarmListener
{
  private C _controller;

  private Alarm _alarm = new WeakAlarm(this);
  
  private long _checkInterval;

  DeployControllerAlarm(C controller, long checkInterval)
  {
    _controller = controller;
    _checkInterval = checkInterval;
    
    if (checkInterval > 0)
      _alarm.queue(checkInterval);
  }

  /**
   * Handles the redeploy check alarm.
   */
  @Override
  public final void handleAlarm(Alarm alarm)
  {
    try {
      _controller.alarm();
    } finally {
      if (! _controller.getState().isDestroyed())
        alarm.queue(_checkInterval);
    }
  }
  
  public final void close()
  {
    _alarm.dequeue();
  }

  /**
   * Returns the entry's debug name.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _controller + "]";
  }
}
