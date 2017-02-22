/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeList;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;
import com.caucho.util.JdbcUtil;
import com.caucho.util.L10N;


/**
 * Manages backing for the cache map.
 */
public class MnodeStore {
  private static final L10N L = new L10N(MnodeStore.class);

  private static final Logger log
    = Logger.getLogger(MnodeStore.class.getName());

  private FreeList<CacheMapConnection> _freeConn
    = new FreeList<CacheMapConnection>(32);

  private final String _serverName;

  private final String _tableName;

  private DataSource _dataSource;
  private boolean _isLocalDataSource;

  private String _loadQuery;

  private String _insertQuery;
  private String _updateSaveQuery;
  private String _updateAccessTimeQuery;

  private String _selectExpireQuery;
  private String _deleteExpireQuery;

  private String _selectCacheKeysQuery;

  private String _countQuery;
  private String _updatesSinceQuery;
  private String _remoteUpdatesSinceQuery;
  private String _deleteQuery;

  private long _serverVersion;
  private long _startupLastAccessTime;

  private final AtomicLong _entryCount = new AtomicLong();

  // private long _expireReaperTimeout = 60 * 60 * 1000L;
  // private long _expireReaperTimeout = 15 * 60 * 1000L;

  public MnodeStore(DataSource dataSource,
                    String tableName,
                    String serverName)
    throws Exception
  {
    _dataSource = dataSource;

    _isLocalDataSource = dataSource instanceof DataSourceImpl;

    _serverName = serverName;
    _tableName = tableName;

    if (dataSource == null)
      throw new NullPointerException();

    if (_tableName == null)
      throw new NullPointerException();

    _dataSource = dataSource;
  }

  /**
   * Returns the data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Returns the data source.
   */
  public String getTableName()
  {
    return _tableName;
  }

  /**
   * Returns the max update time detected on startup.
   */
  public long getStartupLastUpdateTime()
  {
    return _startupLastAccessTime;
  }

  /**
   * Returns the max update time detected on startup.
   */
  public long getStartupLastUpdateTime(HashKey cacheKey)
  {
    Connection conn = null;
    ResultSet rs = null;

    try {
      conn = _dataSource.getConnection();

      String sql = ("SELECT MAX(update_time)"
                    + " FROM " + _tableName
                    + " WHERE cache_id=?");

      PreparedStatement pStmt = conn.prepareStatement(sql);

      pStmt.setBytes(1, cacheKey.getHash());

      rs = pStmt.executeQuery(sql);

      if (rs.next()) {
        return rs.getLong(1);
      }

      return 0;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return 0;
    } finally {
      JdbcUtil.close(rs);
      JdbcUtil.close(conn);
    }
  }

  //
  // lifecycle
  //

