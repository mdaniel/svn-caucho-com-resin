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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.caucho.db.index.SqlIndexAlreadyExistsException;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.FreeList;
import com.caucho.util.IoUtil;
import com.caucho.util.JdbcUtil;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;


/**
 * Manages the backing for the file database objects
 */
public class DataStore {
  private static final Logger log
    = Logger.getLogger(DataStore.class.getName());

  private FreeList<DataConnection> _freeConn
    = new FreeList<DataConnection>(32);

  private final String _tableName;
  private final String _mnodeTableName;

  // remove unused data after 15 minutes
  // server/60i0
  // private long _expireTimeout = 60 * 60L * 1000L;
  // private long _expireTimeout = 60 * 60L * 1000L;

  private DataSource _dataSource;

  private final String _insertQuery;
  private final String _loadQuery;
  private final String _dataAvailableQuery;
  // private final String _updateExpiresQuery;
  // private final String _updateAllExpiresQuery;
  private final String _selectOrphanQuery;
  private final String _deleteQuery;
  // private final String _deleteTimeoutQuery;
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

    _tableName = "data";

    if (_tableName == null)
      throw new NullPointerException();

    /*
    _loadQuery = ("SELECT data"
                  + " FROM " + _tableName
                  + " WHERE id=?");
                  */
    _loadQuery = ("SELECT data"
                  + " FROM " + _tableName
                  + " WHERE id=?");

    _dataAvailableQuery = ("SELECT 1"
                           + " FROM " + _tableName
                           + " WHERE id=?");

    _insertQuery = ("INSERT into " + _tableName
                    + " (data) "
                    + "VALUES(?)");

    /*
    // XXX: add random component to expire time?
    _updateExpiresQuery = ("UPDATE " + _tableName
                           + " SET expire_time=?"
                           + " WHERE id=?");
                           */

    /*
    _updateAllExpiresQuery = ("SELECT d.expire_time, d.resin_oid, m.value"
                              + " FROM " + _mnodeTableName + " AS m,"
                              + "  " + _tableName + " AS d"
                              + " WHERE m.value = d.id");
                              */
    /*
    _updateAllExpiresQuery = ("SELECT d.expire_time, d.resin_oid, m.value"
                              + " FROM " + _mnodeTableName + " AS m"
                              + " LEFT JOIN " + _tableName + " AS d"
                              + " ON(m.value = d.id)");
                              */

    _selectOrphanQuery = ("SELECT m.value_data_id, d.id"
                              + " FROM " + _mnodeTableName + " AS m"
                              + " LEFT JOIN " + _tableName + " AS d"
                              + " ON(m.value_data_id=d.id)");
    /*
    _deleteTimeoutQuery = ("DELETE FROM " + _tableName
                           + " WHERE expire_time < ?");
                           */
    _deleteQuery = ("DELETE FROM " + _tableName
                    + " WHERE id = ?");

    _validateQuery = ("VALIDATE " + _tableName);

    _countQuery = "SELECT count(*) FROM " + _tableName;
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
    // _alarm.queue(_expireTimeout);

