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

package com.caucho.server.distcache;

import com.caucho.config.ConfigException;
import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.db.index.SqlIndexAlreadyExistsException;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.FreeList;
import com.caucho.util.IoUtil;
import com.caucho.util.HashKey;
import com.caucho.util.JdbcUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;
import com.caucho.server.admin.Management;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages the backing for the file database objects
 */
public class DataStore implements AlarmListener {
  private static final L10N L = new L10N(DataStore.class);
  private static final Logger log
    = Logger.getLogger(DataStore.class.getName());
  
  private FreeList<DataConnection> _freeConn
    = new FreeList<DataConnection>(32);

  private final String _tableName;
  private final String _mnodeTableName;

  // remove unused data after 1 hour
  private long _expireTimeout = 3600L * 1000L;

  private DataSource _dataSource;
    
  private String _insertQuery;
  private String _loadQuery;
  private String _dataAvailableQuery;
  private String _updateExpiresQuery;
  private String _timeoutQuery;
  
  private String _countQuery;

  private Alarm _alarm;
  private long _expireReaperTimeout = 15 * 60 * 1000L;
  
  public DataStore(String serverName,
		   MnodeStore mnodeStore)
    throws Exception
  {
    _dataSource = mnodeStore.getDataSource();
    _mnodeTableName = mnodeStore.getTableName();
    
    _tableName = serverNameToTableName(serverName);
    
    if (_tableName == null)
      throw new NullPointerException();


    init();
  }

  DataSource getDataSource()
  {
    return _dataSource;
  }

  private void init()
    throws Exception
  {
    _loadQuery = ("SELECT data"
		  + " FROM " + _tableName
		  + " WHERE id=?");
    
    _dataAvailableQuery = ("SELECT 1"
			   + " FROM " + _tableName
			   + " WHERE id=?");

    _insertQuery = ("INSERT into " + _tableName
		    + " (id,expire_time,data) "
		    + "VALUES(?,?,?)");

    // XXX: add random component to expire time?
    _updateExpiresQuery = ("UPDATE " + _tableName
			   + " SET expire_time=?"
			   + " WHERE expire_time<? AND EXISTS "
			   + "      (SELECT * FROM " + _mnodeTableName
			   +   "       WHERE " + _tableName + ".id = " + _mnodeTableName + ".value)");

    _timeoutQuery = ("DELETE FROM " + _tableName
		     + " WHERE expire_time < ?");
    
    _countQuery = "SELECT count(*) FROM " + _tableName;

    initDatabase();

    _alarm = new Alarm(this);
    handleAlarm(_alarm);
  }

  /**
   * Create the database, initializing if necessary.
   */
  private void initDatabase()
    throws Exception
  {
    Connection conn = _dataSource.getConnection();

    try {
      Statement stmt = conn.createStatement();
      
      try {
	String sql = ("SELECT id, expire_time, data"
                      + " FROM " + _tableName + " WHERE 1=0");

	ResultSet rs = stmt.executeQuery(sql);
	rs.next();
	rs.close();

	return;
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
	log.finer(this + " " + e.toString());
      }

      try {
	stmt.executeQuery("DROP TABLE " + _tableName);
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }

      String sql = ("CREATE TABLE " + _tableName + " (\n"
                    + "  id BINARY(32) PRIMARY KEY,\n"
		    + "  expire_time BIGINT,\n"
		    + "  data BLOB)");


      log.fine(sql);

      stmt.executeUpdate(sql);
    } finally {
      conn.close();
    }
  }

  /**
   * Reads the object from the data store.
   *
   * @param id the hash identifier for the data
   * @param os the WriteStream to hold the data
   *
   * @return true on successful load
   */
  public boolean load(HashKey id, WriteStream os)
  {
    DataConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      pstmt.setBytes(1, id.getHash());

      ResultSet rs = pstmt.executeQuery();
      
      if (rs.next()) {
	InputStream is = rs.getBinaryStream(1);

	try {
	  os.writeStream(is);
	} finally {
	  is.close();
	}

	if (log.isLoggable(Level.FINER))
	  log.finer(this + " load " + id + " length:" + os.getPosition());

	return true;
      }

      if (log.isLoggable(Level.FINER))
	log.finer(this + " no data loaded for " + id);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }

