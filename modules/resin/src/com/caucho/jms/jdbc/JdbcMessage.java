/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jms.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.EOFException;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Types;
import java.sql.SQLException;

import javax.jms.TextMessage;
import javax.jms.BytesMessage;
import javax.jms.StreamMessage;
import javax.jms.ObjectMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;

import javax.sql.DataSource;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.jdbc.JdbcMetaData;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.selector.Selector;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.message.TextMessageImpl;
import com.caucho.jms.message.BytesMessageImpl;
import com.caucho.jms.message.StreamMessageImpl;
import com.caucho.jms.message.ObjectMessageImpl;
import com.caucho.jms.message.MapMessageImpl;

import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.ContextLoaderObjectInputStream;

/**
 * Represents a JDBC message.
 */
public class JdbcMessage {
  static final Logger log = Log.open(JdbcMessage.class);
  static final L10N L = new L10N(JdbcMessage.class);

  private static final int MESSAGE = 0;
  private static final int TEXT = 1;
  private static final int BYTES = 2;
  private static final int STREAM = 3;
  private static final int OBJECT = 4;
  private static final int MAP = 5;
  
  private JdbcManager _jdbcManager;
  private DataSource _dataSource;

  private String _messageTable;
  private String _messageSequence;

  public JdbcMessage(JdbcManager jdbcManager)
  {
    _jdbcManager = jdbcManager;
  }

