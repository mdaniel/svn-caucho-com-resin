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

package com.caucho.jms;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import javax.sql.DataSource;

import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.jms.session.ConnectionImpl;

import com.caucho.jms.jdbc.JdbcManager;
import com.caucho.jms.jdbc.JdbcQueue;
import com.caucho.jms.jdbc.JdbcTopic;

import com.caucho.jms.memory.MemoryQueue;
import com.caucho.jms.memory.MemoryTopic;

/**
 * A sample connection factory.
 */
public class ConnectionFactoryImpl implements ConnectionFactory  {
  static final Logger log = Log.open(ConnectionFactoryImpl.class);
  static final L10N L = new L10N(ConnectionFactoryImpl.class);

  private String _clientID;
  
  private String _user;
  private String _password;

  private JdbcManager _jdbcManager;

  private List<ConnectionImpl> _connections =
    Collections.synchronizedList(new ArrayList<ConnectionImpl>());

  private HashMap<String,Queue> _queues =
    new HashMap<String,Queue>();

  private HashMap<String,Topic> _topics =
    new HashMap<String,Topic>();

  public ConnectionFactoryImpl()
  {
  }

  /**
   * Sets the user.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Sets the client id.
   */
  public void setClientID(String id)
  {
    _clientID = id;
  }

  /**
   * Sets the JDBC manager.
   */
  public void setDataSource(DataSource dataSource)
  {
    if (_jdbcManager == null)
      _jdbcManager = new JdbcManager();

    _jdbcManager.setDataSource(dataSource);
  }

  /**
   * Returns the JDBC manager.
   */
  public JdbcManager getJdbcManager()
  {
    return new JdbcManager();
  }

  /**
   * Initialize the connection factory.
   */
  public void init()
    throws ConfigException, SQLException
  {
    if (_jdbcManager != null)
      _jdbcManager.init();
  }

  /**
   * Creates a new queue connection
   */
  public Connection createConnection()
    throws JMSException
  {
    return createConnection(_user, _password);
  }

  /**
   * Creates a new connection
   *
   * @param username the username to authenticate with the server.
   * @param password the password to authenticate with the server.
   *
   * @return the created connection
   */
  public Connection createConnection(String username, String password)
    throws JMSException
  {
    authenticate(username, password);

    ConnectionImpl conn = new ConnectionImpl(this);

    if (_clientID != null) {
      if (findByClientID(_clientID) != null)
	throw new JMSException(L.l("ClientID[{0}] is only allowed for a single connection.",
				   _clientID));
      conn.setClientID(_clientID);
    }

    addConnection(conn);
    
    return conn;
  }

  protected void addConnection(ConnectionImpl conn)
  {
    _connections.add(conn);
  }

  /**
   * Returns the connection named by the specified client id.
   */
  public ConnectionImpl findByClientID(String id)
  {
    for (int i = 0; i < _connections.size(); i++) {
      ConnectionImpl conn = _connections.get(i);

      try {
	if (id.equals(conn.getClientID()))
	  return conn;
      } catch (Throwable e) {
      }
    }

    return null;
  }

  /**
   * Removes a connection once closed.
   */
  public void removeConnection(ConnectionImpl conn)
  {
    _connections.remove(conn);
  }

  /**
   * Creates queue.
   */
  public Queue createQueue(String name)
    throws JMSException
  {
    try {
      synchronized (_queues) {
	Queue queue = _queues.get(name);

	if (queue != null)
	  return queue;

	if (_jdbcManager != null) {
	  JdbcQueue jdbcQueue = new JdbcQueue();
	  jdbcQueue.setJdbcManager(_jdbcManager);
	  jdbcQueue.setQueueName(name);
	  jdbcQueue.init();

	  _queues.put(name, jdbcQueue);

	  return jdbcQueue;
	}
	else {
	  MemoryQueue memoryQueue = new MemoryQueue();
	  memoryQueue.setQueueName(name);

	  _queues.put(name, memoryQueue);

	  return memoryQueue;
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Creates topics.
   */
  public Topic createTopic(String name)
    throws JMSException
  {
    try {
      synchronized (_topics) {
	Topic topic = _topics.get(name);

	if (topic != null)
	  return topic;

	if (_jdbcManager != null) {
	  JdbcTopic jdbcTopic = new JdbcTopic();
	  jdbcTopic.setJdbcManager(_jdbcManager);
	  jdbcTopic.setTopicName(name);
	  jdbcTopic.init();

	  _topics.put(name, jdbcTopic);

	  return jdbcTopic;
	}
	else {
	  MemoryTopic memoryTopic = new MemoryTopic();
	  memoryTopic.setTopicName(name);

	  _topics.put(name, memoryTopic);

	  return memoryTopic;
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  protected void authenticate(String username, String password)
    throws JMSException
  {
    if (_user != null && ! _user.equals(username) ||
	_password != null && ! _password.equals(password)) {
      throw new JMSSecurityException(L.l("'{0}' is an unknown user",
					 username));
    }
  }
}
