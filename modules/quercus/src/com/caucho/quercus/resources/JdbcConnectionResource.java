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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.resources;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.*;

import com.caucho.quercus.env.*;

/**
 * Represents a JDBC Connection value.
 */
public class JdbcConnectionResource extends ResourceValue {
  private static final Logger log
    = Logger.getLogger(JdbcConnectionResource.class.getName());

  private Connection _conn;
  // cached statement
  private Statement _stmt;

  private DatabaseMetaData _dmd;

  private HashMap<TableKey,JdbcTableMetaData> _tableMetadataMap
    = new HashMap<TableKey,JdbcTableMetaData>();

  private JdbcResultResource _rs;
  private int _affectedRows;

  private String _errorMessage = "";
  private int _errorCode;
  private boolean _fieldCount = false;
  private SQLWarning _warnings;

  /**
   * mysqli_multi_query populates _resultValues
   * NB: any updates (ie: INSERT, UPDATE, DELETE) will
   * have the update counts ignored.
   *
   * Has been stored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   *
   * _hasBeenStored is set to true by default.
   * if _hasBeenUsed == false, then
   * _resultValues.get(_nextResultValue)
   * is ready to be used by the next call to
   * mysqli_store_result or mysqli_use_result.
   */
  private ArrayList<JdbcResultResource> _resultValues = new ArrayList<JdbcResultResource>();
  private int _nextResultValue = 0;
  private boolean _hasBeenUsed = true;

  public JdbcConnectionResource(Connection conn)
  {
    _conn = conn;
  }

  /**
   * Returns the affected rows from the last query.
   */
  public int getAffectedRows()
  {
    return _affectedRows;
  }

  public void setAffectedRows(int i)
  {
    _affectedRows = i;
  }

  /**
   * @return _fieldCount
   */
  public int getFieldCount()
  {
    if (_rs == null)
      return 0;
    else
      return _rs.getFieldCount();
  }

