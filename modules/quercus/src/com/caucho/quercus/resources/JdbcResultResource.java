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

package com.caucho.quercus.resources;

import com.caucho.util.Log;
import com.caucho.quercus.lib.mysql.MysqliModule;
import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.env.*;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC Result value.
 */
public class JdbcResultResource extends ResourceValue {
  private static final Logger log = Log.open(JdbcResultResource.class);

  public static final int FETCH_ASSOC = 0x1;
  public static final int FETCH_NUM = 0x2;
  public static final int FETCH_BOTH = FETCH_ASSOC | FETCH_NUM;

  public static final StringValue INTEGER = new StringValueImpl("int");
  public static final StringValue BLOB = new StringValueImpl("blob");
  public static final StringValue STRING = new StringValueImpl("string");
  public static final StringValue DATE = new StringValueImpl("date");
  public static final StringValue DATETIME = new StringValueImpl("datetime");
  public static final StringValue REAL = new StringValueImpl("real");

  private Statement _stmt;
  private ResultSet _rs;
  private int _fieldOffset;
  private JdbcConnectionResource _conn;

  private ResultSetMetaData _metaData;
  private Value[] _columnNames;

  public JdbcResultResource(Statement stmt,
                            ResultSet rs,
                            JdbcConnectionResource conn)
  {
    _stmt = stmt;
    _rs = rs;
    _conn = conn;
  }

  public JdbcResultResource(ResultSetMetaData md, JdbcConnectionResource conn)
  {
    _metaData = md;
    _conn = conn;
  }

  /**
   * getter and setter for _conn needed by fetch_field and other functions that do a query on the connection which
   * created this result set
   */
  public JdbcConnectionResource getConnection()
  {
    return _conn;
  }

