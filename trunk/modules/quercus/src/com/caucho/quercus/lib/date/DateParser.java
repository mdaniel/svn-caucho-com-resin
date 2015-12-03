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

import com.caucho.util.QDate;

public class DateParser
{
  private static final int INT = 1;
  private static final int PERIOD = 2;
  private static final int AGO = 3;
  private static final int AM = 4;
  private static final int PM = 5;
  private static final int MONTH = 6;
  private static final int WEEKDAY = 7;
  private static final int UTC = 8;

  private static final int UNIT_YEAR = 1;
  private static final int UNIT_MONTH = 2;
  private static final int UNIT_FORTNIGHT = 3;
  private static final int UNIT_WEEK = 4;
  private static final int UNIT_DAY = 5;
  private static final int UNIT_HOUR = 6;
  private static final int UNIT_MINUTE = 7;
  private static final int UNIT_SECOND = 8;
  private static final int UNIT_NOW = 9;

  private static final int NULL_VALUE = Integer.MAX_VALUE;

  private QDate _date;

  private CharSequence _s;
  private int _index;
  private int _length;

  private StringBuilder _sb = new StringBuilder();

  private int _peekToken;

  private int _value;
  private int _digits;
  private int _unit;
  private int _weekday;

  private boolean _hasDate;
  private boolean _hasTime;

  private static final long MINUTE = 60000L;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;

  public DateParser(CharSequence s, QDate date)
  {
    _date = date;
    _s = s;
    _length = s.length();
  }

  public long parse()
  {
    _value = NULL_VALUE;
    _unit = 0;

    while (true) {
      int token = nextToken();

      if (token == '-') {
        token = nextToken();

        if (token == INT)
          _value = -_value;
        else {
          _peekToken = token;
          continue;
        }
      }

      if (token < 0) {
        if (_hasDate && ! _hasTime)
          _date.setTime(0, 0, 0, 0);

        return _date.getGMTTime();
      }
      else if (token == INT) {
        int digits = _digits;
        int value = _value;

        token = nextToken();

        if (token == PERIOD) {
          parsePeriod();
        }
        else if (token == ':') {
          parseTime();
          _hasTime = true;
        }
        else if (token == '-') {
          parseISODate(value);
          _hasDate = true;
        }
        else if (token == '/') {
          parseUSDate(value);
          _hasDate = true;
        }
        else if (token == MONTH) {
          parseDayMonthDate(value);
          _hasDate = true;
        }
        else {
          _peekToken = token;

          parseBareInt(value, digits);
        }
      }
      else if (token == PERIOD) {
        parsePeriod();
      }
      else if (token == WEEKDAY) {
        addWeekday(_value, _weekday);
        _value = NULL_VALUE;
      }
      else if (token == MONTH) {
        parseMonthDate(_value);
        _hasDate = true;
      }
      else if (token == '@') {
        token = nextToken();

        if (token == INT) {
          int value = _value;
          _value = NULL_VALUE;

          _date.setGMTTime(value * 1000L);

          token = nextToken();
          if (token == '.') {
            token = nextToken();

            if (token != INT)
              _peekToken = token;
          }
          else {
            _peekToken = token;
          }
        }
      }
    }
  }

  private void parsePeriod()
  {
    int value = _value;
    int unit = _unit;

    _value = NULL_VALUE;
    _unit = 0;

    int token = nextToken();
    if (token == AGO)
      value = -value;
    else
      _peekToken = token;

    addTime(value, unit);
  }

  private void parseISODate(int value1)
  {
    //int year = _date.getYear();
    //int month = 0;
    //int day = 0;

    if (value1 < 0)
      value1 = - value1;

    int token = nextToken();

    int value2 = 0;

    if (token == INT) {
      value2 = _value;
      _value = NULL_VALUE;
    }
    else {
      _peekToken = token;
      return;
    }

    token = nextToken();

    if (token == '-') {
      token = nextToken();

      if (token == INT) {
        if (value1 < 0)
          _date.setYear(value1);
        else if (value1 <= 68)
          _date.setYear(2000 + value1);
        else if (value1 < 100)
          _date.setYear(1900 + value1);
        else
          _date.setYear(value1);

        _date.setMonth(value2 - 1);
        _date.setDayOfMonth(_value);
      }
      else {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        _peekToken = token;
      }
    }
    else {
      _date.setMonth(value1 - 1);
      _date.setDayOfMonth(value2);

      _peekToken = token;
    }
  }

