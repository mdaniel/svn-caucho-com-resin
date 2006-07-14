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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

import com.caucho.util.L10N;


/**
 * mysqli object oriented API facade
 */
public class MysqliResult extends JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(MysqliResult.class.getName());
  private static final L10N L
    = new L10N(MysqliResult.class);

  /**
   * Constructor for MysqliResult
   *
   * @param stmt the corresponding statement
   * @param rs the corresponding result set
   * @param conn the corresponding connection
   */
  public MysqliResult(Statement stmt,
                      ResultSet rs,
                      Mysqli conn)
  {
    super(stmt, rs, conn);
  }

  /**
   * Constructor for MysqliResult
   *
   * @param metaData the corresponding result set meta data
   * @param conn the corresponding connection
   */
  public MysqliResult(ResultSetMetaData metaData,
                      Mysqli conn)
  {
    super(metaData, conn);
  }

  public String getResourceType()
  {
    return "mysql result";
  }

  /**
   * Seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   *
   * @param env the PHP executing environment
   * @param rowNumber the row offset
   * @return true on success or false on failure
   */
  public boolean data_seek(Env env, int rowNumber)
  {
    if (seek(env, rowNumber)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Fetch a result row as an associative, a numeric array, or both.
   *
   * @param type one of MYSQLI_ASSOC, MYSQLI_NUM, or MYSQLI_BOTH (default)
   * By using the MYSQLI_ASSOC constant this function will behave
   * identically to the mysqli_fetch_assoc(), while MYSQLI_NUM will
   * behave identically to the mysqli_fetch_row() function. The final
   * option MYSQLI_BOTH will create a single array with the attributes
   * of both.
   *
   * @return a result row as an associative, a numeric array, or both
   * or null if there are no more rows in the result set
   */
  @ReturnNullAsFalse
  public ArrayValue fetch_array(Env env,
                                @Optional("MYSQLI_BOTH") int type)
  {
    return fetchArray(env, type);
  }

  /**
   * Returns an associative array representing the row.
   *
   * @return an associative array representing the row
   * or null if there are no more rows in the result set
   */
  public ArrayValue fetch_assoc(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_ASSOC);
  }

  /**
   * Returns field metadata for a single field.
   *
   * @param env the PHP executing environment
   * @param offset the field number
   * @return the field metadata or false if no field
   * information for specified offset is available
   */
  public Value fetch_field_direct(Env env, int offset)
  {
    return fetchFieldDirect(env, offset);
  }

  /**
   * Returns the next field in the result set.
   *
   * @param env the PHP executing environment
   * @return the next field in the result set or
   * false if no information is available
   */
  public Value fetch_field(Env env)
  {
    return fetchNextField(env);
  }

  /**
   * Returns metadata for all fields in the result set.
   *
   * @param env the PHP executing environment
   * @return an array of objects which contains field
   * definition information or FALSE if no field
   * information is available
   */
  public Value fetch_fields(Env env)
  {
    return getFieldDirectArray(env);
  }

  /**
   * Returns the lengths of the columns of the
   * current row in the result set.
   *
   * @return an array with the lengths of the
   * columns of the current row in the result set
   * or false if you call it before calling
   * mysqli_fetch_row/array/object or after
   * retrieving all rows in the result set
   */
  public Value fetch_lengths()
  {
    return getLengths();
  }

  /**
   * Returns an object representing the current row.
   *
   * @param env the PHP executing environment
   * @return an object that corresponds to the
   * fetched row or NULL if there are no more
   * rows in resultset
   */
  public Value fetch_object(Env env)
  {
    return fetchObject(env);
  }

  /**
   * Returns an array representing the current row.
   *
   * @return an array that corresponds to the
   * fetched row or NULL if there are no more
   * rows in result set
   */
  public ArrayValue fetch_row(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_NUM);
  }

  /**
   * Returns the number of fields in the result set.
   *
   * @param env the PHP executing environment
   * @return the number of fields in the result set
   */
  public int field_count(Env env)
  {
    return getFieldCount();
  }

  /**
   * returns an object containing the following field information:
   *
   * name: The name of the column
   * orgname: The original name if an alias was specified
   * table: The name of the table
   * orgtable: The original name if an alias was specified
   * def: default value for this field, represented as a string
   * max_length: The maximum width of the field for the result set
   * flags: An integer representing the bit-flags for the field (see _constMap).
   * type: The data type used for this field (an integer... also see _constMap)
   * decimals: The number of decimals used (for integer fields)
   *
   * @param env the PHP executing environment
   * @param fieldOffset 0 <= fieldOffset < number of fields
   * @return an object or BooleanValue.FALSE
   */
  protected Value fetchFieldDirect(Env env,
                                   int fieldOffset)
  {
    Value fieldTable = getFieldTable(env, fieldOffset);
    Value fieldName = getFieldName(env, fieldOffset);
    Value fieldAlias = getFieldNameAlias(fieldOffset);
    Value fieldType = getJdbcType(fieldOffset);
    Value fieldLength = getFieldLength(env, fieldOffset);
    Value fieldScale = getFieldScale(fieldOffset);
    Value fieldCatalog = getFieldCatalog(fieldOffset);

    if ((fieldTable == BooleanValue.FALSE)
      || (fieldName == BooleanValue.FALSE)
      || (fieldAlias == BooleanValue.FALSE)
      || (fieldType == BooleanValue.FALSE)
      || (fieldLength == BooleanValue.FALSE)
      || (fieldScale == BooleanValue.FALSE)) {
      return BooleanValue.FALSE;
    }

    String sql = "SHOW FULL COLUMNS FROM " + fieldTable + " LIKE \'" + fieldName + "\'";

    MysqliResult metaResult;

    metaResult = ((Mysqli) getConnection()).metaQuery(sql, fieldCatalog.toString());

    if (metaResult == null)
      return BooleanValue.FALSE;

    return metaResult.fetchFieldImproved(env,
                                         fieldLength.toInt(),
                                         fieldAlias.toString(),
                                         fieldName.toString(),
                                         fieldTable.toString(),
                                         fieldType.toInt(),
                                         fieldScale.toInt());
  }

  /**
   * @see Value fetchFieldDirect
   *
   * increments the fieldOffset counter by one;
   *
   * @param env the PHP executing environment
   * @return
   */
  protected Value fetchNextField(Env env)
  {
    int fieldOffset = getFieldOffset();

    Value result = fetchFieldDirect(env, fieldOffset);

    setFieldOffset(fieldOffset + 1);

    return result;
  }

  /**
   * Sets the field metadata cursor to the
   * given offset. The next call to
   * mysqli_fetch_field() will retrieve the
   * field definition of the column associated
   * with that offset.
   *
   * @param env the PHP executing environment
   * @return previous value of field cursor
   */
  public boolean field_seek(Env env, int offset)
  {
    setFieldOffset(offset);

    return true;
  }

  /**
   * Get current field offset of a result pointer.
   *
   * @param env the PHP executing environment
   * @return current offset of field cursor
   */
  public int field_tell(Env env)
  {
    return getFieldOffset();
  }

  /**
   * Closes the result.
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
   *
   * @param env the PHP executing environment
   * @return array of fieldDirect objects
   */
  public Value getFieldDirectArray(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    try {
      int numColumns = getMetaData().getColumnCount();

      for (int i = 0; i < numColumns; i++) {
        array.put(fetchFieldDirect(env, i));
      }

      return array;
    } catch (SQLException e) {
      log.log(Level.FINE,  e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get the number of fields in the result set.
   *
   * @return the number of columns in the result set
   */
  public int num_fields()
  {
    return getFieldCount();
  }

  /**
   * Get the number of rows in the result set.
   *
   * @return the number of rows in the result set
   */
  public Value num_rows()
  {
    int numRows = getNumRows();

    if (numRows < 0) {
      return BooleanValue.FALSE;
    }

    return LongValue.create(numRows);
  }

  /**
   * Returns a string representation for this object.
   *
   * @return a string representation for this object
   */
  public String toString()
  {
    return "MysqliResult[" + super.toString() + "]";
  }
}