  protected void init()
    throws Exception
  {
    _loadQuery = ("SELECT value_hash,value_data_id,value_data_time,value_length,"
                  + "     cache_id,flags,"
                  + "     item_version,server_version,"
                  + "     access_timeout,modified_timeout,"
                  + "     access_time,modified_time"
                  + " FROM " + _tableName
                  + " WHERE id=?");

    _insertQuery = ("INSERT into " + _tableName
                    + " (id,value_hash,value_data_id,value_data_time,value_length,cache_id,flags,"
                    + "  item_version,server_version,"
                    + "  access_timeout,modified_timeout,"
                    + "  access_time,modified_time)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");

    _updateSaveQuery
      = ("UPDATE " + _tableName
         + " SET value_hash=?,value_data_id=?,value_data_time=?,value_length=?,cache_id=?,flags=?,"
         + "     item_version=?,server_version=?,"
         + "     access_timeout=?,modified_timeout=?,"
         + "     access_time=?,modified_time=?"
         + " WHERE id=? AND item_version<=?");

    _updateAccessTimeQuery
      = ("UPDATE " + _tableName
         + " SET access_timeout=?,access_time=?"
         + " WHERE id=? AND item_version=?");

    /*
    _selectExpireQuery = ("SELECT resin_oid,id,cache_id,value_data_id FROM " + _tableName
                          + " WHERE ? < resin_oid"
                          + " AND (access_time + 1.25 * access_timeout < ?"
                          + "      OR modified_time + modified_timeout < ?)"
                          + " LIMIT 4096");
    */

    /*
    _selectExpireQuery = ("SELECT resin_oid,id,cache_id,value_data_id,"
                          + "     access_time,access_timeout,"
                          + "     modified_time,modified_timeout"
                          + " FROM " + _tableName
                          + " WHERE ? < resin_oid"
                          + " LIMIT 4096");
                          */
    _selectExpireQuery = ("SELECT resin_oid,id,cache_id,value_data_id,value_data_time,"
                          + "     access_time,access_timeout,"
                          + "     modified_time,modified_timeout"
                          + " FROM " + _tableName
                          + " WHERE (access_time + 1.25 * access_timeout < ?"
                          + "        OR modified_time + 1.25 * modified_timeout < ?)");
    
    _deleteExpireQuery = ("DELETE FROM " + _tableName
                          + " WHERE (access_time + 5 * access_timeout < ?"
                          + "        OR modified_time + 5 * modified_timeout < ?)");

    _selectCacheKeysQuery = ("SELECT resin_oid,id FROM " + _tableName
                             + " WHERE cache_id=? AND ? < resin_oid"
                             + " LIMIT 4096");

    _deleteQuery = ("DELETE FROM " + _tableName
                    + " WHERE id=?");

    _countQuery = "SELECT count(*) FROM " + _tableName;

    _updatesSinceQuery = ("SELECT id,value_hash,value_data_id,value_data_time,value_length,"
                          + "cache_id,flags,item_version,"
                          + "     access_timeout,modified_timeout,"
                          + "     access_time,modified_time"
                          + " FROM " + _tableName
                          + " WHERE ? <= access_time"
                          + "   AND bitand(flags, " + CacheConfig.FLAG_TRIPLICATE + ") <> 0"
                          + " LIMIT 1024");

    _remoteUpdatesSinceQuery = ("SELECT id,value_hash,value_data_id,value_data_time,value_length,"
                                + "     cache_id,flags,item_version,"
                                + "     access_timeout,modified_timeout,"
                                +"      access_time,modified_time"
                                + " FROM " + _tableName
                                + " WHERE ? = cache_id AND ? <= access_time"
                                + " LIMIT 1024");

    initDatabase();

    _serverVersion = initVersion();
    _startupLastAccessTime = initLastAccessTime();

    long initCount = getCountImpl();

    if (initCount > 0) {
      _entryCount.set(initCount);
    }
  }

  /**
   * Create the database, initializing if necessary.
   */
  protected void initDatabase()
    throws Exception
  {
    Connection conn = _dataSource.getConnection();

    try {
      Statement stmt = conn.createStatement();

      try {
        String sql = ("SELECT id, value_hash, value_data_id, value_data_time, value_length, cache_id, flags,"
                      + "     access_timeout, modified_timeout,"
                      + "     access_time, modified_time"
                      + "     server_version, item_version"
                      + " FROM " + _tableName + " WHERE 1=0");

        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        rs.close();

        return;
      } catch (Exception e) {
        log.log(Level.ALL, e.toString(), e);
        log.finer(this + " " + e.toString());
      }

      try {
        stmt.executeQuery("DROP TABLE " + _tableName);
      } catch (Exception e) {
        log.log(Level.ALL, e.toString(), e);
        log.finer(this + " " + e.toString());
      }

      String sql = ("CREATE TABLE " + _tableName + " (\n"
                    + "  id BINARY(32) PRIMARY KEY,\n"
                    + "  cache_id BINARY(32),\n"
                    + "  value_hash BIGINT,\n"
                    + "  value_data_id BIGINT,\n"
                    + "  value_data_time BIGINT,\n"
                    + "  value_length BIGINT,\n"
                    + "  access_timeout BIGINT,\n"
                    + "  modified_timeout BIGINT,\n"
                    + "  access_time BIGINT,\n"
                    + "  modified_time BIGINT,\n"
                    + "  item_version BIGINT,\n"
                    + "  flags BIGINT,\n"
                    + "  server_version INTEGER)");

      log.fine(sql);

      stmt.executeUpdate(sql);
    } finally {
      conn.close();
    }
  }

