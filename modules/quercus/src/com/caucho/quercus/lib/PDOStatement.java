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

package com.caucho.quercus.lib;

import java.sql.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.ReadOnly;

/**
 * PDO object oriented API facade.
 */
public class PDOStatement
  implements Iterable<Value>
{
  private static final Logger log = Logger.getLogger(PDOStatement.class.getName());
  private static final L10N L = new L10N(PDOStatement.class);

  private static final Value[] NULL_VALUES = new Value[0];

  private final Env _env;
  private final PDOError _error;

  private final String _query;

  private Statement _statement;
  private PreparedStatement _preparedStatement;

  private ResultSet _resultSet;
  private ResultSetMetaData _resultSetMetaData;
  private boolean _resultSetExhausted = true;

  private int _fetchMode = PDO.FETCH_BOTH;
  private Value[] _fetchModeArgs = NULL_VALUES;

  PDOStatement(Env env, Connection conn, String query, boolean isPrepared)
    throws SQLException
  {
    _env = env;
    _error = new PDOError(_env);

    _query = query;

    // XXX: following would be better as annotation on destroy() method
    env.addResource(new ResourceValue() {
      public void close()
      {
        destroy();
      }
    });

    if (isPrepared) {
      _statement = null;
      _preparedStatement = conn.prepareStatement(query);
      _preparedStatement.setEscapeProcessing(false);
    }
    else {
      _preparedStatement = null;

      Statement statement = null;

      try {
        statement = conn.createStatement();
        statement.setEscapeProcessing(false);

        if (statement.execute(query)) {
          _resultSet = statement.getResultSet();
          _resultSetExhausted = false;
        }

        _statement = statement;

        statement = null;

      } finally {
        try {
          if (statement != null)
            statement.close();
        } catch (SQLException e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  private boolean advanceResultSet()
  {
    if (_resultSet == null || _resultSetExhausted)
      return false;

    try {
      boolean isNext =  _resultSet.next();

      if (!isNext)
        _resultSetExhausted = true;

      return isNext;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }
  }

  private ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_resultSetMetaData == null)
      _resultSetMetaData = _resultSet.getMetaData();

    return _resultSetMetaData;
  }

  public boolean bindColumn(Value column, @Reference Value param, @Optional("-1") int type)
  {
    throw new UnimplementedException();
  }

  public boolean bindParam(Value parameter,
                           @Reference Value variable,
                           @Optional("-1") int dataType,
                           @Optional("-1") int length,
                           @Optional Value driverOptions)
  {
    throw new UnimplementedException();
  }

  public boolean bindValue(Value parameter,
                           Value value,
                           @Optional("-1") int dataType)
  {
    throw new UnimplementedException();
  }

  /**
   * Closes the current cursor.
   */
  public boolean closeCursor()
  {
    if (_resultSet == null)
      return false;

    ResultSet resultSet = _resultSet;

    _resultSet = null;
    _resultSetMetaData = null;
    _resultSetExhausted = true;

    try {
      resultSet.close();
    }
    catch (SQLException e) {
      _error.error(e);

      return false;
    }

    return true;
  }

  /**
   * Returns the number of columns.
   */
  public int columnCount()
  {
    if (_resultSet == null)
      return 0;

    try {
      return getMetaData().getColumnCount();
    }
    catch (SQLException e) {
      _error.error(e);

      return 0;
    }
  }

  public String errorCode()
  {
    return _error.errorCode();
  }

  public ArrayValue errorInfo()
  {
    return _error.errorInfo();
  }

  public boolean execute(@Optional @ReadOnly ArrayValue inputParameters)
  {
    closeCursor();

    try {
      if (inputParameters != null) {
        for (Map.Entry<Value, Value> entry : inputParameters.entrySet()) {
          Value key = entry.getKey();
          Value value = entry.getValue();

          int index;

          if (!key.isNumber())
            throw new UnimplementedException("key " + key);

          index = key.toInt() + 1;

          // XXX: handling of params and types needs improvement
          if (value instanceof DoubleValue) {
            _preparedStatement.setDouble(index, value.toDouble());
          }
          else if (value instanceof LongValue) {
            _preparedStatement.setLong(index, value.toLong());
          }
          else if (value instanceof StringValue) {
            _preparedStatement.setString(index, value.toString());
          }
        }
      }

      if (_preparedStatement.execute()) {
        _resultSet = _preparedStatement.getResultSet();
        _resultSetExhausted = false;
      }

      return true;
    } catch (SQLException e) {
      _error.error(e);

      return false;
    }
  }

  /**
   * Fetch the next row.
   *
   * @param fetchMode the mode, 0 to use the value set by {@link #setFetchMode}.
   * @return a value, BooleanValue.FALSE if there are no more rows or an error occurs.
   */
  public Value fetch(@Optional int fetchMode, @Optional Value[] args)
  {
    if (fetchMode == 0) {
      fetchMode = _fetchMode;
      args = _fetchModeArgs;

      fetchMode = fetchMode & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));
    }
    else {
      if ((fetchMode & PDO.FETCH_GROUP) != 0) {
        _error.warning(L.l("FETCH_GROUP is not allowed"));
        return BooleanValue.FALSE;
      }
      else if ((fetchMode & PDO.FETCH_UNIQUE) != 0) {
        _error.warning(L.l("FETCH_UNIQUE is not allowed"));
        return BooleanValue.FALSE;
      }
    }

    if (args == null)
      args = NULL_VALUES;

    boolean isClasstype = (fetchMode & PDO.FETCH_CLASSTYPE) != 0;
    boolean isSerialize = (fetchMode & PDO.FETCH_SERIALIZE) != 0;

    fetchMode = fetchMode & (~(PDO.FETCH_CLASSTYPE | PDO.FETCH_SERIALIZE));

    switch (fetchMode) {
      case PDO.FETCH_ASSOC:
        if (args.length > 0) {
          closeCursor();
          _error.warning(L.l("args unexpected"));
          return BooleanValue.FALSE;
        }

        return fetchArrayAssoc();

      case PDO.FETCH_NUM:
        if (args.length > 0) {
          closeCursor();
          _error.warning(L.l("args unexpected"));
          return BooleanValue.FALSE;
        }

        return fetchArrayNum();

      case PDO.FETCH_BOTH:
        if (args.length > 0) {
          closeCursor();
          _error.warning(L.l("args unexpected"));
          return BooleanValue.FALSE;
        }

        return fetchArrayBoth();

      case PDO.FETCH_OBJ:
        return fetchObject(null, NULL_VALUES);

      case PDO.FETCH_LAZY:
        return fetchLazy(null, NULL_VALUES);

      case PDO.FETCH_BOUND:
        return fetchBound();

      case PDO.FETCH_COLUMN:
        return fetchColumn();

      case PDO.FETCH_CLASS:
        return fetchClass();

      case PDO.FETCH_INTO:
        return fetchInto();

      case PDO.FETCH_FUNC:
        return fetchFunc();

      case PDO.FETCH_NAMED:
        return fetchNamed();

    default:
      _error.warning(L.l("invalid fetch mode {0}",  fetchMode));
      closeCursor();
      return BooleanValue.FALSE;
    }
  }

  public Value fetchAll(@Optional int fetchMode, @Optional Value[] args)
  {
    ArrayValueImpl rows = new ArrayValueImpl();

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    fetchMode = fetchMode & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    while (true) {
      Value value = fetch(fetchMode, args);

      if (_error.isError()) {
        rows.clear();
        return rows;
      }

      if (_resultSetExhausted)
        break;

      rows.put(value);
    }

    return rows;
  }

  private Value fetchArrayAssoc()
  {
    try {
      if (!advanceResultSet())
        return BooleanValue.FALSE;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getMetaData().getColumnCount();

      for (int i = 0; i < columnCount; i++) {
        String name = getMetaData().getColumnName(i + 1);
        Value value = getColumnValue(i);

        array.put(new StringValue(name), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return BooleanValue.FALSE;
    }
  }

  private Value fetchArrayBoth()
  {
    try {
      if (!advanceResultSet())
        return BooleanValue.FALSE;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getMetaData().getColumnCount();

      for (int i = 0; i < columnCount; i++) {
        String name = getMetaData().getColumnName(i + 1);
        Value value = getColumnValue(i);

        array.put(new StringValue(name), value);
        array.put(LongValue.create(i), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return BooleanValue.FALSE;
    }
  }

  private Value fetchArrayNum()
  {
    try {
      if (!advanceResultSet())
        return BooleanValue.FALSE;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getMetaData().getColumnCount();

      for (int i = 0; i < columnCount; i++) {
        Value value = getColumnValue(i);

        array.put(value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return BooleanValue.FALSE;
    }
  }

  private Value fetchBound()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  private Value fetchClass()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  public Value fetchColumn()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  private Value fetchFunc()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  private Value fetchInto()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  private Value fetchLazy(String className, Value[] args)
  {
    // XXX: need to check why lazy is no different than object
    return fetchObject(className, args);
  }

  private Value fetchNamed()
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    throw new UnimplementedException();
  }

  public Value fetchObject(@Optional String className, @Optional Value[] args)
  {
    if (!advanceResultSet())
      return BooleanValue.FALSE;

    if (className != null)
      throw new UnimplementedException();

    if (args.length > 0)
      throw new UnimplementedException();


    try {
      Value object = _env.createObject();

      int columnCount = getMetaData().getColumnCount();

      for (int i = 0; i < columnCount; i++) {
        String name = getMetaData().getColumnName(i + 1);
        Value value = getColumnValue(i);

        object.putField(_env, name, value);
      }

      return object;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return BooleanValue.FALSE;
    }

  }

  public Value getAttribute(int attribute)
  {
    throw new UnimplementedException();
  }

  /**
   * @param column 0-based column index
   */
  public Value getColumnMeta(int column)
  {
    throw new UnimplementedException();
  }

  /**
   * @param column 0-based column index
   */
  private Value getColumnValue(int column)
    throws SQLException
  {
    if (_resultSet == null)
      return DefaultValue.DEFAULT;

    column++;

    // XXX: needs tests
    switch (getMetaData().getColumnType(column)) {
      case Types.NULL:
        return NullValue.NULL;

      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      {
        long value = _resultSet.getLong(column);

        if (_resultSet.wasNull())
          return NullValue.NULL;
        else
          return new LongValue(value);
      }

      case Types.DOUBLE:
      {
        double value = _resultSet.getDouble(column);

        if (_resultSet.wasNull())
          return NullValue.NULL;
        else
          return new DoubleValue(value);
      }

      // XXX: lob

      default:
      {
        String value = _resultSet.getString(column);

        if (value == null || _resultSet.wasNull())
          return NullValue.NULL;
        else
          return new StringValue(value);
      }
    }

  }

  /**
   * Returns an iterator of the values.
   */
  public Iterator<Value> iterator()
  {
    Value value = fetchAll(0, null);

    if (value instanceof ArrayValue)
      return ((ArrayValue) value).values().iterator();
    else {
      Set<Value> emptySet = Collections.emptySet();
      return emptySet.iterator();
    }
  }

  public boolean nextRowset()
  {
    throw new UnimplementedException();
  }

  public int rowCount()
  {
    if (_resultSet == null)
      return 0;

    try {
      int row = _resultSet.getRow();

      try {
        _resultSet.last();

        return _resultSet.getRow();
      }
      finally {
        if (row == 0)
          _resultSet.beforeFirst();
        else
          _resultSet.absolute(row);
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      return 0;
    }
  }

  public boolean setAttribute(int attribute, Value value)
  {
    throw new UnimplementedException();
  }

  /**
   * Sets the fetch mode, the default is {@link PDO.FETCH_BOTH}.
   */
  public boolean setFetchMode(int fetchMode, Value[] args)
  {
    _fetchMode = fetchMode;
    _fetchModeArgs = args;

    return true;
  }

  public String toString()
  {
    return "PDOStatement[" + _query + "]";
  }

  private void destroy()
  {
    ResultSet resultSet = _resultSet;
    Statement statement = _statement;
    PreparedStatement preparedStatement = _preparedStatement;

    _resultSet = null;
    _resultSetMetaData = null;
    _resultSetExhausted = true;
    _statement = null;
    _preparedStatement = null;

    if (resultSet != null)  {
      try {
        resultSet.close();
      }
      catch (SQLException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (statement != null)  {
      try {
        statement.close();
      }
      catch (SQLException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (preparedStatement != null)  {
      try {
        preparedStatement.close();
      }
      catch (SQLException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
}
