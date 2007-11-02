/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.timer;

import javax.ejb.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.logging.*;

import com.caucho.util.L10N;
import com.caucho.loader.EnvironmentLocal;

/**
 * Implements the timer service
 */
public class EjbTimerService implements TimerService {
  private static final L10N L = new L10N(EjbTimerService.class);
  protected static final Logger log
    = Logger.getLogger(EjbTimerService.class.getName());

  private static final EnvironmentLocal<EjbTimerService> _localTimer
    = new EnvironmentLocal<EjbTimerService>();

  public static EjbTimerService getLocal(ClassLoader loader)
  {
    synchronized (_localTimer) {
      EjbTimerService timer = _localTimer.get(loader);
      
      if (timer == null) {
	timer = new EjbTimerService();

	_localTimer.set(timer, loader);
      }
      
      return timer;
    }
  }
  /**
   * Creates a timer for a duration.
   */
  public Timer createTimer(long duration, Serializable info)
    throws EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates an interval timer
   */
  public Timer createTimer(long initialDuration,
			   long intervalDuration,
			   Serializable info)
    throws EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a timer
   */
  public Timer createTimer(Date expiration,
			   Serializable info)
    throws EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a timer
   */
  public Timer createTimer(Date expiration,
			   long interval,
			   Serializable info)
    throws EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns the timers
   */
  public Collection getTimers()
    throws EJBException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "EjbTimerService[]";
  }
}
