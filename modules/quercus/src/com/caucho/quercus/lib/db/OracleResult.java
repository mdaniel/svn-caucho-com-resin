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

package com.caucho.quercus.lib.db;

import java.sql.*;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ArrayValue;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

/**
 * oracle result set class (postgres has NO object oriented API)
 */
public class OracleResult extends JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(OracleResult.class.getName());
  private static final L10N L = new L10N(OracleResult.class);

  public OracleResult(Statement stmt,
                      ResultSet rs,
                      JdbcConnectionResource conn)
  {
    super(stmt, rs, conn);
  }

  public OracleResult(ResultSetMetaData md,
                      JdbcConnectionResource conn)
  {
    super(md, conn);
  }

  /**
   * seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   * Returns TRUE on success or FALSE on failure
   */
  public boolean data_seek(Env env, int rowNumber)
  {
    if (setRowNumber(rowNumber))
      return true;
    else {
      env.warning(L.l("Offset {0} is invalid (or the query data is unbuffered)", rowNumber));

      return false;
    }
  }

  /**
   * Returns an associative array representing the row.
   */
  @ReturnNullAsFalse
  public ArrayValue fetch_assoc()
  {
    return fetchArray(JdbcResultResource.FETCH_ASSOC);
  }

  /**
   * Returns the field metadata
   */
  @ReturnNullAsFalse
  public Value fetch_field(Env env)
  {
    return fetchNextField(env);
  }

  /**
   * Returns the field metadata
   */
  @ReturnNullAsFalse
  public Value fetch_fields(Env env)
  {
    return getFieldDirectArray(env);
  }

  /**
   * Returns the field lengths
   */
  @ReturnNullAsFalse
  public Value fetch_lengths()
  {
    return getLengths();
  }

  /**
   * Returns the field table
   */
  @ReturnNullAsFalse
  public Value fetch_field_catalog(int offset)
  {
    return getFieldCatalog(offset);
  }

  /**
   * Returns the field metadata.
   */
  @ReturnNullAsFalse
  public Value fetch_field_direct(Env env, int offset)
  {
    return fetchFieldDirect(env, offset);
  }

  /**
   * Returns the field length
   */
  @ReturnNullAsFalse
  public Value fetch_field_length(Env env, int offset)
  {
    return getFieldLength(env, offset);
  }

  /**
   * Returns the field name
   */
  @ReturnNullAsFalse
  public Value fetch_field_name(Env env, int offset)
  {
    return getFieldName(env, offset);
  }

  /**
   * Returns the table corresponding to the field.
   */
  @ReturnNullAsFalse
  public Value fetch_field_table(Env env, int offset)
  {
    return getFieldTable(env, offset);
  }

  /**
   * Returns the field type
   */
  @ReturnNullAsFalse
  public Value fetch_field_type(Env env, int offset)
  {
    return getFieldType(env, offset);
  }

  /**
   * Returns an object representing the row.
   */
  @ReturnNullAsFalse
  public Value fetch_object(Env env)
  {
    return fetchObject(env);
  }

  /**
   * Returns an object representing the row.
   */
  @ReturnNullAsFalse
  public ArrayValue fetch_row()
  {
    return fetchArray(JdbcResultResource.FETCH_NUM);
  }

  /**
   * Returns the number of fields
   */
  public int field_count(Env env)
  {
    return getFieldCount();
  }

  /**
   * Sets the index into field metadata
   */
  public boolean field_seek(Env env, int offset)
  {
    setFieldOffset(offset);

    return true;
  }

  /**
   * Returns the index into field metadata
   */
  public int field_tell(Env env)
  {
    return getFieldOffset();
  }

  /**
   * Closes the result
   */
  public void free()
  {
    close();
  }

  /**
   * Closes the result
   */
  public void free_result()
  {
    close();
  }

  /**
   * returns the number of columns in the result set.
   */
  public int num_fields()
  {
    return getFieldCount();
  }

  /**
   * Returns the number of rows in the result set.
   */
  @ReturnNullAsFalse
  public Value num_rows()
  {
    return getNumRows();
  }

  /**
   * Returns the value of a result field.
   */
  @ReturnNullAsFalse
  public Value result(Env env, int row, Value field)
  {
    return getResultField(env, row, field);
  }

  /**
   * Closes the result.
   */
  public void close()
  {
    super.close();
  }

  /**
   * Returns a string representation for this object
   */
  public String toString()
  {
    if (!_closed)
      return "OracleResult[" + super.toString() + "]";
    else
      return "OracleResult[closed]";
  }
}