    return false;
  }

  /**
   * Checks if we have the data
   *
   * @param id the hash identifier for the data
   *
   * @return true on successful load
   */
  public boolean isDataAvailable(HashKey id)
  {
    DataConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      pstmt.setBytes(1, id.getHash());

      ResultSet rs = pstmt.executeQuery();
      
      if (rs.next()) {
	return true;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }

    return false;
  }

  /**
   * Reads the object from the data store.
   *
   * @param id the hash identifier for the data
   * @param os the WriteStream to hold the data
   *
   * @return true on successful load
   */
  public InputStream openInputStream(HashKey id)
  {
    DataConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      pstmt.setBytes(1, id.getHash());

      ResultSet rs = pstmt.executeQuery();
      
      if (rs.next()) {
	InputStream is = rs.getBinaryStream(1);

	InputStream dataInputStream = new DataInputStream(conn, is);
	conn = null;

	return dataInputStream;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }

    return null;
  }
  
  /**
   * Saves the data, returning true on success.
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  public boolean save(HashKey id, StreamSource source, int length)
    throws IOException
  {
    if (insert(id, source.openInputStream(), length))
      return true;
    else if (updateExpires(id))
      return true;
    else if (insert(id, source.openInputStream(), length))
      return true;
    else {
      log.warning(this + " can't save data '" + id + "'");
      
      return false;
    }
  }
  
  /**
   * Stores the data, returning true on success
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  private boolean insert(HashKey id, InputStream is, int length)
  {
    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareInsert();
      stmt.setBytes(1, id.getHash());
      stmt.setLong(2, _expireTimeout + Alarm.getCurrentTime());
      stmt.setBinaryStream(3, is, length);

      int count = stmt.executeUpdate();
        
      if (log.isLoggable(Level.FINER)) 
	log.finer(this + " insert " + id + " length:" + length);
	  
      return true;
    } catch (SqlIndexAlreadyExistsException e) {
      // the data already exists in the cache, so this is okay
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);

      return true;
    } catch (SQLException e) {
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }

    return false;
  }

  /**
   * Updates the expires time for the data.
   *
   * @param id the hash identifier for the data
   *
   * @return true if the database contains the id
   */
  public boolean updateExpires(HashKey id)
  {
    DataConnection conn = null;
    
    try {
      conn = getConnection();
      PreparedStatement pstmt = conn.prepareUpdateExpires();

      long expireTime = _expireTimeout + Alarm.getCurrentTime();
      
      pstmt.setLong(1, expireTime);
      pstmt.setLong(2, expireTime);

      int count = pstmt.executeUpdate();
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " updateExpires " + id);

      return count > 0;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }

    return false;
  }

  /**
   * Clears the expired data
   */
  public void removeExpiredData()
  {
    DataConnection conn = null;

    try {
      conn = getConnection();
  
      long now = Alarm.getCurrentTime();
      
      PreparedStatement pstmt = conn.prepareUpdateExpires();

      pstmt.setLong(1, now + 3L * 3600 * 1000L);
      pstmt.setLong(2, now);

      pstmt.executeUpdate();
      
      pstmt = conn.prepareTimeout();
	
      pstmt.setLong(1, now);

      int count = pstmt.executeUpdate();

      if (count > 0)
	log.finer(this + " expired " + count + " old data");
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      conn.close();
    }
  }

  //
  // statistics
  //

  public long getCount()
  {
    DataConnection conn = null;
    
    try {
      conn = getConnection();
      PreparedStatement stmt = conn.prepareCount();
      
      ResultSet rs = stmt.executeQuery();

      if (rs != null && rs.next()) {
	long value = rs.getLong(1);
	
	rs.close();
	
	return value;
      }
      
      return -1;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }
    
    return -1;
  }

  public void handleAlarm(Alarm alarm)
  {
    if (_dataSource != null) {
      try {
	removeExpiredData();
      } finally {
	alarm.queue(_expireTimeout / 2);
      }
    }
  }

  public void destroy()
  {
    _dataSource = null;
    _freeConn = null;
    
    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();
  }

  private DataConnection getConnection()
    throws SQLException
  {
    DataConnection cConn = _freeConn.allocate();

    if (cConn == null) {
      Connection conn = _dataSource.getConnection();
      cConn = new DataConnection(conn);
    }

    return cConn;
  }

  private String serverNameToTableName(String serverName)
  {
    if (serverName == null || "".equals(serverName))
      return "resin_data_default";
    
    StringBuilder cb = new StringBuilder();
    cb.append("resin_data_");
    
    for (int i = 0; i < serverName.length(); i++) {
      char ch = serverName.charAt(i);

      if ('a' <= ch && ch <= 'z') {
	cb.append(ch);
      }
      else if ('A' <= ch && ch <= 'Z') {
	cb.append(ch);
      }
      else if ('0' <= ch && ch <= '9') {
	cb.append(ch);
      }
      else if (ch == '_') {
	cb.append(ch);
      }
      else
	cb.append('_');
    }

    return cb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _tableName + "]";
  }

  class DataInputStream extends InputStream {
    private DataConnection _conn;
    private InputStream _is;

    DataInputStream(DataConnection conn, InputStream is)
    {
      _conn = conn;
      _is = is;
    }

    public int read()
      throws IOException
    {
      return _is.read();
    }

    public int read(byte []buffer, int offset, int length)
      throws IOException
    {
      return _is.read(buffer, offset, length);
    }

    public void close()
    {
      DataConnection conn = _conn;
      _conn = null;
      
      InputStream is = _is;
      _is = null;

      IoUtil.close(is);
      
      if (conn != null)
	conn.close();
    }
  }

  class DataConnection {
    private Connection _conn;
    
    private PreparedStatement _loadStatement;
    private PreparedStatement _dataAvailableStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateExpiresStatement;
    private PreparedStatement _timeoutStatement;
    
    private PreparedStatement _countStatement;

    DataConnection(Connection conn)
    {
      _conn = conn;
    }

    PreparedStatement prepareLoad()
      throws SQLException
    {
      if (_loadStatement == null)
	_loadStatement = _conn.prepareStatement(_loadQuery);

      return _loadStatement;
    }

    PreparedStatement prepareDataAvailable()
      throws SQLException
    {
      if (_dataAvailableStatement == null)
	_dataAvailableStatement = _conn.prepareStatement(_dataAvailableQuery);

      return _dataAvailableStatement;
    }

    PreparedStatement prepareInsert()
      throws SQLException
    {
      if (_insertStatement == null)
	_insertStatement = _conn.prepareStatement(_insertQuery);

      return _insertStatement;
    }

    PreparedStatement prepareUpdateExpires()
      throws SQLException
    {
      if (_updateExpiresStatement == null)
	_updateExpiresStatement = _conn.prepareStatement(_updateExpiresQuery);

      return _updateExpiresStatement;
    }

    PreparedStatement prepareTimeout()
      throws SQLException
    {
      if (_timeoutStatement == null)
	_timeoutStatement = _conn.prepareStatement(_timeoutQuery);

      return _timeoutStatement;
    }

    PreparedStatement prepareCount()
      throws SQLException
    {
      if (_countStatement == null)
	_countStatement = _conn.prepareStatement(_countQuery);

      return _countStatement;
    }

    void close()
    {
      if (_freeConn == null || ! _freeConn.free(this)) {
	try {
	  _conn.close();
	} catch (SQLException e) {
	}
      }
    }
  }
}
