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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The primitive double type.
 */
public class PrimitiveDoubleType extends PrimitiveType {
  private static final L10N L = new L10N(PrimitiveDoubleType.class);

  private static final PrimitiveDoubleType DOUBLE_TYPE = new PrimitiveDoubleType();

  private PrimitiveDoubleType()
  {
  }

  /**
   * Returns the boolean type.
   */
  public static PrimitiveDoubleType create()
  {
    return DOUBLE_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "double";
  }

  /**
   * Returns true for a numeric type.
   */
  public boolean isNumeric()
  {
    return true;
  }

  /**
   * Returns the type as a foreign key.
   */
  public AmberType getForeignType()
  {
    return DoubleType.create();
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    return manager.getCreateColumnSQL(Types.DOUBLE, length, precision, scale);
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getDouble(" + indexVar + " + " + index + ")");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("rs.getDouble(columnNames[" + index + "])");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    out.println(pstmt + ".setDouble(" + index + "++, " + value + ");");
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetNull(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    out.println(pstmt + ".setNull(" + index + "++, java.sql.Types.DOUBLE);");
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return "new Double(" + value + ")";
  }

  /**
   * Converts the value.
   */
  public String generateCastFromObject(String value)
  {
    return "((Number) " + value + ").doubleValue()";
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (value instanceof Number)
      pstmt.setString(index, value.toString());
    else
      throw new IllegalArgumentException("Invalid argument for setParameter.");
  }
}