  /**
   * Returns the version
   */
  private int initVersion()
    throws Exception
  {
    Connection conn = _dataSource.getConnection();
    ResultSet rs = null;

    try {
      Statement stmt = conn.createStatement();

      String sql = ("SELECT MAX(server_version)"
                    + " FROM " + _tableName);

      rs = stmt.executeQuery(sql);

      if (rs.next())
        return rs.getInt(1) + 1;
    } finally {
      JdbcUtil.close(rs);
      conn.close();
    }

    return 1;
  }

  /**
   * Returns the maximum update time on startup
   */
  private long initLastAccessTime()
    throws Exception
  {
    Connection conn = _dataSource.getConnection();
    ResultSet rs = null;

    try {
      Statement stmt = conn.createStatement();

      String sql = ("SELECT MAX(access_time)"
                    + " FROM " + _tableName);

      rs = stmt.executeQuery(sql);
      if (rs.next())
        return rs.getLong(1);
    } finally {
      JdbcUtil.close(rs);

      conn.close();
    }

    return 0;
  }

  public void close()
  {
  }

  /**
   * Returns the maximum update time on startup
   */
  public ArrayList<CacheData> getUpdates(long updateTime,
                                          int offset)
  {
    Connection conn = null;
    ResultSet rs = null;

    try {
      conn = _dataSource.getConnection();

      String sql;

      sql = _updatesSinceQuery;
      /*
      sql = ("SELECT id,value,flags,item_version,update_time"
             + " FROM " + _tableName
             + " WHERE ?<=update_time"
             + " LIMIT 1024");
      */

      PreparedStatement pstmt = conn.prepareStatement(sql);

      pstmt.setLong(1, updateTime);

      ArrayList<CacheData> entryList = new ArrayList<CacheData>();

      rs = pstmt.executeQuery();

      rs.relative(offset);
      while (rs.next()) {
        byte []keyHash = rs.getBytes(1);
        long valueHash = rs.getLong(2);
        long valueDataId = rs.getLong(3);
        long valueDataTime = rs.getLong(4);
        long valueLength = rs.getLong(5);
        byte []cacheHash = rs.getBytes(6);
        long flags = rs.getLong(7);
        long version = rs.getLong(8);
        long accessTimeout = rs.getLong(9);
        long modifiedTimeout = rs.getLong(10);
        long accessTime = rs.getLong(11);
        long modifiedTime = rs.getLong(12);

        long leaseTimeout = 30000;

        HashKey cacheKey = cacheHash != null ? new HashKey(cacheHash) : null;

        if (keyHash == null)
          continue;

        entryList.add(new CacheData(new HashKey(keyHash),
                                    cacheKey,
                                    valueHash, valueDataId, valueDataTime, valueLength, version,
                                    flags,
                                    accessTimeout,
                                    modifiedTimeout,
                                    leaseTimeout,
                                    accessTime,
                                    modifiedTime));
      }

      if (entryList.size() > 0)
        return entryList;
      else
        return null;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);
      JdbcUtil.close(conn);
    }

