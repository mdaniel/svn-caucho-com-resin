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

package com.caucho.v5.sql.spy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.SQLExceptionWrapper;

/**
 * Spying on a statement;
 */
public class SpyStatement implements java.sql.Statement {
  protected final static Logger log
    = Logger.getLogger(SpyStatement.class.getName());
  protected final static L10N L = new L10N(SpyConnection.class);

  protected String _id;
  
  // The spy connection
  protected SpyConnection _conn;
  
  // The underlying connection
  protected Statement _stmt;

  SpyStatement(String id, SpyConnection conn, Statement stmt)
  {
    _id = id;

    _conn = conn;
    _stmt = stmt;
  }
  
  protected long start()
  {
    return CurrentTime.getExactTime();
  }
  
  protected void log(long start, String msg)
  {
    long delta = CurrentTime.getExactTime() - start;
    
    log.fine("[" + delta + "ms] " + getId() + ":" + msg);
  }

  public String getId()
  {
    if (_id == null)
      _id = _conn.createStatementId();

    return _id;
  }

  public Statement getStatement()
  {
    return _stmt;
  }

  @Override
  public void addBatch(String sql)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.addBatch(sql);
      
      if (log.isLoggable(Level.FINE))
        log(start, "addBatch(" + sql + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-addBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void cancel()
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.cancel();
      
      if (log.isLoggable(Level.FINE))
        log(start, "cancel()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-cancel(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void clearBatch()
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.clearBatch();
      
      if (log.isLoggable(Level.FINE))
        log(start, "clearBatch()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-clearBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void clearWarnings()
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.clearWarnings();
      
      if (log.isLoggable(Level.FINE))
        log(start, "clearWarnings()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-clearWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void close()
    throws SQLException
  {
    long start = start();
    
    try {
      Statement stmt = _stmt;
      _stmt = null;
      
      if (stmt != null)
        stmt.close();
      
      if (log.isLoggable(Level.FINE))
        log(start, "stmt-close()");
      
    } catch (RuntimeException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-close(" + e + ")");
    } catch (Exception e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-close(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public java.sql.ResultSet executeQuery(String sql)
    throws SQLException
  {
    long start = start();
    
    try {
      ResultSet rs = _stmt.executeQuery(sql);
      
      if (log.isLoggable(Level.FINE))
        log(start, "executeQuery(" + sql + ")");

      return rs;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeQuery(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int executeUpdate(String sql)
    throws SQLException
  {
    long start = start();
    
    try {
      int count = _stmt.executeUpdate(sql);

      if (log.isLoggable(Level.FINE))
        log(start, "executeUpdate(" + sql + ") -> " + count);
      
      return count;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeUpdate(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public boolean execute(String sql)
    throws SQLException
  {
    long start = start();
    
    try {
      boolean hasResult = _stmt.execute(sql);

      if (log.isLoggable(Level.FINE))
        log(start, "execute(" + sql + ") -> " + hasResult);
      
      return hasResult;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-execute(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int[]executeBatch()
    throws SQLException
  {
    long start = start();
    
    try {
      int []result = _stmt.executeBatch();

      if (log.isLoggable(Level.FINE))
        log(start, "executeBatch()");
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    long start = start();
    
    try {
      ResultSet result = _stmt.getResultSet();

      if (log.isLoggable(Level.FINE))
        log(start, "getResultSet() -> " + (result != null ? result.getClass().getName() : ""));
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getResultSet(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getUpdateCount()
    throws SQLException
  {
    long start = start();
    
    try {
      int updateCount = _stmt.getUpdateCount();

      if (log.isLoggable(Level.FINE))
        log(start, "getUpdateCount() -> " + updateCount);
      
      return updateCount;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getUpdateCount(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public java.sql.Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  @Override
  public int getFetchDirection()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getFetchDirection();

      if (log.isLoggable(Level.FINE))
        log(start, "getFetchDirection() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getFetchSize()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getFetchSize();

      if (log.isLoggable(Level.FINE))
        log(start, "getFetchSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getMaxFieldSize()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getMaxFieldSize();

      if (log.isLoggable(Level.FINE))
        log(start, "getMaxFieldSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getMaxRows()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getMaxRows();

      if (log.isLoggable(Level.FINE))
        log(start, "getMaxRows() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  @Override
  public void setMaxRows(int max)
    throws SQLException
  {
    long start = start();
    
    try {
      if (log.isLoggable(Level.FINE))
        log(start, "setMaxRows(" + max + ")");

      _stmt.setMaxRows(max);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public boolean getMoreResults()
    throws SQLException
  {
    long start = start();
    
    try {
      boolean result = _stmt.getMoreResults();

      if (log.isLoggable(Level.FINE))
        log(start, "getMoreResults() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getMoreResults(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getQueryTimeout()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getQueryTimeout();

      if (log.isLoggable(Level.FINE))
        log(start, "getQueryTimeout() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getResultSetConcurrency()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getResultSetConcurrency();

      if (log.isLoggable(Level.FINE))
        log(start, "getResultSetConcurrency() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, ":exn-getResultSetConcurrency(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public int getResultSetType()
    throws SQLException
  {
    long start = start();
    
    try {
      int result = _stmt.getResultSetType();

      if (log.isLoggable(Level.FINE))
        log(start, "getResultSetType() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getResultSetType(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public SQLWarning getWarnings()
    throws SQLException
  {
    long start = start();
    
    try {
      SQLWarning result = _stmt.getWarnings();

      if (log.isLoggable(Level.FINE))
        log(start, "getWarnings() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setCursorName(String name)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setCursorName(name);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setCursorName(" + name + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setCursorName(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setEscapeProcessing(enable);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setEscapeProcessing(" + enable + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setEscapeProcessing(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setFetchDirection(int direction)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setFetchDirection(direction);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setFetchDirection(" + direction + ")");

    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setFetchSize(int rows)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setFetchSize(rows);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setFetchSize(" + rows + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void setMaxFieldSize(int max)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setMaxFieldSize(max);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setMaxFieldSize(" + max + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  @Override
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setQueryTimeout(seconds);
      
      if (log.isLoggable(Level.FINE))
        log(start, "setQueryTimeout(" + seconds + ")");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public boolean getMoreResults(int count)
    throws SQLException
  {
    long start = start();
    
    try {
      boolean moreResults = _stmt.getMoreResults(count);

      if (log.isLoggable(Level.FINE))
        log(start, "getMoreResults(" + count + ") -> " + moreResults);

      return moreResults;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "getMoreResults(" + e + ")");

      throw e;
    }
  }

  @Override
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    long start = start();
    
    try {
      ResultSet generatedKeys = _stmt.getGeneratedKeys();

      if (log.isLoggable(Level.FINE))
        log(start, "getGeneratedKeys() -> " + generatedKeys);

      return generatedKeys;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "getGeneratedKeys(" + e + ")");

      throw e;
    }
  }

  @Override
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    long start = start();
    
    try {
      int rowsUpdated = _stmt.executeUpdate(query, resultType);

      if (log.isLoggable(Level.FINE)) {
        log(start, "executeUpdate(" + query 
            + ", resultType="      + resultType
            + ") -> " + rowsUpdated);
      }

      return rowsUpdated;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeUpdate(" + e + ")");

      throw e;
    }
  }
  
  @Override
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    long start = start();
    
    try {
      int rowsUpdated = _stmt.executeUpdate(query, columns);

      if (log.isLoggable(Level.FINE)) { 
        List<Integer> list = new ArrayList<Integer>();
        for (int column : columns)
          list.add(column);

        log(start, "executeUpdate(" + query
            + ", columns=" + list
            + ") -> " + rowsUpdated);
      }

      return rowsUpdated;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeUpdate(" + e + ")");

      throw e;
    }
  }
  
  @Override
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    long start = start();
    
    try {
      int rowsUpdated = _stmt.executeUpdate(query, columns);

      if (log.isLoggable(Level.FINE)){ 
        log(start, "executeUpdate(" + query
            + ", columns=" + Arrays.asList(columns)
            + ") -> " + rowsUpdated);
      }

      return rowsUpdated;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-executeUpdate(" + e + ")");

      throw e;
    }
  }
  
  @Override
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    long start = start();
    
    try {
      boolean isResultSet = _stmt.execute(query, resultType);

      if (log.isLoggable(Level.FINE)) {
        log(start, "execute(" + query
            + ", resultType=" + resultType
            + ") -> " + isResultSet);
      }

      return isResultSet;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-execute(" + e + ")");

      throw e;
    }
  }
  
  @Override
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    long start = start();
    
    try {
      boolean isResultSet = _stmt.execute(query, columns);

      if (log.isLoggable(Level.FINE)) {
        List<Integer> list = new ArrayList<Integer>();
        for (int column : columns)
          list.add(column);

          log(start, "execute(" + query + ", columns=" + list + ")");
      }

      return isResultSet;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-execute(" + e + ")");

      throw e;
    }
  }
  
  @Override
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    long start = start();
    
    try {
      boolean isResultSet = _stmt.execute(query, columns);

      if (log.isLoggable(Level.FINE)) {
        log(start, "execute(" + query
            + ", columns=" + Arrays.asList(columns) + ") -> " + isResultSet);
      }

      return isResultSet;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-execute(" + e + ")");

      throw e;
    }
  }

  @Override
  public int getResultSetHoldability()
    throws SQLException
  {
    long start = start();
    
    try {
      int holdability =  _stmt.getResultSetHoldability();

      if (log.isLoggable(Level.FINE))
        log(start, "getResultSetHoldability() -> " + holdability);

      return holdability;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-getResultSetHoldability(" + e + ")");

      throw e;
    }
  }

  @Override
  public boolean isClosed()
    throws SQLException
  {
    long start = start();
    
    try {
      boolean closed = _stmt.isClosed();

      if (log.isLoggable(Level.FINE))
        log(start, "isClosed() -> " + closed);

      return closed;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "isClosed(" + e + ")");

      throw e;
    }
  }

  @Override
  public void setPoolable(boolean poolable)
    throws SQLException
  {
    long start = start();
    
    try {
      _stmt.setPoolable(poolable);

      if (log.isLoggable(Level.FINE))
        log(start, "setPoolable(" + poolable + ")");
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-setPoolable(" + e + ")");

      throw e;
    }
  }

  @Override
  public boolean isPoolable()
    throws SQLException
  {
    long start = start();
    
    try {
      boolean isPoolable = _stmt.isPoolable();

      if (log.isLoggable(Level.FINE))
        log(start, "isPoolable() -> " + isPoolable);

      return isPoolable;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log(start, "exn-isPoolable(" + e + ")");

      throw e;
    }
  }

  @Override
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try {
      T t = _stmt.unwrap(iface);

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":unwrap(" + iface + ") -> " + t);

      return t;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-unwrap(" + e + ")");

      throw e;
    }
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    try {
      boolean isWrapper = _stmt.isWrapperFor(iface);

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":isWrapperFor(" + iface + ") -> " + isWrapper);
      
      return isWrapper;
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-isWrapperFor(" + e + ")");

      throw e;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  @Override
  public void closeOnCompletion() throws SQLException
  {

  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException
  {
    return false;
  }
}
