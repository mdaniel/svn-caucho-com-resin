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

package com.caucho.server.cluster;

import com.caucho.config.ConfigException;
import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.util.Alarm;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages the backing for the file store.
 */
public class FileBacking {
  private static final L10N L = new L10N(FileBacking.class);
  private static final Logger log = Log.open(FileBacking.class);
  
  private FreeList<ClusterConnection> _freeConn
    = new FreeList<ClusterConnection>(32);
  
  private String _name;
  
  private Path _path;

  private DataSource _dataSource;

  private String _tableName;
  private String _loadQuery;
  private String _updateQuery;
  private String _accessQuery;
  private String _setExpiresQuery;
  private String _insertQuery;
  private String _invalidateQuery;
  private String _timeoutQuery;
  private String _dumpQuery;
  private String _countQuery;

  /**
   * Returns the path to the directory.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the path to the saved file.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Sets the table name
   */
  public void setTableName(String table)
  {
    _tableName = table;
  }

  public boolean init(int clusterLength)
    throws Exception
  {
    if (_path == null)
      throw new ConfigException(L.l("file-backing needs path."));
    
    if (_tableName == null)
      throw new ConfigException(L.l("file-backing needs tableName."));

    int length = clusterLength;

    if (length <= 0)
      length = 1;
    
    _loadQuery = "SELECT access_time,data FROM " + _tableName + " WHERE id=?";
    _insertQuery = ("INSERT into " + _tableName + " (id,data,mod_time,access_time,expire_interval,server1,server2,server3) " +
		    "VALUES(?,?,?,?,?,?,?,?)");
    _updateQuery = "UPDATE " + _tableName + " SET data=?, mod_time=?, access_time=? WHERE id=?";
    _accessQuery = "UPDATE " + _tableName + " SET access_time=? WHERE id=?";
    _setExpiresQuery = "UPDATE " + _tableName + " SET expire_interval=? WHERE id=?";
    _invalidateQuery = "DELETE FROM " + _tableName + " WHERE id=?";

    // access window is 1/4 the expire interval
    _timeoutQuery = "DELETE FROM " + _tableName + " WHERE access_time + 5 * expire_interval / 4 < ?";

    _dumpQuery = ("SELECT id, expire_interval, data FROM " + _tableName +
		  " WHERE ? <= mod_time AND " +
		  "   (?=server1 OR ?=server2 OR ?=server3)");
    
    _countQuery = "SELECT count(*) FROM " + _tableName;
    
    try {
      _path.mkdirs();
    } catch (IOException e) {
    }

    DataSourceImpl dataSource = new DataSourceImpl();
    dataSource.setPath(_path);
    dataSource.setRemoveOnError(true);
    dataSource.init();
    
    _dataSource = dataSource;

    initDatabase();

    return true;
  }

  /**
   * Returns the data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
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
      
      boolean hasDatabase = false;

      try {
	String sql = "SELECT expire_interval FROM " + _tableName + " WHERE 1=0";

	ResultSet rs = stmt.executeQuery(sql);
	rs.next();
	rs.close();

	return;
      } catch (Exception e) {
	log.finer(e.toString());
      }

      try {
	stmt.executeQuery("DROP TABLE " + _tableName);
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }

      String sql = ("CREATE TABLE " + _tableName + " (\n" +
		    "  id VARBINARY(64) PRIMARY KEY,\n" +
		    "  data BLOB,\n" +
		    "  expire_interval INTEGER,\n" +
		    "  access_time INTEGER,\n" +
		    "  mod_time INTEGER,\n" +
		    "  mod_count BIGINT,\n" +
		    "  server1 INTEGER,\n" +
		    "  server2 INTEGER,\n" + 
		    "  server3 INTEGER)");

      log.fine(sql);

      stmt.executeUpdate(sql);
    } finally {
      conn.close();
    }
  }

  public long start()
    throws Exception
  {
    long delta = - Alarm.getCurrentTime();

    Connection conn = null;
    try {
      conn = _dataSource.getConnection();
      
      Statement stmt = conn.createStatement();

      String sql = "SELECT MAX(access_time) FROM " + _tableName;

      ResultSet rs = stmt.executeQuery(sql);

      if (rs.next())
	delta = rs.getInt(1) * 60000L - Alarm.getCurrentTime();
    } finally {
      if (conn != null)
	conn.close();
    }

    return delta;
  }

  /**
   * Clears the old objects.
   */
  public void clearOldObjects(long maxIdleTime)
    throws SQLException
  {
    Connection conn = null;

    try {
      if (maxIdleTime > 0) {
	conn = _dataSource.getConnection();

	PreparedStatement pstmt = conn.prepareStatement(_timeoutQuery);
  
        long now = Alarm.getCurrentTime();
        int nowMinute = (int) (now / 60000L);
	
	pstmt.setInt(1, nowMinute);

        int count = pstmt.executeUpdate();

	// System.out.println("OBSOLETE:" + count);

	if (count > 0)
	  log.fine(this + " purged " + count + " old sessions");

	pstmt.close();
      }
    } finally {
      if (conn != null)
	conn.close();
    }
  }

