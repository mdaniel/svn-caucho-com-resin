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

package com.caucho.sql.spy;

import com.caucho.v5.util.L10N;
import com.caucho.v5.util.SQLExceptionWrapper;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.*;

/**
 * Spying on a statement;
 */
public class SpyPreparedStatement extends SpyStatement
  implements java.sql.PreparedStatement {
  private final static Logger log
    = Logger.getLogger(SpyPreparedStatement.class.getName());
  protected static L10N L = new L10N(SpyPreparedStatement.class);

  private String _sql;
  protected PreparedStatement _pstmt;

  SpyPreparedStatement(String id, SpyConnection conn,
                       PreparedStatement stmt, String sql)
  {
    super(id, conn, stmt);

    _pstmt = stmt;
    _sql = sql;
  }

  @Override
  public java.sql.ResultSet executeQuery()
    throws SQLException
  {
    long start = start();
    
    try {
      ResultSet rs = _pstmt.executeQuery();

      if (log.isLoggable(Level.FINE))
        log(start, "executeQuery(" + _sql + ")");

      return rs;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeQuery(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int executeUpdate()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _pstmt.executeUpdate();

      if (log.isLoggable(Level.FINE))
        log(start, "executeUpdate(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeUpdate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public boolean execute()
    throws SQLException
  {
    long start = start();
    
    try {
      boolean result = _pstmt.execute();

      if (log.isLoggable(Level.FINE))
        log(start, "execute(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-execute(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void addBatch()
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.addBatch();

      if (log.isLoggable(Level.FINE))
        log(start, "addBatch()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-addBatch(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void clearParameters()
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.clearParameters();
      
      if (log.isLoggable(Level.FINE))
        log(start, "clearParameters()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-clearParameters(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    long start = start();
    
    try {
      ResultSetMetaData result = _pstmt.getMetaData();

      if (log.isLoggable(Level.FINE))
        log(start, "getMetaData() -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getMetaData(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public ParameterMetaData getParameterMetaData()
    throws SQLException
  { 
    long start = start();
    
    try {
      ParameterMetaData md = _pstmt.getParameterMetaData();
      
      if (log.isLoggable(Level.FINE))
        log(start, "getParameterMetaData()");
      
      return md;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getParameterMetaData() -> " + e);
      
      throw e;
    }
  }

  @Override
  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNull(parameterIndex, sqlType);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setNull(" + parameterIndex + ",type=" + sqlType + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNull(parameterIndex, sqlType, typeName);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setNull(" + parameterIndex + ",type=" + sqlType +
            ",typeName=" + typeName + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBoolean(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setBoolean(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBoolean(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setByte(int index, byte value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setByte(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setByte(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setByte(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setShort(int index, short value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setShort(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setShort(" + index + "," + value + ")");

    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setShort(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setInt(int index, int value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setInt(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setInt(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setInt(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setLong(int index, long value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setLong(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setLong(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setLong(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setFloat(int index, float value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setFloat(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setFloat(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setFloat(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setDouble(int index, double value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setDouble(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setDouble(" + index + "," + value + ")");

    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setDouble(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBigDecimal(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setBigDecimal(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBigDecimal(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setString(int index, String value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setString(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setString(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setString(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBytes(int index, byte []value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBytes(index, value);
      
      if (log.isLoggable(Level.FINE)) {
        if (value != null)
          log(start, "setBytes(" + index + ",len=" + value.length + ")");
        else
          log(start, ":setBytes(" + index + ",null");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBytes(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setDate(int index, Date value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setDate(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setDate(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setDate(index, value, cal);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setDate(" + index + "," + value + ",cal=" + cal + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setTime(int index, Time value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setTime(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setTime(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setTime(index, value, cal);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setTime(" + index + "," + value + ",cal=" + cal + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setTimestamp(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setTimestamp(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setTimestamp(index, value, cal);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setTimestamp(" + index + "," + value + ",cal=" + cal + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setAsciiStream(index, value, length);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setAsciiStream(" + index + "," + value + ",len=" + length + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setAsciiStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setUnicodeStream(index, value, length);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setUnicodeStream(" + index + "," + value + ",len=" + length + ")");

    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setUnicodeStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBinaryStream(index, value, length);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setBinaryStream(" + index + "," + value + ",len=" + length + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBinaryStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setCharacterStream(index, value, length);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setCharacterStream(" + index + "," + value + ",len=" + length + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setObject(index, value, type, scale);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setObject(" + index + "," + value +
            ",type=" + type + ",scale=" + scale + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setObject(index, value, type);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setObject(" + index + "," + value +
            ",type=" + type +  ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setObject(int index, Object value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setObject(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setObject(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }
  
  @Override
  public void setRef(int index, Ref value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setRef(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setRef(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setRef(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBlob(int index, Blob value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBlob(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setBlob(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBlob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setClob(int index, Clob value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setClob(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setClob(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setArray(int index, Array value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setArray(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setArray(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setArray(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setURL(int index, URL value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setURL(index, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setURL(" + index + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setURL(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setRowId(int parameterIndex, RowId x)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setRowId(parameterIndex, x);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setRowId(" + parameterIndex + "," + x + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setRowId(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNString(int parameterIndex, String value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNString(parameterIndex, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setNString(" + parameterIndex + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNString(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNCharacterStream(parameterIndex, value, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setNCharacterStream(" + parameterIndex
            + "," + value
            + ","
            + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNClob(int parameterIndex, NClob value)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNClob(parameterIndex, value);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setNClob(" + parameterIndex + "," + value + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setClob(parameterIndex, reader, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setClob(" + parameterIndex
            + "," + reader
            + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBlob(parameterIndex, inputStream, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setBlob(" + parameterIndex
            + "," + inputStream
            + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBlob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNClob(parameterIndex, reader, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setNClob(" + parameterIndex
            + "," + reader + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setSQLXML(parameterIndex, xmlObject);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setSQLXML(" + parameterIndex
            + "," + xmlObject + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setSQLXML(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setAsciiStream(parameterIndex, x, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setAsciiStream(" + parameterIndex
            + "," + x + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setAsciiStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBinaryStream(parameterIndex, x, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setBinaryStream(" + parameterIndex
            + "," + x + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBinaryStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setCharacterStream(parameterIndex, reader, length);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setCharacterStream(" + parameterIndex
            + "," + reader + "," + length + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setAsciiStream(parameterIndex, x);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setAsciiStream(" + parameterIndex + "," + x + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setAsciiStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBinaryStream(parameterIndex, x);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setBinaryStream(" + parameterIndex + "," + x + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBinaryStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setCharacterStream(parameterIndex, reader);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setCharacterStream(" + parameterIndex
            + "," + reader + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader reader)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNCharacterStream(parameterIndex, reader);
      
      if (log.isLoggable(Level.FINE)) {
        log(start, "setNCharacterStream(" + parameterIndex
            + "," + reader + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setClob(parameterIndex, reader);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setClob(" + parameterIndex + "," + reader + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setBlob(parameterIndex, inputStream);
      
      if (log.isLoggable(Level.FINE)) {
          log(start, "setBlob(" + parameterIndex
              + "," + inputStream + ")");
      }
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setBlob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    long start = start();
    
    try {
      _pstmt.setNClob(parameterIndex, reader);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setNClob(" + parameterIndex + "," + reader + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setNClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }
}
