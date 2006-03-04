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
 * @author Sam
 */

package com.caucho.quercus.lib;

import java.sql.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;

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

  private static final Value FETCH_FAILURE = new BooleanValue(false) {};
  private static final Value FETCH_EXHAUSTED = new BooleanValue(false) {};
  private static final Value FETCH_CONTINUE = new BooleanValue(false) {};
  private static final Value FETCH_SUCCESS = new BooleanValue(true) {};

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
  private ArrayList<BindColumn> _bindColumns;

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

      if (!isNext)
        return false;

      if (_bindColumns != null) {
        for (BindColumn bindColumn : _bindColumns)
          if (!bindColumn.bind())
            return false;
      }

      return isNext;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }
  }

  public boolean bindColumn(Value column, @Reference Value var, @Optional("-1") int type)
  {
    if (_bindColumns == null)
      _bindColumns = new ArrayList<BindColumn>();

    try {
      _bindColumns.add(new BindColumn(column, var, type));
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }

    return true;
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
      return getResultSetMetaData().getColumnCount();
    }
    catch (SQLException e) {
      _error.error(e);

      return 0;
    }
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

      if (args.length == 0)
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
        return fetchAssoc(args);

      case PDO.FETCH_BOTH:
        return fetchBoth(args);

      case PDO.FETCH_BOUND:
        return fetchBound();

      case PDO.FETCH_COLUMN:
        return fetchColumn(args);

      case PDO.FETCH_CLASS:
        return fetchClass(args);

      case PDO.FETCH_FUNC:
        return fetchFunc();

      case PDO.FETCH_INTO:
        return fetchInto(args);

      case PDO.FETCH_LAZY:
        return fetchLazy(args);

      case PDO.FETCH_NAMED:
        return fetchNamed(args);

      case PDO.FETCH_NUM:
        return fetchNum(args);

      case PDO.FETCH_OBJ:
        return fetchObject(args);

    default:
      _error.warning(L.l("invalid fetch mode {0}",  fetchMode));
      closeCursor();
      return BooleanValue.FALSE;
    }
  }

  public Value fetchAll(@Optional int fetchMode, @Optional Value[] args)
  {
    _error.clear();

    if (fetchMode == 0) {
      fetchMode = _fetchMode;

      if (args.length == 0)
        args = _fetchModeArgs;
    }

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    if (isGroup)
      throw new UnimplementedException("PDO.FETCH_GROUP");

    if (isUnique)
      throw new UnimplementedException("PDO.FETCH_UNIQUE");

    fetchMode = fetchMode & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    if (fetchMode == PDO.FETCH_LAZY) {
      _error.warning(L.l("PDO::FETCH_LAZY can't be used with PDOStatement::fetchAll()"));
      return BooleanValue.FALSE;
    }

    ArrayValueImpl rows = new ArrayValueImpl();

    while (true) {
      Value value = fetch(fetchMode, args);

      if (value == FETCH_FAILURE) {
        rows.clear();
        return rows;
      }

      if (value == FETCH_EXHAUSTED)
        break;

      if (value == FETCH_CONTINUE)
        continue;

      rows.put(value);
    }

    return rows;
  }

  private Value fetchAssoc(Value[] args)
  {
    if (args.length > 0) {
      closeCursor();
      _error.notice(L.l("args unexpected"));
      return FETCH_FAILURE;
    }

    try {
      if (!advanceResultSet())
        return FETCH_EXHAUSTED;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        array.put(new StringValueImpl(name), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }
  }

  private Value fetchBoth(Value[] args)
  {
    if (args.length > 0) {
      closeCursor();
      _error.notice(L.l("args unexpected"));
      return FETCH_FAILURE;
    }

    return fetchBoth();
  }

  private Value fetchBoth()
  {
    try {
      if (!advanceResultSet())
        return FETCH_EXHAUSTED;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        array.put(new StringValueImpl(name), value);
        array.put(new LongValue(i - 1), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }
  }

  private Value fetchBound()
  {
    if (!advanceResultSet())
      return FETCH_EXHAUSTED;

    return FETCH_SUCCESS;
  }

  private Value fetchClass(Value[] args)
  {
    String className;
    Value[] ctorArgs;

    if (args.length == 0 || args.length > 2)
      return fetchBoth();

    className = args[0].toString();

    if (args.length == 2) {
      if (args[1].isArray()) {
        // XXX: inefiicient, but args[1].getValueArray(_env) doesn't handle references
        ArrayValue argsArray = (ArrayValue) args[1];

        ctorArgs = new Value[argsArray.getSize()];

        int i = 0;

        for (Value key : argsArray.keySet())
          ctorArgs[i++] = argsArray.getRef(key);
      }
      else
        return fetchBoth();
    }
    else
      ctorArgs = NULL_VALUES;

    return fetchObject(className, ctorArgs);
  }

  private Value fetchColumn(@Optional Value[] args)
  {
    if (args.length != 1)
      return fetchBoth();

    Value arg = args[0];

    return fetchColumn(arg.toInt());
  }

  /**
   * @param column 0-based column number
   */
  public Value fetchColumn(@Optional int column)
  {
    if (!advanceResultSet())
      return FETCH_EXHAUSTED;

    try {
      if (column < 0 || column >= getResultSetMetaData().getColumnCount())
        return FETCH_CONTINUE;

      return getColumnValue(column + 1);
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }
  }

  private Value fetchFunc()
  {
    return fetchBoth();
  }

  private Value fetchInto(Value[] args)
  {
    if (args.length != 1)
      return fetchBoth();

    Value var = args[0];

    if (!var.isObject())
      return fetchBoth();

    if (!advanceResultSet())
      return FETCH_EXHAUSTED;

    try {
      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        var.putField(_env, name, value);
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }

    return var;
  }

  private Value fetchLazy(Value[] args)
  {
    return fetchLazy(null, NULL_VALUES);
  }

  private Value fetchLazy(String className, Value[] args)
  {
    // XXX: need to check why lazy is no different than object
    return fetchObject(className, args);
  }

  private Value fetchNamed(Value[] args)
  {
    try {
      if (!advanceResultSet())
        return FETCH_EXHAUSTED;

      ArrayValue array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        Value name = new StringValueImpl(getResultSetMetaData().getColumnName(i));
        Value value = getColumnValue(i);

        Value existingValue = array.get(name);

        if (! (existingValue instanceof UnsetValue)) {

          if (! existingValue.isArray()) {
            ArrayValue arrayValue = new ArrayValueImpl();
            arrayValue.put(existingValue);
            array.put(name, arrayValue);
            existingValue = arrayValue;
          }

          existingValue.put(value);
        }
        else
          array.put(name, value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }
  }

  private Value fetchNum(Value[] args)
  {
    if (args.length > 0) {
      closeCursor();
      _error.notice(L.l("args unexpected"));
      return FETCH_FAILURE;
    }

    try {
      if (!advanceResultSet())
        return FETCH_EXHAUSTED;

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        Value value = getColumnValue(i);

        array.put(value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return FETCH_FAILURE;
    }
  }

  private Value fetchObject(Value[] args)
  {
    return fetchObject(null, NULL_VALUES);
  }

  public Value fetchObject(@Optional String className, @Optional Value[] args)
  {
    AbstractQuercusClass cl;

    if (className != null) {
      cl = _env.findAbstractClass(className);

      if (cl == null)
        return fetchBoth();
    }
    else
      cl = null;

    if (!advanceResultSet())
      return FETCH_EXHAUSTED;

    try {
      Value object;

      if (cl != null)
        object = cl.evalNew(_env, args);
      else
        object = _env.createObject();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        object.putField(_env, name, value);
      }

      return object;
    }
    catch (Throwable ex) {
      _error.error(ex);
      return FETCH_FAILURE;
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
   * @param column 1-based column index
   */
  private Value getColumnValue(int column)
    throws SQLException
  {
    return getColumnValue(column, -1, -1);
  }

  /**
   * @param column 1-based column index
   * @param jdbcType a Jdbc type, or -1 if it is unknonw
   * @param returnType a PDO.PARAM_* type, or -1
   */
  private Value getColumnValue(int column, int jdbcType, int returnType)
    throws SQLException
  {
    if (returnType != -1)
      throw new UnimplementedException("parm type " + returnType);

    if (jdbcType == -1)
      jdbcType = getResultSetMetaData().getColumnType(column);

    // XXX: needs tests

    switch (jdbcType) {
      case Types.NULL:
        return NullValue.NULL;

      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      {
        String value = _resultSet.getString(column);

        if (value == null || _resultSet.wasNull())
          return NullValue.NULL;
        else
          return new StringValueImpl(value);
      }

      case Types.DOUBLE:
      {
        double value = _resultSet.getDouble(column);

        if (_resultSet.wasNull())
          return NullValue.NULL;
        else
          return (new DoubleValue(value)).toStringValue();
      }

      // XXX: lob

      default:
      {
        String value = _resultSet.getString(column);

        if (value == null || _resultSet.wasNull())
          return NullValue.NULL;
        else
          return new StringValueImpl(value);
      }
    }

  }

  private ResultSetMetaData getResultSetMetaData()
    throws SQLException
  {
    if (_resultSetMetaData == null)
      _resultSetMetaData = _resultSet.getMetaData();

    return _resultSetMetaData;
  }

  /**
   * Returns an iterator of the values.
   */
  public Iterator<Value> iterator()
  {
    Value value = fetchAll(0, NULL_VALUES);

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
    int fetchStyle = fetchMode;

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    if (isGroup)
      throw new UnimplementedException("PDO.FETCH_GROUP");

    if (isUnique)
      throw new UnimplementedException("PDO.FETCH_UNIQUE");

    fetchStyle = fetchStyle & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    boolean isClasstype = (fetchMode & PDO.FETCH_CLASSTYPE) != 0;
    boolean isSerialize = (fetchMode & PDO.FETCH_SERIALIZE) != 0;

    fetchStyle = fetchStyle & (~(PDO.FETCH_CLASSTYPE | PDO.FETCH_SERIALIZE));

    switch (fetchStyle) {
      case PDO.FETCH_CLASS:
        if (args.length < 1 || args.length > 2)
          return false;

        if (_env.findAbstractClass(args[0].toString()) == null)
          return false;

        if (args.length == 2 && !(args[1].isArray()))
          return false;

        break;

      case PDO.FETCH_COLUMN:
        if (args.length != 1)
          return false;
        break;

     case PDO.FETCH_FUNC:
       _error.warning(L.l("PDO::FETCH_FUNC can only be used with PDOStatement::fetchAll()"));
       return false;

      case PDO.FETCH_INTO:
        if (args.length != 1)
          return false;

        if (! args[0].isObject())
          return false;

        break;

      default:
        break;
    }

    _fetchMode = fetchMode;
    _fetchModeArgs = args;
    return true;

  }

  public String toString()
  {
    return "PDOStatement[" + _query + "]";
  }

  private class BindColumn {
    private final String _columnAsName;
    private final Value _var;
    private final int _type;

    private int _column;
    private int _jdbcType;

    private boolean _isInit;
    private boolean _isValid;

    /**
     * @param column 1-based column index
     * @param var reference that receives the value
     * @param type a PARM_* type, -1 for default
     */
    private BindColumn(Value column, Value var, int type)
      throws SQLException
    {
      assert column != null;
      assert var != null;

      if (column.isNumber()) {
        _column = column.toInt();
        _columnAsName = null;
      }
      else {
        _columnAsName = column.toString();
      }

      _var = var;
      _type = type;

      if (_resultSet != null)
        init();
    }

    private boolean init()
      throws SQLException
    {
      if (_isInit)
        return true;

      ResultSetMetaData resultSetMetaData = getResultSetMetaData();

      int columnCount = resultSetMetaData.getColumnCount();

      if (_columnAsName != null) {

        for (int i = 1; i <= columnCount; i++) {
          String name = resultSetMetaData.getColumnName(i);
          if (name.equals(_columnAsName)) {
            _column = i;
            break;
          }
        }
      }

      _isValid = _column > 0 && _column <= columnCount;

      if (_isValid) {
        _jdbcType = resultSetMetaData.getColumnType(_column);
      }

      _isInit = true;

      return true;
    }

    public boolean bind()
      throws SQLException
    {
      if (!init())
        return false;

      if (!_isValid) {
        // this matches php behaviour
        _var.set(StringValueImpl.EMPTY);
      }
      else {
        Value value = getColumnValue(_column, _jdbcType, _type);

        _var.set(value);
      }

      return true;
    }
  }
}
