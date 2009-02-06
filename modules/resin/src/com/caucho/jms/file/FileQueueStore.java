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

package com.caucho.jms.file;

import java.io.*;
import java.lang.IllegalStateException;
import java.util.logging.*;
import javax.sql.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import javax.jms.*;
import javax.annotation.*;

import com.caucho.jms.queue.*;
import com.caucho.jms.message.*;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.config.ConfigException;
import com.caucho.db.*;
import com.caucho.db.jdbc.*;
import com.caucho.util.L10N;
import com.caucho.java.*;
import com.caucho.server.cluster.Server;
import com.caucho.server.resin.*;
import com.caucho.util.FreeList;
import com.caucho.vfs.*;

/**
 * Implements a file queue.
 */
public class FileQueueStore
{
  private static final L10N L = new L10N(FileQueueStore.class);
  private static final Logger log
    = Logger.getLogger(FileQueueStore.class.getName());

  private static final EnvironmentLocal<FileQueueStore> _localStore
    = new EnvironmentLocal<FileQueueStore>();

  private static final MessageType []MESSAGE_TYPE = MessageType.values();

  private static final int START_LIMIT = 8192;

  private FreeList<StoreConnection> _freeList
    = new FreeList<StoreConnection>(32);

  private Path _path;
  private DataSource _db;
  private String _name = "default";
  private String _tablePrefix = "jms";

  private MessageFactory _messageFactory;

  private String _queueTable;
  private String _messageTable;

  private long _queueId;

  public FileQueueStore(Path path, String serverId)
  {
    _messageFactory = new MessageFactory();

    init(path, serverId);
  }

