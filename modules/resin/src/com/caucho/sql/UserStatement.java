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

package com.caucho.sql;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.SQLException;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

/**
 * User-view of a statement;
 */
public class UserStatement implements Statement {
  protected final static Logger log = Log.open(UserStatement.class);
  protected final static L10N L = new L10N(UserStatement.class);
  
  // The connection
  protected UserConnection _conn;
  
  // The underlying connection
  protected Statement _stmt;

  // True if the statement is changed in a way that forbids its caching.
  protected boolean _isChanged;

  UserStatement(UserConnection conn, Statement stmt)
  {
    _conn = conn;
    _stmt = stmt;
  }

  /**
   * Returns true if the statement is chanced in a way that forbids caching.
   */
  boolean isChanged()
  {
    return _isChanged;
  }

  /**
   * Returns the underlying statement.
   */
  public Statement getStatement()
  {
    return _stmt;
  }

  public void addBatch(String sql)
    throws SQLException
  {
    _stmt.addBatch(sql);
  }

  public void cancel()
    throws SQLException
  {
    _stmt.cancel();
  }

  public void clearBatch()
    throws SQLException
  {
    _stmt.clearBatch();
  }

  public void clearWarnings()
    throws SQLException
  {
    _stmt.clearWarnings();
  }

  /**
   * Closes the statement.
   */
  public void close()
    throws SQLException
  {
    Statement stmt = _stmt;
    _stmt = null;

    if (stmt != null) {
      _conn.closeStatement(stmt);
    
      stmt.close();
    }
  }

  /**
   * queries the database with the given sql.
   */
  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    return _stmt.executeQuery(sql);
  }

  /**
   * updates the database with the given sql.
   */
  public int executeUpdate(String sql)
    throws SQLException
  {
    return _stmt.executeUpdate(sql);
  }

  /**
   * Execute an update with the given result type.
   */
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    return _stmt.executeUpdate(query, resultType);
  }

  /**
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }

  /**
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }

  /**
   * executes the given sql.
   */
  public boolean execute(String sql)
    throws SQLException
  {
    return _stmt.execute(sql);
  }

  /**
   * executes the given query with its result type.
   */
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    return _stmt.execute(query, resultType);
  }

  /**
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }

  /**
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }

  /**
   * Executes the batched sql.
   */
  public int[]executeBatch()
    throws SQLException
  {
    return _stmt.executeBatch();
  }

  /**
   * Returns the result set of the last query.
   */
  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    return _stmt.getResultSet();
  }

  /**
   * Returns the update count of the last query.
   */
  public int getUpdateCount()
    throws SQLException
  {
    return _stmt.getUpdateCount();
  }

  /**
   * Returns the underlying connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  /**
   * Returns the current fetch direction.
   */
  public int getFetchDirection()
    throws SQLException
  {
    return _stmt.getFetchDirection();
  }

  /**
   * Sets the fetch direction.
   */
  public void setFetchDirection(int direction)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setFetchDirection(direction);
  }

  /**
   * Returns the fetch size.
   */
  public int getFetchSize()
    throws SQLException
  {
    return _stmt.getFetchSize();
  }

  /**
   * Sets the fetch size.
   */
  public void setFetchSize(int rows)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setFetchSize(rows);
  }

  /**
   * Returns the maximum field size.
   */
  public int getMaxFieldSize()
    throws SQLException
  {
    return _stmt.getMaxFieldSize();
  }

  /**
   * Sets the maximum field size.
   */
  public void setMaxFieldSize(int max)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setMaxFieldSize(max);
  }

  /**
   * Returns the maximum rows returned by a query.
   */
  public int getMaxRows()
    throws SQLException
  {
    return _stmt.getMaxRows();
  }

  /**
   * Sets the maximum rows returned by a query.
   */
  public void setMaxRows(int max)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setMaxRows(max);
  }

  /**
   * Returns true if more results are available.
   */
  public boolean getMoreResults()
    throws SQLException
  {
    return _stmt.getMoreResults();
  }

  /**
   * Returns the current query timeout.
   */
  public int getQueryTimeout()
    throws SQLException
  {
    return _stmt.getQueryTimeout();
  }

  /**
   * Sets the query timeout.
   */
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setQueryTimeout(seconds);
  }

  /**
   * Returns the statement's result set concurrency setting.
   */
  public int getResultSetConcurrency()
    throws SQLException
  {
    return _stmt.getResultSetConcurrency();
  }

  /**
   * Returns the statement's result set type.
   */
  public int getResultSetType()
    throws SQLException
  {
    return _stmt.getResultSetType();
  }

  /**
   * Returns the current sql warnings.
   */
  public SQLWarning getWarnings()
    throws SQLException
  {
    return _stmt.getWarnings();
  }

  /**
   * Sets the current cursor name.
   */
  public void setCursorName(String name)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setCursorName(name);
  }

  /**
   * Enables escape processing.
   */
  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    _isChanged = true;
    
    _stmt.setEscapeProcessing(enable);
  }

  /**
   * Returns the next count results.
   */
  public boolean getMoreResults(int count)
    throws SQLException
  {
    return _stmt.getMoreResults(count);
  }

  /**
   * Returns the generated keys for the update.
   */
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    return _stmt.getGeneratedKeys();
  }

  /**
   * Returns the result set holdability.
   */
  public int getResultSetHoldability()
    throws SQLException
  {
    return _stmt.getResultSetHoldability();
  }
}
