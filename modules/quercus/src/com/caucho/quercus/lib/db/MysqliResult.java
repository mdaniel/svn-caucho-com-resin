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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

/**
 * mysqli object oriented API facade
 */
public class MysqliResult extends JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(MysqliResult.class.getName());

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

  /**
   * seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   * Returns TRUE on success or FALSE on failure
   */
  public boolean data_seek(Env env, int rowNumber)
  {
    return seek(env, rowNumber);
  }

  /**
   * Returns an array representing the row.
   */
  @ReturnNullAsFalse
  public ArrayValue fetch_array(@Optional("MYSQLI_BOTH") int type)
  {
    return fetchArray(type);
  }

  /**
   * Returns an associative array representing the row.
   */
  public ArrayValue fetch_assoc()
  {
    return fetchArray(JdbcResultResource.FETCH_ASSOC);
  }

  /**
   * Returns the field metadata
   */
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
  public Value fetch_object(Env env)
  {
    return fetchObject(env);
  }

  /**
   * Returns an object representing the row.
   */
  public ArrayValue fetch_row()
  {
    return fetchArray(JdbcResultResource.FETCH_NUM);
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
   */
  public Value fetchNextField(Env env)
  {
    int fieldOffset = getFieldOffset();

    Value result = fetchFieldDirect(env, fieldOffset);

    setFieldOffset(fieldOffset + 1);

    return result;
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
  public Integer num_rows()
  {
    return new Integer(getNumRows());
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
   * Returns a string representation for this object.
   */
  public String toString()
  {
    return "MysqliResult[" + super.toString() + "]";
  }
}
