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
import java.sql.SQLException;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.JMSException;

import javax.sql.DataSource;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;

import com.caucho.jms.session.SessionImpl;

/**
 * Represents a JDBC queue browser.
 */
public class JdbcQueueBrowser implements QueueBrowser {
  static final Logger log = Log.open(JdbcQueueBrowser.class);
  static final L10N L = new L10N(JdbcQueueBrowser.class);

  private SessionImpl _session;
  private JdbcManager _jdbcManager;

  private JdbcQueue _queue;

  private String _messageSelector;
  private Selector _selector;
  
  private boolean _isClosed;

  public JdbcQueueBrowser(SessionImpl session, String messageSelector,
			  JdbcQueue queue)
    throws JMSException
  {
    _session = session;
    _jdbcManager = queue.getJdbcManager();
    _queue = queue;

    _messageSelector = messageSelector;
    if (_messageSelector != null) {
      SelectorParser parser = new SelectorParser();
      _selector = parser.parse(messageSelector);
    }
  }

  /**
   * Returns the queue.
   */
  public Queue getQueue()
  {
    return _queue;
  }

  /**
   * Returns the message selector.
   */
  public String getMessageSelector()
  {
    return _messageSelector;
  }

  /**
   * Returns an enumeration of the entries.
   */
  public Enumeration getEnumeration()
  {
    return new BrowserEnumeration();
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
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "JdbcQueueBrowser[" + _queue + "]";
  }

  class BrowserEnumeration implements Enumeration {
    private long _maxId = -1;
    private Message _msg;

    public boolean hasMoreElements()
    {
      if (_msg == null)
	getNextMessage();

      return _msg != null;
    }

    public Object nextElement()
    {
      if (_msg == null)
	getNextMessage();

      Message msg = _msg;
      _msg = null;
      
      return msg;
    }

    /**
     * Gets the next message matching the selector.
     */
    private Message getNextMessage()
    {
      try {
	Connection conn = _jdbcManager.getDataSource().getConnection();
	String messageTable = _jdbcManager.getMessageTable();

	try {
	  String sql = ("SELECT m_id, msg_type, delivered, body, header" +
			" FROM " + messageTable +
			" WHERE ?<m_id AND queue=?" +
			"  AND consumer IS NULL AND ?<expire");
	  
	  PreparedStatement pstmt = conn.prepareStatement(sql);
	  pstmt.setLong(1, _maxId);
	  pstmt.setInt(2, _queue.getId());
	  pstmt.setLong(3, Alarm.getCurrentTime());

	  ResultSet rs = pstmt.executeQuery();
	  while (rs.next()) {
	    _maxId = rs.getLong(1);

	    _msg = _jdbcManager.getJdbcMessage().readMessage(rs);

	    if (_selector == null || _selector.isMatch(_msg))
	      break;
	    else
	      _msg = null;
	  }

	  rs.close();
	  pstmt.close();
	} finally {
	  conn.close();
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      return _msg;
    }
  }
}