  /**
   * Returns JdbcResultResource of available databases
   */
  public Value getCatalogs()
  {
    clearErrors();
    
    try {
      if (_dmd == null)
        _dmd = _conn.getMetaData();

      ResultSet rs = _dmd.getCatalogs();

      if (rs != null)
        return new JdbcResultResource(_stmt, rs, this);
      else
        return BooleanValue.FALSE;
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * @return current catalog or false if error
   */
  public Value getCatalog()
  {
    clearErrors();
    
    try {
      return new StringValueImpl(_conn.getCatalog());
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the client version
   */
  public String getClientInfo()
  {
    try {
      if (_dmd == null)
        _dmd = _conn.getMetaData();

      return _dmd.getDatabaseProductVersion();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * Returns the connection
   */
  public Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  /**
   * Returns the last error code.
   */
  public int getErrorCode()
  {
    return _errorCode;
  }

  /**
   * Returns the last error message.
   */
  public String getErrorMessage()
  {
    return _errorMessage;
  }

  /**
   *
   * returns the URL string for the given connection
   * IE: jdbc:mysql://localhost:3306/test
   * XXX: PHP returns Localhost via UNIX socket
   */
  public String getHostInfo()
    throws SQLException
  {
    if (_dmd == null)
      _dmd = _conn.getMetaData();

    return _dmd.getURL();
  }

  /**
   * returns the server version
   * XXX: PHP seems to return the same value
   * for client_info and server_info
   */
  public String getServerInfo()
    throws SQLException
  {
    return getMetaData().getDatabaseProductVersion();
  }

  /**
   * Returns the table metadata.
   */
  public JdbcTableMetaData getTableMetaData(String catalog,
					    String schema,
					    String table)
    throws SQLException
  {
    TableKey key = new TableKey(catalog, schema, table);

    JdbcTableMetaData tableMd = _tableMetadataMap.get(key);

    if (tableMd != null)
      return tableMd;

    tableMd = new JdbcTableMetaData(catalog, schema, table, getMetaData());

    _tableMetadataMap.put(key, tableMd);

    return tableMd;
  }

  private DatabaseMetaData getMetaData()
    throws SQLException
  {
    if (_dmd == null)
      _dmd = _conn.getMetaData();

    return _dmd;
  }

  /**
   * indicates if one or more result sets are
   * available from a multi query
   *
   * _hasBeenStored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   */
  public boolean moreResults()
  {
    return !_hasBeenUsed || _nextResultValue < _resultValues.size() - 1;
  }

  /**
   * prepares the next resultset from
   * a multi_query
   */
  public boolean nextResult()
  {
    if (_nextResultValue + 1 < _resultValues.size()) {
      _hasBeenUsed = false;
      _nextResultValue++;
      return true;
    } else
      return false;
  }

  /**
   * returns the next jdbcResultValue
   */
  public Value storeResult()
  {
    if (!_hasBeenUsed) {
      _hasBeenUsed = true;

      return _resultValues.get(_nextResultValue);
    } else
      return BooleanValue.FALSE;
  }

  /**
   * splits a string of multiple queries separated
   * by ";" into an arraylist of strings
   */
  private ArrayList<String> splitMultiQuery(String sql)
  {
    ArrayList<String> result = new ArrayList<String>();
    String query = "";
    int length = sql.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = sql.charAt(i);

      if (c == '\\') {
        query += c;
        if (i < length - 1) {
          query += sql.charAt(i+1);
          i++;
        }
        continue;
      }

      if (inQuotes) {
        query += c;
        if (c == '\'') {
          inQuotes = false;
        }
        continue;
      }

      if (c == '\'') {
        query += c;
        inQuotes = true;
        continue;
      }

      if (c == ';') {
        result.add(query.trim());
        query = "";
      } else
        query += c;
    }

    if (query != null)
      result.add(query.trim());

    return result;
  }

  /**
   * Execute a single query.
   */
  public Value query(String sql)
  {
    clearErrors();
    
    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();
    _rs = null;

    Statement stmt = null;

    try {
      stmt = _conn.createStatement();
      stmt.setEscapeProcessing(false); // php/1406

      if (stmt.execute(sql)) {
	ResultSet rs = stmt.getResultSet();

	_rs = new JdbcResultResource(stmt, rs, this);
        _affectedRows = 0;
        _resultValues.add(_rs);
        _warnings = stmt.getWarnings();
      } else {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
	stmt.close();
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        _errorMessage = e.getMessage();
        _errorCode = e.getErrorCode();
        log.log(Level.WARNING, e.toString(), e);
        return BooleanValue.FALSE;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }

    if (_resultValues.size() > 0) {
      _nextResultValue = 0;
      _hasBeenUsed = false;
      return _resultValues.get(0);
    } else
      return BooleanValue.TRUE;
  }

  /**
   * Used by the
   * various mysqli functions to query the database
   * for metadata about the resultset which is
   * not in ResultSetMetaData.
   *
   * This function DOES NOT clear existing resultsets.
   */
  public Value metaQuery(String sql,
                         String catalog)
  {
    clearErrors();
    
    Value currentCatalog = getCatalog();

    try {
      _conn.setCatalog(catalog);

      // need to create statement after setting catalog or
      // else statement will have wrong catalog
      Statement stmt = _conn.createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(sql)) {
        Value result = new JdbcResultResource(stmt, stmt.getResultSet(), this);
        _conn.setCatalog(currentCatalog.toString());
        return result;
      } else {
        _conn.setCatalog(currentCatalog.toString());
        return BooleanValue.FALSE;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Used for multiple queries. the
   * JdbcConnectionResource now stores the
   * result sets so that mysqli_store_result
   * and mysqli_use_result can return result values.
   *
   * XXX: this may not function correctly in the
   * context of a transaction.  Unclear wether
   * mysqli_multi_query was designed with transactions
   * in mind.
   *
   * XXX: multiQuery sets fieldCount to true or false
   * depending on the last query entered.  Not sure what
   * actual PHP intention is.
   */
  public boolean multiQuery(String sql)
  {
    clearErrors();
    
    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();

    ArrayList<String> splitQuery = splitMultiQuery(sql);

    Statement stmt = null;

    try {
      _rs = null;

      for (String s : splitQuery) {
        stmt = _conn.createStatement();
        stmt.setEscapeProcessing(false);
        if (stmt.execute(s)) {
          _affectedRows = 0;
	  _rs = new JdbcResultResource(stmt, stmt.getResultSet(), this);
          _resultValues.add(_rs);
          _warnings = stmt.getWarnings();
        } else {
          _affectedRows = stmt.getUpdateCount();
          _warnings = stmt.getWarnings();
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        _errorMessage = e.getMessage();
        _errorCode = e.getErrorCode();
        log.log(Level.WARNING, e.toString(), e);
        return false;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    if (_resultValues.size() > 0) {
      _nextResultValue = 0;
      _hasBeenUsed = false;
    }

    return true;
  }

  /**
   * sets auto-commmit to true or false
   */
  public boolean setAutoCommit(boolean mode)
  {
    clearErrors();
    
    try {
      _conn.setAutoCommit(mode);
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * commits the transaction of the current connection
   */
  public boolean commit()
  {
    clearErrors();
    
    try {
      _conn.commit();
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * rolls the current transaction back
   *
   * NOTE: quercus doesn't seem to support the idea
   * of savepoints
   */
  public boolean rollback()
  {
    clearErrors();
    
    try {
      _conn.rollback();
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }
  /**
   * Sets the catalog
   */
  public void setCatalog(String name)
    throws SQLException
  {
    clearErrors();
    
    _conn.setCatalog(name);
  }

  /**
   * returns a string with the status of the connection
   */
  public Value stat()
  {
    clearErrors();
    
    StringBuilder str = new StringBuilder();

    try {
      Statement stmt = _conn.createStatement();
      stmt.execute("SHOW STATUS");

      ResultSet rs = stmt.getResultSet();

      while (rs.next()) {
        if (str.length() > 0)
          str.append(' ');
        str.append(rs.getString(1));
        str.append(": ");
        str.append(rs.getString(2));
      }

      return new StringValueImpl(str.toString());
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return _conn.toString();
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    try {
      Statement stmt = _stmt;
      _stmt = null;

      if (stmt != null)
        stmt.close();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      Connection conn = _conn;
      // XXX: since the above code doesn't check for _conn == null can't null
      // _conn = null;

      if (conn != null)
        conn.close();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * This functions queries the connection with "SHOW WARNING"
   *
   * @return # of warnings
   */
  public int getWarningCount()
  {
    if (_warnings != null) {
      Value warningResult = metaQuery("SHOW WARNINGS",getCatalog().toString());
      Value warningCount = null;

      if (warningResult instanceof JdbcResultResource) {
        warningCount = JdbcResultResource.getNumRows(((JdbcResultResource) warningResult).getResultSet());
      }

      if ((warningCount != null) && (warningCount != BooleanValue.FALSE))
        return warningCount.toInt();
      else
        return 0;
    } else
      return 0;
  }

  private void clearErrors()
  {
    _errorMessage = null;
    _errorCode = 0;
    _warnings = null;
  }

  static class TableKey {
    private final String _catalog;
    private final String _schema;
    private final String _table;

    TableKey(String catalog, String schema, String table)
    {
      _catalog = catalog;
      _schema = schema;
      _table = table;
    }

    public int hashCode()
    {
      int hash = 37;

      if (_catalog != null)
	hash = 65537 * hash + _catalog.hashCode();

      if (_schema != null)
	hash = 65537 * hash + _schema.hashCode();

      if (_table != null)
	hash = 65537 * hash + _table.hashCode();

      return hash;
    }

    public boolean equals(Object o)
    {
      if (this == o)
	return true;
      else if (! (o instanceof TableKey))
	return false;

      TableKey key = (TableKey) o;

      if ((_catalog == null) != (key._catalog == null))
	return false;
      else if (_catalog != null && ! _catalog.equals(key._catalog))
	return false;

      if ((_schema == null) != (key._schema == null))
	return false;
      else if (_schema != null && ! _schema.equals(key._schema))
	return false;

      if ((_table == null) != (key._table == null))
	return false;
      else if (_table != null && ! _table.equals(key._table))
	return false;

      return true;
    }
  }
}