  private void parseUSDate(int value1)
  {
    //int year = _date.getYear();
    //int month = 0;
    //int day = 0;

    if (value1 < 0)
      value1 = - value1;

    int token = nextToken();

    int value2 = 0;

    if (token == INT) {
      value2 = _value;
    }
    else {
      _peekToken = token;
      return;
    }

    _value = NULL_VALUE;
    token = nextToken();

    if (token == '/') {
      token = nextToken();

      if (token == INT) {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        if (_value < 0)
          _date.setYear(_value);
        else if (_value <= 68)
          _date.setYear(2000 + _value);
        else if (_value < 100)
          _date.setYear(1900 + _value);
        else
          _date.setYear(_value);
      }
      else {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        _peekToken = token;
      }
      _value = NULL_VALUE;
    }
    else {
      _date.setMonth(value1 - 1);
      _date.setDayOfMonth(value2);

      _peekToken = token;
    }
  }

  private void parseDayMonthDate(int value1)
  {
    //int year = _date.getYear();
    //int month = 0;
    //int day = 0;

    if (value1 < 0)
      value1 = - value1;

    int value2 = _value;

    _value = NULL_VALUE;
    int token = nextToken();

    if (token == '-') {
      _value = NULL_VALUE;
      token = nextToken();
    }

    // check that this is a real number (a year) rather than an ordinal
    if (token == INT && _digits > 0) {
      _date.setDayOfMonth(value1);
      _date.setMonth(value2 - 1);

      if (_value < 0)
        _date.setYear(_value);
      else if (_value <= 68)
        _date.setYear(2000 + _value);
      else if (_value < 100)
        _date.setYear(1900 + _value);
      else
        _date.setYear(_value);

      _value = NULL_VALUE;
    }
    else {
      _date.setDayOfMonth(value1);
      _date.setMonth(value2 - 1);

      _peekToken = token;
    }
  }

  private void parseMonthDate(int value1)
  {
    if (value1 < 0)
      value1 = - value1;

    _value = NULL_VALUE;
    int token = nextToken();

    if (token == '-') {
      _value = NULL_VALUE;
      token = nextToken();
    }

    if (token == INT) {
      int value2 = _value;

      _value = NULL_VALUE;
      token = nextToken();
      if (token == '-') {
        _value = NULL_VALUE;
        token = nextToken();
      }

      if (token == INT) {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        if (_value < 0)
          _date.setYear(_value);
        else if (_value <= 68)
          _date.setYear(2000 + _value);
        else if (_value < 100)
          _date.setYear(1900 + _value);
        else
          _date.setYear(_value);

        _value = NULL_VALUE;
      }
      else {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        _peekToken = token;
      }
    }
    else {
      _date.setMonth(value1 - 1);

      _peekToken = token;
    }
  }

  private void parseTime()
  {
    int hour = _value;
    _value = NULL_VALUE;
    if (hour < 0)
      hour = - hour;

    _date.setHour(hour);
    _date.setMinute(0);
    _date.setSecond(0);
    _date.setMillisecond(0);

    int token = nextToken();

    if (token == INT) {
      _date.setMinute(_value);
      _value = NULL_VALUE;
    }
    else {
      _peekToken = token;
      return;
    }

    token = nextToken();

    if (token == ':') {
      token = nextToken();

      if (token == INT) {
        _date.setSecond(_value);
        _value = NULL_VALUE;
      }
      else {
        _peekToken = token;
        return;
      }

      token = nextToken();

      if (token == '.') { // milliseconds
        token = nextToken();

        if (token == INT) {
          token = nextToken();
        }
        else {
          _peekToken = token;
          //_value = NULL_VALUE;

          return;
        }
      }
    }

    if (token == AM) {
      hour = _date.getHour();

      if (hour == 12)
        _date.setHour(0);
    }
    else if (token == PM) {
      hour = _date.getHour();

      if (hour == 12)
        _date.setHour(12);
      else
        _date.setHour(hour + 12);
    }
    else
      _peekToken = token;

    parseTimezone();
  }