  /**
   * Load the session from the jdbc store.
   *
   * @param session the session to fill.
   *
   * @return true if the load was valid.
   */
  public boolean loadSelf(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    String uniqueId = clusterObj.getUniqueId();

    ClusterConnection conn = getConnection();
    try {
      PreparedStatement stmt = conn.prepareLoad();
      stmt.setString(1, uniqueId);

      ResultSet rs = stmt.executeQuery();
      boolean validLoad = false;

      if (rs.next()) {
	//System.out.println("LOAD: " + uniqueId);
	long accessTime = rs.getInt(1) * 60000L;
	
        InputStream is = rs.getBinaryStream(2);

        if (log.isLoggable(Level.FINE))
          log.fine("load local object: " + uniqueId);
      
        validLoad = clusterObj.load(is, obj);

	if (validLoad)
	  clusterObj.setAccessTime(accessTime);

        is.close();
      }
      else if (log.isLoggable(Level.FINE))
        log.fine("no local object loaded for " + uniqueId);
      else {
	// System.out.println("NO-LOAD: " + uniqueId);
      }

      rs.close();

      return validLoad;
    } finally {
      conn.close();
    }
  }

  /**
   * Updates the object's access time.
   *
   * @param obj the object to store.
   */
  public void updateAccess(String uniqueId)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement stmt = conn.prepareAccess();

      long now = Alarm.getCurrentTime();
      int nowMinutes = (int) (now / 60000L);
      stmt.setInt(1, nowMinutes);
      stmt.setString(2, uniqueId);

      int count = stmt.executeUpdate();

      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine("access cluster: " + uniqueId);
	return;
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Sets the object's expire_interval.
   *
   * @param obj the object to store.
   */
  public void setExpireInterval(String uniqueId, long expireInterval)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement stmt = conn.prepareSetExpireInterval();

      int expireMinutes = (int) (expireInterval / 60000L);
      stmt.setInt(1, expireMinutes);
      stmt.setString(2, uniqueId);

      int count = stmt.executeUpdate();

      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine("set expire interval: " + uniqueId + " " + expireInterval);
	return;
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Removes the named object from the store.
   */
  public void remove(String uniqueId)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement pstmt = conn.prepareInvalidate();
      pstmt.setString(1, uniqueId);

      int count = pstmt.executeUpdate();
      