  /**
   * Returns column count.
   */
  public int getFieldCount()
  {
    try {
      return getMetaData().getColumnCount();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * helper function for getResultField returns a 0-based column number
   */
  private int getColumnNumber(String colName,
                              ResultSetMetaData rsmd)
    throws SQLException
  {

    int numColumns = rsmd.getColumnCount();

    if (colName.indexOf('.') == -1) {
      for (int i = 1; i <= numColumns; i++) {
        if (colName.equals(rsmd.getColumnName(i)))
          return (i - 1);
      }
      return -1;
    } else {
      for (int i = 1; i <= numColumns; i++) {
        if (colName.equals(rsmd.getTableName(i) + '.' + rsmd.getColumnName(i)))
          return (i - 1);
      }
      return -1;
    }

  }

  /**
   * Returns the value at a particular row and column.
   */
  public Value getResultField(Env env, int row, Value field)
  {
    try {
      ResultSetMetaData md = getMetaData();

      int colNumber;

      if (field.isNumber())
        colNumber = field.toInt();
      else
        colNumber = getColumnNumber(field.toString(), _metaData);

      if (colNumber < 0 || colNumber >= md.getColumnCount()) {
        env.invalidArgument("field", field);
        return BooleanValue.FALSE;
      }

      int currentRow = _rs.getRow();

      if ((!_rs.absolute(row + 1)) || _rs.isAfterLast()) {
        if (currentRow > 0)
          _rs.absolute(currentRow);
        else
          _rs.beforeFirst();

        return BooleanValue.FALSE;
      }

      return getColumnValue(_rs, _metaData, colNumber + 1);

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  // XXX: s/b removed
  ResultSet getResultSet()
  {
    return _rs;
  }

  /**
   * @return array of fieldDirect objects
   */
  public Value getFieldDirectArray(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      int numColumns = _metaData.getColumnCount();

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
   * returns an object with the following fields: name, table, max_length,
   * not_null, primary_key, multiple_key, numeric,
   * blob, type, unsigned, zerofill.
   * <p/>
   * NOTE: does not have a field for unique_key.
   */
  public Value fetchField(Env env,
                          int maxLength,
                          String tableName,
                          String type)
  {
    Value result = env.createObject();
    LongValue one = new LongValue(1);
    LongValue zero = new LongValue(0);

    try {
      _rs.next();
      result.put(new StringValueImpl("name"), new StringValueImpl(_rs.getString(1)));
      result.put(new StringValueImpl("table"), new StringValueImpl(tableName));
      result.put(new StringValueImpl("max_length"), new LongValue(maxLength));

      if (_rs.getString(4).indexOf("YES") == -1)
        result.put(new StringValueImpl("not_null"), one);
      else
        result.put(new StringValueImpl("not_null"), zero);

      if (_rs.getString(5).indexOf("PRI") != -1)
        result.put(new StringValueImpl("primary_key"), one);
      else
        result.put(new StringValueImpl("primary_key"), zero);

      if (_rs.getString(5).indexOf("MUL") != -1)
        result.put(new StringValueImpl("multiple_key"), one);
      else
        result.put(new StringValueImpl("multiple_key"), zero);

      if ((_rs.getString(2).indexOf("int") != -1) || (_rs.getString(2).indexOf("real") != -1))
        result.put(new StringValueImpl("numeric"), one);
      else
        result.put(new StringValueImpl("numeric"), zero);

      if (_rs.getString(2).indexOf("blob") != -1)
        result.put(new StringValueImpl("blob"), one);
      else
        result.put(new StringValueImpl("blob"), zero);

      result.put(new StringValueImpl("type"), new StringValueImpl(type));

      if (_rs.getString(2).indexOf("unsigned") != -1)
        result.put(new StringValueImpl("unsigned"), one);
      else
        result.put(new StringValueImpl("unsigned"), zero);

      if (_rs.getString(2).indexOf("zerofill") != -1)
        result.put(new StringValueImpl("zerofill"), one);
      else
        result.put(new StringValueImpl("zerofill"), zero);

      return result;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns an object with the following fields:
   *
   * name: The name of the column
   * orgname: The original name if an alias was specified
   * table: The name of the table
   * orgtable: The original name if an alias was specified
   * def: default value for this field, represented as a string
   * max_length: The maximum width of the field for the result set
   * flags: An integer representing the bit-flags for the field
   * type: An integer respresenting the data type used for this field
   * decimals: The number of decimals used (for integer fields)
   */
  private Value fetchFieldImproved(Env env,
                                   int maxLength,
                                   String name,
                                   String originalName,
                                   String table,
                                   int type,
                                   int scale)
  {
    Value result = env.createObject();

    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      _rs.next();
      result.put(new StringValueImpl("name"), new StringValueImpl(name));
      result.put(new StringValueImpl("orgname"), new StringValueImpl(originalName));
      result.put(new StringValueImpl("table"), new StringValueImpl(table));
      //XXX: orgtable same as table
      result.put(new StringValueImpl("orgtable"), new StringValueImpl(table));
      if (_rs.getString(6) != null)
        result.put(new StringValueImpl("def"), new StringValueImpl(_rs.getString(6)));
      else
        result.put(new StringValueImpl("def"), new StringValueImpl(""));
      result.put(new StringValueImpl("max_length"), new LongValue(maxLength));

      //generate flags
      long flags = 0;

      if (_rs.getString(4).indexOf("YES") == -1)
        flags += MysqliModule.NOT_NULL_FLAG;

      if (_rs.getString(5).indexOf("PRI") != -1) {
        flags += MysqliModule.PRI_KEY_FLAG;
        flags += MysqliModule.PART_KEY_FLAG;
      }

      if (_rs.getString(5).indexOf("MUL") != -1) {
        flags += MysqliModule.MULTIPLE_KEY_FLAG;
        flags += MysqliModule.PART_KEY_FLAG;
      }

      if ((_rs.getString(2).indexOf("blob") != -1) ||
          (type == Types.LONGVARCHAR) ||
          (type == Types.LONGVARBINARY))
        flags += MysqliModule.BLOB_FLAG;

      if (_rs.getString(2).indexOf("unsigned") != -1)
        flags += MysqliModule.UNSIGNED_FLAG;

      if (_rs.getString(2).indexOf("zerofill") != -1)
        flags += MysqliModule.ZEROFILL_FLAG;

      if ((_rs.getString(3).indexOf("bin") != -1) ||
          (type == Types.LONGVARBINARY) ||
          (type == Types.DATE) ||
          (type == Types.TIMESTAMP))
        flags += MysqliModule.BINARY_FLAG;

      if (_rs.getString(2).indexOf("enum") != -1)
        flags += MysqliModule.ENUM_FLAG;

      if (_rs.getString(7).indexOf("auto") != -1)
        flags += MysqliModule.AUTO_INCREMENT_FLAG;

      if (_rs.getString(2).indexOf("set") != -1)
        flags += MysqliModule.SET_FLAG;

      if ((type == Types.BIGINT) ||
          (type == Types.BIT) ||
          (type == Types.BOOLEAN) ||
          (type == Types.DECIMAL) ||
          (type == Types.DOUBLE) ||
          (type == Types.REAL) ||
          (type == Types.INTEGER) ||
          (type == Types.SMALLINT))
        flags += MysqliModule.NUM_FLAG;

      result.put(new StringValueImpl("flags"), new LongValue(flags));
      //generate PHP type
      int quercusType = 0;
      switch (type) {
      case Types.DECIMAL:
        quercusType = MysqliModule.MYSQL_TYPE_DECIMAL;
        break;
      case Types.BIT:
        quercusType = MysqliModule.MYSQL_TYPE_TINY;
        break;
      case Types.SMALLINT:
        quercusType = MysqliModule.MYSQL_TYPE_SHORT;
        break;
      case Types.INTEGER: {
        if (_rs.getString(2).indexOf("medium") == -1)
          quercusType = MysqliModule.MYSQL_TYPE_LONG;
        else
          quercusType = MysqliModule.MYSQL_TYPE_INT24;
        break;
      }
      case Types.REAL:
        quercusType = MysqliModule.MYSQL_TYPE_FLOAT;
        break;
      case Types.DOUBLE:
        quercusType = MysqliModule.MYSQL_TYPE_DOUBLE;
        break;
      case Types.BIGINT:
        quercusType = MysqliModule.MYSQL_TYPE_LONGLONG;
        break;
      case Types.DATE:
        quercusType = MysqliModule.MYSQL_TYPE_DATE;
        break;
      case Types.TIMESTAMP:
        quercusType = MysqliModule.MYSQL_TYPE_DATETIME;
        break;
      case Types.LONGVARBINARY:
      case Types.LONGVARCHAR:
        quercusType = MysqliModule.MYSQL_TYPE_BLOB;
        break;
      case Types.CHAR:
        quercusType = MysqliModule.MYSQL_TYPE_STRING;
        break;
      case Types.VARCHAR:
        quercusType = MysqliModule.MYSQL_TYPE_VAR_STRING;
        break;
      // XXX: may need to revisit default
      default:
        quercusType = MysqliModule.MYSQL_TYPE_NULL;
        break;
      }
      result.put(new StringValueImpl("type"), new LongValue(quercusType));
      result.put(new StringValueImpl("decimals"), new LongValue(scale));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }

    return result;
  }


  /**
   * returns the following field flags: not_null, primary_key, multiple_key, blob, unsigned zerofill, binary, enum,
   * auto_increment and timestamp
   * <p/>
   * it does not return the MySQL / PHP flag unique_key
   * <p/>
   * QuercusMysqlModule generates a special result set with the appropriate values
   */
  public Value getFieldFlags()
  {
    try {
      StringBuilder flags = new StringBuilder();

      _rs.next();
      if (_rs.getString(4).indexOf("YES") == -1)
        flags.append("not_null");

      if (_rs.getString(5).indexOf("PRI") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("primary_key");
      } else {
        if (_rs.getString(5).indexOf("MUL") != -1) {
          if (flags.length() > 0)
            flags.append(' ');
          flags.append("multiple_key");
        }
      }

      if (_rs.getString(2).indexOf("blob") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("blob");
      }

      if (_rs.getString(2).indexOf("unsigned") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("unsigned");
      }

      if (_rs.getString(2).indexOf("zerofill") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("zerofill");
      }

      if (_rs.getString(3).indexOf("bin") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("binary");
      }

      if (_rs.getString(2).indexOf("enum") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("enum");
      }

      if (_rs.getString(7).indexOf("auto_increment") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("auto_increment");
      }

      if (_rs.getString(2).indexOf("date") != -1) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("timestamp");
      }

      return new StringValueImpl(flags.toString());

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the table corresponding to the field.
   */
  public Value getFieldTable(Env env, int fieldOffset)
  {
    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else {
	String tableName = _metaData.getTableName(fieldOffset + 1);

	if (tableName == null || tableName.equals(""))
	  return BooleanValue.FALSE;
	else
	  return new StringValueImpl(tableName);
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * special function used my mysql_insert_id() to return the ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query
   */
  public long getInsertID()
    throws SQLException
  {
    _rs.next();
    return _rs.getLong(1);
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
  public Value fetchFieldDirect(Env env,
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

    Value metaResult = _conn.metaQuery(sql, fieldCatalog.toString());

    if (!(metaResult instanceof JdbcResultResource))
      return BooleanValue.FALSE;

    return ((JdbcResultResource) metaResult).fetchFieldImproved(env,
                                                             fieldLength.toInt(),
                                                             fieldAlias.toString(),
                                                             fieldName.toString(),
                                                             fieldTable.toString(),
                                                             fieldType.toInt(),
                                                             fieldScale.toInt());
  }

  /**
   * returns an ArrayValue of lengths of the columns in the most recently accessed row. If a function like
   * mysql_fetch_array() has not yet been called this will return BooleanValue.FALSE
   */
  public Value getLengths()
  {
    Value result;
    ArrayValue array = new ArrayValueImpl();

    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      int numColumns = _metaData.getColumnCount();

      for (int i = 1; i <= numColumns; i++) {
        array.put(new LongValue(_rs.getObject(i).toString().length()));
      }
      result = array;

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * @return length of field for specified column
   */
  public Value getFieldLength(Env env, int fieldOffset)
  {
    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        return new LongValue((long) _metaData.getPrecision(fieldOffset + 1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   *
   * @return number of digits to the right of the decimal point
   */
  public Value getFieldScale(int fieldOffset)
  {
    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return new LongValue((long) _metaData.getScale(fieldOffset + 1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  public Value getNumRows()
  {
    return getNumRows(_rs);
  }

  /**
   * returns number of rows returned in query
   */
  public static Value getNumRows(ResultSet rs)
  {
    try {
      int currentRow = rs.getRow();

      try {
        rs.last();
        int count = rs.getRow();

        return new LongValue((long) count);
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);

        return BooleanValue.FALSE;
      } finally {
        if (currentRow == 0)
          rs.beforeFirst();
        else
          rs.absolute(currentRow);
      }

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns number of columns returned in query
   */
  public Value getNumFields()
  {

    try {
      Value result = NullValue.NULL;

      if (_metaData == null)
        _metaData = _rs.getMetaData();

      int count = _metaData.getColumnCount();

      if (count != 0) {
        result = new LongValue((long) count);
      }

      return result;

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return NullValue.NULL;
    }
  }

  /**
   * helper function for mysql_tablename expects that this _rs was created by a call to mysql_list_tables().
   * <p/>
   * note PHP i is zero based
   */
  public Value getTable(int i)
  {
    try {
      _rs.absolute(i + 1);
      return new StringValueImpl(_rs.getString(1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * getFieldName returns a StringValue with the column name
   *
   * @param fieldOffset need to add 1 because java is 1 based index and quercus is 0 based
   */
  public Value getFieldName(Env env, int fieldOffset)
  {

    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        return new StringValueImpl(_metaData.getColumnName(fieldOffset + 1));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * getFieldNameAlias returns a StringValue with the column Alias
   *
   * @param fieldOffset need to add 1 because java is 1 based index and quercus is 0 based
   */
  public Value getFieldNameAlias(int fieldOffset)
  {

    try {

      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return new StringValueImpl(_metaData.getColumnLabel(fieldOffset + 1));

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * gets type from Types enumeration
   */
  private Value getJdbcType(int fieldOffset)
  {
    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      return new LongValue(_metaData.getColumnType(fieldOffset + 1));

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * getFieldType returns a StringValue with the column type
   *
   * @param fieldOffset need to add 1 because java is 1 based index and quercus is 0 based
   */
  public Value getFieldType(Env env, int fieldOffset)
  {
    try {

      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        return getColumnPHPName(_metaData.getColumnType(fieldOffset + 1));

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns an object with properties that correspond to the fetched row and moves the internal data pointer ahead.
   */
  public Value fetchObject(Env env)
  {
    try {
      if (_rs.next()) {
        Value result = env.createObject();

        if (_metaData == null)
          _metaData = _rs.getMetaData();

        int count = _metaData.getColumnCount();

        for (int i = 0; i < count; i++) {
          String name = _metaData.getColumnName(i + 1);
          Value value = getColumnValue(_rs, _metaData, i + 1);

          result.putField(env, name, value);
        }

        return result;

      } else {
        return NullValue.NULL;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetch the next line as an array.
   */
  public Value fetchArray(int type)
  {
    try {
      if (_rs.next()) {
        ArrayValue array = new ArrayValueImpl();

        if (_metaData == null)
          _metaData = _rs.getMetaData();

        int count = _metaData.getColumnCount();

        if ((type & FETCH_ASSOC) != 0) {
          _columnNames = new Value[count];

          for (int i = 0; i < count; i++)
            _columnNames[i] = new StringValueImpl(_metaData.getColumnName(i + 1));
        }

        for (int i = 0; i < count; i++) {
          Value value = getColumnValue(_rs, _metaData, i + 1);

          if ((type & FETCH_NUM) != 0)
            array.put(LongValue.create(i), value);

          if ((type & FETCH_ASSOC) != 0)
            array.put(_columnNames[i], value);
        }

        return array;
      } else {
        return NullValue.NULL;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns a StringValue corresponding to the PHP return string
   */
  private Value getColumnPHPName(int dataType)
  {
    switch (dataType) {
    case Types.BIGINT:
    case Types.BIT:
    case Types.INTEGER:
    case Types.SMALLINT:
      {
        return INTEGER;
      }
    case Types.LONGVARBINARY:
    case Types.LONGVARCHAR:
      {
        return BLOB;
      }
    case Types.CHAR:
    case Types.VARCHAR:
      {
        return STRING;
      }
    case Types.DATE:
      {
        return DATE;
      }
    case Types.TIMESTAMP:
      {
        return DATETIME;
      }
    case Types.DECIMAL:
    case Types.DOUBLE:
    case Types.REAL:
      {
        return REAL;
      }
    default:
      {
        return NullValue.NULL;
      }
    }
  }

  public static Value getColumnValue(ResultSet rs,
                                     ResultSetMetaData metaData,
                                     int column)
    throws SQLException
  {
    switch (metaData.getColumnType(column)) {
    case Types.NULL:
      return NullValue.NULL;

    case Types.BIT:
    case Types.TINYINT:
    case Types.SMALLINT:
    case Types.INTEGER:
    case Types.BIGINT:
      {
        long value = rs.getLong(column);

        if (rs.wasNull())
          return NullValue.NULL;
        else
          return LongValue.create(value);
      }

    case Types.DOUBLE:
      {
        double value = rs.getDouble(column);

        if (rs.wasNull())
          return NullValue.NULL;
        else
          return new DoubleValue(value);
      }

    default:
      {
        String strValue = rs.getString(column);

        if (strValue == null || rs.wasNull())
          return NullValue.NULL;
        else
          return new StringValueImpl(strValue);
      }
    }
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    try {
      if (_rs != null)
        _rs.close();

      if (_stmt != null)
        _stmt.close();

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    }

  }

  // XXX: quercusbb seems to want this?
  public Value toKey()
  {
    return new StringValueImpl("JdbcResultResource$" + System.identityHashCode(this));
  }

  /**
   * Converts to a string.
   *
   * @param env
   */
  public String toString(Env env)
  {
    return "ResultSet[]";
  }

  /**
   * created so tests will pass
   */
  public String toString()
  {
    return "com.caucho.quercus.resources.JdbcResultResource";
  }

  /**
   * points to row right before "rowNumber" next fetch_array will increment to proper row
   */
  public boolean setRowNumber(int rowNumber)
  {
    return setRowNumber(_rs, rowNumber);
  }

  public static boolean setRowNumber(ResultSet rs,
                                     int rowNumber)
  {
    // throw error if rowNumber is after last row
    Value numRows = getNumRows(rs);
    if (!numRows.isNumber() || numRows.toLong() <= rowNumber || rowNumber < 0) {
      return false;
    }

    try {
      if (rowNumber == 0)
        rs.beforeFirst();
      else
        rs.absolute(rowNumber);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }

    return true;
  }
  /**
   * @param fieldOffset PHP is 0-based
   */
  public void setFieldOffset(int fieldOffset)
  {
    this._fieldOffset = fieldOffset;
  }

  public int getFieldOffset()
  {
    return this._fieldOffset;
  }

  public Value getFieldCatalog(int fieldOffset)
  {
    try {
      if (_metaData == null)
        _metaData = _rs.getMetaData();

      if (_metaData.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return new StringValueImpl(_metaData.getCatalogName(fieldOffset + 1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  private ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_metaData == null)
      _metaData = _rs.getMetaData();

    return _metaData;
  }
}