    _alarm.queue(0);
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
                      + " FROM " + _tableName + " WHERE 1=0");

        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        rs.close();
        
        isOld = true;
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
        log.finer(this + " " + e.toString());
      }

      if (! isOld) {
        try {
          String sql = ("SELECT id, data"
              + " FROM " + _tableName + " WHERE 1=0");

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
        stmt.executeQuery("DROP TABLE " + _tableName);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      String sql = ("CREATE TABLE " + _tableName + " (\n"
                    + "  id IDENTITY,\n"
                    + "  data BLOB)");


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
  public boolean load(long id, WriteStream os)
  {
    try {
      Blob blob = loadBlob(id);

      if (blob != null) {
        InputStream is = blob.getBinaryStream();

        if (is == null)
          return false;
        
        long startPosition = os.getPosition();
        
        try {
          os.writeStream(is);
        } finally {
          is.close();
        }

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " load " + id
                    + " length:" + (os.getPosition() - startPosition));
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
  public Blob loadBlob(long id)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);

      rs = pstmt.executeQuery();

      if (rs.next()) {
        Blob blob = rs.getBlob(1);
        
        return blob;
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
  public boolean isDataAvailable(long id)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);
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
  public InputStream openInputStream(long id)
  {
    DataConnection conn = null;
    ResultSet rs = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareLoad();
      // pstmt.setBytes(1, id.getHash());
      pstmt.setLong(1, id);

      rs = pstmt.executeQuery();

      if (rs.next()) {
        InputStream is = rs.getBinaryStream(1);
        
        if (is == null) {
          System.out.println("ID: " + id);
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
  public long save(StreamSource source, int length)
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
  public long save(InputStream is, int length)
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
  private long insert(InputStream is, int length)
  {
    if (is == null) {
      throw new NullPointerException();
    }
    
    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareInsert();
      stmt.setBinaryStream(1, is, length);
      
      int count = stmt.executeUpdate();


      // System.out.println("INSERT: " + id);
      
      if (count > 0) {
        _entryCount.addAndGet(1);
        
        ResultSet keys = stmt.getGeneratedKeys();
        if (keys.next()) {
          long id = keys.getLong("id");
          
          // System.out.println("INDEX: " + dataIndex);
          if (log.isLoggable(Level.FINER)) {
            log.finer(this + " insert " + Long.toHexString(id)
                      + " length:" + length);
          }
          
          return id;
        }
        
        throw new IllegalStateException();
      }
      else {
        return 0;
      }
    } catch (SqlIndexAlreadyExistsException e) {
      // the data already exists in the cache, so this is okay
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);

      System.out.println("EXISTS:");
      return 1;
    } catch (SQLException e) {
      e.printStackTrace();
      log.finer(this + " " + e.toString());
      log.log(Level.FINEST, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }

    return 0;
  }

  /**
   * Removes the data, returning true on success
   *
   * @param id the data's unique id.
   */
  public boolean remove(long id)
  {
    if (id <= 0) {
      throw new IllegalStateException();
    }

    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement stmt = conn.prepareDelete();
      stmt.setLong(1, id);
      
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

  /**
   * Clears the expired data
   */
  /*
  public void removeExpiredData()
  {
    validateDatabase();

    long now = CurrentTime.getCurrentTime();

    updateExpire(now);
    
    // selectOrphans();

    DataConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareDeleteTimeout();

      pstmt.setLong(1, now);

      int count = pstmt.executeUpdate();

      if (count > 0) {
        log.finer(this + " expired " + count + " old data");
      
        _entryCount.addAndGet(-count);
      }

      // System.out.println(this + " EXPIRE: " + count);
    } catch (SQLException e) {
      e.printStackTrace();
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }
  }
  */

  /**
   * Update used expire times.
   */
  private void deleteOrphans()
  {
    DataConnection conn = null;
    ResultSet rs = null;

    boolean isValid = false;
    
    try {
      conn = getConnection();

      PreparedStatement pstmt = conn.prepareSelectOrphan();

      rs = pstmt.executeQuery();

      try {
        while (rs.next()) {
          long id= 0;
          long oid = rs.getLong(2);
          
          if (oid <= 0) {
            if (log.isLoggable(Level.FINER)) {
              log.finer(this + " delete orphan " + Long.toHexString(id));
            }
            
            rs.deleteRow();
          }
        }
      } finally {
        rs.close();
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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _tableName + "]";
  }

  class DeleteAlarm implements AlarmListener {
    public void handleAlarm(Alarm alarm)
    {
      if (_dataSource != null) {
        deleteOrphans();
      }
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
  }

  class DataConnection {
    private Connection _conn;

    private PreparedStatement _loadStatement;
    private PreparedStatement _dataAvailableStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _updateAllExpiresStatement;
    private PreparedStatement _selectOrphanStatement;
    private PreparedStatement _updateExpiresStatement;
    private PreparedStatement _deleteTimeoutStatement;
    private PreparedStatement _deleteStatement;
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
      if (_insertStatement == null) {
        _insertStatement = _conn.prepareStatement(_insertQuery,
                                                  Statement.RETURN_GENERATED_KEYS);
      }

      return _insertStatement;
    }

    /*
    PreparedStatement prepareUpdateAllExpires()
      throws SQLException
    {
      if (_updateAllExpiresStatement == null)
        _updateAllExpiresStatement = _conn.prepareStatement(_updateAllExpiresQuery,
                                                            TYPE_FORWARD_ONLY,
                                                            CONCUR_UPDATABLE);

      return _updateAllExpiresStatement;
    }
    */

    PreparedStatement prepareSelectOrphan()
      throws SQLException
    {
      if (_selectOrphanStatement == null)
        _selectOrphanStatement = _conn.prepareStatement(_selectOrphanQuery);

      return _selectOrphanStatement;
    }

    /*
    PreparedStatement prepareUpdateExpires()
      throws SQLException
    {
      if (_updateExpiresStatement == null)
        _updateExpiresStatement = _conn.prepareStatement(_updateExpiresQuery);

      return _updateExpiresStatement;
    }

    PreparedStatement prepareDeleteTimeout()
      throws SQLException
    {
      if (_deleteTimeoutStatement == null)
        _deleteTimeoutStatement = _conn.prepareStatement(_deleteTimeoutQuery);

      return _deleteTimeoutStatement;
    }
    */

    PreparedStatement prepareDelete()
      throws SQLException
    {
      if (_deleteStatement == null)
        _deleteStatement = _conn.prepareStatement(_deleteQuery);

      return _deleteStatement;
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
}
