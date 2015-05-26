/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.sql;

import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User-view of prepared statements
 */
public class UserCallableStatement extends UserPreparedStatement
  implements CallableStatement {
  protected final static Logger log
    = Logger.getLogger(UserCallableStatement.class.getName());
  protected static L10N L = new L10N(UserCallableStatement.class);

  protected CallableStatement _cstmt;

  UserCallableStatement(UserConnection conn,
                        CallableStatement cStmt)
  {
    super(conn, cStmt);
    
    _cstmt = cStmt;

    if (cStmt == null)
      throw new NullPointerException();
  }

  /**
   * The array value
   */
  @Override
  public Array getArray(int i)
    throws SQLException
  {
    try {
      return _cstmt.getArray(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The array value
   */
  @Override
  public Array getArray(String name)
    throws SQLException
  {
    try {
      return _cstmt.getArray(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The big decimal value
   */
  @Override
  public BigDecimal getBigDecimal(int i)
    throws SQLException
  {
    try {
      return _cstmt.getBigDecimal(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The bigDecimal value
   */
  @Override
  public BigDecimal getBigDecimal(String name)
    throws SQLException
  {
    try {
      return _cstmt.getBigDecimal(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The big decimal value
   */
  @Override
  @SuppressWarnings("deprecation")
  public BigDecimal getBigDecimal(int i, int scale)
    throws SQLException
  {
    try {
      return _cstmt.getBigDecimal(i, scale);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The blob value
   */
  @Override
  public Blob getBlob(int i)
    throws SQLException
  {
    try {
      return _cstmt.getBlob(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The blob value
   */
  @Override
  public Blob getBlob(String name)
    throws SQLException
  {
    try {
      return _cstmt.getBlob(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The boolean value
   */
  @Override
  public boolean getBoolean(int i)
    throws SQLException
  {
    try {
      return _cstmt.getBoolean(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The boolean value
   */
  @Override
  public boolean getBoolean(String name)
    throws SQLException
  {
    try {
      return _cstmt.getBoolean(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The byte value
   */
  @Override
  public byte getByte(int i)
    throws SQLException
  {
    try {
      return _cstmt.getByte(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The byte value
   */
  @Override
  public byte getByte(String name)
    throws SQLException
  {
    try {
      return _cstmt.getByte(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The bytes value
   */
  @Override
  public byte []getBytes(int i)
    throws SQLException
  {
    try {
      return _cstmt.getBytes(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The bytes value
   */
  @Override
  public byte []getBytes(String name)
    throws SQLException
  {
    try {
      return _cstmt.getBytes(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The clob value
   */
  @Override
  public Clob getClob(int i)
    throws SQLException
  {
    try {
      return _cstmt.getClob(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The clob value
   */
  @Override
  public Clob getClob(String name)
    throws SQLException
  {
    try {
      return _cstmt.getClob(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The date value
   */
  @Override
  public Date getDate(int i)
    throws SQLException
  {
    try {
      return _cstmt.getDate(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The date value
   */
  @Override
  public Date getDate(String name)
    throws SQLException
  {
    try {
      return _cstmt.getDate(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The date value
   */
  @Override
  public Date getDate(int i, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getDate(i, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The date value
   */
  @Override
  public Date getDate(String name, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getDate(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The double value
   */
  @Override
  public double getDouble(int i)
    throws SQLException
  {
    try {
      return _cstmt.getDouble(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The double value
   */
  @Override
  public double getDouble(String name)
    throws SQLException
  {
    try {
      return _cstmt.getDouble(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The float value
   */
  @Override
  public float getFloat(int i)
    throws SQLException
  {
    try {
      return _cstmt.getFloat(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The float value
   */
  @Override
  public float getFloat(String name)
    throws SQLException
  {
    try {
      return _cstmt.getFloat(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The int value
   */
  @Override
  public int getInt(int i)
    throws SQLException
  {
    try {
      return _cstmt.getInt(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The int value
   */
  @Override
  public int getInt(String name)
    throws SQLException
  {
    try {
      return _cstmt.getInt(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The long value
   */
  @Override
  public long getLong(int i)
    throws SQLException
  {
    try {
      return _cstmt.getLong(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The long value
   */
  @Override
  public long getLong(String name)
    throws SQLException
  {
    try {
      return _cstmt.getLong(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The object value
   */
  @Override
  public Object getObject(int i)
    throws SQLException
  {
    try {
      return _cstmt.getObject(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The object value
   */
  @Override
  public Object getObject(String name)
    throws SQLException
  {
    try {
      return _cstmt.getObject(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The object value
   */
  @Override
  public Object getObject(int i, Map<String,Class<?>> map)
    throws SQLException
  {
    try {
      return _cstmt.getObject(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The object value
   */
  @Override
  public Object getObject(String name, Map<String,Class<?>> map)
    throws SQLException
  {
    try {
      return _cstmt.getObject(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The ref value
   */
  @Override
  public Ref getRef(int i)
    throws SQLException
  {
    try {
      return _cstmt.getRef(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The ref value
   */
  @Override
  public Ref getRef(String name)
    throws SQLException
  {
    try {
      return _cstmt.getRef(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The short value
   */
  @Override
  public short getShort(int i)
    throws SQLException
  {
    try {
      return _cstmt.getShort(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The short value
   */
  @Override
  public short getShort(String name)
    throws SQLException
  {
    try {
      return _cstmt.getShort(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The string value
   */
  @Override
  public String getString(int i)
    throws SQLException
  {
    try {
      return _cstmt.getString(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The string value
   */
  @Override
  public String getString(String name)
    throws SQLException
  {
    try {
      return _cstmt.getString(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The time value
   */
  @Override
  public Time getTime(int i)
    throws SQLException
  {
    try {
      return _cstmt.getTime(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The time value
   */
  @Override
  public Time getTime(String name)
    throws SQLException
  {
    try {
      return _cstmt.getTime(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The time value
   */
  @Override
  public Time getTime(int i, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getTime(i, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The time value
   */
  @Override
  public Time getTime(String name, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getTime(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The timestamp value
   */
  @Override
  public Timestamp getTimestamp(int i)
    throws SQLException
  {
    try {
      return _cstmt.getTimestamp(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The timestamp value
   */
  @Override
  public Timestamp getTimestamp(String name)
    throws SQLException
  {
    try {
      return _cstmt.getTimestamp(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The timestamp value
   */
  @Override
  public Timestamp getTimestamp(int i, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getTimestamp(i, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The timestamp value
   */
  @Override
  public Timestamp getTimestamp(String name, Calendar cal)
    throws SQLException
  {
    try {
      return _cstmt.getTimestamp(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }
  
  /**
   * The URL value
   */
  @Override
  public URL getURL(int i)
    throws SQLException
  {
    try {
      return _cstmt.getURL(i);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * The URL value
   */
  @Override
  public URL getURL(String name)
    throws SQLException
  {
    try {
      return _cstmt.getURL(name);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(int parameterIndex, int sqlType)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterIndex, sqlType);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(int parameterIndex, int sqlType, int scale)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterIndex, sqlType, scale);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(int parameterIndex, int sqlType,
                                   String typeName)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterIndex, sqlType, typeName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(String parameterName, int sqlType)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterName, sqlType);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(String parameterName, int sqlType, int scale)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterName, sqlType, scale);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Registers the out parameter.
   */
  @Override
  public void registerOutParameter(String parameterName, int sqlType,
                                   String typeName)
    throws SQLException
  {
    try {
      _cstmt.registerOutParameter(parameterName, sqlType, typeName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the asciiStream
   */
  @Override
  public void setAsciiStream(String parameterName,
                             InputStream x,
                             int length)
    throws SQLException
  {
    try {
      _cstmt.setAsciiStream(parameterName, x, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the bigDecimal
   */
  @Override
  public void setBigDecimal(String parameterName,
                            BigDecimal x)
    throws SQLException
  {
    try {
      _cstmt.setBigDecimal(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the binaryStream
   */
  @Override
  public void setBinaryStream(String parameterName,
                              InputStream x,
                              int length)
    throws SQLException
  {
    try {
      _cstmt.setBinaryStream(parameterName, x, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the boolean
   */
  @Override
  public void setBoolean(String parameterName,
                         boolean x)
    throws SQLException
  {
    try {
      _cstmt.setBoolean(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the byte
   */
  @Override
  public void setByte(String parameterName,
                      byte x)
    throws SQLException
  {
    try {
      _cstmt.setByte(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the bytes
   */
  @Override
  public void setBytes(String parameterName,
                       byte []x)
    throws SQLException
  {
    try {
      _cstmt.setBytes(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the character stream
   */
  @Override
  public void setCharacterStream(String parameterName,
                                 Reader reader,
                                 int length)
    throws SQLException
  {
    try {
      _cstmt.setCharacterStream(parameterName, reader, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the date
   */
  @Override
  public void setDate(String parameterName,
                      Date x)
    throws SQLException
  {
    try {
      _cstmt.setDate(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the date
   */
  @Override
  public void setDate(String parameterName,
                      Date x,
                      Calendar cal)
    throws SQLException
  {
    try {
      _cstmt.setDate(parameterName, x, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the double
   */
  @Override
  public void setDouble(String parameterName,
                        double x)
    throws SQLException
  {
    try {
      _cstmt.setDouble(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the float
   */
  @Override
  public void setFloat(String parameterName,
                        float x)
    throws SQLException
  {
    try {
      _cstmt.setFloat(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the int
   */
  @Override
  public void setInt(String parameterName,
                        int x)
    throws SQLException
  {
    try {
      _cstmt.setInt(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the long
   */
  @Override
  public void setLong(String parameterName,
                        long x)
    throws SQLException
  {
    try {
      _cstmt.setLong(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the null
   */
  @Override
  public void setNull(String parameterName,
                      int sqlType)
    throws SQLException
  {
    try {
      _cstmt.setNull(parameterName, sqlType);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the null
   */
  @Override
  public void setNull(String parameterName,
                      int sqlType,
                      String typeName)
    throws SQLException
  {
    try {
      _cstmt.setNull(parameterName, sqlType, typeName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the object
   */
  @Override
  public void setObject(String parameterName,
                        Object x)
    throws SQLException
  {
    try {
      _cstmt.setObject(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the object
   */
  @Override
  public void setObject(String parameterName,
                        Object x, int type)
    throws SQLException
  {
    try {
      _cstmt.setObject(parameterName, x, type);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the object
   */
  @Override
  public void setObject(String parameterName,
                        Object x, int type, int scale)
    throws SQLException
  {
    try {
      _cstmt.setObject(parameterName, x, type, scale);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the short
   */
  @Override
  public void setShort(String parameterName,
                        short x)
    throws SQLException
  {
    try {
      _cstmt.setShort(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the string
   */
  @Override
  public void setString(String parameterName,
                        String x)
    throws SQLException
  {
    try {
      _cstmt.setString(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the time
   */
  @Override
  public void setTime(String parameterName,
                      Time x)
    throws SQLException
  {
    try {
      _cstmt.setTime(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the time
   */
  @Override
  public void setTime(String parameterName,
                      Time x,
                      Calendar cal)
    throws SQLException
  {
    try {
      _cstmt.setTime(parameterName, x, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the timestamp
   */
  @Override
  public void setTimestamp(String parameterName,
                           Timestamp x)
    throws SQLException
  {
    try {
      _cstmt.setTimestamp(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the timestamp
   */
  @Override
  public void setTimestamp(String parameterName,
                      Timestamp x,
                      Calendar cal)
    throws SQLException
  {
    try {
      _cstmt.setTimestamp(parameterName, x, cal);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Sets the URL
   */
  @Override
  public void setURL(String parameterName,
                           URL x)
    throws SQLException
  {
    try {
      _cstmt.setURL(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  /**
   * Returns true if the last out parameter was null.
   */
  @Override
  public boolean wasNull()
    throws SQLException
  {
    try {
      return _cstmt.wasNull();
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public RowId getRowId(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getRowId(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public RowId getRowId(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getRowId(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setRowId(String parameterName, RowId x)
    throws SQLException
  {
    try {
      _cstmt.setRowId(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNString(String parameterName, String value)
    throws SQLException
  {
    try {
      _cstmt.setNString(parameterName, value);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNCharacterStream(String parameterName,
                                  Reader value,
                                  long length)
    throws SQLException
  {
    try {
      _cstmt.setNCharacterStream(parameterName, value, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(String parameterName, NClob value)
    throws SQLException
  {
    try {
      _cstmt.setNClob(parameterName, value);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setClob(String parameterName, Reader reader, long length)
    throws SQLException
  {
    try {
      _cstmt.setClob(parameterName, reader, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setBlob(String parameterName,
                      InputStream inputStream,
                      long length)
    throws SQLException
  {
    try {
      _cstmt.setBlob(parameterName, inputStream, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(String parameterName, Reader reader, long length)
    throws SQLException
  {
    try {
      _cstmt.setNClob(parameterName, reader, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public NClob getNClob(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getNClob(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public NClob getNClob(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getNClob(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setSQLXML(String parameterName, SQLXML xmlObject)
    throws SQLException
  {
    try {
      _cstmt.setSQLXML(parameterName, xmlObject);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public SQLXML getSQLXML(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getSQLXML(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public SQLXML getSQLXML(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getSQLXML(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public String getNString(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getNString(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public String getNString(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getNString(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public Reader getNCharacterStream(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getNCharacterStream(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public Reader getNCharacterStream(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getNCharacterStream(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public Reader getCharacterStream(int parameterIndex)
    throws SQLException
  {
    try {
      return _cstmt.getCharacterStream(parameterIndex);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public Reader getCharacterStream(String parameterName)
    throws SQLException
  {
    try {
      return _cstmt.getCharacterStream(parameterName);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setBlob(String parameterName, Blob x)
    throws SQLException
  {
    try {
      _cstmt.setBlob(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setClob(String parameterName, Clob x)
    throws SQLException
  {
    try {
      _cstmt.setClob(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setAsciiStream(String parameterName,
                             InputStream x,
                             long length)
    throws SQLException
  {
    try {
      _cstmt.setAsciiStream(parameterName, x, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setBinaryStream(String parameterName,
                              InputStream x,
                              long length)
    throws SQLException
  {
    try {
      _cstmt.setBinaryStream(parameterName, x, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setCharacterStream(String parameterName,
                                 Reader reader,
                                 long length)
    throws SQLException
  {
    try {
      _cstmt.setCharacterStream(parameterName, reader, length);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x)
    throws SQLException
  {
    try {
      _cstmt.setAsciiStream(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x)
    throws SQLException
  {
    try {
      _cstmt.setBinaryStream(parameterName, x);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader)
    throws SQLException
  {
    try {
      _cstmt.setCharacterStream(parameterName, reader);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNCharacterStream(String parameterName, Reader value)
    throws SQLException
  {
    try {
      _cstmt.setNCharacterStream(parameterName, value);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setClob(String parameterName, Reader reader)
    throws SQLException
  {
    try {
      _cstmt.setClob(parameterName, reader);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setBlob(String parameterName, InputStream inputStream)
    throws SQLException
  {
    try {
      _cstmt.setBlob(parameterName, inputStream);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(String parameterName, Reader reader)
    throws SQLException
  {
    try {
      _cstmt.setNClob(parameterName, reader);
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    if (iface.isAssignableFrom(this.getClass()))
      return (T) this;
    else if (iface.isAssignableFrom(_cstmt.getClass()))
      return (T) _cstmt;
    else
      return _cstmt.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    if (iface.isAssignableFrom(this.getClass()))
      return true;
    else if (iface.isAssignableFrom(_cstmt.getClass()))
      return true;
    else
      return _cstmt.isWrapperFor(iface);
  }

  @Override
  public void closeOnCompletion() throws SQLException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T getObject(String parameterName, Class<T> type)
    throws SQLException
  {
    // TODO Auto-generated method stub
    return null;
  }
}
