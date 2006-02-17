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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

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

import com.caucho.jms.message.MessageImpl;

import com.caucho.jms.session.SessionImpl;
import com.caucho.jms.session.MessageConsumerImpl;

/**
 * A jdbc queue.
 */
public class JdbcQueue extends JdbcDestination implements Queue {
  static final Logger log = Log.open(JdbcQueue.class);
  static final L10N L = new L10N(JdbcQueue.class);

  private int _id;

  public JdbcQueue()
  {
  }

  /**
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return getName();
  }

  /**
   * Sets the queue's name.
   */
  public void setQueueName(String name)
  {
    setName(name);
  }

  /**
   * Returns the JDBC id for the queue.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Initializes the JdbcQueue
   */
  public void init()
    throws ConfigException, SQLException
  {
    if (_jdbcManager.getDataSource() == null)
      throw new ConfigException(L.l("JdbcQueue requires a <data-source> element."));
    
    if (getName() == null)
      throw new ConfigException(L.l("JdbcQueue requires a <queue-name> element."));
    
    _jdbcManager.init();

    _id = createDestination(getName(), false);
  }

  /**
   * Creates a consumer.
   */
  public MessageConsumerImpl createConsumer(SessionImpl session,
					    String selector,
					    boolean noWait)
    throws JMSException
  {
    return new JdbcQueueConsumer(session, selector, _jdbcManager, this);
  }

  /**
   * Creates a browser.
   */
  public QueueBrowser createBrowser(SessionImpl session, String selector)
    throws JMSException
  {
    return new JdbcQueueBrowser(session, selector, this);
  }

  /**
   * Sends the message to the queue.
   */
  public void send(Message message)
    throws JMSException
  {
    long expireTime = message.getJMSExpiration();
    if (expireTime <= 0)
      expireTime = Long.MAX_VALUE / 2;

    purgeExpiredMessages();
    
    try {
      _jdbcManager.getJdbcMessage().send(message, _id, expireTime);
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    messageAvailable();
  }

  /**
   * Removes the first message matching the selector.
   */
  public void commit(int session)
    throws JMSException
  {
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "JdbcQueue[" + getName() + "]";
  }
}

