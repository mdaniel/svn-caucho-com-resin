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

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

/**
 * Spying on a statement;
 */
public class SpyStatement implements java.sql.Statement {
  protected final static Logger log = Log.open(SpyStatement.class);
  protected final static L10N L = new L10N(SpyConnection.class);

  protected String _id;
  
  // The spy connection
  protected Connection _conn;
  
  // The underlying connection
  protected Statement _stmt;

  SpyStatement(String id, Connection conn, Statement stmt)
  {
    _id = id;

    _conn = conn;
    _stmt = stmt;
  }

  public Statement getStatement()
  {
    return _stmt;
  }

  public void addBatch(String sql)
    throws SQLException
  {
    try {
      log.info(_id + ":addBatch(" + sql + ")");
      
      _stmt.addBatch(sql);
    } catch (Throwable e) {
      log.info(_id + ":exn-addBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void cancel()
    throws SQLException
  {
    try {
      log.info(_id + ":cancel()");
      
      _stmt.cancel();
    } catch (Throwable e) {
      log.info(_id + ":exn-cancel(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearBatch()
    throws SQLException
  {
    try {
      log.info(_id + ":clearBatch()");
      
      _stmt.clearBatch();
    } catch (Throwable e) {
      log.info(_id + ":exn-clearBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    try {
      log.info(_id + ":clearWarnings()");
      
      _stmt.clearWarnings();
    } catch (Throwable e) {
      log.info(_id + ":exn-clearWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void close()
    throws SQLException
  {
    try {
      log.info(_id + ":close()");
      
      _stmt.close();
    } catch (Throwable e) {
      log.info(_id + ":exn-close(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.ResultSet executeQuery(String sql)
    throws SQLException
  {
    try {
      log.info(_id + ":executeQuery(" + sql + ")");
      
      ResultSet rs = _stmt.executeQuery(sql);

      return rs;
    } catch (Throwable e) {
      log.info(_id + ":exn-executeQuery(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int executeUpdate(String sql)
    throws SQLException
  {
    try {
      int count = _stmt.executeUpdate(sql);

      log.info(_id + ":executeUpdate(" + sql + ") -> " + count);
      
      return count;
    } catch (Throwable e) {
      log.info(_id + ":exn-executeUpdate(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean execute(String sql)
    throws SQLException
  {
    try {
      boolean hasResult = _stmt.execute(sql);

      log.info(_id + ":execute(" + sql + ") -> " + hasResult);
      
      return hasResult;
    } catch (Throwable e) {
      log.info(_id + ":exn-execute(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int[]executeBatch()
    throws SQLException
  {
    try {
      int []result = _stmt.executeBatch();

      log.info(_id + ":executeBatch()");
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-executeBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    try {
      ResultSet result = _stmt.getResultSet();

      log.info(_id + ":getResultSet() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getResultSet(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getUpdateCount()
    throws SQLException
  {
    try {
      int updateCount = _stmt.getUpdateCount();

      log.info(_id + ":getUpdateCount() -> " + updateCount);
      
      return updateCount;
    } catch (Throwable e) {
      log.info(_id + ":exn-getUpdateCount(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.Connection getConnection()
    throws SQLException
  {
    int updateCount = _stmt.getUpdateCount();

    log.info(_id + ":getConnection()");
      
    return _conn;
  }

  public int getFetchDirection()
    throws SQLException
  {
    try {
      int result = _stmt.getFetchDirection();

      log.info(_id + ":getFetchDirection() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getFetchSize()
    throws SQLException
  {
    try {
      int result = _stmt.getFetchSize();

      log.info(_id + ":getFetchSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getMaxFieldSize()
    throws SQLException
  {
    try {
      int result = _stmt.getMaxFieldSize();

      log.info(_id + ":getMaxFieldSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getMaxRows()
    throws SQLException
  {
    try {
      int result = _stmt.getMaxRows();

      log.info(_id + ":getMaxRows() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  public void setMaxRows(int max)
    throws SQLException
  {
    try {
      log.info(_id + ":setMaxRows(" + max + ")");

      _stmt.setMaxRows(max);
    } catch (Throwable e) {
      log.info(_id + ":exn-setMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean getMoreResults()
    throws SQLException
  {
    try {
      boolean result = _stmt.getMoreResults();

      log.info(_id + ":getMoreResults() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getMoreResults(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getQueryTimeout()
    throws SQLException
  {
    try {
      int result = _stmt.getQueryTimeout();

      log.info(_id + ":getQueryTimeout() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getResultSetConcurrency()
    throws SQLException
  {
    try {
      int result = _stmt.getResultSetConcurrency();

      log.info(_id + ":getResultSetConcurrency() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getResultSetConcurrency(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getResultSetType()
    throws SQLException
  {
    try {
      int result = _stmt.getResultSetType();

      log.info(_id + ":getResultSetType() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getResultSetType(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    try {
      SQLWarning result = _stmt.getWarnings();

      log.info(_id + ":getWarnings() -> " + result);
      
      return result;
    } catch (Throwable e) {
      log.info(_id + ":exn-getWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setCursorName(String name)
    throws SQLException
  {
    try {
      log.info(_id + ":setCursorName(" + name + ")");

      _stmt.setCursorName(name);
    } catch (Throwable e) {
      log.info(_id + ":exn-setCursorName(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    try {
      log.info(_id + ":setEscapeProcessing(" + enable + ")");

      _stmt.setEscapeProcessing(enable);
    } catch (Throwable e) {
      log.info(_id + ":exn-setEscapeProcessing(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFetchDirection(int direction)
    throws SQLException
  {
    try {
      log.info(_id + ":setFetchDirection(" + direction + ")");

      _stmt.setFetchDirection(direction);
    } catch (Throwable e) {
      log.info(_id + ":exn-setFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFetchSize(int rows)
    throws SQLException
  {
    try {
      log.info(_id + ":setFetchSize(" + rows + ")");

      _stmt.setFetchSize(rows);
    } catch (Throwable e) {
      log.info(_id + ":exn-setFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setMaxFieldSize(int max)
    throws SQLException
  {
    try {
      log.info(_id + ":setMaxFieldSize(" + max + ")");

      _stmt.setMaxFieldSize(max);
    } catch (Throwable e) {
      log.info(_id + ":exn-setMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    try {
      log.info(_id + ":setQueryTimeout(" + seconds + ")");

      _stmt.setQueryTimeout(seconds);
    } catch (Throwable e) {
      log.info(_id + ":exn-setQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  // jdk 1.4
  public boolean getMoreResults(int count)
    throws SQLException
  {
    return _stmt.getMoreResults(count);
  }
  
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    return _stmt.getGeneratedKeys();
  }
  
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    return _stmt.executeUpdate(query, resultType);
  }
  
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }
  
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }
  
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    return _stmt.execute(query, resultType);
  }
  
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }
  
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }

  public int getResultSetHoldability()
    throws SQLException
  {
    return _stmt.getResultSetHoldability();
  }
}
