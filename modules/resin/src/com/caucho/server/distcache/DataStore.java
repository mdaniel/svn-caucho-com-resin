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

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.index.SqlIndexAlreadyExistsException;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeList;
import com.caucho.util.IoUtil;
import com.caucho.util.JdbcUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;


/**
 * Manages the backing for the file database objects
 */
public class DataStore {
  private static final L10N L = new L10N(DataStore.class);

  private static final Logger log
    = Logger.getLogger(DataStore.class.getName());

  private FreeList<DataConnection> _freeConn
    = new FreeList<DataConnection>(32);

  private final String _dataTableName;
  private final String _mnodeTableName;

  // remove unused data after 15 minutes
  // server/60i0
  // private long _expireTimeout = 60 * 60L * 1000L;
  //private long _expireOrphanTimeout = 24 * 60L * 60L * 1000L;

  //private long _expireOrphanTimeout = 60 * 60L * 1000L;
  //private long _expireOrphanTimeout = 60 * 60L * 1000L;
  private long _expireOrphanTimeout = 60 * 60L * 1000L;

  // data must live for at least 15min because of timing issues during
  // creation. The reaper must not remove data while it's being added
  private long _expireDataMinIdle = 15 * 60L * 1000L;

  private DataSource _dataSource;

  private final String _insertQuery;
  private final String _loadQuery;

  private final String _selectMnodeDataIdQuery;
  private final String _selectDataIdQuery;

  private final String _deleteQuery;
  private final String _deleteOrphanQuery;
  private final String _validateQuery;

  private final String _countQuery;

  private final ConcurrentArrayList<MnodeOrphanListener> _orphanListeners
    = new ConcurrentArrayList<MnodeOrphanListener>(MnodeOrphanListener.class);

  private final AtomicLong _entryCount = new AtomicLong();

  private Alarm _alarm;

  public DataStore(String serverName,
                   MnodeStore mnodeStore)
    throws Exception
  {
    _dataSource = mnodeStore.getDataSource();
    _mnodeTableName = mnodeStore.getTableName();

    _dataTableName = "data";

    if (_dataTableName == null)
      throw new NullPointerException();

    /*
    _loadQuery = ("SELECT data"
                  + " FROM " + _tableName
                  + " WHERE id=?");
                  */
    _loadQuery = ("SELECT data"
                  + " FROM " + _dataTableName
                  + " WHERE id=? AND time=?");

    _insertQuery = ("INSERT into " + _dataTableName
                    + " (data,time) "
                    + "VALUES(?,?)");

    /*
    _selectOrphanQuery = ("SELECT d.id, m.value_data_id"
                              + " FROM " + _dataTableName + " AS d"
                              + " LEFT OUTER JOIN " + _mnodeTableName + " AS m"
                              + " ON(m.value_data_id=d.id)"
                              + " WHERE d.resin_oid>?"
                              + " LIMIT 1024");
                              */
    _selectMnodeDataIdQuery = ("SELECT value_data_id"
                        + " FROM " + _mnodeTableName);

    _selectDataIdQuery = ("SELECT id,time"
                     + " FROM " + _dataTableName
                     + " WHERE time < ?");

    _deleteQuery = ("DELETE FROM " + _dataTableName
                    + " WHERE id = ? and time=?");

    _deleteOrphanQuery = ("DELETE FROM " + _dataTableName
                          + " WHERE id = ? and time=?");

    _validateQuery = ("VALIDATE " + _dataTableName);

    _countQuery = "SELECT count(*) FROM " + _dataTableName;
    
    if (_expireOrphanTimeout < 15 * 60000) {
      log.warning("Short orphan timing for testing " + _expireOrphanTimeout);
    }
  }

  DataSource getDataSource()
  {
    return _dataSource;
  }

