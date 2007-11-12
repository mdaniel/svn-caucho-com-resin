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
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.timer;

import javax.ejb.*;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.*;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.util.L10N;

/**
 * Implements the EJB timer handle.
 */
public class EjbTimerHandle implements TimerHandle {
  private static final L10N L = new L10N(EjbTimerHandle.class);
  protected static final Logger log
    = Logger.getLogger(EjbTimerHandle.class.getName());

  private Date _expiration;
  private long _interval;
  private Serializable _info;
  private String _serverId;
  private long _timerId;

  EjbTimerHandle(Date expiration,
                 long interval,
                 Serializable info,
                 String serverId,
                 long timerId)
  {
    _expiration = expiration;
    _interval = interval;
    _info = info;
    _serverId = serverId;
    _timerId = timerId;
  }

  /**
   * Returns the timer.
   */
  public Timer getTimer()
    throws NoSuchObjectLocalException, EJBException
  {
    try {
      EjbServerManager manager = EjbServerManager.getLocal();

      checkNotNull(manager);

      AbstractServer server = manager.getServer(_serverId);

      checkNotNull(server);

      EjbTimerService timerService
        = (EjbTimerService) server.getTimerService();

      checkNotNull(timerService);

      EjbTimer timer = timerService.__caucho_find(_timerId);

      checkNotNull(timer);

      return timer;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      throw new EJBException(e);
    }
  }

  private void checkNotNull(Object obj)
    throws NoSuchObjectLocalException
  {
    if (obj == null)
      throw new NoSuchObjectLocalException(this + " not found");
  }

  public String toString()
  {
    return "EjbTimerHandle[" + _serverId + ", "
      + _timerId + ", "
      + _expiration + ", "
      + _interval + ", "
      + _info + "]";
  }
}