      if (log.isLoggable(Level.FINE))
        log.fine("invalidate: " + uniqueId);
    } finally {
      conn.close();
    }
  }

  /**
   * Reads from the store.
   */
  public long read(String uniqueId, WriteStream os)
    throws IOException
  {
    Connection conn = null;
    try {
      conn = _dataSource.getConnection();

      PreparedStatement pstmt = conn.prepareStatement(_loadQuery);
      pstmt.setString(1, uniqueId);

      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
	long accessTime = rs.getInt(1) * 60000L;
	
	InputStream is = rs.getBinaryStream(2);

	os.writeStream(is);

	is.close();

	return accessTime;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (conn != null)
	  conn.close();
      } catch (SQLException e) {
      }
    }

    return -1;
  }

  /**
   * Stores the cluster object on the local store.
   *
   * @param uniqueId the object's unique id.
   * @param id the input stream to the serialized object
   * @param length the length object the serialized object
   * @param expireInterval how long the object lives w/o access
   */
  public void storeSelf(String uniqueId,
			ReadStream is, int length,
			long expireInterval,
			int primary, int secondary, int tertiary)
  {
    ClusterConnection conn = null;

    try {
      conn = getConnection();
      // Try to update first, and insert if fail.
      // The binary stream can be reused because it won't actually be
      // read on a failure

      //System.out.println("SAVE: " + uniqueId);
      if (storeSelfUpdate(conn, uniqueId, is, length)) {
	//System.out.println("SAVE-UPDATE: " + uniqueId);
      }
      else if (storeSelfInsert(conn, uniqueId, is, length, expireInterval,
			       primary, secondary, tertiary)) {
	//System.out.println("SAVE-INSERT: " + uniqueId);
      }
      else {
	// XXX: For now, avoid this case since the self-update query doesn't
	// check for any update count, i.e. it can't tell which update is
	// the most recent.  Also, the input stream would need to change
	// to a tempStream to allow the re-write
	
	/*
	if (storeSelfUpdate(conn, uniqueId, is, length)) {
	  // The second update is for the rare case where
	  // two threads try to update the database simultaneously
	}
	else {
	  log.fine(L.l("Can't store session {0}", uniqueId));
	}
	*/
      }
    } catch (SQLException e) {
      e.printStackTrace();
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }
  }
  
  /**
   * Stores the cluster object on the local store using an update query.
   *
   * @param conn the database connection
   * @param uniqueId the object's unique id.
   * @param id the input stream to the serialized object
   * @param length the length object the serialized object
   */
  private boolean storeSelfUpdate(ClusterConnection conn, String uniqueId,
				  ReadStream is, int length)
  {
    try {
      PreparedStatement stmt = conn.prepareUpdate();
      stmt.setBinaryStream(1, is, length);

      long now = Alarm.getCurrentTime();
      int nowMinutes = (int) (now / 60000L);
      stmt.setInt(2, nowMinutes);
      stmt.setInt(3, nowMinutes);
      stmt.setString(4, uniqueId);

      int count = stmt.executeUpdate();
        
      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine("update cluster: " + uniqueId + " length:" + length);
	  
	return true;
      }
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return false;
  }
  
  private boolean storeSelfInsert(ClusterConnection conn, String uniqueId,
				  ReadStream is, int length,
				  long expireInterval,
				  int primary, int secondary, int tertiary)
  {
    try {
      PreparedStatement stmt = conn.prepareInsert();
        
      stmt.setString(1, uniqueId);

      stmt.setBinaryStream(2, is, length);
      
      int nowMinutes = (int) (Alarm.getCurrentTime() / 60000L);
      
      stmt.setInt(3, nowMinutes);
      stmt.setInt(4, nowMinutes);
      stmt.setInt(5, (int) (expireInterval / 60000L));

      stmt.setInt(6, primary);
      stmt.setInt(7, secondary);
      stmt.setInt(8, tertiary);

      stmt.executeUpdate();
        
      if (log.isLoggable(Level.FINE))
	log.fine("insert cluster: " + uniqueId + " length:" + length);

      return true;
    } catch (SQLException e) {
      System.out.println(e);
      
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }

  //
  // statistics
  //

  public long getObjectCount()
    throws SQLException
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement stmt = conn.prepareCount();
      
      ResultSet rs = stmt.executeQuery();

      if (rs != null && rs.next()) {
	long value = rs.getLong(1);
	rs.close();
	return value;
      }
      
      return -1;
    } catch (SQLException e) {
      e.printStackTrace();
      log.log(Level.FINE, e.toString(), e);
    } finally {
      conn.close();
    }
    
    return -1;
  }

  public void destroy()
  {
    _dataSource = null;
    _freeConn = null;
  }

  private ClusterConnection getConnection()
    throws SQLException
  {
    ClusterConnection cConn = _freeConn.allocate();

    if (cConn == null) {
      Connection conn = _dataSource.getConnection();
      cConn = new ClusterConnection(conn);
    }

    return cConn;
  }

  public String serverNameToTableName(String serverName)
  {
    if (serverName == null)
      return "srun";
    
    StringBuilder cb = new StringBuilder();
    cb.append("srun_");
    
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

  public String toString()
  {
    return "ClusterStore[" + _name + "]";
  }

  class ClusterConnection {
    private Connection _conn;
    
    private PreparedStatement _loadStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _accessStatement;
    private PreparedStatement _setExpiresStatement;
    private PreparedStatement _invalidateStatement;
    private PreparedStatement _timeoutStatement;
    private PreparedStatement _countStatement;

    ClusterConnection(Connection conn)
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

    PreparedStatement prepareUpdate()
      throws SQLException
    {
      if (_updateStatement == null)
	_updateStatement = _conn.prepareStatement(_updateQuery);

      return _updateStatement;
    }

    PreparedStatement prepareInsert()
      throws SQLException
    {
      if (_insertStatement == null)
	_insertStatement = _conn.prepareStatement(_insertQuery);

      return _insertStatement;
    }

    PreparedStatement prepareAccess()
      throws SQLException
    {
      if (_accessStatement == null)
	_accessStatement = _conn.prepareStatement(_accessQuery);

      return _accessStatement;
    }

    PreparedStatement prepareSetExpireInterval()
      throws SQLException
    {
      if (_setExpiresStatement == null)
	_setExpiresStatement = _conn.prepareStatement(_setExpiresQuery);

      return _setExpiresStatement;
    }

    PreparedStatement prepareInvalidate()
      throws SQLException
    {
      if (_invalidateStatement == null)
	_invalidateStatement = _conn.prepareStatement(_invalidateQuery);

      return _invalidateStatement;
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
      _freeConn.free(this);
    }
  }
}