  protected void init()
    throws Exception
  {
    initDatabase();

    long count = getCountImpl();

    if (count > 0) {
      _entryCount.set(count);
    }

    _alarm = new Alarm(new DeleteAlarm());
    //_alarm.queue(_expireOrphanTimeout);
    _alarm.queue(60000);

    //_alarm.queue(0);
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

      boolean isOld = false;

      try {
        String sql = ("SELECT expire_time"
                      + " FROM " + _dataTableName + " WHERE 1=0");

        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        rs.close();

        isOld = true;
      } catch (Exception e) {
        log.log(Level.ALL, e.toString(), e);
        log.finest(this + " " + e.toString());
      }

      if (! isOld) {
        try {
          String sql = ("SELECT id, data, time"
              + " FROM " + _dataTableName + " WHERE 1=0");

          ResultSet rs = stmt.executeQuery(sql);
          rs.next();
          rs.close();

          return;
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
          log.finer(this + " " + e.toString());
        }
      }

      try {
        stmt.executeQuery("DROP TABLE " + _dataTableName);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      String sql = ("CREATE TABLE " + _dataTableName + " (\n"
                    + "  id IDENTITY,\n"
                    + "  data BLOB,\n"
                    + "  time BIGINT)");

      log.fine(sql);

      stmt.executeUpdate(sql);
    } finally {
      conn.close();
    }
  }

  public void addOrphanListener(MnodeOrphanListener listener)
  {
    _orphanListeners.add(listener);
  }

  public void removeOrphanListener(MnodeOrphanListener listener)
  {
    _orphanListeners.remove(listener);
  }

  /**
   * Reads the object from the data store.
   *
   * @param id the hash identifier for the data
   * @param os the WriteStream to hold the data
   *
   * @return true on successful load
   */
  public boolean load(long id,
                      long valueDataTime,
                      WriteStream os)
  {
    try {
      Blob blob = loadBlob(id, valueDataTime);

      if (blob != null) {
        InputStream is = blob.getBinaryStream();

        if (is == null)
          return false;

        try {
          os.writeStream(is);
        } finally {
          is.close();
        }

        return true;
      }

      if (log.isLoggable(Level.FINER))
        log.finer(this + " no data loaded for " + Long.toHexString(id));
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
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
  public Blob loadBlob(long id, long valueDataTime)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);
      pstmt.setLong(2, valueDataTime);

      rs = pstmt.executeQuery();

      if (rs.next()) {
        Blob blob = rs.getBlob(1);

        return blob;
      }
      else {
        System.out.println("NODATA: " + Long.toHexString(id) + " " + valueDataTime);
      }

      if (log.isLoggable(Level.FINER))
        log.finer(this + " no blob data loaded for " + id);
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
   * Checks if we have the data
   *
   * @param id the hash identifier for the data
   *
   * @return true on successful load
   */
  public boolean isDataAvailable(long id, long dataTime)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);
      pstmt.setLong(2, dataTime);
      rs = pstmt.executeQuery();

      if (rs.next()) {
        return true;
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

  /**
   * Reads the object from the data store.
   *
   * @param id the hash identifier for the data
   * @param os the WriteStream to hold the data
   *
   * @return true on successful load
   */
  public InputStream openInputStream(long id, long dataTime)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);
      pstmt.setLong(2, dataTime);

      rs = pstmt.executeQuery();

      if (rs.next()) {
        InputStream is = rs.getBinaryStream(1);

        if (is == null) {
          System.err.println(Thread.currentThread().getName() + " MISSING-DATA FOR ID: 0x" + Long.toHexString(id));

          if (log.isLoggable(Level.FINE)) {
            Thread.dumpStack();
          }

          return null;
        }

        InputStream dataInputStream = new DataInputStream(conn, rs, is);
        conn = null;

        return dataInputStream;
      }
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
   * Saves the data, returning true on success.
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  public DataItem save(StreamSource source, int length)
    throws IOException
  {
    return insert(source.openInputStream(), length);
  }

