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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import com.caucho.v5.health.meter.ActiveTimeSensor;
import com.caucho.v5.util.L10N;

/**
 * User-view of prepared statements
 */
public class UserPreparedStatement extends UserStatement
  implements PreparedStatement {
  protected static L10N L = new L10N(UserPreparedStatement.class);

  protected PreparedStatement _pstmt;
  protected PreparedStatementCacheItem _cacheItem;

  private boolean _isClosed;

  private ActiveTimeSensor _timeProbe;
  
  UserPreparedStatement(UserConnection conn,
                        PreparedStatement pStmt,
                        PreparedStatementCacheItem cacheItem)
  {
    super(conn, pStmt);
    
    _pstmt = pStmt;
    _cacheItem = cacheItem;
    _timeProbe = conn.getTimeProbe();

    if (pStmt == null)
      throw new NullPointerException();
  }
  
  UserPreparedStatement(UserConnection conn,
                        PreparedStatement pStmt)
  {
    this(conn, pStmt, null);
  }

  /**
   * Returns the underlying statement.
   */
  public PreparedStatement getPreparedStatement()
  {
    return _pstmt;
  }

  /**
   * Executes the prepared statement's query.
   */
  @Override
  public ResultSet executeQuery()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.executeQuery();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Executes the prepared statement's sql as an update
   */
  @Override
  public int executeUpdate()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.executeUpdate();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Executes the prepared statement's sql as an update or query
   */
  @Override
  public boolean execute()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.execute();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Adds the statement as a batch.
   */
  @Override
  public void addBatch()
    throws SQLException
  {
    try {
      _pstmt.addBatch();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Clears the statement's parameters.
   */
  @Override
  public void clearParameters()
    throws SQLException
  {
    try {
      _pstmt.clearParameters();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Returns the result metadata.
   */
  @Override
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try {
      return _pstmt.getMetaData();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Returns the prepared statement's meta data.
   */
  @Override
  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    try {
      return _pstmt.getParameterMetaData();
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
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
    else if (iface.isAssignableFrom(_pstmt.getClass()))
      return (T) _pstmt;
    else
      return _pstmt.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    if (iface.isAssignableFrom(this.getClass()))
      return true;
    else if (iface.isAssignableFrom(_pstmt.getClass()))
      return true;
    else
      return _pstmt.isWrapperFor(iface);
  }

  /**
   * Sets the parameter as a null.
   */
  @Override
  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    try {
      _pstmt.setNull(parameterIndex, sqlType);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a null.
   */
  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    try {
      _pstmt.setNull(parameterIndex, sqlType, typeName);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a boolean.
   */
  @Override
  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    try {
      _pstmt.setBoolean(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a byte.
   */
  @Override
  public void setByte(int index, byte value)
    throws SQLException
  {
    try {
      _pstmt.setByte(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);

      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a short.
   */
  @Override
  public void setShort(int index, short value)
    throws SQLException
  {
    try {
      _pstmt.setShort(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an int
   */
  @Override
  public void setInt(int index, int value)
    throws SQLException
  {
    try {
      _pstmt.setInt(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a long
   */
  @Override
  public void setLong(int index, long value)
    throws SQLException
  {
    try {
      _pstmt.setLong(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a float
   */
  @Override
  public void setFloat(int index, float value)
    throws SQLException
  {
    try {
      _pstmt.setFloat(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a double
   */
  @Override
  public void setDouble(int index, double value)
    throws SQLException
  {
    try {
      _pstmt.setDouble(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a BigDecimal
   */
  @Override
  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    try {
      _pstmt.setBigDecimal(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a string
   */
  @Override
  public void setString(int index, String value)
    throws SQLException
  {
    try {
      _pstmt.setString(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a byte array.
   */
  @Override
  public void setBytes(int index, byte []value)
    throws SQLException
  {
    try {
      _pstmt.setBytes(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a date
   */
  @Override
  public void setDate(int index, Date value)
    throws SQLException
  {
    try {
      _pstmt.setDate(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  @Override
  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setDate(index, value, cal);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  @Override
  public void setTime(int index, Time value)
    throws SQLException
  {
    try {
      _pstmt.setTime(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  @Override
  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setTime(index, value, cal);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a timestamp
   */
  @Override
  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    try {
      _pstmt.setTimestamp(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a timestamp
   */
  @Override
  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setTimestamp(index, value, cal);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an ascii stream.
   */
  @Override
  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setAsciiStream(index, value, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a unicode stream.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setUnicodeStream(index, value, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a binary stream.
   */
  @Override
  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setBinaryStream(index, value, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an character stream.
   */
  @Override
  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    try {
      _pstmt.setCharacterStream(index, value, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an object with the given type and scale.
   */
  @Override
  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value, type, scale);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setAsciiStream(int parameterIndex,
                             InputStream x,
                             long length)
    throws SQLException
  {
    try {
      _pstmt.setAsciiStream(parameterIndex, x, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setBinaryStream(int parameterIndex,
                              InputStream x,
                              long length)
    throws SQLException
  {
    try {
      _pstmt.setBinaryStream(parameterIndex, x, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setCharacterStream(int parameterIndex,
                                 Reader reader,
                                 long length)
    throws SQLException
  {
    try {
      _pstmt.setCharacterStream(parameterIndex, reader, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    try {
      _pstmt.setAsciiStream(parameterIndex, x);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    try {
      _pstmt.setBinaryStream(parameterIndex, x);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader)
    throws SQLException
  {
    try {
      _pstmt.setCharacterStream(parameterIndex, reader);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value)
    throws SQLException
  {
    try {
      _pstmt.setNCharacterStream(parameterIndex, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    try {
      _pstmt.setClob(parameterIndex, reader);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream)
    throws SQLException
  {
    try {
      _pstmt.setBlob(parameterIndex, inputStream);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    try {
      _pstmt.setNClob(parameterIndex, reader);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an object with the given type.
   */
  @Override
  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value, type);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a object.
   */
  @Override
  public void setObject(int index, Object value)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets teh parameter as a ref.
   */
  @Override
  public void setRef(int index, Ref value)
    throws SQLException
  {
    try {
      _pstmt.setRef(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a blob.
   */
  @Override
  public void setBlob(int index, Blob value)
    throws SQLException
  {
    try {
      _pstmt.setBlob(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a clob.
   */
  @Override
  public void setClob(int index, Clob value)
    throws SQLException
  {
    try {
      _pstmt.setClob(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as an array
   */
  @Override
  public void setArray(int index, Array value)
    throws SQLException
  {
    try {
      _pstmt.setArray(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Sets the parameter as a URL.
   */
  @Override
  public void setURL(int index, URL value)
    throws SQLException
  {
    try {
      _pstmt.setURL(index, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setRowId(int parameterIndex, RowId x)
    throws SQLException
  {
    try {
      _pstmt.setRowId(parameterIndex, x);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNString(int parameterIndex, String value)
    throws SQLException
  {
    try {
      _pstmt.setNString(parameterIndex, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNCharacterStream(int parameterIndex,
                                  Reader value,
                                  long length)
    throws SQLException
  {
    try {
      _pstmt.setNCharacterStream(parameterIndex, value, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(int parameterIndex, NClob value)
    throws SQLException
  {
    try {
      _pstmt.setNClob(parameterIndex, value);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    try {
      _pstmt.setClob(parameterIndex, reader, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setBlob(int parameterIndex,
                      InputStream inputStream,
                      long length)
    throws SQLException
  {
    try {
      _pstmt.setBlob(parameterIndex, inputStream, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    try {
      _pstmt.setNClob(parameterIndex, reader, length);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject)
    throws SQLException
  {
    try {
      _pstmt.setSQLXML(parameterIndex, xmlObject);
    } catch (RuntimeException e) {
      onRuntimeException(e);
      
      throw e;
    } catch (SQLException e) {
      onSqlException(e);
      
      throw e;
    }
  }

  /**
   * Closes the prepared statement.
   */
  @Override
  public void close()
    throws SQLException
  {
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
    }
    
    clearParameters();

    if (_cacheItem == null)
      super.close();
    else if (! isPoolable())
      _cacheItem.destroy();
    else
      _cacheItem.toIdle();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + '[' + _pstmt + ']';
  }
}
