/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.sql;

import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * User-view of prepared statements
 */
public class UserPreparedStatement extends UserStatement
  implements PreparedStatement {
  protected final static Logger log = Log.open(UserPreparedStatement.class);
  protected static L10N L = new L10N(UserPreparedStatement.class);

  protected PreparedStatement _pstmt;
  protected PreparedStatementCacheItem _cacheItem;

  private boolean _isClosed;
  
  UserPreparedStatement(UserConnection conn,
			PreparedStatement pStmt,
			PreparedStatementCacheItem cacheItem)
  {
    super(conn, pStmt);
    
    _pstmt = pStmt;
    _cacheItem = cacheItem;

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
  public ResultSet executeQuery()
    throws SQLException
  {
    return _pstmt.executeQuery();
  }

  /**
   * Executes the prepared statement's sql as an update
   */
  public int executeUpdate()
    throws SQLException
  {
    return _pstmt.executeUpdate();
  }

  /**
   * Executes the prepared statement's sql as an update or query
   */
  public boolean execute()
    throws SQLException
  {
    return _pstmt.execute();
  }

  /**
   * Adds the statement as a batch.
   */
  public void addBatch()
    throws SQLException
  {
    _pstmt.addBatch();
  }

  /**
   * Clears the statement's parameters.
   */
  public void clearParameters()
    throws SQLException
  {
    _pstmt.clearParameters();
  }

  /**
   * Returns the result metadata.
   */
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    return _pstmt.getMetaData();
  }

  /**
   * Returns the prepared statement's meta data.
   */
  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    return _pstmt.getParameterMetaData();
  }

  /**
   * Sets the parameter as a null.
   */
  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    _pstmt.setNull(parameterIndex, sqlType);
  }

  /**
   * Sets the parameter as a null.
   */
  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    _pstmt.setNull(parameterIndex, sqlType, typeName);
  }

  /**
   * Sets the parameter as a boolean.
   */
  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    _pstmt.setBoolean(index, value);
  }

  /**
   * Sets the parameter as a byte.
   */
  public void setByte(int index, byte value)
    throws SQLException
  {
    _pstmt.setByte(index, value);
  }

  /**
   * Sets the parameter as a short.
   */
  public void setShort(int index, short value)
    throws SQLException
  {
    _pstmt.setShort(index, value);
  }

  /**
   * Sets the parameter as an int
   */
  public void setInt(int index, int value)
    throws SQLException
  {
    _pstmt.setInt(index, value);
  }

  /**
   * Sets the parameter as a long
   */
  public void setLong(int index, long value)
    throws SQLException
  {
    _pstmt.setLong(index, value);
  }

  /**
   * Sets the parameter as a float
   */
  public void setFloat(int index, float value)
    throws SQLException
  {
    _pstmt.setFloat(index, value);
  }

  /**
   * Sets the parameter as a double
   */
  public void setDouble(int index, double value)
    throws SQLException
  {
    _pstmt.setDouble(index, value);
  }

  /**
   * Sets the parameter as a BigDecimal
   */
  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    _pstmt.setBigDecimal(index, value);
  }

  /**
   * Sets the parameter as a string
   */
  public void setString(int index, String value)
    throws SQLException
  {
    _pstmt.setString(index, value);
  }

  /**
   * Sets the parameter as a byte array.
   */
  public void setBytes(int index, byte []value)
    throws SQLException
  {
    _pstmt.setBytes(index, value);
  }

  /**
   * Sets the parameter as a date
   */
  public void setDate(int index, Date value)
    throws SQLException
  {
    _pstmt.setDate(index, value);
  }

  /**
   * Sets the parameter as a time
   */
  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    _pstmt.setDate(index, value, cal);
  }

  /**
   * Sets the parameter as a time
   */
  public void setTime(int index, Time value)
    throws SQLException
  {
    _pstmt.setTime(index, value);
  }

  /**
   * Sets the parameter as a time
   */
  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    _pstmt.setTime(index, value, cal);
  }

  /**
   * Sets the parameter as a timestamp
   */
  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    _pstmt.setTimestamp(index, value);
  }

  /**
   * Sets the parameter as a timestamp
   */
  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    _pstmt.setTimestamp(index, value, cal);
  }

  /**
   * Sets the parameter as an ascii stream.
   */
  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    _pstmt.setAsciiStream(index, value, length);
  }

  /**
   * Sets the parameter as a unicode stream.
   */
  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    _pstmt.setUnicodeStream(index, value, length);
  }

  /**
   * Sets the parameter as a binary stream.
   */
  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    _pstmt.setBinaryStream(index, value, length);
  }

  /**
   * Sets the parameter as an character stream.
   */
  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    _pstmt.setCharacterStream(index, value, length);
  }

  /**
   * Sets the parameter as an object with the given type and scale.
   */
  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    _pstmt.setObject(index, value, type, scale);
  }

  /**
   * Sets the parameter as an object with the given type.
   */
  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    _pstmt.setObject(index, value, type);
  }

  /**
   * Sets the parameter as a object.
   */
  public void setObject(int index, Object value)
    throws SQLException
  {
    _pstmt.setObject(index, value);
  }

  /**
   * Sets teh parameter as a ref.
   */
  public void setRef(int index, Ref value)
    throws SQLException
  {
    _pstmt.setRef(index, value);
  }

  /**
   * Sets the parameter as a blob.
   */
  public void setBlob(int index, Blob value)
    throws SQLException
  {
    _pstmt.setBlob(index, value);
  }

  /**
   * Sets the parameter as a clob.
   */
  public void setClob(int index, Clob value)
    throws SQLException
  {
    _pstmt.setClob(index, value);
  }

  /**
   * Sets the parameter as an array
   */
  public void setArray(int index, Array value)
    throws SQLException
  {
    _pstmt.setArray(index, value);
  }

  /**
   * Sets the parameter as a URL.
   */
  public void setURL(int index, URL value)
    throws SQLException
  {
    _pstmt.setURL(index, value);
  }

  /**
   * Closes the prepared statement.
   */
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
    else if (_isChanged)
      _cacheItem.destroy();
    else
      _cacheItem.toIdle();
  }

  public String toString()
  {
    return "UserPreparedStatement[" + _pstmt + "]";
  }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