  public static FileQueueStore create()
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("FileQueueStore requires an active Resin instance"));

    ClassLoader loader = server.getClassLoader();

    synchronized (_localStore) {
      FileQueueStore store = _localStore.get(loader);

      if (store == null) {
	Path path = server.getResinDataDirectory();
	String serverId = server.getServerId();

	store = new FileQueueStore(path, serverId);

	_localStore.set(store, loader);
      }

      return store;
    }
  }

  private void init(Path path, String serverId)
  {
    if (path == null)
      throw new NullPointerException();
    
    if (serverId == null)
      throw new NullPointerException();

    try {
      path.mkdirs();
    } catch (IOException e) {
      log.log(Level.ALL, e.toString(), e);
    }

    if (! path.isDirectory())
      throw new ConfigException(L.l("FileQueue requires a valid persistent directory {0}.",
				    path.getURL()));
    
    if ("".equals(serverId))
      serverId = "default";

    _queueTable = escapeName("jms_queue_" + serverId);
    _messageTable = escapeName("jms_message_" + serverId);

    try {
      DataSourceImpl db = new DataSourceImpl(path);
      db.setRemoveOnError(true);
      db.init();

      _db = db;

      Connection conn = _db.getConnection();

      initDatabase(conn);
      
      conn.close();
    } catch (SQLException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a new message to the persistent store.
   */
  public long send(byte []queueHash,
		   MessageImpl msg,
		   int priority,
		   long expireTime)
  {
    StoreConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement sendStmt = conn.prepareSend();

      sendStmt.setBytes(1, queueHash);
      sendStmt.setInt(2, priority);
      sendStmt.setLong(3, expireTime);
      sendStmt.setString(4, msg.getJMSMessageID());
      sendStmt.setBinaryStream(5, msg.propertiesToInputStream(), 0);
      sendStmt.setInt(6, msg.getType().ordinal());
      sendStmt.setBinaryStream(7, msg.bodyToInputStream(), 0);

      sendStmt.executeUpdate();

      if (log.isLoggable(Level.FINE))
	log.fine(this + " send " + msg);

      ResultSet rs = sendStmt.getGeneratedKeys();

      if (! rs.next())
	throw new java.lang.IllegalStateException();

      long id = rs.getLong(1);

      rs.close();

      return id;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  boolean receiveStart(byte []queueHash, FileQueueImpl fileQueue)
  {
    StoreConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement receiveStartStmt = conn.prepareReceiveStart();

      receiveStartStmt.setBytes(1, queueHash);

      ResultSet rs = receiveStartStmt.executeQuery();
      int count = 0;

      while (rs.next()) {
	count++;
	long id = rs.getLong(1);
	String msgId = rs.getString(2);
	int priority = rs.getInt(3);
	long expire = rs.getLong(4);
	MessageType type = MESSAGE_TYPE[rs.getInt(5)];
	  
	fileQueue.addEntry(id, msgId, -1, priority, expire, type, null);
      }

      rs.close();

      System.out.println("COUNT: " + count + " " + receiveStartStmt);

      return count < START_LIMIT;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public MessageImpl readMessage(long id, MessageType type)
  {
    StoreConnection conn = null;

    try {
      conn = getConnection();

      PreparedStatement readStmt = conn.prepareRead();

      readStmt.setLong(1, id);

      ResultSet rs = readStmt.executeQuery();

      if (rs.next()) {
	MessageImpl msg;

	type = MESSAGE_TYPE[rs.getInt(1)];

	switch (type) {
	case NULL:
	  msg = new MessageImpl();
	  break;
	case BYTES:
	  msg = new BytesMessageImpl();
	  break;
	case MAP:
	  msg = new MapMessageImpl();
	  break;
	case OBJECT:
	  msg = new ObjectMessageImpl();
	  break;
	case STREAM:
	  msg = new StreamMessageImpl();
	  break;
	case TEXT:
	  msg = new TextMessageImpl();
	  break;
	default:
	  msg = new MessageImpl();
	  break;
	}
	  
	String msgId = rs.getString(2);

	msg.setJMSMessageID(msgId);

	InputStream is = rs.getBinaryStream(3);
	if (is != null) {
	  msg.readProperties(is);

	  is.close();
	}

	is = rs.getBinaryStream(4);
	if (is != null) {
	  msg.readBody(is);

	  is.close();
	}

	return (MessageImpl) msg;
      }

      rs.close();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }

    return null;
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public MessageImpl receive(byte []queueHash)
  {
    StoreConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement receiveStmt = conn.prepareReceive();

      receiveStmt.setBytes(1, queueHash);

      ResultSet rs = receiveStmt.executeQuery();

      if (rs.next()) {
	long id = rs.getLong(1);

	rs.close();

	PreparedStatement deleteStmt = conn.prepareDelete();

	deleteStmt.setLong(1, id);

	deleteStmt.executeUpdate();

	return null;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }

    return null;
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public void remove(String id)
  {
    StoreConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement removeStmt = conn.prepareRemove();
      
      removeStmt.setString(1, id);

      removeStmt.executeUpdate();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  void delete(long id)
  {
    StoreConnection conn = null;
    
    try {
      conn = getConnection();

      PreparedStatement deleteStmt = conn.prepareDelete();
      
      deleteStmt.setLong(1, id);

      deleteStmt.executeUpdate();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      freeConnection(conn);
    }
  }

  private void initDatabase(Connection conn)
    throws SQLException
  {
    String sql = ("select id, priority, is_valid"
		  + " from " + _messageTable + " where 1=0");
    
    Statement stmt = conn.createStatement();

    try {
      ResultSet rs = stmt.executeQuery(sql);

      rs.close();
      
      return;
    } catch (SQLException e) {
      log.finer(e.toString());
    }

    try {
      stmt.executeUpdate("drop table " + _queueTable);
    } catch (SQLException e) {
      log.finer(e.toString());
    }

    try {
      stmt.executeUpdate("drop table " + _messageTable);
    } catch (SQLException e) {
      log.finer(e.toString());
    }

    sql = ("create table " + _queueTable + " ("
	   + "  id bigint primary key auto_increment,"
	   + "  name varchar(128)"
	   + ")");

    stmt.executeUpdate(sql);

    sql = ("create table " + _messageTable + " ("
	   + "  id bigint primary key auto_increment,"
	   + "  queue_id binary(32),"
	   + "  priority integer,"
	   + "  expire datetime,"
	   + "  msg_id varchar(64),"
	   + "  header blob,"
	   + "  type integer,"
	   + "  body blob,"
	   + "  is_valid bit"
	   + ")");

    stmt.executeUpdate(sql);
  }

  private StoreConnection getConnection()
    throws SQLException
  {
    StoreConnection storeConn = _freeList.allocate();

    if (storeConn != null) {
      return storeConn;
    } else {
      Connection conn = _db.getConnection();

      return new StoreConnection(conn);
    }
  }

  private void freeConnection(StoreConnection conn)
  {
    if (conn != null)
      _freeList.free(conn);
  }

  private static String escapeName(String name)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if ('a' <= ch && ch <= 'z'
	  || 'A' <= ch && ch <= 'Z'
	  || '0' <= ch && ch <= '0'
	  || ch == '_') {
	sb.append(ch);
      }
      else
	sb.append('_');
    }

    return sb.toString();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _messageTable + "]";
  }

  class StoreConnection {
    private Connection _conn;

    private PreparedStatement _sendStmt;
    private PreparedStatement _receiveStartStmt;
    private PreparedStatement _readStmt;
    private PreparedStatement _receiveStmt;
    private PreparedStatement _removeStmt;
    private PreparedStatement _deleteStmt;

    StoreConnection(Connection conn)
    {
      _conn = conn;
    }

    PreparedStatement prepareSend()
      throws SQLException
    {
      if (_sendStmt == null) {
	String sql = ("insert into " + _messageTable
		      + " (queue_id,priority,expire,msg_id,header,type,body,is_valid)"
		      + " VALUES(?,?,?,?,?,?,?,1)");
    
	_sendStmt = _conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      }

      return _sendStmt;
    }

    PreparedStatement prepareReceive()
      throws SQLException
    {
      if (_receiveStmt == null) {
	String sql = ("select id,msg_id,header,body from " + _messageTable
		      + " WHERE queue_id=? LIMIT 1");
    
	_receiveStmt = _conn.prepareStatement(sql);
      }

      return _receiveStmt;
    }

    PreparedStatement prepareRead()
      throws SQLException
    {
      if (_readStmt == null) {
	String sql = ("select type,msg_id,header,body from " + _messageTable
		      + " WHERE id=?");
    
	_readStmt = _conn.prepareStatement(sql);
      }

      return _readStmt;
    }

    PreparedStatement prepareReceiveStart()
      throws SQLException
    {
      if (_receiveStartStmt == null) {
	String sql = ("select id,msg_id,priority,expire,type"
		      + " from " + _messageTable
		      + " WHERE queue_id=? AND is_valid=1 ORDER BY id"
		      + " LIMIT " + START_LIMIT);
    
	_receiveStartStmt = _conn.prepareStatement(sql);
      }

      return _receiveStartStmt;
    }
    
    PreparedStatement prepareRemove()
      throws SQLException
    {
      if (_removeStmt == null) {
	String sql = ("update " + _messageTable
		      + " set body=null, is_valid=0, expire=now() + 120000"
		      + " WHERE id=?");
    
	_removeStmt = _conn.prepareStatement(sql);
      }

      return _removeStmt;
    }

    PreparedStatement prepareDelete()
      throws SQLException
    {
      if (_deleteStmt == null) {
	String sql = ("delete from " + _messageTable
		      + " WHERE id=?");
    
	_deleteStmt = _conn.prepareStatement(sql);
      }

      return _deleteStmt;
    }
  }
}
