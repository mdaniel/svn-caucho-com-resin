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
package com.caucho.scheduling;

import java.util.concurrent.atomic.AtomicReference;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Trigger;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

/**
 * Implements a cron-style trigger. This trigger is primarily intended for the
 * EJB calendar style timer service functionality.
 */
public class CronTrigger implements Trigger {
  private static final L10N L = new L10N(CronTrigger.class);

  private AtomicReference<QDate> _localCalendar = new AtomicReference<QDate>();

  private boolean[] _seconds;
  private boolean[] _minutes;
  private boolean[] _hours;
  private boolean[] _days;
  private boolean[] _months;
  private boolean[] _daysOfWeek;
  private boolean[] _years;
  private long _start = -1;
  private long _end = -1;

  /**
   * Creates new cron trigger.
   * 
   * @param cronExpression
   *          The cron expression to create the trigger from.
   * @param start
   *          The date the trigger should begin firing, in milliseconds. -1
   *          indicates that no start date should be enforced.
   * @param end
   *          The date the trigger should end firing, in milliseconds. -1
   *          indicates that no end date should be enforced.
   */
  public CronTrigger(final CronExpression cronExpression, final long start,
      final long end)
  {
    if (cronExpression.getSecond() != null) {
      _seconds = parseRange(cronExpression.getSecond(), 0, 59);
    }

    if (cronExpression.getMinute() != null) {
      _minutes = parseRange(cronExpression.getMinute(), 0, 59);
    }

    if (cronExpression.getHour() != null) {
      _hours = parseRange(cronExpression.getHour(), 0, 23);
    }

    if (cronExpression.getDayOfWeek() != null) {
      _daysOfWeek = parseRange(cronExpression.getDayOfWeek(), 0, 7);
    }

    if (_daysOfWeek[7]) {
      _daysOfWeek[0] = _daysOfWeek[7];
    }

    if (cronExpression.getDayOfMonth() != null) {
      _days = parseRange(cronExpression.getDayOfMonth(), 1, 31);
    }

    if (cronExpression.getMonth() != null) {
      _months = parseRange(cronExpression.getMonth(), 1, 12);
    }

    _start = start;
    _end = end;
  }

  /**
   * parses a range, following cron rules.
   */
  private boolean[] parseRange(String range, int rangeMin, int rangeMax)
      throws ConfigException
  {
    boolean[] values = new boolean[rangeMax + 1];

    int j = 0;
    while (j < range.length()) {
      char character = range.charAt(j);

      int min = 0;
      int max = 0;
      int step = 1;

      if (character == '*') {
        min = rangeMin;
        max = rangeMax;
        j++;
      } else if ('0' <= character && character <= '9') {
        for (; j < range.length() && '0' <= (character = range.charAt(j))
            && character <= '9'; j++) {
          min = 10 * min + character - '0';
        }

        if (j < range.length() && character == '-') {
          for (j++; j < range.length() && '0' <= (character = range.charAt(j))
              && character <= '9'; j++) {
            max = 10 * max + character - '0';
          }
        } else
          max = min;
      } else
        throw new ConfigException(L.l("'{0}' is an illegal cron range", range));

      if (min < rangeMin)
        throw new ConfigException(L.l(
            "'{0}' is an illegal cron range (min value is too small)", range));
      else if (rangeMax < max)
        throw new ConfigException(L.l(
            "'{0}' is an illegal cron range (max value is too large)", range));

      if (j < range.length() && (character = range.charAt(j)) == '/') {
        step = 0;

        for (j++; j < range.length() && '0' <= (character = range.charAt(j))
            && character <= '9'; j++) {
          step = 10 * step + character - '0';
        }

        if (step == 0)
          throw new ConfigException(L
              .l("'{0}' is an illegal cron range", range));
      }

      if (range.length() <= j) {
      } else if (character == ',')
        j++;
      else {
        throw new ConfigException(L.l("'{0}' is an illegal cron range", range));
      }

      for (; min <= max; min += step)
        values[min] = true;
    }

    return values;
  }

  /**
   * Gets the next time this trigger should be fired.
   * 
   * @param now
   *          The current time.
   * @return The next time this trigger should be fired.
   */
  @Override
  public long nextTime(long now)
  {
    QDate calendar = allocateCalendar();

    // Round up to seconds.
    long time = now + 1000 - now % 1000;

    calendar.setGMTTime(time);

    calendar = getNextTime(calendar);

    long nextTime = calendar.getGMTTime();

    freeCalendar(calendar);

    if (now < nextTime)
      return nextTime;
    else
      return nextTime(now + 3600000L); // Daylight savings time.
  }

