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

package com.caucho.jms.jdbc;

import com.caucho.jms.JMSExceptionWrapper;
import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.session.MessageConsumerImpl;
import com.caucho.jms.session.SessionImpl;
import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC queue consumer.
 */
public class JdbcQueueConsumer extends MessageConsumerImpl
  implements QueueReceiver, AlarmListener {
  static final Logger log = Log.open(JdbcQueueConsumer.class);
  static final L10N L = new L10N(JdbcQueueConsumer.class);

  private final static long QUEUE_TIMEOUT = 3600 * 1000L;
  
  private JdbcManager _jdbcManager;

  private JdbcQueue _queue;

  private long _consumerId;

  private boolean _autoAck;
  
  private boolean _isClosed;
  private Alarm _alarm;

  private long _lastPurgeTime;

  public JdbcQueueConsumer(SessionImpl session, String messageSelector,
			   JdbcManager jdbcManager, JdbcQueue queue)
    throws JMSException
  {
    super(session, messageSelector, queue, false);
    
    _jdbcManager = jdbcManager;
    _queue = queue;

    if (session.getAcknowledgeMode() == session.AUTO_ACKNOWLEDGE ||
	session.getAcknowledgeMode() == session.DUPS_OK_ACKNOWLEDGE)
      _autoAck = true;

    createQueue();

    _alarm = new Alarm(this, QUEUE_TIMEOUT / 4);

    if (log.isLoggable(Level.FINE))
      log.fine("JdbcQueueConsumer[" + queue + "," + _consumerId + "] created");
  }

  /**
   * Returns the queue.
   */
  public Queue getQueue()
  {
    return _queue;
  }

  /**
   * Creates an ephemeral topic.
   */
  private void createQueue()
    throws JMSException
  {
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String consumerTable = _jdbcManager.getConsumerTable();
      String consumerSequence = _jdbcManager.getConsumerSequence();
      String messageTable = _jdbcManager.getMessageTable();
    
      Connection conn = dataSource.getConnection();
      try {
	if (consumerSequence != null) {
	  String sql = _jdbcManager.getMetaData().selectSequenceSQL(consumerSequence);
	  PreparedStatement pstmt = conn.prepareStatement(sql);

	  long id = 0;

	  ResultSet rs = pstmt.executeQuery();
	  if (rs.next())
	    id = rs.getLong(1);
	  else
	    throw new RuntimeException("Expected result in customer create");

	  sql = ("INSERT INTO " + consumerTable +
		 " (s_id, queue, expire) VALUES (?, ?,?)");

	  pstmt = conn.prepareStatement(sql);

	  pstmt.setLong(1, id);
	  pstmt.setInt(2, _queue.getId());
	  pstmt.setLong(3, Alarm.getCurrentTime() + QUEUE_TIMEOUT);

	  pstmt.executeUpdate();
	}
	else {
	  String sql = ("INSERT INTO " + consumerTable +
			" (queue, expire) VALUES (?,?)");

	  PreparedStatement pstmt;
	  pstmt = conn.prepareStatement(sql,
					PreparedStatement.RETURN_GENERATED_KEYS);

	  pstmt.setInt(1, _queue.getId());
	  pstmt.setLong(2, Alarm.getCurrentTime() + QUEUE_TIMEOUT);

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
   * Deletes an ephemeral queue.
   */
  private void deleteQueue()
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
   * Receives a message from the queue.
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    try {
      purgeExpiredConsumers();
      _queue.purgeExpiredMessages();
      
      long minId = -1;

      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("SELECT m_id, msg_type, delivered, body, header" +
		      " FROM " + messageTable +
		      " WHERE ?<m_id AND queue=?" +
		      "   AND consumer IS NULL AND ?<=expire" +
		      " ORDER BY m_id");

	PreparedStatement selectStmt = conn.prepareStatement(sql);

	try {
	  selectStmt.setFetchSize(1);
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}

	if (_autoAck) {
	  sql = ("DELETE FROM " + messageTable +
		 " WHERE m_id=? AND consumer IS NULL");
	}
	else
	  sql = ("UPDATE " + messageTable +
		 " SET consumer=?, delivered=1" +
		 " WHERE m_id=? AND consumer IS NULL");
      
	PreparedStatement updateStmt = conn.prepareStatement(sql);

	long id = -1;
	while (true) {
	  id = -1;

	  selectStmt.setLong(1, minId);
	  selectStmt.setInt(2, _queue.getId());
	  selectStmt.setLong(3, Alarm.getCurrentTime());

	  MessageImpl msg = null;

	  ResultSet rs = selectStmt.executeQuery();
	  while (rs.next()) {
	    id = rs.getLong(1);

	    minId = id;

	    msg = jdbcMessage.readMessage(rs);

	    if (_selector == null || _selector.isMatch(msg))
	      break;
	    else
	      msg = null;
	  }

	  rs.close();

	  if (msg == null)
	    return null;

	  if (_autoAck) {
	    updateStmt.setLong(1, id);
	  }
	  else {
	    updateStmt.setLong(1, _consumerId);
	    updateStmt.setLong(2, id);
	  }
	  
	  int updateCount = updateStmt.executeUpdate();
	
	  if (updateCount == 1)
	    return msg;
	}
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
    if (_autoAck)
      return;
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
    
      Connection conn = dataSource.getConnection();

      try {
	String sql = ("DELETE FROM " +  messageTable + " " +
		      "WHERE consumer=?");
      
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
   * Rollback all received messages from the session.
   */
  public void rollback()
    throws JMSException
  {
    if (_autoAck)
      return;
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
    
      Connection conn = dataSource.getConnection();

      try {
	String sql = ("UPDATE " +  messageTable +
		      " SET consumer=NULL " +
		      " WHERE consumer=?");
      
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

    if (now < _lastPurgeTime + QUEUE_TIMEOUT)
      return;

    _lastPurgeTime = now;
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      String consumerTable = _jdbcManager.getConsumerTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
	String sql = ("UPDATE " + messageTable +
		      " SET consumer=NULL" +
		      " WHERE consumer IS NOT NULL" +
		      "  AND EXISTS(SELECT * FROM " + consumerTable +
		      "             WHERE s_id=consumer AND expire<?)");

	PreparedStatement pstmt = conn.prepareStatement(sql);
	pstmt.setLong(1, Alarm.getCurrentTime());

	int count = pstmt.executeUpdate();
	pstmt.close();

	if (count > 0)
	  log.fine("JMSQueue[" + _queue.getName() + "] recovered " + count + " messages");

	sql = ("DELETE FROM " + consumerTable +
	       " WHERE expire<?");

	pstmt = conn.prepareStatement(sql);
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
	pstmt.setLong(1, Alarm.getCurrentTime() + QUEUE_TIMEOUT);
	pstmt.setLong(2, _consumerId);

	pstmt.executeUpdate();
      } finally {
	conn.close();
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(QUEUE_TIMEOUT / 4);
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

    _alarm.dequeue();
    
    try {
      deleteQueue();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    super.close();
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "JdbcQueueConsumer[" + _queue + "," + _consumerId + "]";
  }
}

