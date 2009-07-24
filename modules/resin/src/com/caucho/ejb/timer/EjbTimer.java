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
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.timer;

import javax.ejb.*;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import com.caucho.ejb.AbstractContext;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.server.util.ScheduledThreadPool;

/**
 * Implements the EJB timer.
 */
public class EjbTimer implements javax.ejb.Timer, Runnable {
  private static final L10N L = new L10N(EjbTimer.class);
  protected static final Logger log
    = Logger.getLogger(EjbTimer.class.getName());

  private ScheduledThreadPool _threadPool;
  private Future _future;

  private Date _expiration;
  private long _interval;
  private Serializable _info;
  private AbstractContext _context;
  private long _timerId;

  private static long _currentTimerId;

  EjbTimer(Date expiration, Serializable info, AbstractContext context)
  {
    this(expiration, -1, info, context);
  }

  EjbTimer(Date expiration,
           long interval,
           Serializable info,
           AbstractContext context)
  {
    _expiration = expiration;
    _interval = interval;
    _info = info;

    if (context == null)
      throw new NullPointerException();

    _context = context;
    _timerId = _currentTimerId++;

    long initialDelay = getTimeRemaining();

    _threadPool = ScheduledThreadPool.getLocal();

    if (interval <= 0)
      _future = _threadPool.schedule(this,
                                     initialDelay,
                                     TimeUnit.MILLISECONDS);
    else
      _future = _threadPool.scheduleWithFixedDelay(this,
                                                   initialDelay,
                                                   interval,
                                                   TimeUnit.MILLISECONDS);
  }

  /**
   * Cancels the timer.
   */
  public void cancel()
    throws NoSuchObjectLocalException, EJBException
  {
    try {
      _future.cancel(true);
    } finally {
      _expiration = new Date(Alarm.getCurrentTime());
      _interval = -1;
    }
  }

  /**
   * Returns the timer handle.
   */
  public TimerHandle getHandle()
    throws NoSuchObjectLocalException, EJBException
  {
    return new EjbTimerHandle(_expiration,
                              _interval,
                              _info,
                              _context.getServer().getEJBName(),
                              _timerId);
  }

  /**
   * Returns timer information.
   */
  public Serializable getInfo()
    throws NoSuchObjectLocalException, EJBException
  {
    return _info;
  }

  /**
   * Returns the time corresponding to the next scheduled expiration.
   */
  public Date getNextTimeout()
    throws NoSuchObjectLocalException, EJBException
  {
    checkExpiration();

    return _expiration;
  }

  /**
   * Returns the time remaining in milliseconds.
   */
  public long getTimeRemaining()
    throws NoSuchObjectLocalException, EJBException
  {
    return checkExpiration();
  }

  public void run()
  {
    _context.__caucho_timeout_callback(this);
  }

  long __caucho_getId()
  {
    return _timerId;
  }

  private long checkExpiration()
  {
    long delay = _expiration.getTime() - Alarm.getCurrentTime();

    if (delay < 0 && _interval < 0)
      throw new NoSuchObjectLocalException(this + " is expired");

    return delay;
  }
  
  public ScheduleExpression getSchedule()
    throws IllegalStateException, NoSuchObjectLocalException, EJBException
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isPersistent() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return ("EjbTimer[" + _timerId + ", "
            + _expiration + ", "
            + _interval + ", "
            + _info + "]");
  }
}