    return null;
  }

  /**
   * Returns the maximum update time on startup
   */
  public ArrayList<CacheData> getUpdates(HashKey cacheKey,
                                         long updateTime,
                                         int offset)
  {
    Connection conn = null;
    ResultSet rs = null;

    try {
      conn = _dataSource.getConnection();

      String sql;

      sql = _remoteUpdatesSinceQuery;

      PreparedStatement pstmt = conn.prepareStatement(sql);

      pstmt.setBytes(1, cacheKey.getHash());
      pstmt.setLong(2, updateTime);

      ArrayList<CacheData> entryList = new ArrayList<CacheData>();

      rs = pstmt.executeQuery();

      rs.relative(offset);
      while (rs.next()) {
        byte []keyHash = rs.getBytes(1);

        long valueHash = rs.getLong(2);
        long valueIndex = rs.getLong(3);
        long valueDataTime = rs.getLong(4);
        long valueLength = rs.getLong(5);
        byte []cacheHash = rs.getBytes(6);
        long flags = rs.getLong(7);
        long version = rs.getLong(8);
        long accessTimeout = rs.getLong(9);
        long modifiedTimeout = rs.getLong(10);
        long accessTime = rs.getLong(11);
        long modifiedTime = rs.getLong(12);

        long leaseTimeout = 30000;

        /*
        HashKey cacheKey = cacheHash != null ? new HashKey(cacheHash) : null;
        */

        if (keyHash == null)
          continue;

        entryList.add(new CacheData(new HashKey(keyHash),
                                    HashKey.create(cacheHash),
                                    valueHash,
                                    valueIndex,
                                    valueDataTime,
                                    valueLength,
                                    version,
                                    flags,
                                    accessTimeout,
                                    modifiedTimeout,
                                    leaseTimeout,
                                    accessTime,
                                    modifiedTime));
      }

      if (entryList.size() > 0)
        return entryList;
      else
        return null;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);
      JdbcUtil.close(conn);
    }

    return null;
  }

  /**
   * Reads the object from the data store.
   *
   * @param id the hash identifier for the data
   * @return true on successful load
   */
  public MnodeEntry load(HashKey id)
  {
    CacheMapConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      pstmt.setBytes(1, id.getHash());

      rs = pstmt.executeQuery();

      if (rs.next()) {
        long valueHash = rs.getLong(1);
        long valueDataId = rs.getLong(2);
        long valueDataTime = rs.getLong(3);
        long valueLength = rs.getLong(4);

        byte []cacheHash = rs.getBytes(5);
        long flags = rs.getLong(6);
        long itemVersion = rs.getLong(7);
        long serverVersion = rs.getLong(8);
        long accessedExpireTimeout = rs.getLong(9);
        long modifiedExpireTimeout = rs.getLong(10);
        long accessTime = rs.getLong(11);
        long modifiedTime = rs.getLong(12);
        // long accessTime = CurrentTime.getCurrentTime();

        long leaseTimeout = 300000;

        MnodeEntry entry;
        entry = new MnodeEntry(valueHash, valueLength,
                               itemVersion,
                               flags,
                               accessedExpireTimeout, modifiedExpireTimeout,
                               leaseTimeout,
                               valueDataId, valueDataTime,
                               null,
                               accessTime, modifiedTime,
                               serverVersion == _serverVersion,
                               false);

        if (log.isLoggable(Level.FINER))
          log.finer(this + " load " + id + " " + entry);

        return entry;
      }

      if (log.isLoggable(Level.FINEST))
        log.finest(this + " load: no mnode for cache key " + id);

      return null;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);

      if (conn != null)
        conn.close();
    }

    return null;
  }

  /**
   * Returns the maximum update time on startup
   */
  private boolean selectCacheKeys(KeysIterator iter,
                                  HashKey cacheKey,
                                  long oid)
  {
    Connection conn = null;
    ResultSet rs = null;

    try {
      conn = _dataSource.getConnection();

      String sql;

      sql = _selectCacheKeysQuery;

      PreparedStatement pstmt = conn.prepareStatement(sql);

      pstmt.setBytes(1, cacheKey.getHash());
      pstmt.setLong(2, oid);

      rs = pstmt.executeQuery();

      boolean isValue = false;

      while (rs.next()) {
        long newOid = rs.getLong(1);
        byte []keyHash = rs.getBytes(2);

        iter.addKey(newOid, keyHash);

        isValue = true;
      }

      return isValue;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);
      JdbcUtil.close(conn);
    }

    return false;
  }

  /**
   * Stores the data, returning true on success
   *
   * @param id the key hash
   * @param value the value hash
   * @param idleTimeout the item's timeout
   */
  public boolean insert(HashKey id,
                        HashKey cacheKey,
                        MnodeValue mnodeUpdate,
                        long valueDataId,
                        long valueDataTime,
                        long lastAccessTime,
                        long lastModifiedTime)
  {
    if ((valueDataId == 0) != (mnodeUpdate.getValueHash() == 0)) {
      throw new IllegalStateException(L.l("data {0} vs hash {1} mismatch for cache mnode {2}",
                                          valueDataId,
                                          mnodeUpdate.getValueHash(),
                                          id));
    }
    CacheMapConnection conn = null;

    try {
      conn = getConnection();
      
      PreparedStatement stmt = conn.prepareInsert();
      stmt.setBytes(1, id.getHash());
      stmt.setLong(2, mnodeUpdate.getValueHash());
      /*
      System.out.println("V-HASH: " + mnodeUpdate.getValueDataId());
      if (mnodeUpdate.getValueDataId() == 16384)
        Thread.dumpStack();
        */
      stmt.setLong(3, valueDataId);
      stmt.setLong(4, valueDataTime);
      stmt.setLong(5, mnodeUpdate.getValueLength());
      stmt.setBytes(6, cacheKey.getHash());

      stmt.setLong(7, mnodeUpdate.getFlags());
      stmt.setLong(8, mnodeUpdate.getVersion());
      stmt.setLong(9, _serverVersion);
      stmt.setLong(10, mnodeUpdate.getAccessedExpireTimeout());
      stmt.setLong(11, mnodeUpdate.getModifiedExpireTimeout());

      long now = CurrentTime.getCurrentTime();
      stmt.setLong(12, lastAccessTime);
      stmt.setLong(13, lastModifiedTime);
      
      int count = stmt.executeUpdate();

      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " insert key=" + id + " " + mnodeUpdate + " count=" + count);
      }

      if (count > 0) {
        _entryCount.addAndGet(1);
      }
      else {
        System.out.println("INS_FAILED: " + stmt);
      }

      return true;
    } catch (SQLException e) {
      //e.printStackTrace();
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }

    return false;
  }

  /**
   * Stores the data, returning true on success
   *
   * @param id the key hash
   * @param value the value hash
   * @param idleTimeout the item's timeout
   */
  public boolean updateSave(byte []key,
                            byte []cacheHash,
                            MnodeValue mnodeUpdate,
                            long valueDataId,
                            long valueDataTime,
                            long lastAccessTime,
                            long lastModifiedTime)
  {
    CacheMapConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareUpdateSave();
      
      stmt.setLong(1, mnodeUpdate.getValueHash());
      stmt.setLong(2, valueDataId);
      stmt.setLong(3, valueDataTime);
      stmt.setLong(4, mnodeUpdate.getValueLength());
      stmt.setBytes(5, cacheHash);
      stmt.setLong(6, mnodeUpdate.getFlags());

      stmt.setLong(7, mnodeUpdate.getVersion());
      stmt.setLong(8, _serverVersion);
      stmt.setLong(9, mnodeUpdate.getAccessedExpireTimeout());
      stmt.setLong(10, mnodeUpdate.getModifiedExpireTimeout());


      stmt.setLong(11, lastAccessTime);
      stmt.setLong(12, lastModifiedTime);
      /*
      + " SET value_hash=?,value_data_id=?,value_length=?,cache_id=?,flags=?,"
      + "     item_version=?,server_version=?,"
      + "     access_timeout=?,update_timeout=?,update_time=?"
      */

      stmt.setBytes(13, key);
      stmt.setLong(14, mnodeUpdate.getVersion());
      
      int count = stmt.executeUpdate();

      if (log.isLoggable(Level.FINER)) {
        if (count > 0)
          log.finer(this + " updateSave " + HashKey.create(key) + " " + mnodeUpdate);
        else
          log.finer(this + " updateSave-failed " + HashKey.create(key) + " " + mnodeUpdate);
      }

      return count > 0;
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }

    return false;
  }

  /**
   * Updates the update time, returning true on success
   *
   * @param id the key hash
   * @param itemVersion the value version
   * @param idleTimeout the item's timeout
   * @param updateTime the item's timeout
   */
  public boolean updateAccessTime(HashKey id,
                                  long itemVersion,
                                  long accessTimeout,
                                  long accessTime)
  {
    CacheMapConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.preparedUpdateAccessTime();
      stmt.setLong(1, accessTimeout);
      stmt.setLong(2, accessTime);

      stmt.setBytes(3, id.getHash());
      stmt.setLong(4, itemVersion);

      int count = stmt.executeUpdate();

      if (log.isLoggable(Level.FINER))
        log.finer(this + " updateUpdateTime key=" + id);

      return count > 0;
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
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
   * @return true on successful load
   */
  public boolean remove(byte []key)
  {
    CacheMapConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareDelete();
      pstmt.setBytes(1, key);

      int count = pstmt.executeUpdate();

      if (count > 0) {
        _entryCount.addAndGet(-1);

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " remove " + HashKey.create(key));
        }

        return true;
      }
      else {
        return false;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);

      if (conn != null)
        conn.close();
    }

    return false;
  }

  public Iterator<HashKey> getKeys(HashKey cacheKey)
  {
    return new KeysIterator(cacheKey);
  }
  
  public ExpiredState createExpiredState()
  {
    return new ExpiredState();
  }

  public class ExpiredState {
    private long _lastOid;

    /**
     * Clears the expired data
     */
    public ArrayList<Mnode> selectExpiredData()
    {
      ArrayList<Mnode> expireList = new ArrayList<Mnode>();

      CacheMapConnection conn = null;

      try {
        conn = getConnection();
        PreparedStatement pstmt = conn.prepareSelectExpire();

        long now = CurrentTime.getCurrentTime();

        //pstmt.setLong(1, _lastOid);
        //pstmt.setLong(2, now);
        //pstmt.setLong(3, now);
        
        pstmt.setLong(1, now);
        pstmt.setLong(2, now);

        ResultSet rs = pstmt.executeQuery();
        
        boolean isValue = false;

        while (rs.next()) {
          isValue = true;

          long oid = rs.getLong(1);
          byte []key = rs.getBytes(2);
          byte []cacheHash = rs.getBytes(3);
          long dataId = rs.getLong(4);
          long dataTime = rs.getLong(5);

          long accessTime = rs.getLong(6);
          long accessTimeout = rs.getLong(7);

          long modifiedTime = rs.getLong(8);
          long modifiedTimeout = rs.getLong(9);
          
          _lastOid = Math.max(_lastOid, oid);

          if (accessTime + 1.25 * accessTimeout < now
              || modifiedTime + modifiedTimeout < now) {
            expireList.add(new ExpiredMnode(oid, key, cacheHash, dataId, dataTime));
          }
        }
        
        if (! isValue) {
          _lastOid = 0;
        }
      } catch (Exception e) {
        _lastOid = 0;
        e.printStackTrace();
        log.log(Level.FINE, e.toString(), e);
      } finally {
        conn.close();
      }

      return expireList;
    }

    /**
     * Clears the expired data
     */
    public int removeExpiredData()
    {
      CacheMapConnection conn = null;

      try {
        conn = getConnection();
        PreparedStatement pstmt = conn.prepareDeleteExpire();

        long now = CurrentTime.getCurrentTime();

        //pstmt.setLong(1, _lastOid);
        //pstmt.setLong(2, now);
        //pstmt.setLong(3, now);

        pstmt.setLong(1, now);
        pstmt.setLong(2, now);

        int count = pstmt.executeUpdate();

        return count;
      } catch (Exception e) {
        e.printStackTrace();
        log.log(Level.FINE, e.toString(), e);
        
        return 0;
      } finally {
        conn.close();
      }
    }
  }      
  //
  // statistics
  //

  public long getCount()
  {
    return _entryCount.get();
  }

  private long getCountImpl()
  {
    CacheMapConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();
      PreparedStatement stmt = conn.prepareCount();

      rs = stmt.executeQuery();

      if (rs != null && rs.next()) {
        long value = rs.getLong(1);

        rs.close();

        return value;
      }

      return -1;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      JdbcUtil.close(rs);

      conn.close();
    }

    return -1;
  }

  public void destroy()
  {
    _dataSource = null;
    _freeConn = null;
  }

  private CacheMapConnection getConnection()
    throws SQLException
  {
    CacheMapConnection cConn = _freeConn.allocate();

    if (cConn == null) {
      Connection conn = _dataSource.getConnection();
      cConn = new CacheMapConnection(conn);
    }

    return cConn;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _serverName + "]";
  }

  private class CacheMapConnection {
    private Connection _conn;

    private PreparedStatement _loadStatement;

    private PreparedStatement _insertStatement;
    private PreparedStatement _updateSaveStatement;
    private PreparedStatement _updateAccessTimeStatement;

    private PreparedStatement _selectExpireStatement;
    private PreparedStatement _deleteExpireStatement;
    private PreparedStatement _deleteStatement;

    private PreparedStatement _countStatement;

    CacheMapConnection(Connection conn)
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

    PreparedStatement prepareInsert()
      throws SQLException
    {
      if (_insertStatement == null)
        _insertStatement = _conn.prepareStatement(_insertQuery);

      return _insertStatement;
    }

    PreparedStatement prepareUpdateSave()
      throws SQLException
    {
      if (_updateSaveStatement == null)
        _updateSaveStatement = _conn.prepareStatement(_updateSaveQuery);

      return _updateSaveStatement;
    }

    PreparedStatement preparedUpdateAccessTime()
      throws SQLException
    {
      if (_updateAccessTimeStatement == null) {
        _updateAccessTimeStatement
          = _conn.prepareStatement(_updateAccessTimeQuery);
      }

      return _updateAccessTimeStatement;
    }

    PreparedStatement prepareSelectExpire()
      throws SQLException
    {
      if (_selectExpireStatement == null)
        _selectExpireStatement = _conn.prepareStatement(_selectExpireQuery);

      return _selectExpireStatement;
    }

    PreparedStatement prepareDeleteExpire()
      throws SQLException
    {
      if (_deleteExpireStatement == null)
        _deleteExpireStatement = _conn.prepareStatement(_deleteExpireQuery);

      return _deleteExpireStatement;
    }

    PreparedStatement prepareDelete()
      throws SQLException
    {
      if (_deleteStatement == null)
        _deleteStatement = _conn.prepareStatement(_deleteQuery);

      return _deleteStatement;
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
      if (! _isLocalDataSource
          || _freeConn == null
          || ! _freeConn.freeCareful(this)) {
        try {
          _conn.close();
        } catch (SQLException e) {
        }
      }
    }
  }

  public static class Mnode {
    private final long _oid;

    Mnode(long oid)
    {
      _oid = oid;
    }

    public final long getOid()
    {
      return _oid;
    }
  }

  public static final class ExpiredMnode extends Mnode {
    private final byte []_key;
    private final byte []_cacheHash;
    private final long _dataId;
    private final long _dataTime;

    ExpiredMnode(long oid,
                 byte []key,
                 byte []cacheHash,
                 long dataId,
                 long dataTime)
    {
      super(oid);
      _key = key;
      _cacheHash = cacheHash;
      _dataId = dataId;
      _dataTime = dataTime;
    }

    public final byte []getKey()
    {
      return _key;
    }

    public final byte []getCacheHash()
    {
      return _cacheHash;
    }

    public final long getDataId()
    {
      return _dataId;
    }

    public final long getDataTime()
    {
      return _dataTime;
    }

    public String toString()
    {
      return (getClass().getSimpleName()
          + "[" + Hex.toHex(_key, 0, 4)
          + "," + Long.toHexString(_dataId) + "]");
    }
  }

  class KeysIterator implements Iterator<HashKey> {
    private HashKey _cacheKey;

    private long _startOid;
    private ArrayList<HashKey> _keys = new ArrayList<HashKey>();
    private boolean _isClosed;

    KeysIterator(HashKey cacheKey)
    {
      _cacheKey = cacheKey;
    }

    @Override
    public boolean hasNext()
    {
      if (_keys.size() == 0) {
        loadKeys();
      }

      return _keys.size() > 0;
    }

    @Override
    public HashKey next()
    {
      if (_keys.size() == 0) {
        loadKeys();
      }

      if (_keys.size() == 0) {
        return null;
      }

      return _keys.remove(0);
    }

    void addKey(long newOid, byte[] keyHash)
    {
      _startOid = Math.max(_startOid, newOid);
      _keys.add(HashKey.create(keyHash));
    }

    private void loadKeys()
    {
      if (! _isClosed) {
        if (! selectCacheKeys(this, _cacheKey, _startOid)) {
          _isClosed = true;
        }
      }
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
}
