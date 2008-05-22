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

package com.caucho.server.cluster;

import com.caucho.config.ConfigException;
import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.util.Alarm;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

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
 * Manages the backing for the file objectStore.
 */
public class FileBacking {
  private static final L10N L = new L10N(FileBacking.class);
  private static final Logger log
    = Logger.getLogger(FileBacking.class.getName());
  
  private FreeList<ClusterConnection> _freeConn
    = new FreeList<ClusterConnection>(32);
  
  private String _name;
  
  private Path _path;

  private DataSource _dataSource;
  
  // version identifier for this instance of the backing. Each new start
  // will increment the version
  private int _version = 1;

  private String _tableName;
  private String _loadQuery;
  private String _loadIfVersionQuery;
  private String _updateQuery;
  private String _updateMetadataQuery;
  private String _accessQuery;
  private String _setExpiresQuery;
  private String _insertQuery;
  private String _invalidateQuery;
  private String _timeoutQuery;
  private String _dumpQuery;
  private String _countQuery;
  
  public FileBacking()
  {
  }
  
  public FileBacking(String name)
  {
    _name = name;
  }

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
    
    _loadQuery = ("SELECT access_time,data_hash,data"
		  + " FROM " + _tableName
		  + " WHERE id=? AND is_valid=1");
    
    _loadIfVersionQuery = ("SELECT access_time,data_hash,data"
			   + " FROM " + _tableName
			   + " WHERE id=? AND is_valid=1 AND version=?");
    
    _insertQuery = ("INSERT into " + _tableName
		    + " (id,store_id,is_valid,data_hash,data,mod_time,access_time,expire_interval,server1,server2,server3,version) "
		    + "VALUES(?,?,1,?,?,?,?,?,?,?,?,?)");
    
    _updateQuery = ("UPDATE " + _tableName
		    + " SET data_hash=?, data=?, mod_time=?, access_time=?,is_valid=1,version=?"
		    + " WHERE id=?");
    
    _updateMetadataQuery = ("UPDATE " + _tableName
		            + " SET is_valid=0"
   		            + " WHERE id=? AND data_hash <> ?");

    _accessQuery = "UPDATE " + _tableName + " SET access_time=? WHERE id=?";
    _setExpiresQuery = "UPDATE " + _tableName + " SET expire_interval=? WHERE id=?";
    _invalidateQuery = "UPDATE " + _tableName + " SET is_valid=0 WHERE id=?";

    // objectAccess window is 1/4 the expire interval
    _timeoutQuery = ("DELETE FROM " + _tableName
		     + " WHERE access_time + 5 * expire_interval / 4 < ?");

    _dumpQuery = ("SELECT id, is_valid, expire_interval, data"
		  + " FROM " + _tableName
		  + " WHERE ? <= mod_time AND "
		  + "   (?=server1 OR ?=server2 OR ?=server3)");
    
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
    
    initVersion();

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
      
