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
 * @author Sam
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * PDO object oriented API facade.
 */
public class PDOStatement
  extends JdbcPreparedStatementResource
  implements Iterable<Value>, EnvCleanup
{
  private static final L10N L = new L10N(PDOStatement.class);

  private static final Value[] NULL_VALUES = new Value[0];

  private final PDO _pdo;
  private final PDOError _error;

  // XXX: need to make public @Name("queryString")
  public final String queryString;

  private int _fetchMode = PDO.FETCH_BOTH;
  private Value[] _fetchModeArgs = NULL_VALUES;
  private HashMap<Value,BoundColumn> _boundColumnMap;

  private HashMap<String,Integer> _parameterNameMap;

  private HashMap<Integer,ColumnType> _paramTypes;
  private HashMap<Integer,Value> _paramValues;

  // protected so it's not callable by PHP code
  protected PDOStatement(Env env,
                         PDO pdo,
                         PDOError error,
                         String query,
                         boolean isPrepared,
                         ArrayValue options,
                         boolean isCatchException)
    throws SQLException
  {
    super(pdo.getConnection());
    env.addCleanup(this);

    _pdo = pdo;
    _error = error;

    if (options != null && options.getSize() > 0) {
      env.notice(L.l("PDOStatement options unsupported"));
    }

    if (isPrepared) {
      query = parseQueryString(env, query);

      prepare(env, query);

      // php/1s41 - oracle can't handle this
      //_preparedStatement.setEscapeProcessing(false);
    }
    else {
      setQuery(query);

      try {
        setStatement(pdo.getConnection().createStatement(env));
        execute(env, false);
      }
      catch (SQLException e) {
        if (isCatchException) {
          _error.error(env, e);
        }
        else {
          throw e;
        }
      }
    }

    this.queryString = query;
  }

  //side-effect, updates _parameterNameMap
  private String parseQueryString(Env env, String query)
  {
    int len = query.length();
    StringBuilder sb = new StringBuilder(len);

    int parameterCount = 0;

    for (int i = 0; i < len; i++) {
      int ch = query.charAt(i);

      if (ch == '\'' || ch == '"') {
        sb.append((char) ch);

        while (true) {
          i++;

          int ch2 = query.charAt(i);

          if (ch2 < 0) {
            env.error(L.l("missing ending quote in query: {0}", query));
          }

          sb.append((char) ch2);

          if (ch2 == ch) {
            break;
          }
        }
      }
      else if (ch == '?') {
        parameterCount++;

        sb.append((char) ch);
      }
      else if (ch == ':') {
        if (i + 1 < len && query.charAt(i + 1) == ':') {
          int j = i + 1;

          for (; j < len && query.charAt(j) == ':'; j++) {
          }

          sb.append(query, i, j + 1);
          i = j;

          continue;
        }

        int start = i + 1;

        while (true) {
          i++;

          int ch2 = -1;
          if (i < len) {
            ch2 = query.charAt(i);
          }

          // XXX: check what characters are allowed
          if (ch2 < 0 || ! Character.isJavaIdentifierPart(ch2)) {
            String name = query.substring(start, i);

            if (_parameterNameMap == null) {
              _parameterNameMap = new HashMap<String,Integer>();
            }

            Integer index = Integer.valueOf(parameterCount++);
            _parameterNameMap.put(name, index);

            sb.append('?');

            if (ch2 >= 0) {
              sb.append((char) ch2);
            }

            break;
          }
        }
      }
      else {
        sb.append((char) ch);
      }
    }

    return sb.toString();
  }

  public boolean bindColumn(Env env,
                            Value column,
                            @Reference Value var,
                            @Optional("-1") int type)
  {
    try {
      if (_boundColumnMap == null) {
        _boundColumnMap = new HashMap<Value,BoundColumn>();
      }

      ColumnType columnType = null;

      if (type == -1) {
      }
      else if (type == PDO.PARAM_INT) {
        columnType = ColumnType.LONG;
      }
      else if (type == PDO.PARAM_BOOL) {
        columnType = ColumnType.BOOLEAN;
      }
      else if (type == PDO.PARAM_STR) {
        columnType = ColumnType.STRING;
      }
      else if (type == PDO.PARAM_NULL) {
        columnType = ColumnType.NULL;
      }
      else if (type == PDO.PARAM_LOB) {
        columnType = ColumnType.LOB;
      }
      else if (type == PDO.PARAM_STMT) {
        throw new UnimplementedException(L.l("PDO::PARAM_STMT"));
      }
      else {
        env.warning(L.l("unknown column type: {0}", type));

        return false;
      }

      ResultSetMetaData metaData = getMetaData();
      BoundColumn boundColumn
        = new BoundColumn(metaData, column, var, columnType);

      _boundColumnMap.put(column, boundColumn);

      return true;
    }
    catch (SQLException e) {
      _error.warning(env, e.getMessage());

      return false;
    }
  }

  public boolean bindParam(Env env,
                           @ReadOnly Value parameter,
                           @Reference Value value,
                           @Optional("PDO::PARAM_STR") int dataType,
                           @Optional("-1") int length,
                           @Optional Value driverOptions)
  {
    if (length != -1) {
      throw new UnimplementedException("length");
    }

    if (! driverOptions.isDefault()) {
      throw new UnimplementedException("driverOptions");
    }

    boolean isInputOutput = (dataType & PDO.PARAM_INPUT_OUTPUT) != 0;

    if (isInputOutput) {
      dataType = dataType & (~PDO.PARAM_INPUT_OUTPUT);
      if (true) throw new UnimplementedException("PARAM_INPUT_OUTPUT");
    }

    Integer index = resolveParameter(parameter);

    if (index == null) {
      _error.warning(env, L.l("unknown parameter: '{0}'", parameter));

      return false;
    }

    ColumnType type;

    switch (dataType) {
      case PDO.PARAM_BOOL:
        type = ColumnType.BOOLEAN;
        break;
      case PDO.PARAM_INT:
        type = ColumnType.LONG;
        break;
      case PDO.PARAM_LOB:
        type = ColumnType.LOB;
        break;
      case PDO.PARAM_NULL:
        type = ColumnType.NULL;
        break;
      case PDO.PARAM_STR:
        type = ColumnType.STRING;
        break;
      case PDO.PARAM_STMT:
        throw new UnimplementedException(L.l("PDO::PARAM_STMT"));
      default:
        _error.warning(env, L.l("unknown dataType '{0}'", dataType));
        return false;
    }

    if (_paramTypes == null) {
      _paramTypes = new HashMap<Integer,ColumnType>();
      _paramValues = new HashMap<Integer,Value>();
    }

    _paramTypes.put(index, type);
    _paramValues.put(index, value);

    return true;
  }

  public boolean bindValue(Env env,
                           @ReadOnly Value parameter,
                           @ReadOnly Value value,
                           @Optional("PDO::PARAM_STR") int dataType)
  {
    if (dataType == -1) {
      dataType = PDO.PARAM_STR;
    }

    value = value.toValue();

    return bindParam(env, parameter, value, dataType, -1, DefaultValue.DEFAULT);
  }

  /**
   * Closes the current cursor.
   */
  public boolean closeCursor(Env env)
  {
    return freeResult();
  }

  /**
   * Returns the number of columns.
   */
  public int columnCount(Env env)
  {
    return getColumnCount(env);
  }

  @Override
  public boolean close()
  {
    return super.close();
  }

  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    close();
  }

  public String errorCode(Env env)
  {
    return _error.getErrorCode();
  }

  public ArrayValue errorInfo()
  {
    return _error.getErrorInfo();
  }

  @Override
  protected void setError(Env env, SQLException e) {
    e.printStackTrace();

    _error.error(env, e);
  }

  /**
   * Execute the statement.
   *
   * @param inputParameters an array containing input values to correspond to
   * the bound parameters for the statement.
   *
   * @return true for success, false for failure
   */
  public boolean execute(Env env, @Optional @ReadOnly Value inputParameters)
  {
    _error.clear();

    ArrayValue parameters;

    if (inputParameters.isArray()) {
      parameters = inputParameters.toArrayValue(env);
    }
    else if (inputParameters.isDefault()) {
      parameters = null;
    }
    else {
      env.warning(L.l("'{0}' is an unexpected argument, expected array",
                      inputParameters));
      return false;
    }

    closeCursor(env);

    if (parameters != null) {
      int size = parameters.getSize();

      ColumnType[] types = new ColumnType[size];
      Value[] values = new Value[size];

      for (Map.Entry<Value, Value> entry : parameters.entrySet()) {
        Value key = entry.getKey();
        Value value = entry.getValue();

        int index;
        if (key.isNumberConvertible()) {
          index = key.toInt();
        }
        else {
          index = resolveParameter(key);
        }

        ColumnType type = ColumnType.getColumnType(value);

        if (type == null) {
          _error.warning(env, L.l("unknown type {0} ({1}) for parameter index {2}",
                                  value.getType(), value.getClass(), index));
          return false;
        }

        types[index] = type;
        values[index] = value;
      }

      bindParams(env, types, values);
    }
    else if (_paramTypes != null) {
      int size = _paramTypes.size();

      ColumnType[]types = new ColumnType[size];
      Value[] values = new Value[size];

      for (Map.Entry<Integer,ColumnType> entry : _paramTypes.entrySet()) {
        Integer index = entry.getKey();
        ColumnType type = entry.getValue();

        int i = index.intValue();

        types[i] = type;
        values[i] = _paramValues.get(index);
      }

      bindParams(env, types, values);
    }

    try {
      return execute(env, false);
    }
    catch (SQLException e) {
      _error.error(env, e);

      return false;
    }
  }

  @Override
  protected boolean executeImpl(Env env)
    throws SQLException
  {
    _pdo.setLastExecutedStatement(this);

    return super.executeImpl(env);
  }

  @Override
  protected JdbcResultResource createResultSet(ResultSet rs)
  {
    return new JdbcResultResource(rs, _pdo.getColumnCase());
  }

  /**
   * Fetch the next row.
   *
   * @param fetchMode the mode, 0 to use the value
   * set by {@link #setFetchMode}.
   * @return a value, BooleanValue.FALSE if there
   * are no more rows or an error occurs.
   */
  public Value fetch(Env env,
                     @Optional int fetchMode,
                     @Optional("0") int cursorOrientation,
                     @Optional("0") int cursorOffset)
  {
    if (cursorOrientation != 0)
      throw new UnimplementedException("fetch with cursorOrientation");

    if (cursorOffset != 0)
      throw new UnimplementedException("fetch with cursorOffset");

    int columnIndex = 0;

    return fetchImpl(env, fetchMode, columnIndex);
  }

  /**
   *
   * @param fetchMode
   * @param columnIndex 0-based column index when fetchMode is FETCH_BOTH
   */
  public Value fetchAll(Env env,
                        @Optional("0") int fetchMode,
                        @Optional("-1") int columnIndex)
  {
    int effectiveFetchMode;

    if (fetchMode == 0) {
      effectiveFetchMode = _fetchMode;
    }
    else {
      effectiveFetchMode = fetchMode;
    }

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    if (isGroup)
      throw new UnimplementedException("PDO.FETCH_GROUP");

    if (isUnique)
      throw new UnimplementedException("PDO.FETCH_UNIQUE");

    effectiveFetchMode = effectiveFetchMode
        & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    switch (effectiveFetchMode) {
      case PDO.FETCH_COLUMN:
        break;

      case PDO.FETCH_LAZY:
        _error.warning(env, L.l("PDO::FETCH_LAZY can't be used with PDOStatement::fetchAll()"));
        return BooleanValue.FALSE;

      default:
        if (columnIndex != -1) {
          _error.warning(env, L.l("unexpected arguments"));
          return BooleanValue.FALSE;
        }
    }

    ArrayValueImpl rows = new ArrayValueImpl();

    while (true) {
      Value value = fetchImpl(env, effectiveFetchMode, columnIndex);

      if (value == BooleanValue.FALSE) {
        break;
      }

      rows.put(value);
    }

    return rows;
  }

  private Value fetchBoth(Env env, JdbcResultResource rs)
  {
    Value value = rs.fetchBoth(env, false);

    if (value == NullValue.NULL) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  private Value fetchBound(Env env, JdbcResultResource rs)
  {
    try {
      if (! rs.next()) {
        return BooleanValue.FALSE;
      }

      return BooleanValue.TRUE;
    }
    catch (SQLException e) {
      _error.warning(env, e.getMessage());

      return BooleanValue.FALSE;
    }
  }

  private void bindColumns(Env env, JdbcResultResource rs)
    throws SQLException
  {
    if (_boundColumnMap != null) {
      for (BoundColumn binding : _boundColumnMap.values()) {
        binding.bind(env, rs);
      }
    }
  }

  private Value fetchClass(Env env, JdbcResultResource rs)
  {
    String className;
    Value[] ctorArgs;

    if (_fetchModeArgs.length == 0 || _fetchModeArgs.length > 2) {
      return fetchBoth(env, rs);
    }

    className = _fetchModeArgs[0].toString();

    if (_fetchModeArgs.length == 2) {
      if (_fetchModeArgs[1].isArray()) {
        // XXX: inefiicient, but args[1].getValueArray(_env)
        // doesn't handle references
        ArrayValue argsArray = (ArrayValue) _fetchModeArgs[1];

        ctorArgs = new Value[argsArray.getSize()];

        int i = 0;

        for (Value key : argsArray.keySet())
          ctorArgs[i++] = argsArray.getVar(key);
      }
      else
        return fetchBoth(env, rs);
    }
    else {
      ctorArgs = NULL_VALUES;
    }

    return fetchObject(env, className, ctorArgs);
  }

  /**
   * @param column 0-based column number
   */
  public Value fetchColumn(Env env, @Optional int column)
  {
    if (column < 0 && _fetchModeArgs.length > 0) {
      column = _fetchModeArgs[0].toInt();
    }

    try {
      if (column < 0 || column >= getMetaData().getColumnCount()) {
        return BooleanValue.FALSE;
      }

      JdbcResultResource rs = getResultSet();

      if (rs == null || ! rs.next()) {
        return BooleanValue.FALSE;
      }

      return rs.getColumnValue(env, column + 1);
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  private Value fetchFunc(Env env)
  {
    throw new UnimplementedException();
  }

  /**
   * Fetch the next row.
   *
   * @param fetchMode the mode, 0 to use the value set by {@link #setFetchMode}.
   * @return a value, BooleanValue.FALSE if there are no more
   *  rows or an error occurs.
   */
  private Value fetchImpl(Env env, int fetchMode, int columnIndex)
  {
    JdbcResultResource rs = getResultSet();

    if (rs == null) {
      return BooleanValue.FALSE;
    }

    if (fetchMode == 0) {
      fetchMode = _fetchMode;

      fetchMode = fetchMode & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));
    }
    else {
      if ((fetchMode & PDO.FETCH_GROUP) != 0) {
        _error.warning(env, L.l("FETCH_GROUP is not allowed"));
        return BooleanValue.FALSE;
      }
      else if ((fetchMode & PDO.FETCH_UNIQUE) != 0) {
        _error.warning(env, L.l("FETCH_UNIQUE is not allowed"));
        return BooleanValue.FALSE;
      }
    }

    boolean isClasstype = (fetchMode & PDO.FETCH_CLASSTYPE) != 0;
    boolean isSerialize = (fetchMode & PDO.FETCH_SERIALIZE) != 0;

    fetchMode = fetchMode & (~(PDO.FETCH_CLASSTYPE | PDO.FETCH_SERIALIZE));

    Value value;

    switch (fetchMode) {
      case PDO.FETCH_ASSOC:
        value = fetchAssoc(env, rs);
        break;
      case PDO.FETCH_BOTH:
        value = fetchBoth(env, rs);
        break;
      case PDO.FETCH_BOUND:
        value = fetchBound(env, rs);
        break;
      case PDO.FETCH_COLUMN:
        value = fetchColumn(env, columnIndex);
        break;
      case PDO.FETCH_CLASS:
        value = fetchClass(env, rs);
        break;
      case PDO.FETCH_FUNC:
        value = fetchFunc(env);
        break;
      case PDO.FETCH_INTO:
        value = fetchInto(env, rs);
        break;
      case PDO.FETCH_LAZY:
        value = fetchLazy(env);
        break;
      case PDO.FETCH_NAMED:
        value = fetchNamed(env, rs);
        break;
      case PDO.FETCH_NUM:
        value = fetchNum(env, rs);
        break;
      case PDO.FETCH_OBJ:
        value = fetchObject(env, rs);
        break;
    default:
      _error.warning(env, L.l("invalid fetch mode {0}",  fetchMode));
      closeCursor(env);
      value = BooleanValue.FALSE;
    }

    try {
      if (value != BooleanValue.FALSE) {
        bindColumns(env, rs);
      }

      return value;
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  private Value fetchNum(Env env, JdbcResultResource rs)
  {
    Value value = rs.fetchNum(env);

    if (value == NullValue.NULL) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  private Value fetchAssoc(Env env, JdbcResultResource rs)
  {
    Value value = rs.fetchAssoc(env);

    if (value == NullValue.NULL) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  private Value fetchObject(Env env, JdbcResultResource rs)
  {
    Value value = rs.fetchObject(env, null, Value.NULL_ARGS);

    if (value == NullValue.NULL) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  private Value fetchInto(Env env, JdbcResultResource rs)
  {
    if (_fetchModeArgs.length == 0) {
      return BooleanValue.FALSE;
    }

    if (! _fetchModeArgs[0].isObject()) {
      return BooleanValue.FALSE;
    }

    try {
      if (! rs.next()) {
        return BooleanValue.FALSE;
      }

      Value var = _fetchModeArgs[0];

      int columnCount = getMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = rs.getColumnLabel(i);
        Value value = getColumnValue(env, i);

        var.putField(env, name, value);
      }

      return var;
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  private Value fetchLazy(Env env)
  {
    // XXX: need to check why lazy is no different than object
    return fetchObject(env, null, NULL_VALUES);
  }

  private Value fetchNamed(Env env, JdbcResultResource rs)
  {
    try {
      ArrayValue array = new ArrayValueImpl();

      int columnCount = getMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        Value name = env.createString(rs.getColumnLabel(i));
        Value value = getColumnValue(env, i);

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
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  public Value fetchObject(Env env,
                           @Optional String className,
                           @Optional Value[] args)
  {
    JdbcResultResource rs = getResultSet();

    if (rs == null) {
      return BooleanValue.FALSE;
    }

    Value value = rs.fetchObject(env, className, args);

    if (value == NullValue.NULL) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  public Value getAttribute(Env env, int attribute)
  {
    _error.unsupportedAttribute(env, attribute);

    return BooleanValue.FALSE;
  }

  /**
   * @param column 0-based column index
   */
  public Value getColumnMeta(Env env, int column)
  {
    throw new UnimplementedException();
  }

  /**
   * @param column 1-based column index
   */
  private Value getColumnValue(Env env, int column)
    throws SQLException
  {
    JdbcResultResource rs = getResultSet();

    return rs.getColumnValue(env, column);
  }

  /**
   * Returns an iterator of the values.
   */
  public Iterator<Value> iterator()
  {
    Value value = fetchAll(Env.getInstance(), 0, -1);

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

  private Integer resolveParameter(Value parameter)
  {
    Integer index = null;

    if (parameter.isLong()) {
      // slight optimization for normal case
      index = Integer.valueOf(parameter.toInt() - 1);
    }
    else {
      String name = parameter.toString();

      if (name.length() > 1 && name.charAt(0) == ':') {
        name = name.substring(1);
      }

      if (_parameterNameMap != null) {
        index = _parameterNameMap.get(name);
      }
      else {
        index = Integer.valueOf(parameter.toInt());
      }
    }

    return index;
  }

  public int rowCount(Env env)
  {
    JdbcResultResource rs = getResultSet();

    if (rs == null) {
      return 0;
    }

    return rs.getNumRows();
  }

  public boolean setAttribute(Env env, int attribute, Value value)
  {
    return setAttribute(env, attribute, value, false);
  }

  public boolean setAttribute(Env env,
                              int attribute,
                              Value value,
                              boolean isFromConstructor)
  {
    if (isFromConstructor) {
      switch (attribute) {
        case PDO.CURSOR_FWDONLY:
        case PDO.CURSOR_SCROLL:
          return setCursor(env, attribute);
      }
    }

    _error.unsupportedAttribute(env, attribute);

    return false;
  }

  private boolean setCursor(Env env, int attribute)
  {
    switch (attribute) {
      case PDO.CURSOR_FWDONLY:
        throw new UnimplementedException();
      case PDO.CURSOR_SCROLL:
        throw new UnimplementedException();

      default:
        _error.unsupportedAttribute(env, attribute);
        return false;
    }
  }


  /**
   * Sets the fetch mode, the default is {@link PDO.FETCH_BOTH}.
   */
  public boolean setFetchMode(Env env, int fetchMode, Value[] args)
  {
    _fetchMode = PDO.FETCH_BOTH;
    _fetchModeArgs = NULL_VALUES;

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
      case PDO.FETCH_ASSOC:
      case PDO.FETCH_BOTH:
      case PDO.FETCH_BOUND:
      case PDO.FETCH_LAZY:
      case PDO.FETCH_NAMED:
      case PDO.FETCH_NUM:
      case PDO.FETCH_OBJ:
        if (args.length > 0) {
          env.warning(L.l("this fetch mode does not accept any arguments"));

          return false;
        }
        break;

      case PDO.FETCH_CLASS:
        if (args.length < 1 || args.length > 2)
          return false;

        if (env.findClass(args[0].toString()) == null)
          return false;

        if (args.length == 2 && !(args[1].isNull() || args[1].isArray())) {
          env.warning(L.l("constructor args must be an array"));

          return false;
        }

        break;

      case PDO.FETCH_COLUMN:
        if (args.length != 1)
          return false;

        break;

     case PDO.FETCH_FUNC:
       _error.warning(env, L.l("PDO::FETCH_FUNC can only be used with PDOStatement::fetchAll()"));
       return false;

      case PDO.FETCH_INTO:
        if (args.length != 1 || !args[0].isObject())
          return false;

        break;

      default:
        _error.warning(env, L.l("invalid fetch mode"));
        break;
    }

    _fetchModeArgs = args;
    _fetchMode = fetchMode;

    return true;
  }

  @Override
  protected boolean isFetchFieldIndexBeforeFieldName()
  {
    return false;
  }

  public String toString()
  {
    String query = getQuery();

    return "PDOStatement[" + query.substring(0, 16) + "]";
  }
}