  private void parseTimezone()
  {
    int token = nextToken();
    int sign = 1;
    boolean hasUTC = false;

    if (token == UTC) {
      token = nextToken();

      hasUTC = true;
    }

    if (token == '-')
      sign = -1;
    else if (token == '+')
      sign = 1;
    else {
      _peekToken = token;

      if (hasUTC) {
        _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
      }

      return;
    }

    token = nextToken();

    if (token != INT) {
      _peekToken = token;

      if (hasUTC) {
        _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
      }

      return;
    }
    else if (_digits == 4) {
      int hours = _value / 100;
      int minutes = _value % 100;

      int value = sign * (hours * 60 + minutes);
      _value = NULL_VALUE;

      // php/191g - php ignores any explicit
      // offset if the date specifies UTC/z/GMT
      if (hasUTC) {
        _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
      }
      else {
        _date.setGMTTime(_date.getGMTTime() - value * 60000L + _date.getZoneOffset());
      }

      return;
    }
    else if (_digits == 2) {
      int value = _value;

      token = nextToken();

      if (token != ':') {
        _value = sign * _value;
        _peekToken = token;

        if (hasUTC) {
          _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
        }

        return;
      }

      token = nextToken();

      if (token != INT) {
        return;
      }

      value = sign * (60 * value + _value);

      _date.setGMTTime(_date.getGMTTime() - value * 60000L + _date.getZoneOffset());
      return;
    }
    else {
      _value = sign * _value;
      _peekToken = token;

      if (hasUTC) {
        _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
      }

      return;
    }
  }

  private void addTime(int value, int unit)
  {
    if (value == NULL_VALUE)
      value = 1;
    else if (value == -NULL_VALUE)
      value = -1;

    switch (unit) {
      case UNIT_YEAR:
        _date.setYear(_date.getYear() + value);
        break;
      case UNIT_MONTH: {
        int month = _date.getMonth() + value;
        int year = _date.getYear();

        if (month < 0 || month >= 12) {
          year = year + month / 12;

          month = month % 12;
        }

        _date.setDate(year, month, _date.getDayOfMonth());
        break;
      }
      case UNIT_FORTNIGHT:
        _date.setDayOfMonth(_date.getDayOfMonth() + 14 * value);
        break;
      case UNIT_WEEK:
        _date.setDayOfMonth(_date.getDayOfMonth() + 7 * value);
        break;
      case UNIT_DAY:
        _date.setDayOfMonth(_date.getDayOfMonth() + value);
        break;
      case UNIT_HOUR:
        _date.setHour(_date.getHour() + value);
        break;
      case UNIT_MINUTE:
        _date.setMinute(_date.getMinute() + value);
        break;
      case UNIT_SECOND:
        _date.setSecond(_date.getSecond() + value);
        break;
    }
  }

  private void addWeekday(int value, int weekday)
  {
    if (value == NULL_VALUE)
      value = 0;
    else if (value == -NULL_VALUE)
      value = -1;

    _date.setDayOfMonth(_date.getDayOfMonth()
        + (8 + weekday - _date.getDayOfWeek()) % 7
        + 7 * value);
  }

  private void parseBareInt(int value, int digits)
  {
    if (digits == 8 && ! _hasDate) {
      _hasDate = true;

      _date.setYear(value / 10000);
      _date.setMonth((value / 100 % 12) - 1);
      _date.setDayOfMonth(value % 100);
    }
    else if (digits == 6 && ! _hasTime) {
      _hasTime = true;
      _date.setHour(value / 10000);
      _date.setMinute(value / 100 % 100);
      _date.setSecond(value % 100);

      parseTimezone();
    }
    else if (digits == 4 && ! _hasTime) {
      _hasTime = true;
      _date.setHour(value / 100);
      _date.setMinute(value % 100);
      _date.setSecond(0);
      parseTimezone();
    }
    else if (digits == 2 && ! _hasTime) {
      _hasTime = true;
      _date.setHour(value);
      _date.setMinute(0);
      _date.setSecond(0);
      parseTimezone();
    }

    int token = nextToken();
    if (token == '.') {
      _value = NULL_VALUE;
      token = nextToken();

      if (token == INT)
        _value = NULL_VALUE;
      else
        _peekToken = token;
    }
    else
      _peekToken = token;
  }

