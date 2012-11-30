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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.date;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.QDate;

/**
 * Date functions.
 */
public class DateTime implements Cloneable
{
  public static final String ATOM = "Y-m-d\\TH:i:sP";
  public static final String COOKIE = "l, d-M-y H:i:s T";
  public static final String ISO8601 = "Y-m-d\\TH:i:sO";
  public static final String RFC822 = "D, d M y H:i:s O";
  public static final String RFC850 = "l, d-M-y H:i:s T";
  public static final String RFC1036 = "D, d M y H:i:s O";
  public static final String RFC1123 = "D, d M Y H:i:s O";
  public static final String RFC2822 = "D, d M Y H:i:s O";
  public static final String RFC3339 = "Y-m-d\\TH:i:sP";
  public static final String RSS = "D, d M Y H:i:s O";
  public static final String W3C = "Y-m-d\\TH:i:sP";

  private QDate _qDate;
  private DateTimeZone _dateTimeZone;

  protected DateTime(QDate qDate, DateTimeZone dateTimeZone)
  {
    _qDate = qDate;
    _dateTimeZone = dateTimeZone;
  }

  protected DateTime(Env env, StringValue timeString)
  {
    this(env, timeString, null);
  }

  protected DateTime(Env env, StringValue timeString, DateTimeZone dateTimeZone)
  {
    if (dateTimeZone == null) {
      dateTimeZone = new DateTimeZone(env);
    }

    _qDate = new QDate(dateTimeZone.getTimeZone(), env.getCurrentTime());
    _dateTimeZone = dateTimeZone;

    init(env, timeString);
  }

  private void init(Env env, StringValue timeString)
  {
    long now = env.getCurrentTime();
    _qDate.setGMTTime(now);

    if (timeString.equals("")) {
      _qDate.setHour(0);
      _qDate.setMinute(0);
      _qDate.setSecond(0);
    }

    DateParser parser = new DateParser(timeString, _qDate);

    parser.parse();
  }

  public static DateTime __construct(Env env,
                                     @Optional Value time,
                                     @Optional DateTimeZone timeZone)
  {
    StringValue timeStr = null;

    if (time.isDefault()) {
      timeStr = env.createString("now");
    }
    else {
      timeStr = time.toStringValue(env);
    }

    if (timeZone == null)
      return new DateTime(env, timeStr);
    else
      return new DateTime(env, timeStr, timeZone);
  }

  /*
  @ReturnNullAsFalse
  public static DateTime createFromFormat(Env env,
                                          String format,
                                          String timeStr,
                                          @Optional DateTimeZone timeZone)
  {

  }
  */

  @Override
  public Object clone()
  {
    QDate qDate = (QDate) _qDate.clone();
    DateTimeZone dateTimeZone = (DateTimeZone) _dateTimeZone.clone();

    return new DateTime(qDate, dateTimeZone);
  }

  public StringValue format(Env env, StringValue format)
  {
    long time = _qDate.getGMTTime() / 1000;

    QDate calendar = new QDate(_qDate.getLocalTimeZone());

    return DateModule.dateImpl(env, format, time, calendar);
  }

  public void modify(StringValue modify)
  {
    DateParser parser = new DateParser(modify, _qDate);

    long time = parser.parse();

    //setTime(time);
  }

  public long getTimestamp()
  {
    return getTime() / 1000;
  }

  public DateTime setTimestamp(long timestamp) {
    setTime(timestamp * 1000);

    // XXX: DateTime may be extended in PHP, so need to return the child object
    return this;
  }

  public DateTimeZone getTimeZone()
  {
    return _dateTimeZone;
  }

  public DateTime setTimeZone(Env env, DateTimeZone dateTimeZone)
  {
    _dateTimeZone = dateTimeZone;

    long time = _qDate.getGMTTime();

    _qDate = new QDate(dateTimeZone.getTimeZone(), env.getCurrentTime());
    _qDate.setGMTTime(time);

    return this;
  }

  public long getOffset()
  {
    return _qDate.getZoneOffset() / 1000;
  }

  public void setTime(int hour,
                      int minute,
                      @Optional int second)
  {
    _qDate.setTime(hour, minute, second, 0);
  }

  public void setDate(int year,
                      int month,
                      int day)
  {
    _qDate.setDate(year, month - 1, day);
  }

  public void setISODate(int year,
                         int week, //yes, week, not month
                         @Optional int day)
  {
    throw new UnimplementedException("DateTime::setISODate()");
  }

  public DateInterval diff(Env env, DateTime dateTime,
                           @Optional boolean isAbsolute)
  {
    DateInterval dateInterval = new DateInterval();

    QDate qDate0 = _qDate;
    QDate qDate1 = dateTime._qDate;

    if (qDate0.getLocalTime() < qDate1.getLocalTime()) {
      qDate0 = dateTime._qDate;
      qDate1 = _qDate;
    }

    int year = qDate0.getYear() - qDate1.getYear();
    int month = qDate0.getMonth() - qDate1.getMonth();
    int day = qDate0.getDayOfMonth() - qDate1.getDayOfMonth();

    int hour = qDate0.getHour() - qDate1.getHour();;
    int minute = qDate0.getMinute() - qDate1.getMinute();
    int second = qDate0.getSecond() - qDate1.getSecond();

    if (second < 0) {
      minute--;

      second += 60;
    }

    if (minute < 0) {
      hour--;

      minute += 60;
    }

    if (hour < 0) {
      day--;

      hour += 24;
    }

    if (day < 0) {
      month--;

      day += qDate1.getDaysInMonth();
    }

    if (month < 0) {
      year--;

      month += 12;
    }

    dateInterval.y = year;
    dateInterval.m = month;
    dateInterval.d = day;

    dateInterval.h = hour;
    dateInterval.i = minute;
    dateInterval.s = second;

    // php/1959
    long diff = qDate0.getLocalTime() - qDate1.getLocalTime();
    long days = diff / (1000 * 60 * 60 * 24);

    dateInterval.days = LongValue.create(days);

    return dateInterval;
  }

  protected QDate getQDate()
  {
    return _qDate;
  }

  protected long getTime()
  {
    return _qDate.getGMTTime();
  }

  protected void setTime(long time)
  {
    _qDate.setGMTTime(time);
  }

  @Override
  public String toString()
  {
    Env env = Env.getInstance();

    return format(env, env.createString("now")).toString();
  }
}