  /**
   * Initializes the JdbcQueue
   */
  public void init()
    throws ConfigException, SQLException
  {
    _messageTable = _jdbcManager.getMessageTable();
    _dataSource = _jdbcManager.getDataSource();

    Connection conn = _dataSource.getConnection();
    try {
      Statement stmt = conn.createStatement();
      String sql = "SELECT 1 FROM " + _messageTable + " WHERE 1=0";

      try {
	ResultSet rs = stmt.executeQuery(sql);
	rs.next();
	rs.close();
	stmt.close();

	return;
      } catch (SQLException e) {
	log.finest(e.toString());
      }

      String blob = _jdbcManager.getBlob();
      String longType = _jdbcManager.getLongType();
							   
      log.info(L.l("creating JMS message table {0}", _messageTable));

      JdbcMetaData metaData = _jdbcManager.getMetaData();
      
      String identity = "";

      if (metaData.supportsIdentity())
	identity = " auto_increment";
      else
	_messageSequence = _messageTable + "_cseq";
      
      sql = ("CREATE TABLE " + _messageTable + " (" +
	     "  m_id " + longType + " PRIMARY KEY" + identity + "," +
	     "  queue INTEGER NOT NULL," +
	     "  connection VARCHAR(255)," +
	     "  consumer " + longType + "," +
	     "  delivered INTEGER NOT NULL," +
	     "  msg_type INTEGER NOT NULL," +
	     "  expire " + longType + " NOT NULL," +
	     "  header " + blob + "," +
	     "  body " + blob +
	     ")");

      stmt.executeUpdate(sql);

      if (_messageSequence != null) {
	stmt.executeUpdate(metaData.createSequenceSQL(_messageSequence, 1));
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Sends the message to the queue.
   */
  public long send(Message message, int queue, long expireTime)
    throws SQLException, IOException, JMSException
  {
    if (log.isLoggable(Level.FINE))
      log.fine("jms jdbc queue:" + queue + " send message");

    TempStream header = new TempStream();
    header.openWrite();
      
    WriteStream ws = new WriteStream(header);
    writeMessageHeader(ws, message);
    ws.close();

    TempStream body = null;

    int type = MESSAGE;

    if (message instanceof TextMessage) {
      TextMessage text = (TextMessage) message;
	
      type = TEXT;
	
      if (text.getText() != null) {
	body = new TempStream();
	body.openWrite();
	
	ws = new WriteStream(body);
	ws.setEncoding("UTF-8");
	ws.print(text.getText());
	ws.close();
      }
    }
    else if (message instanceof BytesMessage) {
      BytesMessage bytes = (BytesMessage) message;

      type = BYTES;

      body = writeBytes(bytes);
    }
    else if (message instanceof StreamMessage) {
      StreamMessage stream = (StreamMessage) message;

      type = STREAM;

      body = writeStream(stream);
    }
    else if (message instanceof ObjectMessage) {
      ObjectMessage obj = (ObjectMessage) message;

      type = OBJECT;

      body = writeObject(obj);
    }
    else if (message instanceof MapMessage) {
      MapMessage obj = (MapMessage) message;

      type = MAP;

      body = writeMap(obj);
    }

    Connection conn = _dataSource.getConnection();
    try {
      String sql;

      if (_messageSequence != null) {
	sql = _jdbcManager.getMetaData().selectSequenceSQL(_messageSequence);

	PreparedStatement pstmt = conn.prepareStatement(sql);;

	long mId = -1;
	
	ResultSet rs = pstmt.executeQuery();
	if (rs.next())
	  mId = rs.getLong(1);
	else
	  throw new RuntimeException("can't create message");
	
	sql = ("INSERT INTO " + _messageTable +
	       "(m_id, queue, msg_type, expire, delivered, header, body) " +
	       "VALUES (?,?,?,?,0,?,?)");

	pstmt = conn.prepareStatement(sql);

	int i = 1;
	pstmt.setLong(i++, mId);
	pstmt.setInt(i++, queue);
	pstmt.setInt(i++, type);
	pstmt.setLong(i++, expireTime);
	pstmt.setBinaryStream(i++, header.openRead(), header.getLength());
	
	if (body != null) 
	  pstmt.setBinaryStream(i++, body.openRead(), body.getLength());
	else
	  pstmt.setString(i++, "");

	pstmt.executeUpdate();
      }
      else {
	sql = ("INSERT INTO " + _messageTable +
	       "(queue, msg_type, expire, delivered, header, body) " +
	       "VALUES (?,?,?,0,?,?)");
	PreparedStatement pstmt;

	pstmt = conn.prepareStatement(sql);

	int i = 1;
	pstmt.setInt(i++, queue);
	pstmt.setInt(i++, type);
	pstmt.setLong(i++, expireTime);
	pstmt.setBinaryStream(i++, header.openRead(), header.getLength());
	
	if (body != null) 
	  pstmt.setBinaryStream(i++, body.openRead(), body.getLength());
	else
	  pstmt.setString(i++, "");

	pstmt.executeUpdate();
      }

      return 0;
    } finally {
      conn.close();
    }
  }

  /**
   * Receives a message from the queue.
   */
  MessageImpl receive(int queue, int session)
    throws SQLException, IOException, JMSException
  {
    long minId = -1;
    
    Connection conn = _dataSource.getConnection();
    try {
      String sql = ("SELECT m_id, msg_type, delivered, body, header" +
		    " FROM " + _messageTable +
		    " WHERE ?<id AND queue=? AND consumer IS NULL" +
		    " ORDER BY id");

      PreparedStatement selectStmt = conn.prepareStatement(sql);

      
      sql = ("UPDATE " + _messageTable + " SET consumer=? " +
	     "WHERE m_id=? AND consumer IS NULL");
      
      PreparedStatement updateStmt = conn.prepareStatement(sql);

      long id = -1;
      while (true) {
	id = -1;
	
	selectStmt.setLong(1, minId);
	selectStmt.setInt(2, queue);

	MessageImpl msg = null;

	ResultSet rs = selectStmt.executeQuery();
	while (rs.next()) {
	  id = rs.getLong(1);

	  minId = id;

	  msg = readMessage(rs);
	}

	rs.close();

	if (msg == null)
	  return null;

	updateStmt.setInt(1, session);
	updateStmt.setLong(2, id);
	  
	int updateCount = updateStmt.executeUpdate();
	
	if (updateCount == 1)
	  return msg;
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Acknowledges all received messages from the session.
   */
  void acknowledge(int session)
    throws SQLException
  {
    Connection conn = _dataSource.getConnection();

    try {
      String sql = ("DELETE FROM " +  _messageTable + " " +
		    "WHERE consumer=?");
      
      PreparedStatement pstmt;
      pstmt = conn.prepareStatement(sql);

      pstmt.setInt(1, session);

      pstmt.executeUpdate();

      pstmt.close();
    } finally {
      conn.close();
    }
  }

  /**
   * Rollback all received messages from the session.
   */
  void rollback(int session)
    throws SQLException
  {
    Connection conn = _dataSource.getConnection();

    try {
      String sql = ("UPDATE " +  _messageTable +
		    " SET reader=NULL " +
		    " WHERE reader=?");
      
      PreparedStatement pstmt;
      pstmt = conn.prepareStatement(sql);

      pstmt.setInt(1, session);

      pstmt.executeUpdate();

      pstmt.close();
    } finally {
      conn.close();
    }
  }

  /**
   * Reads the message from the result stream.
   */
  MessageImpl readMessage(ResultSet rs)
    throws SQLException, IOException, JMSException
  {
    int msgType = rs.getInt(2);
    boolean redelivered = rs.getInt(3) == 1;

    MessageImpl msg;
    
    switch (msgType) {
    case TEXT:
      {
	InputStream is = rs.getBinaryStream(4);

	try {
	  msg = readTextMessage(is);
	} finally {
	  is.close();
	}
	break;
      }
	    
    case BYTES:
      {
	InputStream is = rs.getBinaryStream(4);

	try {
	  msg = readBytesMessage(is);
	} finally {
	  is.close();
	}
	break;
      }
	    
    case STREAM:
      {
	InputStream is = rs.getBinaryStream(4);

	try {
	  msg = readStreamMessage(is);
	} finally {
	  is.close();
	}
	break;
      }
	    
    case OBJECT:
      {
	InputStream is = rs.getBinaryStream(4);

	try {
	  msg = readObjectMessage(is);
	} finally {
	  is.close();
	}
	break;
      }
	    
    case MAP:
      {
	InputStream is = rs.getBinaryStream(4);

	try {
	  msg = readMapMessage(is);
	} finally {
	  is.close();
	}
	break;
      }
	    
    case MESSAGE:
    default:
      {
	msg = new MessageImpl();
	break;
      }
    }
	  
    InputStream is = rs.getBinaryStream(5);
    try {
      readMessageHeader(is, msg);
    } finally {
      is.close();
    }

    msg.setJMSRedelivered(redelivered);

    return msg;
  }

  /**
   * Writes the message header for a Resin message.
   */
  private void writeMessageHeader(WriteStream ws, Message msg)
    throws IOException, JMSException
  {
    Enumeration names = msg.getPropertyNames();
    CharBuffer cb = new CharBuffer();

    while (names.hasMoreElements()) {
       String name = (String) names.nextElement();
      writeValue(ws, cb, name);
      
      String value = msg.getStringProperty(name);
      writeValue(ws, cb, value);
    }
  }

  /**
   * Writes a value to the output stream.
   */
  private void writeValue(WriteStream ws, CharBuffer cb, Object value)
    throws IOException
  {
    if (value == null)
      ws.write('N');
    else {
      cb.clear();
      cb.append(value);
      int length = cb.length();
      char []buf = cb.getBuffer();

      ws.write('S');
      ws.write(length >> 24);
      ws.write(length >> 16);
      ws.write(length >> 8);
      ws.write(length);

      for (int i = 0; i < length; i++) {
	int ch = buf[i];

	ws.write(ch >> 8);
	ws.write(ch);
      }
    }
  }

  /**
   * Writes the bytes message.
   */
  private TempStream writeBytes(BytesMessage bytes)
    throws IOException, JMSException
  {
    TempStream body = new TempStream();
    body.openWrite();
	
    WriteStream ws = new WriteStream(body);
	
    int data;
    //bytes.reset();

    while ((data = bytes.readUnsignedByte()) >= 0) {
      ws.write(data);
    }
    
    ws.close();

    return body;
  }

  /**
   * Writes the stream message.
   */
  private TempStream writeStream(StreamMessage stream)
    throws IOException, JMSException
  {
    TempStream body = new TempStream();
    body.openWrite();
	
    WriteStream ws = new WriteStream(body);
    ObjectOutputStream out = new ObjectOutputStream(ws);

    try {
      while (true) {
	Object data = stream.readObject();

	out.writeObject(data);
      }
    } catch (MessageEOFException e) {
    }

    out.close();
    ws.close();

    return body;
  }

  /**
   * Writes the object message.
   */
  private TempStream writeObject(ObjectMessage obj)
    throws IOException, JMSException
  {
    TempStream body = new TempStream();
    body.openWrite();
	
    WriteStream ws = new WriteStream(body);
    ObjectOutputStream out = new ObjectOutputStream(ws);

    out.writeObject(obj.getObject());

    out.close();
    ws.close();

    return body;
  }

  /**
   * Writes the map message.
   */
  private TempStream writeMap(MapMessage map)
    throws IOException, JMSException
  {
    TempStream body = new TempStream();
    body.openWrite();
	
    WriteStream ws = new WriteStream(body);
    ObjectOutputStream out = new ObjectOutputStream(ws);

    try {
      Enumeration e = map.getMapNames();
      while (e.hasMoreElements()) {
	String name = (String) e.nextElement();
	out.writeUTF(name);
	
	Object data = map.getObject(name);
	out.writeObject(data);
      }
    } catch (MessageEOFException e) {
    }

    out.close();
    ws.close();

    return body;
  }

  /**
   * Writes the message header for a Resin message.
   */
  private void readMessageHeader(InputStream is, Message msg)
    throws IOException, JMSException
  {
    CharBuffer cb = new CharBuffer();

    int type;

    while ((type = is.read()) > 0) {
      String name = (String) readValue(is, type, cb);
      Object value = readValue(is, is.read(), cb);

      msg.setObjectProperty(name, value);
    }
  }

  /**
   * Writes the message header for a Resin message.
   */
  private TextMessageImpl readTextMessage(InputStream is)
    throws IOException, JMSException
  {
    TextMessageImpl text = new TextMessageImpl();
    ByteToChar byteToChar = ByteToChar.create();

    int ch;

    byteToChar.setEncoding("UTF-8");
    while ((ch = is.read()) >= 0) {
      byteToChar.addByte(ch);
    }

    text.setText(byteToChar.getConvertedString());
    
    return text;
  }

  /**
   * Reads a bytes message.
   */
  private BytesMessageImpl readBytesMessage(InputStream is)
    throws IOException, JMSException
  {
    BytesMessageImpl bytes = new BytesMessageImpl();

    int data;

    while ((data = is.read()) >= 0) {
      bytes.writeByte((byte) data);
    }

    bytes.reset();
    
    return bytes;
  }

  /**
   * Reads a stream message.
   */
  private StreamMessageImpl readStreamMessage(InputStream is)
    throws IOException, JMSException
  {
    StreamMessageImpl stream = new StreamMessageImpl();

    ObjectInputStream in = new ContextLoaderObjectInputStream(is);

    try {
      while (true) {
	Object obj = in.readObject();

	stream.writeObject(obj);
      }
    } catch (EOFException e) {
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    in.close();

    stream.reset();
    
    return stream;
  }

  /**
   * Reads a map message.
   */
  private MapMessageImpl readMapMessage(InputStream is)
    throws IOException, JMSException
  {
    MapMessageImpl map = new MapMessageImpl();

    ObjectInputStream in = new ContextLoaderObjectInputStream(is);

    try {
      while (true) {
	String name = in.readUTF();
	Object obj = in.readObject();

	map.setObject(name, obj);
      }
    } catch (EOFException e) {
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    in.close();

    return map;
  }

  /**
   * Reads an object message.
   */
  private ObjectMessageImpl readObjectMessage(InputStream is)
    throws IOException, JMSException
  {
    ObjectMessageImpl msg = new ObjectMessageImpl();

    ObjectInputStream in = new ContextLoaderObjectInputStream(is);

    try {
      Object obj = in.readObject();
      msg.setObject((java.io.Serializable) obj);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    in.close();
    
    return msg;
  }

  /**
   * Writes a value to the output stream.
   */
  private Object readValue(InputStream is, int type, CharBuffer cb)
    throws IOException
  {
    switch (type) {
    case 'N':
      return null;
    case 'S':
      {
	cb.clear();
	int length = readInt(is);

	for (int i = 0; i < length; i++) {
	  char ch = (char) ((is.read() << 8) + is.read());

	  cb.append(ch);
	}

	return cb.toString();
      }
    default:
      throw new IOException(L.l("unknown header type"));
    }
  }

  /**
   * Reads an integer value.
   */
  private int readInt(InputStream is)
    throws IOException
  {
    return ((is.read() << 24) +
	    (is.read() << 16) +
	    (is.read() << 8) +
	    (is.read()));
  }

  /**
   * Removes the first message matching the selector.
   */
  private boolean hasMessage(Selector selector)
    throws JMSException
  {
    return false;
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "JdbcMessage[" + _messageTable + "]";
  }
}

