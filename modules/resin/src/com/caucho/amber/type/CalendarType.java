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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import java.util.Calendar;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

/**
 * The type of a property.
 */
public class CalendarType extends Type {
  private static final L10N L = new L10N(CalendarType.class);

  private static final CalendarType CALENDAR_TYPE = new CalendarType();

  private CalendarType()
  {
  }

  /**
   * Returns the singleton Calendar type.
   */
  public static CalendarType create()
  {
    return CALENDAR_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.util.Calendar";
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
			  String indexVar, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.CalendarType.toCalendar(" + rs + ".getTimestamp(" + indexVar + " + " + index + "))");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String index, String value)
    throws IOException
  {
    out.println("if (" + value + " == null)");
    out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.TIMESTAMP);");
    out.println("else");
    out.println("  " + pstmt + ".setTimestamp(" + index + "++, new java.sql.Timestamp(" + value + ".getTimeInMillis()));");
  }

  /**
   * Gets the value.
   */
  public static Calendar toCalendar(java.util.Date time)
    throws SQLException
  {
    if (time == null)
      return null;
    else {
      Calendar cal = Calendar.getInstance();
      cal.setTime(time);
      
      return cal;
    }
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    java.sql.Timestamp time = rs.getTimestamp(index);

    if (time == null)
      return null;
    else {
      Calendar cal = Calendar.getInstance();
      cal.setTime(time);
      
      return cal;
    }
  }
}
