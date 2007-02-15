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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql.spy;

import com.caucho.log.Log;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

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
  protected final static Logger log = Log.open(SpyPreparedStatement.class);
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

  public java.sql.ResultSet executeQuery()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":executeQuery(" + _sql + ")");

      ResultSet rs = _pstmt.executeQuery();

      return rs;
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-executeQuery(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public int executeUpdate()
    throws SQLException
  {
    try {
      int result = _pstmt.executeUpdate();

      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":executeUpdate(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-executeUpdate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean execute()
    throws SQLException
  {
    try {
      boolean result = _pstmt.execute();

      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":execute(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-execute(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void addBatch()
    throws SQLException
  {
    try {
      _pstmt.addBatch();

      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":addBatch()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-addBatch(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearParameters()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":clearParameters()");

      _pstmt.clearParameters();
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-clearParameters(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try {
      ResultSetMetaData result = _pstmt.getMetaData();

      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":getMetaData() -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-getMetaData(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    return _pstmt.getParameterMetaData();
  }

  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setNull(" + parameterIndex + ",type=" + sqlType + ")");

      _pstmt.setNull(parameterIndex, sqlType);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setNull(" + parameterIndex + ",type=" + sqlType +
              ",typeName=" + typeName + ")");

      _pstmt.setNull(parameterIndex, sqlType, typeName);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setBoolean(" + index + "," + value + ")");

      _pstmt.setBoolean(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setBoolean(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setByte(int index, byte value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setByte(" + index + "," + value + ")");

      _pstmt.setByte(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setByte(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setShort(int index, short value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setShort(" + index + "," + value + ")");

      _pstmt.setShort(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setShort(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setInt(int index, int value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setInt(" + index + "," + value + ")");

      _pstmt.setInt(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setInt(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setLong(int index, long value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setLong(" + index + "," + value + ")");

      _pstmt.setLong(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setLong(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFloat(int index, float value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setFloat(" + index + "," + value + ")");

      _pstmt.setFloat(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setFloat(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDouble(int index, double value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setDouble(" + index + "," + value + ")");

      _pstmt.setDouble(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setDouble(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setBigDecimal(" + index + "," + value + ")");

      _pstmt.setBigDecimal(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setBigDecimal(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setString(int index, String value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setString(" + index + "," + value + ")");

      _pstmt.setString(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setString(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBytes(int index, byte []value)
    throws SQLException
  {
    try {
      if (value != null)
        if (log.isLoggable(Level.INFO))
	  log.info(getId() + ":setBytes(" + index + ",len=" + value.length + ")");
      else
        if (log.isLoggable(Level.INFO))
	  log.info(getId() + ":setBytes(" + index + ",null");

      _pstmt.setBytes(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setBytes(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDate(int index, Date value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setDate(" + index + "," + value + ")");

      _pstmt.setDate(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setDate(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setDate(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTime(int index, Time value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setTime(" + index + "," + value + ")");

      _pstmt.setTime(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setTime(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setTime(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setTimestamp(" + index + "," + value + ")");

      _pstmt.setTimestamp(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setTimestamp(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setTimestamp(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setAsciiStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setAsciiStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setAsciiStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setUnicodeStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setUnicodeStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setUnicodeStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setBinaryStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setBinaryStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setBinaryStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setCharacterStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setCharacterStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setObject(" + index + "," + value +
              ",type=" + type + ",scale=" + scale + ")");

      _pstmt.setObject(index, value, type, scale);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setObject(" + index + "," + value +
              ",type=" + type +  ")");

      _pstmt.setObject(index, value, type);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setObject(" + index + "," + value + ")");

      _pstmt.setObject(index, value);
    } catch (Throwable e) {
      e.printStackTrace();

      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setRef(int index, Ref value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setRef(" + index + "," + value + ")");

      _pstmt.setRef(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setRef(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBlob(int index, Blob value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setBlob(" + index + "," + value + ")");

      _pstmt.setBlob(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setBlob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setClob(int index, Clob value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setClob(" + index + "," + value + ")");

      _pstmt.setClob(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setArray(int index, Array value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":setArray(" + index + "," + value + ")");

      _pstmt.setArray(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.INFO))
	log.info(getId() + ":exn-setArray(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setURL(int index, URL value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }
}