  private QDate getNextTime(QDate currentTime)
  {
    int year = currentTime.getYear();

    QDate nextTime = getNextTimeInYear(currentTime);

    while (nextTime == null) { // TODO Limit year iterations to three.
      year++;

      currentTime.setSecond(0);
      currentTime.setMinute(0);
      currentTime.setHour(0);
      currentTime.setDayOfMonth(1);
      currentTime.setMonth(0); // The QDate implementation uses 0 indexed
      // months, but cron does not.
      currentTime.setYear(year);

      nextTime = getNextTimeInYear(currentTime);
    }

    return nextTime;
  }

  private QDate getNextTimeInYear(QDate currentTime)
  {
    int month = getNextMatch(_months, (currentTime.getMonth() + 1));

    if (month == -1) {
      return null;
    } else {
      if (month > (currentTime.getMonth() + 1)) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(0);
        currentTime.setDayOfMonth(1);
        currentTime.setMonth(month - 1);
      }

      QDate nextTime = getNextTimeInMonth(currentTime);

      while ((month < _months.length) && (nextTime == null)) {
        month++;
        month = getNextMatch(_months, month);

        if (month == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(0);
          currentTime.setDayOfMonth(1);
          currentTime.setMonth(month - 1);

          nextTime = getNextTimeInMonth(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInMonth(QDate currentTime)
  {
    int day = getNextDayMatch(currentTime.getDayOfMonth(), currentTime
        .getDayOfWeek(), currentTime.getDayOfMonth(), currentTime
        .getDaysInMonth());

    if (day == -1) {
      return null;
    } else {
      if (day > currentTime.getDayOfMonth()) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(0);
        currentTime.setDayOfMonth(day);
      }

      QDate nextTime = getNextTimeInDay(currentTime);

      if (nextTime == null) {
        day++;
        day = getNextDayMatch(currentTime.getDayOfMonth(), currentTime
            .getDayOfWeek(), day, currentTime.getDaysInMonth());

        if (day == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(0);
          currentTime.setDayOfMonth(day);

          return getNextTimeInDay(currentTime);
        }
      }

      return nextTime;
    }
  }

  private int getNextDayMatch(int initialDayOfMonth, int initialDayOfWeek,
      int day, int daysInMonth)
  {
    while (day <= daysInMonth) {
      day = getNextMatch(_days, day, (daysInMonth + 1));

      if (day == -1) {
        return -1;
      }

      int dayOfWeek = (((initialDayOfWeek - 1) + ((day - initialDayOfMonth) % 7)) % 7) + 1;
      int nextDayOfWeek = getNextMatch(_daysOfWeek, dayOfWeek);

      if (nextDayOfWeek == dayOfWeek) {
        return day;
      } else if (nextDayOfWeek == -1) {
        day += ((7 - dayOfWeek) + 1);
      } else {
        day += (nextDayOfWeek - dayOfWeek);
      }
    }

    return -1;
  }

  private QDate getNextTimeInDay(QDate currentTime)
  {
    int hour = getNextMatch(_hours, currentTime.getHour());

    if (hour == -1) {
      return null;
    } else {
      if (hour > currentTime.getHour()) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(hour);
      }

      QDate nextTime = getNextTimeInHour(currentTime);

      if (nextTime == null) {
        hour++;
        hour = getNextMatch(_hours, hour);

        if (hour == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(hour);

          return getNextTimeInHour(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInHour(QDate currentTime)
  {
    int minute = getNextMatch(_minutes, currentTime.getMinute());

    if (minute == -1) {
      return null;
    } else {
      if (minute > currentTime.getMinute()) {
        currentTime.setSecond(0);
        currentTime.setMinute(minute);
      }

      QDate nextTime = getNextTimeInMinute(currentTime);

      if (nextTime == null) {
        minute++;
        minute = getNextMatch(_minutes, minute);

        if (minute == -1) {
          return null;
        } else {
          currentTime.setSecond(0);          
          currentTime.setMinute(minute);

          return getNextTimeInMinute(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInMinute(QDate currentTime)
  {
    int second = getNextMatch(_seconds, currentTime.getSecond());

    if (second == -1) {
      return null;
    } else {
      currentTime.setSecond(second);

      return currentTime;
    }
  }

  private int getNextMatch(boolean[] range, int start)
  {
    return getNextMatch(range, start, range.length);
  }

  private int getNextMatch(boolean[] range, int start, int end)
  {
    for (int match = start; match < end; match++) {
      if (range[match]) {
        return match;
      }
    }

    return -1;
  }

  private QDate allocateCalendar()
  {
    QDate calendar = _localCalendar.getAndSet(null);

    if (calendar == null) {
      calendar = QDate.createLocal();
    }

    return calendar;
  }

  private void freeCalendar(QDate cal)
  {
    _localCalendar.set(cal);
  }
}