  /**
   * Saves the data, returning true on success.
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  public DataItem save(InputStream is, int length)
    throws IOException
  {
    return insert(is, length);
  }

  /**
   * Stores the data, returning true on success
   *
   * @param id the object's unique id.
   * @param is the input stream to the serialized object
   * @param length the length object the serialized object
   */
  private DataItem insert(InputStream is, int length)
  {
    if (is == null) {
      throw new NullPointerException();
    }

    DataConnection conn = null;
    long now = CurrentTime.getCurrentTime();

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareInsert();
      stmt.setBinaryStream(1, is, length);

      stmt.setLong(2, now);

      int count = stmt.executeUpdate();

      // System.out.println("INSERT: " + id);

      if (count > 0) {
        _entryCount.addAndGet(1);

        ResultSet keys = stmt.getGeneratedKeys();
        if (keys.next()) {
          long id = keys.getLong("id");

          return new DataItem(id, now);
        }

        throw new IllegalStateException();
      }
      else {
        return null;
      }
    } catch (SqlIndexAlreadyExistsException e) {
      // the data already exists in the cache, so this is okay
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);

      return null;
    } catch (SQLException e) {
      e.printStackTrace();
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }

    return null;
  }

  /**
   * Removes the data, returning true on success
   *
   * @param id the data's unique id.
   */
  public boolean remove(long id, long time)
  {
    if (id <= 0) {
      throw new IllegalStateException(L.l("remove of 0 value"));
    }

    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareDelete();
      stmt.setLong(1, id);
      stmt.setLong(2, time);

      int count = stmt.executeUpdate();

      // System.out.println("INSERT: " + id);

      if (count > 0) {
        _entryCount.addAndGet(-1);

        return true;
      }
      else {
        return false;
      }
    } catch (SQLException e) {
      e.printStackTrace();
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }

    return false;
  }

  /*
  private void notifyOrphan(byte []valueHash)
  {
    if (valueHash == null)
      return;

    for (MnodeOrphanListener listener : _orphanListeners) {
      listener.onOrphanValue(new HashKey(valueHash));
    }
  }
  */

  /**
   * Clears the expired data
   */
  public void validateDatabase()
  {
    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareValidate();

      pstmt.executeUpdate();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, e.toString(), e);
      else
        log.warning(this + " " + e);
    } finally {
      if (conn != null)
        conn.close();
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
    DataConnection conn = null;
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

      if (conn != null)
        conn.close();
    }

    return -1;
  }

  public boolean isClosed()
  {
    return _dataSource == null;
  }

  public void destroy()
  {
    _dataSource = null;
    // _freeConn = null;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null) {
      alarm.dequeue();
    }
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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _dataTableName + "]";
  }

  private class DeleteAlarm implements AlarmListener {
    private long _lastOid;

    @Override
    public void handleAlarm(Alarm alarm)
    {
      if (_dataSource != null) {
        try {
          deleteOrphans();
        } finally {
          long timeout;

          if (_lastOid < 0) {
            timeout = _expireOrphanTimeout;
          }
          else {
            timeout = 60000L;
          }

          if (_alarm != null) {
            _alarm.queue(timeout);
          }
        }
      }
    }

    private void deleteOrphans()
    {
      DataConnection conn = null;
      ResultSet rs = null;

      boolean isValid = false;

      try {
        conn = getConnection();

        HashMap<Long,Long> orphanList = selectDataIds(conn);
        HashSet<Long> mnodeDataIds = selectMnodeDataIds(conn);

        for (Long mnodeDataId : mnodeDataIds) {
          orphanList.remove(mnodeDataId);
        }

        PreparedStatement pStmt = conn.prepareDeleteOrphan();
        
        int removeCount = 0;

        for (Map.Entry<Long,Long> entry : orphanList.entrySet()) {
          Long did = entry.getKey();
          Long time = entry.getValue();

          pStmt.setLong(1, did);
          pStmt.setLong(2, time);

          if (pStmt.executeUpdate() > 0) {
            _entryCount.addAndGet(-1);
            removeCount++;
          }
          else {
            //System.out.println("Unable to remove orphan: " + Long.toHexString(did));
          }
        }

        if (orphanList.size() > 0) {
          log.info("DataStore removing " + orphanList.size() + " orphans (remove=" + removeCount + ",entry-count=" + _entryCount.get() + ")");
        }

        isValid = true;
      } catch (SQLException e) {
        e.printStackTrace();
        log.log(Level.FINE, e.toString(), e);
      } catch (Throwable e) {
        e.printStackTrace();
        log.log(Level.FINE, e.toString(), e);
      } finally {
        if (isValid)
          conn.close();
        else
          conn.destroy();
      }
    }

    private HashSet<Long> selectMnodeDataIds(DataConnection conn)
      throws SQLException
    {
      HashSet<Long> dataIds = new HashSet<Long>();

      PreparedStatement pStmt = conn.prepareSelectMnodeDataIds();

      ResultSet rs = pStmt.executeQuery();

      while (rs.next()) {
        dataIds.add(rs.getLong(1));
      }

      return dataIds;
    }

    private HashMap<Long,Long> selectDataIds(DataConnection conn)
      throws SQLException
    {
      HashMap<Long,Long> dataIds = new HashMap<Long,Long>();

      PreparedStatement pStmt = conn.prepareSelectDataIds();
      long time = CurrentTime.getCurrentTime() - _expireDataMinIdle;

      pStmt.setLong(1, time);

      ResultSet rs = pStmt.executeQuery();

      while (rs.next()) {
        dataIds.put(rs.getLong(1), rs.getLong(2));
      }

      return dataIds;
    }
  }

  class DataInputStream extends InputStream {
    private DataConnection _conn;
    private ResultSet _rs;
    private InputStream _is;

    DataInputStream(DataConnection conn, ResultSet rs, InputStream is)
    {
      _conn = conn;
      _rs = rs;
      _is = is;

      if (is == null) {
        throw new NullPointerException();
      }
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

    @Override
    public void close()
    {
      DataConnection conn = _conn;
      _conn = null;

      ResultSet rs = _rs;
      _rs = null;

      InputStream is = _is;
      _is = null;

      IoUtil.close(is);

      JdbcUtil.close(rs);

      if (conn != null)
        conn.close();
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _is + "]";
    }
  }

  private class DataConnection {
    private Connection _conn;

    private PreparedStatement _loadStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _selectMnodeDataIdStatement;
    private PreparedStatement _selectDataIdStatement;
    private PreparedStatement _deleteStatement;
    private PreparedStatement _deleteOrphanStatement;
    private PreparedStatement _validateStatement;

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

    PreparedStatement prepareInsert()
      throws SQLException
    {
      if (_insertStatement == null) {
        _insertStatement = _conn.prepareStatement(_insertQuery,
                                                  Statement.RETURN_GENERATED_KEYS);
      }

      return _insertStatement;
    }

    PreparedStatement prepareSelectMnodeDataIds()
      throws SQLException
    {
      if (_selectMnodeDataIdStatement == null)
        _selectMnodeDataIdStatement = _conn.prepareStatement(_selectMnodeDataIdQuery);

      return _selectMnodeDataIdStatement;
    }

    PreparedStatement prepareSelectDataIds()
      throws SQLException
    {
      if (_selectDataIdStatement == null)
        _selectDataIdStatement = _conn.prepareStatement(_selectDataIdQuery);

      return _selectDataIdStatement;
    }

    PreparedStatement prepareDelete()
      throws SQLException
    {
      if (_deleteStatement == null)
        _deleteStatement = _conn.prepareStatement(_deleteQuery);

      return _deleteStatement;
    }

    PreparedStatement prepareDeleteOrphan()
      throws SQLException
    {
      if (_deleteOrphanStatement == null)
        _deleteOrphanStatement = _conn.prepareStatement(_deleteOrphanQuery);

      return _deleteOrphanStatement;
    }

    PreparedStatement prepareValidate()
      throws SQLException
    {
      if (_validateStatement == null)
        _validateStatement = _conn.prepareStatement(_validateQuery);

      return _validateStatement;
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
      if (_freeConn == null || ! _freeConn.freeCareful(this)) {
        destroy();
      }
    }

    void destroy()
    {
      try {
        _conn.close();
      } catch (SQLException e) {
      }
    }
  }
  
  public static class DataItem {
    private final long _id;
    private final long _time;
    
    DataItem(long id, long time)
    {
      _id = id;
      _time = time;
    }
    
    public long getId()
    {
      return _id;
    }
    
    public long getTime()
    {
      return _time;
    }
  }
}
