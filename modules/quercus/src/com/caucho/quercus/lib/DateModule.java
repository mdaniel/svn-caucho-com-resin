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

package com.caucho.quercus.lib;

import java.util.TimeZone;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;
import com.caucho.util.CharBuffer;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

/**
 * Date functions.
 */
public class DateModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(DateModule.class);
  private static final Logger log
    = Logger.getLogger(DateModule.class.getName());

  public static final int CAL_GREGORIAN = 0;
  public static final int CAL_JULIAN = 1;

  private static final String []_shortDayOfWeek = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
  };

  private static final String []_fullDayOfWeek = {
    "Sunday", "Monday", "Tuesday", "Wednesday",
    "Thursday", "Friday", "Saturday", "Sunday"
  };

  private static final String []_shortMonth = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  };
  private static final String []_fullMonth = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  };

  private QDate _localCalendar = QDate.createLocal();
  private QDate _gmtCalendar = new QDate();

  /**
   * Returns the days in a given month.
   */
  public int cal_days_in_month(int cal, int month, int year)
  {
    QDate date = new QDate();

    date.setYear(year);
    date.setMonth(month - 1);

    return date.getDaysInMonth();
  }

  /**
   * Returns the days in a given month.
   */
  public static boolean checkdate(int month, int day, int year)
  {
    if (! (1 <= year && year <= 32767))
      return false;
    
    if (! (1 <= month && month <= 12))
      return false;

    return 1 <= day && day <= cal_days_in_month(0, month, year);
  }


  /**
   * Returns the formatted date.
   */
  public String date(String format,
		     @Optional("time()") long time)
  {
    return date(format, time, false);
  }

  /**
   * Returns the timestamp of easter.
   */
  public static long easter_date(@Optional("-1") int year)
  {
    QDate date = new QDate();
    
    if (year < 0) {
      date.setGMTTime(Alarm.getCurrentTime());
      
      year = date.getYear();
    }

    int y = year;

    int c = y / 100;
    int n = y - 19 * (y / 19);
    int k = (c - 17) / 25;
    int i = c - c /4 - (c - k) / 3 + 19 * n + 15;
    i = i - 30 * (i / 30);
    i = i - (i / 28) * (1 - ((i / 28) *
			     (29 / (i + 1)) *
			     ((21 - n) / 11)));

    int j = y + y / 4 + i + 2 - c + c / 4;
    j = j - 7 * (j / 7);
    int l = i - j;
    int m = 3 + (l + 40) / 44;
    int d = l + 28 - 31 * (m / 4);

    date.setYear(year);
    date.setMonth(m - 1);
    date.setDayOfMonth(d);

    return date.getGMTTime() / 1000;
  }

  /**
   * Returns the timestamp of easter.
   */
  public static long easter_days(@Optional("-1") int year,
				 @Optional int method)
  {
    return easter_date(year);
  }

  /**
   * Returns an array of the current date.
   */
  public Value getdate(@Optional("time()") long time)
  {
    QDate date = new QDate(false);

    date.setLocalTime(1000 * time);

    ArrayValue array = new ArrayValueImpl();

    array.put("seconds", date.getSecond());
    array.put("minutes", date.getMinute());
    array.put("hours", date.getHour());
    array.put("mday", date.getDayOfMonth());
    array.put("wday", date.getDayOfWeek());
    array.put("mon", date.getMonth() + 1);
    array.put("year", date.getYear());
    array.put("yday", date.getDayOfYear());
    array.put("weekday", _fullDayOfWeek[date.getDayOfWeek()]);
    array.put("month", _fullMonth[date.getMonth()]);
    array.put(new LongValue(0), new LongValue(time));

    return array;
  }

  public Value gettimeofday(@Optional boolean isFloatReturn)
  {
    long gmtTime = Alarm.getCurrentTime();

    if (isFloatReturn) {
      return new DoubleValue(((double) Alarm.getCurrentTime()) / 1000.0);
    }
    else {
      ArrayValueImpl result = new ArrayValueImpl();

      TimeZone localTimeZone = TimeZone.getDefault();

      long sec = gmtTime / 1000L;
      long microsec = (gmtTime - (sec * 1000)) * 1000L;
      long minutesWest = localTimeZone.getRawOffset() / 1000L / 60L * -1L;
      long dstTime = localTimeZone.useDaylightTime() ? 1 : 0;

      result.put("sec", sec);
      result.put("usec", microsec);
      result.put("minuteswest", minutesWest);
      result.put("dsttime", dstTime);

      return result;
    }
  }

  /**
   * Returns the formatted date.
   */
  public String gmdate(String format,
		       @Optional("time()") long time)
  {
    return date(format, time, true);
  }

  /**
   * Returns the formatted date.
   */
  public long gmmktime(int hour,
		       @Optional("-1") int minute,
		       @Optional("-1") int second,
		       @Optional("-1") int month,
		       @Optional("-1") int day,
		       @Optional("-1") int year)
  {
    QDate localDate = new QDate(false);
    QDate gmtDate = new QDate(false);
    long now = Alarm.getCurrentTime();

    localDate.setLocalTime(now);

    long gmtNow = localDate.getGMTTime();

    gmtDate.setGMTTime(gmtNow);

    if (hour >= 0)
      gmtDate.setHour(hour);

    if (minute >= 0)
      gmtDate.setMinute(minute);

    if (second >= 0)
      gmtDate.setSecond(second);

    if (month > 0)
      gmtDate.setMonth(month - 1);

    if (day > 0)
      gmtDate.setDayOfMonth(day);

    if (year > 0)
      gmtDate.setYear(year);

    return gmtDate.getGMTTime() / 1000L;
  }

  /**
   * Returns the formatted date.
   */
  private String date(String format,
		      long time,
		      boolean isGMT)
  {
    long now = 1000 * time;

    QDate calendar = isGMT ? _gmtCalendar : _localCalendar;

    synchronized (calendar) {
      calendar.setGMTTime(now);

      CharBuffer sb = new CharBuffer();
      int len = format.length();

      for (int i = 0; i < len; i++) {
	char ch = format.charAt(i);

	switch (ch) {
	case 'd':
	  {
	    int day = calendar.getDayOfMonth();
	    sb.append(day / 10);
	    sb.append(day % 10);
	    break;
	  }

	case 'D':
	  {
	    int day = calendar.getDayOfWeek();

	    sb.append(_shortDayOfWeek[day - 1]);
	    break;
	  }

	case 'j':
	  {
	    int day = calendar.getDayOfMonth();
	    sb.append(day);
	    break;
	  }

	case 'l':
	  {
	    int day = calendar.getDayOfWeek();

	    sb.append(_fullDayOfWeek[day]);
	    break;
	  }

	case 'S':
	  {
	    int day = calendar.getDayOfMonth();

	    switch (day) {
	    case 1: case 21: case 31:
	      sb.append("st");
	      break;
	    case 2: case 22:
	      sb.append("nd");
	      break;
	    case 3: case 23:
	      sb.append("rd");
	      break;
	    default:
	      sb.append("th");
	      break;
	    }
	    break;
	  }

	case 'w':
	  {
	    int day = calendar.getDayOfWeek();

	    sb.append(day);
	    break;
	  }

	case 'z':
	  {
	    int day = calendar.getDayOfYear();

	    sb.append(day);
	    break;
	  }

	case 'W':
	  {
	    int week = calendar.getWeek();

	    sb.append(week);
	    break;
	  }

	case 'm':
	  {
	    int month = calendar.getMonth() + 1;
	    sb.append(month / 10);
	    sb.append(month % 10);
	    break;
	  }

	case 'M':
	  {
	    int month = calendar.getMonth();
	    sb.append(_shortMonth[month]);
	    break;
	  }

	case 'F':
	  {
	    int month = calendar.getMonth();
	    sb.append(_fullMonth[month]);
	    break;
	  }

	case 'n':
	  {
	    int month = calendar.getMonth() + 1;
	    sb.append(month);
	    break;
	  }

	case 't':
	  {
	    int days = calendar.getDaysInMonth();
	    sb.append(days);
	    break;
	  }

	case 'Y':
	  {
	    int year = calendar.getYear();

	    sb.append((year / 1000) % 10);
	    sb.append((year / 100) % 10);
	    sb.append((year / 10) % 10);
	    sb.append((year) % 10);
	    break;
	  }

	case 'y':
	  {
	    int year = calendar.getYear();

	    sb.append((year / 10) % 10);
	    sb.append((year) % 10);
	    break;
	  }

	case 'L':
	  {
	    if (calendar.isLeapYear())
	      sb.append(1);
	    else
	      sb.append(0);
	    break;
	  }

	case 'a':
	  {
	    int hour = calendar.getHour();

	    if (hour < 12)
	      sb.append("am");
	    else
	      sb.append("pm");
	    break;
	  }

	case 'A':
	  {
	    int hour = calendar.getHour();

	    if (hour < 12)
	      sb.append("AM");
	    else
	      sb.append("PM");
	    break;
	  }

	case 'g':
	  {
	    int hour = calendar.getHour() % 12;

	    if (hour == 0)
	      hour = 12;

	    sb.append(hour);
	    break;
	  }

	case 'G':
	  {
	    int hour = calendar.getHour();

	    sb.append(hour);
	    break;
	  }

	case 'h':
	  {
	    int hour = calendar.getHour() % 12;

	    if (hour == 0)
	      hour = 12;

	    sb.append(hour / 10);
	    sb.append(hour % 10);
	    break;
	  }

	case 'H':
	  {
	    int hour = calendar.getHour();

	    sb.append(hour / 10);
	    sb.append(hour % 10);
	    break;
	  }

	case 'i':
	  {
	    int minutes = calendar.getMinute();

	    sb.append(minutes / 10);
	    sb.append(minutes % 10);
	    break;
	  }

	case 's':
	  {
	    int seconds = calendar.getSecond();

	    sb.append(seconds / 10);
	    sb.append(seconds % 10);
	    break;
	  }

	case 'O':
	  {
	    long offset = calendar.getZoneOffset();

	    int minute = (int) (offset / (60 * 1000));

	    if (minute < 0)
	      sb.append('-');
	    else
	      sb.append('+');

	    sb.append((minute / 60) / 10);
	    sb.append((minute / 60) % 10);
	    sb.append((minute / 10) % 10);
	    sb.append(minute % 10);
	    break;
	  }

	case 'I':
	  {
	    if (calendar.isDST())
	      sb.append('1');
	    else
	      sb.append('0');
	    break;
	  }

	case 'T':
	  {
	    TimeZone zone = calendar.getLocalTimeZone();

	    sb.append(zone.getDisplayName(calendar.isDST(), TimeZone.SHORT));
	    break;
	  }

	case 'Z':
	  {
	    long offset = calendar.getZoneOffset();

	    sb.append(offset / (1000));
	    break;
	  }

	case 'c':
	  {
	    sb.append(calendar.printISO8601());
	    break;
	  }

	case 'r':
	  {
	    sb.append(calendar.printDate());
	    break;
	  }

	case 'U':
	  {
	    sb.append(now / 1000);
	    break;
	  }

	case '\\':
	  sb.append(format.charAt(i++));
	  break;

	default:
	  sb.append(ch);
	  break;
	}
      }

      return sb.toString();
    }
  }

  /**
   * Returns the time including microseconds
   */
  public static Value microtime(@Optional boolean getAsFloat)
  {
    long now = Alarm.getCurrentTime();

    if (getAsFloat) {
      return new DoubleValue((double) now / 1000.0);
    }
    else {
      return new StringValueImpl((now % 1000 * 1000) + " " + (now / 1000));
    }
  }

  /**
   * Returns the formatted date.
   */
  public long mktime(int hour,
		     @Optional("-1") int minute,
		     @Optional("-1") int second,
		     @Optional("-1") int month,
		     @Optional("-1") int day,
		     @Optional("-1") int year)
  {
    QDate date = new QDate(false);

    long now = Alarm.getCurrentTime();

    date.setLocalTime(now);

    if (hour >= 0)
      date.setHour(hour);

    if (minute >= 0)
      date.setMinute(minute);

    if (second >= 0)
      date.setSecond(second);

    if (month > 0)
      date.setMonth(month - 1);

    if (day > 0)
      date.setDayOfMonth(day);

    if (year > 0)
      date.setYear(year);

    return date.getLocalTime() / 1000L;
  }

  /**
   * Returns the formatted date.
   */
  public String strftime(String format,
			 @Optional("-1") long phpTime)
  {
    long time;

    if (phpTime < 0)
      time = Alarm.getCurrentTime();
    else
      time = 1000 * phpTime;

    return QDate.formatLocal(time, format);
  }

  /**
   * Parses the time
   */
  public Value strtotime(String timeString,
			 @Optional("-1") long now)
  {
    try {
      if (now >= 0)
	now = 1000L * now;
      else
	now = Alarm.getCurrentTime();

      if (timeString.equals("now"))
	return new LongValue(now / 1000L);

      long time = new QDate().parseLocalDate(timeString);

      // only handling exact dates

      return new LongValue(time / 1000L);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the current time in seconds.
   */
  public static long time()
  {
    return Alarm.getCurrentTime() / 1000L;
  }
}

