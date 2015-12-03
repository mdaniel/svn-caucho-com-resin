/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

/**
 * Representations of time in milliseconds.
 */
public class Period {
  static L10N L = new L10N(Period.class);

  public static final long SECOND = 1000L;
  public static final long MINUTE = 60L * SECOND;
  public static final long HOUR = 60L * MINUTE;
  public static final long DAY = 24L * HOUR;
  /** 30 days */
  public static final long MONTH = DAY * 30L;
  /** 365 days */
  public static final long YEAR = DAY * 365L;
  public static final long INFINITE = (Long.MAX_VALUE / 2 / 1000) * 1000;
  public static final long FOREVER = INFINITE;
  
  private long _period;

  public Period()
  {
  }

  public Period(long period)
  {
    _period = period;
  }

  /**
   * Returns the default units (default is 1000)
   */
  public long getDefaultUnits()
  {
    return 1000;
  }
  
  /**
   * Sets the text.
   */
  public void addText(String text)
    throws ConfigException
  {
    _period = toPeriod(text, getDefaultUnits());
  }

  /**
   * Replace with the real path.
   */
  public long getPeriod()
  {
    return _period;
  }

  /**
   * Converts a period string to a time.
   *
   * <table>
   * <tr><td>ms<td>milliseconds
   * <tr><td>s<td>seconds
   * <tr><td>m<td>minutes
   * <tr><td>h<td>hours
   * <tr><td>D<td>days
   * <tr><td>W<td>weeks
   * <tr><td>M<td>months
   * <tr><td>Y<td>years
   * </table>
   */
  public static long toPeriod(String value)
    throws ConfigException
  {
    return toPeriod(value, 1000);
  }

  /**
   * Converts a period string to a time.
   *
   * <table>
   * <tr><td>ms<td>milliseconds
   * <tr><td>s<td>seconds
   * <tr><td>m<td>minutes
   * <tr><td>h<td>hours
   * <tr><td>D<td>days
   * <tr><td>W<td>weeks
   * <tr><td>M<td>months
   * <tr><td>Y<td>years
   * </table>
   */
  public static long toPeriod(String value, long defaultUnits)
    throws ConfigException
  {
    if (value == null)
      return 0;
    
    long sign = 1;
    long period = 0;

    int i = 0;
    int length = value.length();
    
    if (length > 0 && value.charAt(i) == '-') {
      sign = -1;
      i++;
    }

    while (i < length) {
      long delta = 0;
      char ch;

      for (; i < length && (ch = value.charAt(i)) >= '0' && ch <= '9'; i++)
        delta = 10 * delta + ch - '0';

      if (length <= i)
        period += defaultUnits * delta;
      else {
        ch = value.charAt(i++);
        switch (ch) {
        case 's':
          period += 1000 * delta;
          break;

        case 'm':
          if (i < value.length() && value.charAt(i) == 's') {
            i++;
            period += delta;
          }
          else
            period += 60 * 1000 * delta;
          break;

        case 'h':
          period += 60L * 60 * 1000 * delta;
          break;

        case 'D':
          period += DAY * delta;
          break;

        case 'W':
          period += 7L * DAY * delta;
          break;

        case 'M':
          period += 30L * DAY * delta;
          break;

        case 'Y':
          period += 365L * DAY * delta;
          break;

        default:
          throw new ConfigException(L.l("Unknown unit `{0}' in period `{1}'. Valid units are:\n  '10ms' milliseconds\n  '10s' seconds\n  '10m' minutes\n  '10h' hours\n  '10D' days\n  '10W' weeks\n  '10M' months\n  '10Y' years",
                                        String.valueOf(ch), value));
        }
      }
    }

    period = sign * period;

    // server/137w
    /*
    if (period < 0)
      return INFINITE;
    else
      return period;
    */
    
    return period;
  }

  /**
   * Calculates the next period end.  The calculation is in local time.
   *
   * @param now the current time in GMT ms since the epoch
   *
   * @return the time of the next period in GMT ms since the epoch
   */
  public static long periodEnd(long now, long period)
  {
    QDate localCalendar = QDate.allocateLocalDate();
    
    long endTime = periodEnd(now, period, localCalendar);
    
    QDate.freeLocalDate(localCalendar);
    
    return endTime;
  }
  
  /**
   * Calculates the next period end.  The calculation is in local time.
   *
   * @param now the current time in GMT ms since the epoch
   *
   * @return the time of the next period in GMT ms since the epoch
   */
  private static long periodEnd(long now, long period, QDate cal)
  {
    if (period < 0) {
      return Long.MAX_VALUE;
    }
    else if (period == 0) {
      return now;
    }
    
    long periodEnd;

    if (period < 30 * DAY) {
      cal.setGMTTime(now);

      long localTime = cal.getLocalTime();

      localTime = localTime + (period - (localTime + 4 * DAY) % period);

      cal.setLocalTime(localTime);

      periodEnd =  cal.getGMTTime();
    }

    else if (period % (30 * DAY) == 0) {
      int months = (int) (period / (30 * DAY));

      cal.setGMTTime(now);
      long year = cal.getYear();
      int month = cal.getMonth();

      cal.setLocalTime(0);
      
      cal.setDate(year, month + months, 1);

      periodEnd = cal.getGMTTime();
    }

    else if (period % (365 * DAY) == 0) {
      long years = (period / (365 * DAY));

      cal.setGMTTime(now);
      long year = cal.getYear();

      cal.setLocalTime(0);

      long newYear = year + (years - year % years);
      
      cal.setDate(newYear, 0, 1);

      periodEnd = cal.getGMTTime();
    }
    else {
      cal.setGMTTime(now);

      long localTime = cal.getLocalTime();

      localTime = localTime + (period - (localTime + 4 * DAY) % period);

      cal.setLocalTime(localTime);

      periodEnd = cal.getGMTTime();
    }
    
    if (periodEnd <= now || now + 2 * period <= periodEnd) {
      periodEnd = now + period;
    }
    
    return periodEnd;
  }

  public String toString()
  {
    return "Period[" + _period + "]";
  }
}
