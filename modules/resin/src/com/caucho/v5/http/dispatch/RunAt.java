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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.dispatch;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.util.IntArray;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;

/**
 * Configuration for a run-at
 */
public class RunAt {
  static L10N L = new L10N(RunAt.class);
  private QDate _cal = QDate.createLocal();

  private long _period = -1;
  
  private IntArray _hourTimes; 
  private IntArray _minuteTimes; 
  
  /**
   * Creates a new servlet configuration object.
   */
  public RunAt()
  {
  }

  /**
   * Adds the text.
   */
  public void addText(String runAt)
    throws ConfigException
  {
    configureRunAt(runAt);
  }

  /**
   * Sets the period
   */
  public void setPeriod(Period period)
    throws ConfigException
  {
    _period = period.getPeriod();
  }

  /**
   * Returns the next time.
   */
  public long getNextTimeout(long now)
  {
    _cal.setGMTTime(now);
    long zone = _cal.getZoneOffset();
    
    if (_period > 0)
      return Period.periodEnd(now + zone, _period) - zone;

    now = now - now % 60000;

    long local = now + zone;
    
    long dayMinutes = (local / 60000) % (24 * 60);
    long hourMinutes = dayMinutes % 60;
    
    long nextDelta = Long.MAX_VALUE;

    for (int i = 0; _hourTimes != null && i < _hourTimes.size(); i++) {
      long time = _hourTimes.get(i);
      long delta = (time - dayMinutes + 24 * 60) % (24 * 60);

      if (delta == 0)
        delta = 24 * 60;
      
      if (delta < nextDelta && delta > 0)
        nextDelta = delta;
    }

    for (int i = 0; _minuteTimes != null && i < _minuteTimes.size(); i++) {
      long time = _minuteTimes.get(i);
      long delta = (time - hourMinutes + 60) % 60;
      
      if (delta == 0)
        delta = 60;

      if (delta < nextDelta && delta > 0)
        nextDelta = delta;
    }

    if (nextDelta < Integer.MAX_VALUE)
      return now + nextDelta * 60000L;
    else
      return Long.MAX_VALUE / 2;
  }

  /**
   * Configures the run-at time.
   */
  private void configureRunAt(String string)
    throws ConfigException
  {
    int len = string.length();
    char ch = 0;
    int i = 0;
    while (true) {
      for (;
           i < len &&
             (Character.isWhitespace(ch = string.charAt(i)) || ch == ',');
           i++) {
      }

      if (i >= len)
        return;

      if (! (ch >= '0' && ch <= '9' || ch == ':'))
        throw new ConfigException(L.l("illegal run-at time `{0}'.  Run-at values are either hour (0:00, 6:30, 12:15) or minute (:15, :30, :45).",
                                      string));
      
      int hour = 0;
      int minute = 0;
      boolean hasHour = false;
      for (; i < len && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++) {
        hasHour = true;
        hour = 10 * hour + ch - '0';
      }

      if (ch == ':') {
        i++;
        for (; i < len && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++)
          minute = 10 * minute + ch - '0';

      }

      if (hasHour) {
        if (_hourTimes == null)
          _hourTimes = new IntArray();
        _hourTimes.add(60 * hour + minute);
      }
      else {
        if (_minuteTimes == null)
          _minuteTimes = new IntArray();
        _minuteTimes.add(minute);
      }
    }
  }
}
