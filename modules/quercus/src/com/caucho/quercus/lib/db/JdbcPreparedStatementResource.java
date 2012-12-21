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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.file.FileReadValue;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Logger;

/**
 * Represents a JDBC Statement value.
 */
public class JdbcPreparedStatementResource
  extends JdbcStatementResource
{
  private static final L10N L = new L10N(JdbcPreparedStatementResource.class);

  private PreparedStatement _preparedStmt;

  private ColumnType[] _types;
  private Value[] _params;

  /**
   * Constructor for JdbcStatementResource
   *
   * @param connV a JdbcConnectionResource connection
   */
  public JdbcPreparedStatementResource(JdbcConnectionResource conn)
  {
    super(conn);
  }

  /**
   * Creates _types and _params array for this prepared statement.
   *
   * @param types  = string of i,d,s,b (ie: "idds")
   * @param params = array of values (probably Vars)
   * @return true on success ir false on failure
   */
  protected boolean bindParams(Env env,
                               ColumnType[] types,
                               Value[] params)
  {
    _types = types;
    _params = params;

    return true;
  }

  private boolean setLobParameter(Env env, int index, Value value)
  {
    if (_preparedStmt == null) {
      return false;
    }

    try {
      if (value == null || value.isNull()) {
        _preparedStmt.setObject(index, null);
      }
      else if (value.isString()) {
        _preparedStmt.setBinaryStream(index,
                                      value.toInputStream(),
                                      value.length());
      }
      else {
        InputStream inputStream = value.toInputStream();

        if (inputStream == null) {
          env.warning(L.l("type {0} ({1}) for parameter index {2} cannot be used for lob",
                          value.getType(), value.getClass(), index));
          return false;
        }

        int length = -1;

        if (value instanceof FileReadValue) {
          length = (int) ((FileReadValue) value).getLength();

          if (length <= 0)
            length = -1;
        }

        if (length < 0) {
          TempBuffer tempBuffer = TempBuffer.allocate();

          try {
            byte[] bytes = new byte[1024];

            int len;

            while ((len = inputStream.read(bytes, 0, 1024)) != -1)
              tempBuffer.write(bytes, 0, len);
          }
          catch (IOException e) {
            env.warning(e);
            return false;
          }

          TempReadStream tempReadStream = new TempReadStream(tempBuffer);
          tempReadStream.setFreeWhenDone(true);

          _preparedStmt.setBinaryStream(index,
                                        new ReadStream(tempReadStream),
                                        tempBuffer.getLength());
        }
        else {
          _preparedStmt.setBinaryStream(index, inputStream, length);
        }
      }
    }
    catch (SQLException e) {
      setError(env, e);
      return false;
    }

    return true;
  }

  @Override
  protected boolean prepareForExecute(Env env)
    throws SQLException
  {
    if (_types == null || _preparedStmt == null) {
      return true;
    }

    int size = _types.length;

    for (int i = 0; i < size; i++) {
      ColumnType type = _types[i];

      if (type == ColumnType.BOOLEAN) {
        _preparedStmt.setBoolean(i + 1, _params[i].toBoolean());
      }
      else if (type == ColumnType.NULL) {
        _preparedStmt.setNull(i + 1, Types.NULL);
      }
      else if (type == ColumnType.LONG) {
        _preparedStmt.setLong(i + 1, _params[i].toInt());
      }
      else if (type == ColumnType.DOUBLE) {
        _preparedStmt.setDouble(i + 1, _params[i].toDouble());
      }
      else if (type == ColumnType.BLOB) {
        // XXX: blob needs to be redone
        // Currently treated as a string
        _preparedStmt.setString(i + 1, _params[i].toString());
      }
      else if (type == ColumnType.STRING) {
        _preparedStmt.setString(i + 1, _params[i].toString());
      }
      else if (type == ColumnType.LOB) {
        setLobParameter(env, i + 1, _params[i]);
      }
      else {
        throw new SQLException("unknown type: " + type);
      }
    }

    return true;
  }

  @Override
  protected boolean executeImpl(Env env)
    throws SQLException
  {
    if (_preparedStmt != null) {
      return _preparedStmt.execute();
    }
    else {
      return super.executeImpl(env);
    }
  }

  /**
   * Returns the internal prepared statement.
   *
   * @return the internal prepared statement
   */
  protected PreparedStatement getPreparedStatement()
  {
    return _preparedStmt;
  }

  /**
   * Counts the number of parameter markers in the query string.
   *
   * @return the number of parameter markers in the query string
   */
  public int paramCount()
  {
    String query = getQuery();

    if (query == null) {
      return -1;
    }

    int count = 0;
    int length = query.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = query.charAt(i);

      if (c == '\\') {
        if (i < length - 1) {
          i++;
        }

        continue;
      }

      if (inQuotes) {
        if (c == '\'')
          inQuotes = false;
        continue;
      }

      if (c == '\'') {
        inQuotes = true;
        continue;
      }

      if (c == '?') {
        count++;
      }
    }

    return count;
  }

  /**
   * Prepares this statement with the given query.
   *
   * @param query SQL query
   * @return true on success or false on failure
   */
  public boolean prepare(Env env, String query)
  {
    try {
      PreparedStatement preparedStmt = _preparedStmt;

      if (preparedStmt != null) {
        preparedStmt.close();
      }

      setQuery(query);

      if (query.length() == 0) {
        return false;
      }

      JdbcConnectionResource conn = getConnection();
      Connection javaConn = getJavaConnection(env);

      if (conn == null) {
        return false;
      }

      if (! isPreparable(query)) {
        Statement stmt = javaConn.createStatement();

        setStatement(stmt);

        return true;
      }

      if (getStatementType() == StatementType.INSERT) {
        preparedStmt = javaConn.prepareStatement(query,
                                                 Statement.RETURN_GENERATED_KEYS);
      }
      else if (this instanceof OracleStatement) {
        preparedStmt = javaConn.prepareCall(query,
                                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                                            ResultSet.CONCUR_READ_ONLY);
      }
      else if (conn.isSeekable()) {
        preparedStmt = javaConn.prepareStatement(query,
                                                 ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                 ResultSet.CONCUR_READ_ONLY);
      }
      else {
        preparedStmt = javaConn.prepareStatement(query);
      }

      _preparedStmt = preparedStmt;
      setStatement(preparedStmt);

      return true;

    }
    catch (SQLException e) {
      setError(env, e);
      return false;
    }
  }

  protected boolean isPreparable(String query)
  {
    // for Google
    SqlParseToken token = getConnection().parseSqlToken(query, null);

    if (token == null) {
      return false;
    }

    switch (token.getFirstChar()) {
      case 'S':
      case 's':
        return ! token.matchesToken("SAVEPOINT");
      case 'R':
      case 'r':
        if (token.matchesToken("RELEASE")) {
          return false;
        }
        else {
          return ! token.matchesToken("ROLLBACK");
        }
      default:
        return true;
    }
  }

  /**
   * Returns a parameter value
   * Known subclasses: see PostgresStatement.execute
   */
  protected Value getParam(int i)
  {
    if (i >= _params.length) {
      return UnsetValue.UNSET;
    }

    return _params[i];
  }

  /**
   * Returns the number of parameters available to binding
   * Known subclasses: see PostgresStatement.execute
   */
  protected int getParamLength()
  {
    return _params.length;
  }

  /**
   * Changes the internal statement.
   */
  protected void setPreparedStatement(PreparedStatement stmt)
  {
    _preparedStmt = stmt;
  }

  /**
   * Sets the given parameter
   * Known subclasses: see PostgresStatement.execute
   */
  protected void setObject(int i, Object param)
    throws SQLException
  {
    // See php/4358, php/43b8, php/43d8, and php/43p8.
    ParameterMetaData pmd = _preparedStmt.getParameterMetaData();
    int type = pmd.getParameterType(i);

    switch (type) {
      case Types.OTHER:
      {
        // See php/43b8
        String typeName = pmd.getParameterTypeName(i);
        if (typeName.equals("interval")) {
          _preparedStmt.setObject(i, param);
        }
        else {
          try {
            Class<?> cl = Class.forName("org.postgresql.util.PGobject");
            Constructor<?> constructor = cl.getDeclaredConstructor();
            Object object = constructor.newInstance();

            Method method = cl.getDeclaredMethod("setType",
                                                 new Class[] {String.class});
            method.invoke(object, new Object[] {typeName});

            method = cl.getDeclaredMethod("setValue",
                                          new Class[] {String.class});
            method.invoke(object, new Object[] {param});

            _preparedStmt.setObject(i, object, type);
          }
          catch (ClassNotFoundException e) {
            throw new SQLException(e);
          }
          catch (NoSuchMethodException e) {
            throw new SQLException(e);
          }
          catch (InvocationTargetException e) {
            throw new SQLException(e.getCause());
          }
          catch (IllegalAccessException e) {
            throw new SQLException(e);
          }
          catch (InstantiationException e) {
            throw new SQLException(e);
          }
        }

        break;
      }

      case Types.DOUBLE:
      {
        // See php/43p8.
        String typeName = pmd.getParameterTypeName(i);
        if (typeName.equals("money")) {
          String s = param.toString();

          if (s.length() == 0) {
            throw new IllegalArgumentException(
                L.l("argument `{0}' cannot be empty", param));
          } else {

            String money = s;

            if (s.charAt(0) == '$')
              s = s.substring(1);
            else
              money = "$" + money;

            try {
              // This will throw an exception if not double while
              // trying to setObject() would not. The error would
              // come late, otherwise. See php/43p8.
              Double.parseDouble(s);
            } catch (Exception ex) {
              throw new IllegalArgumentException(L.l(
                  "cannot convert argument `{0}' to money", param));
            }

            try {
              Class<?> cl = Class.forName("org.postgresql.util.PGmoney");

              Constructor<?> constructor
                = cl.getDeclaredConstructor(new Class[] {String.class});

              Object object = constructor.newInstance(new Object[] {money});

              _preparedStmt.setObject(i, object, Types.OTHER);
            }
            catch (ClassNotFoundException e) {
              throw new SQLException(e);
            }
            catch (NoSuchMethodException e) {
              throw new SQLException(e);
            }
            catch (InvocationTargetException e) {
              throw new SQLException(e.getCause());
            }
            catch (IllegalAccessException e) {
              throw new SQLException(e);
            }
            catch (InstantiationException e) {
              throw new SQLException(e);
            }

            break;
          }
        }
        // else falls to default case
      }

      default:
        _preparedStmt.setObject(i, param, type);
    }
  }
}
