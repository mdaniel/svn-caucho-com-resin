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
import com.caucho.config.ConfigException;
import com.caucho.db.*;
import com.caucho.db.jdbc.*;
import com.caucho.util.L10N;
import com.caucho.java.*;
import com.caucho.server.resin.*;
import com.caucho.vfs.*;

/**
 * Implements a file queue.
 */
public class FileQueueStore
{
  private static final L10N L = new L10N(FileQueueStore.class);
  private static final Logger log
    = Logger.getLogger(FileQueueStore.class.getName());

  private static final MessageType []MESSAGE_TYPE = MessageType.values();

  private Path _path;
  private DataSource _db;
  private String _name = "default";
  private String _tablePrefix = "jms";

  private MessageFactory _messageFactory;

  private String _queueTable;
  private String _messageTable;

  private long _queueId;

  private Connection _conn;

  private PreparedStatement _sendStmt;
  private PreparedStatement _receiveStartStmt;
  private PreparedStatement _readStmt;
  private PreparedStatement _receiveStmt;
  private PreparedStatement _removeStmt;
  private PreparedStatement _deleteStmt;

  public FileQueueStore(MessageFactory messageFactory)
  {
    _messageFactory = messageFactory;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Sets the path to the database
   */
  public void setPath(Path path)
  {
    if (! path.exists()) {
      try {
	path.mkdirs();
      } catch (IOException e) {
	throw ConfigException.create(e);
      }
    }
    
    if (! path.isDirectory())
      throw new ConfigException(L.l("path '{0}' must be a directory",
				    path));

    _path = path;
  }

  /**
   * Returns the path to the backing database
   */
  public Path getPath()
  {
    return _path;
  }

  public void setTablePrefix(String prefix)
  {
    _tablePrefix = prefix;
  }

  public void init()
  {
    if (_path == null)
      _path = WorkDir.getLocalWorkDir();

    if (! _path.isDirectory())
      throw new ConfigException(L.l("FileQueue requires a valid persistent directory."));

    Resin resin = Resin.getLocal();
    
    String serverId = null;

    if (resin != null)
      serverId = resin.getServerId();

    if (serverId == null) {
      serverId = "anon";
    }
    else if ("".equals(serverId))
      serverId = "default";

    _queueTable = escapeName("jms_queue_" + serverId);
    _messageTable = escapeName("jms_message_" + serverId);

    try {
      DataSourceImpl db = new DataSourceImpl(_path);
      db.setRemoveOnError(true);
      db.init();

      _db = db;

      _conn = _db.getConnection();

      initDatabase();
      
      initQueue();

      initStatements();
    } catch (SQLException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a new message to the persistent store.
   */
  public long send(MessageImpl msg, int priority, long expireTime)
  {
    synchronized (this) {
      try {
	_sendStmt.setLong(1, _queueId);
	_sendStmt.setInt(2, priority);
	_sendStmt.setLong(3, expireTime);
	_sendStmt.setString(4, msg.getJMSMessageID());
	_sendStmt.setBinaryStream(5, msg.propertiesToInputStream(), 0);
	_sendStmt.setInt(6, msg.getType().ordinal());
	_sendStmt.setBinaryStream(7, msg.bodyToInputStream(), 0);

	_sendStmt.executeUpdate();

	if (log.isLoggable(Level.FINE))
	  log.fine(this + " send " + msg);

	ResultSet rs = _sendStmt.getGeneratedKeys();

	if (! rs.next())
	  throw new java.lang.IllegalStateException();

	long id = rs.getLong(1);

	rs.close();

	return id;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  void receiveStart(FileQueue fileQueue)
  {
    synchronized (this) {
      try {
	_receiveStartStmt.setLong(1, _queueId);

	ResultSet rs = _receiveStartStmt.executeQuery();

	while (rs.next()) {
	  long id = rs.getLong(1);
	  String msgId = rs.getString(2);
	  int priority = rs.getInt(3);
	  long expire = rs.getLong(4);
	  MessageType type = MESSAGE_TYPE[rs.getInt(5)];
	  
	  FileQueueEntry entry
	    = fileQueue.addEntry(id, msgId, -1, priority, expire, type);
	}

	rs.close();
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public MessageImpl readMessage(long id, MessageType type)
  {
    synchronized (this) {
      try {
	_readStmt.setLong(1, id);

	ResultSet rs = _readStmt.executeQuery();

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
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    return null;
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public MessageImpl receive()
  {
    synchronized (this) {
      try {
	_receiveStmt.setLong(1, _queueId);

	ResultSet rs = _receiveStmt.executeQuery();

	if (rs.next()) {
	  long id = rs.getLong(1);

	  rs.close();

	  Message msg = _messageFactory.createTextMessage("sample");

	  _deleteStmt.setLong(1, id);

	  _deleteStmt.executeUpdate();

	  return (MessageImpl) msg;
	}

	rs.close();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    return null;
  }

  /**
   * Retrieves a message from the persistent store.
   */
  public void remove(String id)
  {
    synchronized (this) {
      try {
	_removeStmt.setString(1, id);

	_removeStmt.executeUpdate();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  /**
   * Retrieves a message from the persistent store.
   */
  void delete(long id)
  {
    synchronized (this) {
      try {
	_deleteStmt.setLong(1, id);

	_deleteStmt.executeUpdate();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  private void initDatabase()
    throws SQLException
  {
    String sql = "select id, priority from " + _messageTable + " where 1=0";
    Statement stmt = _conn.createStatement();

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
	   + "  id bigint auto_increment,"
	   + "  name varchar(128)"
	   + ")");

    stmt.executeUpdate(sql);

    sql = ("create table " + _messageTable + " ("
	   + "  id bigint auto_increment,"
	   + "  queue bigint,"
	   + "  state integer,"
	   + "  priority integer,"
	   + "  expire datetime,"
	   + "  owner_1 bigint,"
	   + "  owner_2 bigint,"
	   + "  msg_id varchar(64),"
	   + "  header blob,"
	   + "  type integer,"
	   + "  body blob,"
	   + "  is_valid bit"
	   + ")");

    stmt.executeUpdate(sql);
  }

  private void initQueue()
    throws SQLException
  {
    String sql = "select id from " + _queueTable + " where name=?";
    
    PreparedStatement stmt = _conn.prepareStatement(sql);
    stmt.setString(1, getName());

    ResultSet rs = stmt.executeQuery();
    if (rs.next()) {
      _queueId = rs.getLong(1);
      rs.close();
      stmt.close();

      return;
    }

    stmt.close();

    sql = "insert into " + _queueTable + " (name) values(?)";
    stmt = _conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

    stmt.setString(1, getName());

    stmt.executeUpdate();

    rs = stmt.getGeneratedKeys();

    if (! rs.next())
      throw new java.lang.IllegalStateException();

    _queueId = rs.getLong(1);

    rs.close();
    stmt.close();
  }

  private void initStatements()
    throws SQLException
  {
    String sql = ("insert into " + _messageTable
		  + " (queue,priority,expire,msg_id,header,type,body,is_valid)"
		  + " VALUES(?,?,?,?,?,?,?,1)");
    
    _sendStmt = _conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    
    sql = ("select id,msg_id,header,body from " + _messageTable
	   + " WHERE queue=? LIMIT 1");
    
    _receiveStmt = _conn.prepareStatement(sql);
    
    sql = ("select type,msg_id,header,body from " + _messageTable
	   + " WHERE id=?");
    
    _readStmt = _conn.prepareStatement(sql);

    sql = ("select id,msg_id,priority,expire,type from " + _messageTable
	   + " WHERE queue=? AND is_valid=1 ORDER BY id");
    
    _receiveStartStmt = _conn.prepareStatement(sql);
    
    _removeStmt = _conn.prepareStatement(sql);
    
    sql = ("update " + _messageTable
	   + " set body=null, is_valid=0, expire=now() + 120000"
	   + " WHERE id=?");
    
    _removeStmt = _conn.prepareStatement(sql);
    
    sql = ("delete from " + _messageTable
	   + " WHERE id=?");
    
    _deleteStmt = _conn.prepareStatement(sql);
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
}
