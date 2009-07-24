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

package com.caucho.ejb.timer;

import javax.ejb.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.*;

import com.caucho.ejb.AbstractContext;
import com.caucho.util.Alarm;
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

  private AbstractContext _context;
  private ArrayList<EjbTimer> _timers = new ArrayList<EjbTimer>();

  EjbTimerService(AbstractContext context)
  {
    _context = context;
  }

  public static EjbTimerService getLocal(ClassLoader loader,
                                         AbstractContext context)
  {
    synchronized (_localTimer) {
      EjbTimerService timer = _localTimer.get(loader);

      if (timer == null) {
        timer = new EjbTimerService(context);

        _localTimer.set(timer, loader);
      }

      return timer;
    }
  }

  public static EjbTimerService getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  public static EjbTimerService getCurrent(ClassLoader loader)
  {
    return _localTimer.get(loader);
  }

  /**
   * Creates a timer for a duration.
   */
  public Timer createTimer(long duration, Serializable info)
    throws EJBException
  {
    if (duration < 0)
      throw new IllegalArgumentException("Timer duration must not be negative");

    Date expiration = new Date(Alarm.getCurrentTime() + duration);

    return createTimer(expiration, info);
  }

  /**
   * Creates an interval timer
   */
  public Timer createTimer(long initialDuration,
                           long intervalDuration,
                           Serializable info)
    throws EJBException
  {
    if (initialDuration < 0)
      throw new IllegalArgumentException("Timer initial duration must not be negative");

    if (intervalDuration < 0)
      throw new IllegalArgumentException("Timer interval duration must not be negative");

    Date initialExpiration = new Date(Alarm.getCurrentTime() + initialDuration);

    return createTimer(initialExpiration, intervalDuration, info);
  }

  /**
   * Creates a timer
   */
  public Timer createTimer(Date expiration,
                           Serializable info)
    throws EJBException
  {
    EjbTimer timer = new EjbTimer(expiration, info, _context);

    _timers.add(timer);

    return timer;
  }

  /**
   * Creates a timer
   */
  public Timer createTimer(Date expiration,
                           long interval,
                           Serializable info)
    throws EJBException
  {
    if (interval < 0)
      throw new IllegalArgumentException("Timer interval must not be negative");

    EjbTimer timer = new EjbTimer(expiration, interval, info, _context);

    _timers.add(timer);

    return timer;
  }

  /**
   * Returns the timers
   */
  public Collection getTimers()
    throws EJBException
  {
    return _timers;
  }

  /**
   * Finds a timer by id
   */
  EjbTimer __caucho_find(long timerId)
  {
    for (EjbTimer timer : _timers) {
      if (timer.__caucho_getId() == timerId)
        return timer;
    }

    return null;
  }
  
  public Timer createCalendarTimer(ScheduleExpression schedule,
                                   Serializable info)
    throws IllegalArgumentException, IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public Timer createCalendarTimer(ScheduleExpression schedule,
                                   TimerConfig timerConfig)
    throws IllegalArgumentException, IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public Timer createIntervalTimer(long initialDuration,
                                   long intervalDuration,
                                   TimerConfig timerConfig)
    throws IllegalArgumentException,
           IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public Timer createSingleActionTimer(Date expiration,
                                       TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  public Timer createIntervalTimer(Date initialExpiration,
                                   long intervalDuration,
                                   TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "EjbTimerService[]";
  }
}
