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

import java.io.IOException;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Blob;
import java.sql.SQLException;

import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.JMSException;

import javax.sql.DataSource;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ByteToChar;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.selector.Selector;

import com.caucho.jms.message.MessageImpl;

import com.caucho.jms.session.MessageConsumerImpl;
import com.caucho.jms.session.SessionImpl;

/**
 * Represents a JDBC topic consumer.
 */
public class JdbcTopicConsumer extends MessageConsumerImpl
  implements AlarmListener, TopicSubscriber {
  static final Logger log = Log.open(JdbcTopicConsumer.class);
  static final L10N L = new L10N(JdbcTopicConsumer.class);

  private final static long TOPIC_TIMEOUT = 3600 * 1000L;
  
  private JdbcManager _jdbcManager;

  private JdbcTopic _topic;

  private String _subscriber;
  private long _consumerId;

  private long _lastPurgeTime;
  private boolean _isClosed;
  private Alarm _alarm;

  public JdbcTopicConsumer(SessionImpl session, String messageSelector,
			   JdbcManager jdbcManager, JdbcTopic topic,
			   boolean noLocal)
    throws JMSException
  {
    super(session, messageSelector, topic, noLocal);
    
    _jdbcManager = jdbcManager;
    _topic = topic;

    createTopic();

    _alarm = new Alarm(this, TOPIC_TIMEOUT / 4);
  }

  public JdbcTopicConsumer(SessionImpl session, String messageSelector,
			   JdbcManager jdbcManager, JdbcTopic topic,
			   boolean noLocal, String name)
    throws JMSException
  {
    super(session, messageSelector, topic, noLocal);
    
    _jdbcManager = jdbcManager;
    _topic = topic;

    _subscriber = name;

    createTopic(name);
  }

  /**
   * Returns the topic.
   */
  public Topic getTopic()
  {
    return _topic;
  }

  /**
   * Creates an ephemeral topic.
   */
  private void createTopic()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
      String consumerSequence = _jdbcManager.getConsumerSequence();
      String messageTable = _jdbcManager.getMessageTable();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("SELECT MAX(m_id)" +
		      " FROM " + messageTable +
		      " WHERE queue=?");
	
	long max = -1;
	
	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);
	pstmt.setInt(1, _topic.getId());
	
	ResultSet rs = pstmt.executeQuery();
	if (rs.next())
	  max = rs.getLong(1);
	rs.close();
	
	if (consumerSequence != null) {
	  sql = _jdbcManager.getMetaData().selectSequenceSQL(consumerSequence);

	  pstmt = conn.prepareStatement(sql);

	  long id = 0;

	  rs = pstmt.executeQuery();
	  if (rs.next())
	    id = rs.getLong(1);
	  else
	    throw new IllegalStateException("no sequence value for consumer.");

	  rs.close();
	  pstmt.close();
	  
	  sql = ("INSERT INTO " + consumerTable +
		 " (s_id, queue, expire, read_id, ack_id) VALUES (?,?,?,?,?)");

	  pstmt = conn.prepareStatement(sql);

	  pstmt.setLong(1, id);
	  pstmt.setInt(2, _topic.getId());
	  pstmt.setLong(3, Alarm.getCurrentTime() + TOPIC_TIMEOUT);
	  pstmt.setLong(4, max);
	  pstmt.setLong(5, max);

	  pstmt.executeUpdate();
	  
	  _consumerId = id;
	}
	else {
	  sql = ("INSERT INTO " + consumerTable +
		 " (queue, expire, read_id, ack_id) VALUES (?,?,?,?)");

	  pstmt = conn.prepareStatement(sql,
					PreparedStatement.RETURN_GENERATED_KEYS);

	  pstmt.setInt(1, _topic.getId());
	  pstmt.setLong(2, Alarm.getCurrentTime() + TOPIC_TIMEOUT);
	  pstmt.setLong(3, max);
	  pstmt.setLong(4, max);

	  pstmt.executeUpdate();

	  ResultSet rsKeys = pstmt.getGeneratedKeys();

	  if (rsKeys.next()) {
	    _consumerId = rsKeys.getLong(1);
	  }
	  else
	    throw new JMSException(L.l("consumer insert didn't create a key"));

	  rsKeys.close();
	}
	
	pstmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Deletes an ephemeral topic.
   */
  private void deleteTopic()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("DELETE FROM " + consumerTable +
		      " WHERE s_id=?");
	
	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);
	pstmt.setLong(1, _consumerId);

	pstmt.executeUpdate();
	pstmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Creates a durable topic subscriber.
   */
  private void createTopic(String name)
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
      String consumerSequence = _jdbcManager.getConsumerSequence();
      String messageTable = _jdbcManager.getMessageTable();

      String clientId = _session.getClientID();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("SELECT s_id" +
		      " FROM " + consumerTable +
		      " WHERE queue=? AND client=? AND name=?");

	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);
	pstmt.setInt(1, _topic.getId());
	pstmt.setString(2, clientId);
	pstmt.setString(3, name);

	ResultSet rs = pstmt.executeQuery();
	if (rs.next()) {
	  _consumerId = rs.getLong(1);
	  rs.close();
	  return;
	}
	
	sql = ("SELECT MAX(m_id)" +
	       " FROM " + messageTable +
	       " WHERE queue=?");
	
	long max = -1;
	
	pstmt = conn.prepareStatement(sql);
	pstmt.setInt(1, _topic.getId());
	
	rs = pstmt.executeQuery();
	if (rs.next())
	  max = rs.getLong(1);
	rs.close();
	pstmt.close();

	if (consumerSequence != null) {
	  sql = _jdbcManager.getMetaData().selectSequenceSQL(consumerSequence);

	  pstmt = conn.prepareStatement(sql);

	  long id = 0;

	  rs = pstmt.executeQuery();
	  if (rs.next())
	    id = rs.getLong(1);
	  else
	    throw new IllegalStateException("no sequence value for consumer.");

	  rs.close();
	  pstmt.close();
	  
	  sql = ("INSERT INTO " + consumerTable +
		 " (s_id, queue, client, name, expire, read_id, ack_id) VALUES (?,?,?,?,?,?,?)");

	  pstmt = conn.prepareStatement(sql);

	  pstmt.setLong(1, id);
	  pstmt.setInt(2, _topic.getId());
	  pstmt.setString(3, clientId);
	  pstmt.setString(4, name);
	  pstmt.setLong(5, Long.MAX_VALUE / 2);
	  pstmt.setLong(6, max);
	  pstmt.setLong(7, max);

	  pstmt.executeUpdate();
	}
	else {
	  sql = ("INSERT INTO " + consumerTable +
		 " (queue, client, name, expire, read_id, ack_id) VALUES (?,?,?,?,?,?)");

	  pstmt = conn.prepareStatement(sql,
					PreparedStatement.RETURN_GENERATED_KEYS);

	  pstmt.setInt(1, _topic.getId());
	  pstmt.setString(2, clientId);
	  pstmt.setString(3, name);
	  pstmt.setLong(4, Long.MAX_VALUE / 2);
	  pstmt.setLong(5, max);
	  pstmt.setLong(6, max);

	  pstmt.executeUpdate();

	  ResultSet rsKeys = pstmt.getGeneratedKeys();

	  if (rsKeys.next()) {
	    _consumerId = rsKeys.getLong(1);
	  }
	  else
	    throw new JMSException(L.l("consumer insert didn't create a key"));

	  rsKeys.close();
	  pstmt.close();
	}
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Receives a message from the topic.
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    purgeExpiredConsumers();
    _topic.purgeExpiredMessages();
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      String consumerTable = _jdbcManager.getConsumerTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("SELECT m_id, msg_type, delivered, body, header" +
		      " FROM " + messageTable + " m," +
		      "      " + consumerTable + " s" +
		      " WHERE s_id=? AND m.queue=s.queue AND s.read_id<m_id" +
		      "   AND ?<m.expire" +
		      " ORDER BY m_id");

	PreparedStatement selectStmt = conn.prepareStatement(sql);

	try {
	  selectStmt.setFetchSize(1);
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      
	long id = -1;
	
	selectStmt.setLong(1, _consumerId);
	selectStmt.setLong(2, Alarm.getCurrentTime());

	MessageImpl msg = null;

	ResultSet rs = selectStmt.executeQuery();
	while (rs.next()) {
	  id = rs.getLong(1);

	  msg = jdbcMessage.readMessage(rs);

	  if (_selector == null || _selector.isMatch(msg))
	    break;
	  else
	    msg = null;
	}

	rs.close();
	selectStmt.close();

	if (msg == null)
	  return null;

	sql = ("UPDATE " + consumerTable +
	       " SET read_id=?" +
	       " WHERE s_id=?");
      
	PreparedStatement updateStmt = conn.prepareStatement(sql);

	updateStmt.setLong(1, id);
	updateStmt.setLong(2, _consumerId);
	  
	updateStmt.executeUpdate();
	updateStmt.close();

	return msg;
      } finally {
	conn.close();
      }
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Acknowledges all received messages from the session.
   */
  public void acknowledge()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
    
      Connection conn = dataSource.getConnection();

      try {
	String sql = ("UPDATE " +  consumerTable +
		      " SET ack_id=read_id " +
		      " WHERE s_id=?");
      
	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);

	pstmt.setLong(1, _consumerId);

	pstmt.executeUpdate();

	pstmt.close();

	deleteOldMessages();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Delete read messages.
   */
  private void deleteOldMessages()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      String consumerTable = _jdbcManager.getConsumerTable();
    
      Connection conn = dataSource.getConnection();

      try {
	String sql;
	
	sql = ("DELETE FROM " + messageTable +
	       " WHERE queue=? AND NOT EXISTS(" + 
               "   SELECT * FROM " + consumerTable +
	       "   WHERE queue=? AND ack_id < m_id)");
      
	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);
	pstmt.setInt(1, _topic.getId());
	pstmt.setInt(2, _topic.getId());

	pstmt.executeUpdate();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Truncates all blobs before a deletion.
   */
  

  /**
   * Rollback all received messages from the session.
   */
  public void rollback()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
    
      Connection conn = dataSource.getConnection();

      try {
	String sql = ("UPDATE " +  consumerTable +
		      " SET read_id=ack_id " +
		      " WHERE s_id=?");
      
	PreparedStatement pstmt;
	pstmt = conn.prepareStatement(sql);

	pstmt.setLong(1, _consumerId);

	pstmt.executeUpdate();

	pstmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Purges expired consumers.
   */
  private void purgeExpiredConsumers()
  {
    long now = Alarm.getCurrentTime();

    if (now < _lastPurgeTime + TOPIC_TIMEOUT)
      return;

    _lastPurgeTime = now;
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      String consumerTable = _jdbcManager.getConsumerTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("DELETE FROM " + consumerTable +
		      " WHERE expire<?");

	PreparedStatement pstmt = conn.prepareStatement(sql);
	pstmt.setLong(1, Alarm.getCurrentTime());

	pstmt.executeUpdate();
      } finally {
	conn.close();
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Handles the alarm by updating the expire count.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (_isClosed)
      return;

    try {
      Connection conn = _jdbcManager.getDataSource().getConnection();
      try {
	String consumerTable = _jdbcManager.getConsumerTable();
	
	String sql = ("UPDATE " + consumerTable +
		      " SET expire=?" +
		      " WHERE s_id=?");

	PreparedStatement pstmt = conn.prepareStatement(sql);
	pstmt.setLong(1, Alarm.getCurrentTime() + TOPIC_TIMEOUT);
	pstmt.setLong(2, _consumerId);

	pstmt.executeUpdate();
      } finally {
	conn.close();
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(TOPIC_TIMEOUT / 4);
    }
  }

  /**
   * Closes the consumer.
   */
  public void close()
    throws JMSException
  {
    if (_isClosed)
      return;
    _isClosed = true;

    if (_alarm != null)
      _alarm.dequeue();
    
    try {
      if (_subscriber == null)
	deleteTopic();
      else {
	// XXX: ejb/6a22
	_session.unsubscribe(_subscriber);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    super.close();
  }

  /**
   * Returns a printable view of the topic.
   */
  public String toString()
  {
    return "JdbcTopicConsumer[" + _topic + "," + _consumerId + "]";
  }
}