  private int nextToken()
  {
    if (_peekToken > 0) {
      int token = _peekToken;
      _peekToken = 0;
      return token;
    }

    while (true) {
      skipSpaces();

      int ch = read();

      if (ch < 0)
        return -1;
      else if (ch == '-')
        return '-';
      else if (ch == '+')
        return '+';
      else if (ch == ':')
        return ':';
      else if (ch == '.')
        return '.';
      else if (ch == '/')
        return '/';
      else if (ch == '@')
        return '@';
      else if ('0' <= ch && ch <= '9') {
        int value = 0;
        int digits = 0;

        for (; '0' <= ch && ch <= '9'; ch = read()) {
          digits++;
          value = 10 * value + ch - '0';
        }

        _value = value;
        _digits = digits;

        unread();

        return INT;
      }
      else if ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z') {
        _sb.setLength(0);

        for (;
            'a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || ch == '.';
            ch = read()) {
          _sb.append(Character.toLowerCase((char) ch));
        }

        unread();

        String s = _sb.toString();

        return parseString(s);
      }
      else {
        // skip
      }
    }
  }

  private int parseString(String s)
  {
    if (s.endsWith("."))
      s = s.substring(0, s.length() - 1);

    if ("now".equals(s)
        || "today".equals(s)) {
      _value = 0;
      _unit = UNIT_NOW;
      return PERIOD;
    }
    else if ("last".equals(s)) {
      _value = -1;
      _digits = 0;
      return INT;
    }
    else if ("this".equals(s)) {
      _value = 0;
      _digits = 0;
      return INT;
    }
    else if ("am".equals(s) || "a.m".equals(s)) {
      return AM;
    }
    else if ("pm".equals(s) || "p.m".equals(s)) {
      return PM;
    }
    else if ("next".equals(s)) {
      _value = 1;
      _digits = 0;
      return INT;
    }
    else if ("third".equals(s)) {
      _value = 3;
      _digits = 0;
      return INT;
    }
    else if ("fourth".equals(s)) {
      _value = 4;
      _digits = 0;
      return INT;
    }
    else if ("fifth".equals(s)) {
      _value = 5;
      _digits = 0;
      return INT;
    }
    else if ("sixth".equals(s)) {
      _value = 6;
      _digits = 0;
      return INT;
    }
    else if ("seventh".equals(s)) {
      _value = 7;
      _digits = 0;
      return INT;
    }
    else if ("eighth".equals(s)) {
      _value = 8;
      _digits = 0;
      return INT;
    }
    else if ("ninth".equals(s)) {
      _value = 9;
      _digits = 0;
      return INT;
    }
    else if ("tenth".equals(s)) {
      _value = 10;
      _digits = 0;
      return INT;
    }
    else if ("eleventh".equals(s)) {
      _value = 11;
      _digits = 0;
      return INT;
    }
    else if ("twelfth".equals(s)) {
      _value = 12;
      _digits = 0;
      return INT;
    }
    else if ("yesterday".equals(s)) {
      _value = -1;
      _unit = UNIT_DAY;
      return PERIOD;
    }
    else if ("tomorrow".equals(s)) {
      _value = 1;
      _unit = UNIT_DAY;
      return PERIOD;
    }
    else if ("ago".equals(s)) {
      return AGO;
    }
    else if ("year".equals(s) || "years".equals(s)) {
      _unit = UNIT_YEAR;
      return PERIOD;
    }
    else if ("month".equals(s) || "months".equals(s)) {
      _unit = UNIT_MONTH;
      return PERIOD;
    }
    else if ("fortnight".equals(s) || "fortnights".equals(s)) {
      _unit = UNIT_FORTNIGHT;
      return PERIOD;
    }
    else if ("week".equals(s) || "weeks".equals(s)) {
      _unit = UNIT_WEEK;
      return PERIOD;
    }
    else if ("day".equals(s) || "days".equals(s)) {
      _unit = UNIT_DAY;
      return PERIOD;
    }
    else if ("hour".equals(s) || "hours".equals(s)) {
      _unit = UNIT_HOUR;
      return PERIOD;
    }
    else if ("minute".equals(s) || "minutes".equals(s)) {
      _unit = UNIT_MINUTE;
      return PERIOD;
    }
    else if ("second".equals(s) || "seconds".equals(s)) {
      // php/191s - if preceded by a value token, treat as a period,
      // otherwise treat as an ordinal
      if (_value == NULL_VALUE) {
        _digits = 0;
        _value = 2;

        return INT;
      }

      _unit = UNIT_SECOND;
      return PERIOD;
    }
    else if ("january".equals(s) || "jan".equals(s)) {
      _value = 1;
      return MONTH;
    }
    else if ("february".equals(s) || "feb".equals(s)) {
      _value = 2;
      return MONTH;
    }
    else if ("march".equals(s) || "mar".equals(s)) {
      _value = 3;
      return MONTH;
    }
    else if ("april".equals(s) || "apr".equals(s)) {
      _value = 4;
      return MONTH;
    }
    else if ("may".equals(s)) {
      _value = 5;
      return MONTH;
    }
    else if ("june".equals(s) || "jun".equals(s)) {
      _value = 6;
      return MONTH;
    }
    else if ("july".equals(s) || "jul".equals(s)) {
      _value = 7;
      return MONTH;
    }
    else if ("august".equals(s) || "aug".equals(s)) {
      _value = 8;
      return MONTH;
    }
    else if ("september".equals(s) || "sep".equals(s) || "sept".equals(s)) {
      _value = 9;
      return MONTH;
    }
    else if ("october".equals(s) || "oct".equals(s)) {
      _value = 10;
      return MONTH;
    }
    else if ("november".equals(s) || "nov".equals(s)) {
      _value = 11;
      return MONTH;
    }
    else if ("december".equals(s) || "dec".equals(s)) {
      _value = 12;
      return MONTH;
    }
    else if ("sunday".equals(s) || "sun".equals(s)) {
      _weekday = 0;
      return WEEKDAY;
    }
    else if ("monday".equals(s) || "mon".equals(s)) {
      _weekday = 1;
      return WEEKDAY;
    }
    else if ("tuesday".equals(s) || "tue".equals(s) || "tues".equals(s)) {
      _weekday = 2;
      return WEEKDAY;
    }
    else if ("wednesday".equals(s) || "wed".equals(s) || "wednes".equals(s)) {
      _weekday = 3;
      return WEEKDAY;
    }
    else if ("thursday".equals(s) || "thu".equals(s)
        || "thur".equals(s) || "thurs".equals(s)) {
      _weekday = 4;
      return WEEKDAY;
    }
    else if ("friday".equals(s) || "fri".equals(s)) {
      _weekday = 5;
      return WEEKDAY;
    }
    else if ("saturday".equals(s) || "sat".equals(s)) {
      _weekday = 6;
      return WEEKDAY;
    }
    else if ("z".equals(s) || "gmt".equals(s) || "utc".equals(s)) {
      return UTC;
    }
    else
      return 0;
  }

  private void skipSpaces()
  {
    while (true) {
      int ch = read();

      if (Character.isWhitespace((char) ch)) {
        continue;
      }
      else if (ch == '(') {
        for (ch = read(); ch > 0 && ch != ')'; ch = read()) {
        }
      }
      else {
        unread();
        return;
      }
    }
  }

  private int read()
  {
    if (_index < _length)
      return _s.charAt(_index++);
    else {
      _index++;
      return -1;
    }
  }

  private void unread()
  {
    _index--;
  }
}