      try {
	String sql = ("SELECT expire_interval,store_id,is_valid,version"
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
                    + "  id BINARY(20) PRIMARY KEY,\n"
                    + "  store_id BINARY(20),\n"
                    + "  data_hash BINARY(20),\n"
		    + "  data BLOB,\n"
                    + "  version INTEGER,\n"
		    + "  expire_interval INTEGER,\n"
		    + "  access_time INTEGER,\n"
		    + "  mod_time INTEGER,\n"
		    + "  server1 SMALLINT,\n"
		    + "  server2 SMALLINT,\n" 
		    + "  server3 SMALLINT,\n"
                    + "  is_valid BIT)");


      log.fine(sql);

      stmt.executeUpdate(sql);
    } finally {
      conn.close();
    }
  }

  /**
   * Finds the old version id from the database.
   */
  private void initVersion()
    throws Exception
  {
    Connection conn = _dataSource.getConnection();

    try {
      Statement stmt = conn.createStatement();
      
      try {
	String sql = ("select max(version)" + " from " + _tableName);

	ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
          _version = rs.getInt(1) + 1;
        }
	rs.close();
        
        // rollover
        if (Integer.MAX_VALUE / 2 < _version) {
          sql = "update " + _tableName + " set version=0";
          stmt.executeUpdate(sql);
          
          _version = 1;
        }
        
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
                    + "  id BINARY(20) PRIMARY KEY,\n"
                    + "  store_id BINARY(20),\n"
                    + "  data_hash BINARY(20),\n"
		    + "  data BLOB,\n"
                    + "  version INTEGER,\n"
		    + "  expire_interval INTEGER,\n"
		    + "  access_time INTEGER,\n"
		    + "  mod_time INTEGER,\n"
		    + "  server1 SMALLINT,\n"
		    + "  server2 SMALLINT,\n" 
		    + "  server3 SMALLINT,\n"
                    + "  is_valid BIT)");


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
   * Load the session from the jdbc objectStore.
   *
   * @param session the session to fill.
   *
   * @return true if the loadImpl was valid.
   */
  public boolean loadSelf(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    return loadSelfImpl(clusterObj, obj, false);
  }

  /**
   * Load the session from the jdbc objectStore.
   *
   * @param session the session to fill.
   *
   * @return true if the loadImpl was valid.
   */
  public boolean loadSelfIfVersion(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    return loadSelfImpl(clusterObj, obj, true);
  }

  /**
   * Load the session from the jdbc objectStore.
   *
   * @param session the session to fill.
   *
   * @return true if the loadImpl was valid.
   */
  private boolean loadSelfImpl(ClusterObject clusterObj, Object obj,
			       boolean isVersion)
    throws Exception
  {
    HashKey objectId = clusterObj.getObjectId();

    ClusterConnection conn = getConnection();
    try {
      PreparedStatement stmt;

      if (isVersion) {
	stmt = conn.prepareLoadIfVersion();
	stmt.setBytes(1, objectId.getHash());
	stmt.setInt(2, _version);
      }
      else {
	stmt = conn.prepareLoad();
	stmt.setBytes(1, objectId.getHash());
      }

      ResultSet rs = stmt.executeQuery();
      boolean validLoad = false;

      if (rs.next()) {
	//System.out.println("LOAD: " + uniqueId);
	long accessTime = rs.getInt(1) * 60000L;
	byte []digest = rs.getBytes(2);
        InputStream is = rs.getBinaryStream(3);

        if (log.isLoggable(Level.FINE))
          log.fine(this + " load key=" + objectId);
      
        validLoad = clusterObj.loadImpl(is, obj, digest);

	if (validLoad)
	  clusterObj.setAccessTime(accessTime);

        is.close();
      }
      else if (log.isLoggable(Level.FINE))
        log.fine(this + " load: no local object loaded for " + objectId);
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
   * Updates the object's objectAccess time.
   *
   * @param obj the object to objectStore.
   */
  public void updateAccess(HashKey id)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement stmt = conn.prepareAccess();

      long now = Alarm.getCurrentTime();
      int nowMinutes = (int) (now / 60000L);
      stmt.setInt(1, nowMinutes);
      stmt.setBytes(2, id.getHash());

      int count = stmt.executeUpdate();

      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine(this + " access " + id);
	return;
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Sets the object's expire_interval.
   *
   * @param obj the object to objectStore.
   */
  public void setExpireInterval(HashKey id, long expireInterval)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement stmt = conn.prepareSetExpireInterval();

      int expireMinutes = (int) (expireInterval / 60000L);
      stmt.setInt(1, expireMinutes);
      stmt.setBytes(2, id.getHash());

      int count = stmt.executeUpdate();

      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine(this + " set expire interval " + expireInterval + " for " + id);
	return;
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Removes the named object from the objectStore.
   */
  public void remove(HashKey id)
    throws Exception
  {
    ClusterConnection conn = getConnection();
    
    try {
      PreparedStatement pstmt = conn.prepareInvalidate();
      pstmt.setBytes(1, id.getHash());

      int count = pstmt.executeUpdate();
      
      if (log.isLoggable(Level.FINE))
        log.fine(this + " remove " + id);
    } finally {
      conn.close();
    }
  }

  /**
   * Reads from the objectStore.
   */
  public byte [] read(HashKey id, WriteStream os)
    throws IOException
  {
    Connection conn = null;
    byte []digest = null;
    
    try {
      conn = _dataSource.getConnection();

      PreparedStatement pstmt = conn.prepareStatement(_loadQuery);
      pstmt.setBytes(1, id.getHash());

      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
	long accessTime = rs.getInt(1) * 60000L;
	digest = rs.getBytes(2);
	InputStream is = rs.getBinaryStream(3);

	os.writeStream(is);

	is.close();

	return digest;
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

    return digest;
  }

  /**
   * Stores the cluster object on the local objectStore.
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   * @param expireInterval how long the object lives w/o objectAccess
   */
  public void storeSelf(HashKey id, HashKey storeId,
			InputStream is, int length,
                        byte []dataHash, long expireInterval,
			int primary, int secondary, int tertiary)
  {
    ClusterConnection conn = null;

    try {
      conn = getConnection();
      // Try to updateImpl first, and insert if fail.
      // The binary stream can be reused because it won't actually be
      // read on a failure

      if (storeSelfUpdate(conn, id, is, length, dataHash)) {
      }
      else if (storeSelfInsert(conn, id, storeId, is, length, dataHash,
                               expireInterval, primary, secondary, tertiary)) {
      }
      else {
	// XXX: For now, avoid this case since the self-updateImpl query doesn't
	// check for any updateImpl count, i.e. it can't tell which updateImpl is
	// the most recent.  Also, the input stream would need to objectModified
	// to a tempStream to allow the re-write
	
	/*
	if (storeSelfUpdate(conn, uniqueId, is, length)) {
	  // The second updateImpl is for the rare case where
	  // two threads try to updateImpl the database simultaneously
	}
	else {
	  log.fine(L.l("Can't objectStore session {0}", uniqueId));
	}
	*/
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }
  }
  
  /**
   * Stores the cluster object on the local objectStore using an updateImpl query.
   *
   * @param conn the database connection
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  private boolean storeSelfUpdate(ClusterConnection conn, HashKey id,
				  InputStream is, int length, byte []dataHash)
  {
    try {
      PreparedStatement stmt = conn.prepareUpdate();
      stmt.setBytes(1, dataHash);
      stmt.setBinaryStream(2, is, length);

      long now = Alarm.getCurrentTime();
      int nowMinutes = (int) (now / 60000L);
      stmt.setInt(3, nowMinutes);
      stmt.setInt(4, nowMinutes);
      stmt.setInt(5, _version);
      
      stmt.setBytes(6, id.getHash());

      int count = stmt.executeUpdate();
        
      if (count > 0) {
	if (log.isLoggable(Level.FINE)) 
	  log.fine(this + " save(update) " + id + " length:" + length);
	  
	return true;
      }
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return false;
  }
  
  private boolean storeSelfInsert(ClusterConnection conn, 
                                  HashKey id, HashKey storeId,
				  InputStream is, int length, byte []dataHash,
				  long expireInterval,
				  int primary, int secondary, int tertiary)
  {
    try {
      PreparedStatement stmt = conn.prepareInsert();
        
      stmt.setBytes(1, id.getHash());
      stmt.setBytes(2, storeId.getHash());

      if (dataHash == null)
	throw new NullPointerException();
      
      stmt.setBytes(3, dataHash);
      stmt.setBinaryStream(4, is, length);
      
      int nowMinutes = (int) (Alarm.getCurrentTime() / 60000L);
      
      stmt.setInt(5, nowMinutes);
      stmt.setInt(6, nowMinutes);
      stmt.setInt(7, (int) (expireInterval / 60000L));

      stmt.setInt(8, primary);
      stmt.setInt(9, secondary);
      stmt.setInt(10, tertiary);
      
      stmt.setInt(11, _version);

      stmt.executeUpdate();
        
      if (log.isLoggable(Level.FINE))
	log.fine(this + " save(insert) " + id + " length:" + length);

      return true;
    } catch (SQLException e) {
      System.out.println(e);
      
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }

  /**
   * Invalidates the item if the hash does not match
   *
   * @param id the object's unique id.
   * @param length the length object the serialized object
   * @param expireInterval how long the object lives w/o objectAccess
   */
  public void updateSelf(HashKey id, byte []dataHash, long expireInterval)
  {
    ClusterConnection conn = null;

    try {
      conn = getConnection();
      // Try to updateImpl first, and insert if fail.
      // The binary stream can be reused because it won't actually be
      // read on a failure

      PreparedStatement stmt = conn.prepareUpdateMetadata();
      
      stmt.setBytes(1, id.getHash());
      stmt.setBytes(2, dataHash);

      stmt.executeUpdate();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
	conn.close();
    }
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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _tableName + "]";
  }

  class ClusterConnection {
    private Connection _conn;
    
    private PreparedStatement _accessStatement;
    private PreparedStatement _loadStatement;
    private PreparedStatement _loadIfVersionStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _updateMetadataStatement;
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

    PreparedStatement prepareLoadIfVersion()
      throws SQLException
    {
      if (_loadIfVersionStatement == null)
	_loadIfVersionStatement = _conn.prepareStatement(_loadIfVersionQuery);

      return _loadIfVersionStatement;
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

    PreparedStatement prepareUpdateMetadata()
      throws SQLException
    {
      if (_updateMetadataStatement == null) {
	_updateMetadataStatement
	  = _conn.prepareStatement(_updateMetadataQuery);
      }

      return _updateMetadataStatement;
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
      if (_freeConn != null)
	_freeConn.free(this);
    }
  }
}
