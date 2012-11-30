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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

public class DateInterval
{
  private static final L10N L = new L10N(DateInterval.class);

  public int y;
  public int m;
  public int d;

  public int h;
  public int i;
  public int s;

  public int invert = 0;
  public Value days = BooleanValue.FALSE;

  public static DateInterval __construct(Env env, String time)
  {
    DateInterval dateInterval = new DateInterval();

    IntervalParser parser = new IntervalParser(dateInterval, time);

    parser.parse();

    return dateInterval;
  }

  protected DateInterval()
  {
  }

  public static DateInterval createFromDateString(Env env, String time)
  {
    throw new UnimplementedException("DateInterval::createFromDateString()");
  }

  public String format(String format)
  {
    throw new UnimplementedException("DateInterval::format()");
  }

  static class IntervalParser {
    private final DateInterval _dateInterval;
    private final String _str;

    private final int _length;
    private int _offset;

    public IntervalParser(DateInterval dateInterval, String str)
    {
      _dateInterval = dateInterval;
      _str = str;

      _length = str.length();
    }

    public void parse() throws ParseException
    {
      int ch = read();

      if (ch != 'P') {
        throw new ParseException("expected P");
      }

      parseDate();

      parseTime();
    }

    private void parseDate() throws ParseException
    {
      while (true) {
        int ch = peek();

        if (ch == 'T' || ch < 0) {
          break;
        }

        int value = readInt();

        ch = read();

        if (ch == 'Y') {
          _dateInterval.y = value;
        }
        else if (ch == 'M') {
          _dateInterval.m = value;
        }
        else if (ch == 'D') {
          _dateInterval.d = value;
        }
        else if (ch == 'W') {
          _dateInterval.d = value * 7;
        }
        else {
          throw new ParseException(L.l("unknown date identifier: '{0}'", (char) ch));
        }
      }
    }

    private int readInt() throws ParseException
    {
      int ch = read();

      if (ch < '0' || ch > '9') {
        throw new ParseException(L.l("expected digit, saw '{0}' ({1})", (char) ch, ch));
      }

      int value = ch - '0';
      while ('0' <= (ch = read()) && ch <= '9') {
        value = value * 10 + ch - '0';
      }

      unread();

      return value;
    }

    private void parseTime() throws ParseException
    {
      int ch = peek();

      if (ch != 'T') {
        return;
      }

      read();

      while (true) {
        ch = peek();

        if (ch < 0) {
          break;
        }

        int value = readInt();

        ch = read();

        if (ch == 'H') {
          _dateInterval.h = value;
        }
        else if (ch == 'M') {
          _dateInterval.i = value;
        }
        else if (ch == 'S') {
          _dateInterval.s = value;
        }
        else {
          throw new ParseException(L.l("unknown time identifier: '{0}'", (char) ch));
        }
      }
    }

    private int peek()
    {
      if (_offset < _length) {
        return _str.charAt(_offset);
      }
      else {
        return -1;
      }
    }

    private int read()
    {
      if (_offset < _length) {
        return _str.charAt(_offset++);
      }
      else {
        return -1;
      }
    }

    private void unread()
    {
      _offset--;
    }
  }

  static class ParseException extends QuercusException {
    public ParseException(String message)
    {
      super(message);
    }
  }
}
