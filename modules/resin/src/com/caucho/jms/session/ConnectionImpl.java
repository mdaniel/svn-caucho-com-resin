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

package com.caucho.jms.session;

import com.caucho.jms.ConnectionFactoryImpl;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A connection.
 */
public class ConnectionImpl implements Connection
{
  static final Logger log
    = Logger.getLogger(ConnectionImpl.class.getName());
  static final L10N L = new L10N(ConnectionImpl.class);

  private static int _clientIdGenerator;

  private ConnectionFactoryImpl _factory;
  
  private String _clientId;
  private boolean _isClientIdSet;
  
  private ExceptionListener _exceptionListener;

  private ArrayList<SessionImpl> _sessions = new ArrayList<SessionImpl>();

  private HashMap<String,TopicSubscriber> _durableSubscriberMap
    = new HashMap<String,TopicSubscriber>();

  private volatile boolean _isActive;
  private volatile boolean _isStopping;
  protected volatile boolean _isClosed;

  public ConnectionImpl(ConnectionFactoryImpl factory)
  {
    _factory = factory;
  }

  /**
   * Returns the connection's client identifier.
   */
  public String getClientID()
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    return _clientId;
  }

  /**
   * Sets the connections client identifier.
   *
   * @param the new client identifier.
   */
  public void setClientID(String clientId)
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    if (_isClientIdSet)
      throw new IllegalStateException(L.l("Can't set client id '{0}' after the connection has been used.",
					  clientId));

    ConnectionImpl oldConn = _factory.findByClientID(clientId);

    if (oldConn != null)
      throw new InvalidClientIDException(L.l("'{0}' is a duplicate client id.",
					     clientId));
    
    _clientId = clientId;
    _isClientIdSet = true;
  }

  /**
   * Returns the connection factory.
   */
  public ConnectionFactoryImpl getConnectionFactory()
  {
    return _factory;
  }

  /**
   * Returns the connection's exception listener.
   */
  public ExceptionListener getExceptionListener()
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    return _exceptionListener;
  }

  /**
   * Returns the connection's exception listener.
   */
  public void setExceptionListener(ExceptionListener listener)
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));

    assignClientID();
    
    _exceptionListener = listener;
  }

  /**
   * Returns the connection's metadata.
   */
  public ConnectionMetaData getMetaData()
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    return new ConnectionMetaDataImpl();
  }

  /**
   * Start (or restart) a connection.
   */
  public void start()
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    assignClientID();

    if (_isActive || _isStopping)
      return;

    _isActive = true;

    for (int i = 0; i < _sessions.size(); i++) {
      _sessions.get(i).start();
    }
  }

  /**
   * Stops the connection temporarily.
   */
  public void stop()
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    if (_isStopping || ! _isActive)
      return;
    
    assignClientID();

    _isStopping = true;

    try {
      for (int i = 0; i < _sessions.size(); i++) {
	_sessions.get(i).stop();
      }
    } finally {
      _isActive = false;
      _isStopping = false;
    }
  }

  /**
   * Returns true if the connection is started.
   */
  boolean isActive()
  {
    return _isActive;
  }

  /**
   * Returns true if the connection is stopping.
   */
  boolean isStopping()
  {
    return _isStopping;
  }

  /**
   * Creates a new connection session.
   */
  public Session createSession(boolean transacted, int acknowledgeMode)
    throws JMSException
  {
    checkOpen();
    
    assignClientID();
    
    return new SessionImpl(this, transacted, acknowledgeMode);
  }

  /**
   * Adds a session.
   */
  protected void addSession(SessionImpl session)
  {
    _sessions.add(session);
    
    if (_isActive)
      session.start();
  }

  /**
   * Removes a session.
   */
  void removeSession(SessionImpl session)
  {
    _sessions.remove(session);
  }

  /**
   * Gets a durable subscriber.
   */
  TopicSubscriber getDurableSubscriber(String name)
  {
    return _durableSubscriberMap.get(name);
  }

  /**
   * Adds a durable subscriber.
   */
  TopicSubscriber putDurableSubscriber(String name, TopicSubscriber subscriber)
  {
    return _durableSubscriberMap.put(name, subscriber);
  }

  /**
   * Removes a durable subscriber.
   */
  TopicSubscriber removeDurableSubscriber(String name)
  {
    return _durableSubscriberMap.remove(name);
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
    createConnectionConsumer(Destination destination,
			     String messageSelector,
			     ServerSessionPool sessionPool,
			     int maxMessages)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
    createDurableConnectionConsumer(Topic topic, String name,
				    String messageSelector,
				    ServerSessionPool sessionPool,
				    int maxMessages)
    throws JMSException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
    
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the connection.
   */
  public void close()
    throws JMSException
  {
    if (_isClosed)
      return;
    
    stop();
    
    _isClosed = true;

    _factory.removeConnection(this);

    ArrayList<SessionImpl> sessions = new ArrayList<SessionImpl>(_sessions);
    _sessions.clear();
    
    for (int i = 0; i < sessions.size(); i++) {
      try {
	sessions.get(i).close();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Checks that the session is open.
   */
  protected void checkOpen()
    throws IllegalStateException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("connection is closed"));
  }

  /**
   * Assigns a random client id.
   *
   * XXX: possibly wrong, i.e. shouldn't assign, for durable subscriptions
   */
  protected void assignClientID()
  {
    if (_clientId == null)
      _clientId = "resin-temp-" + _clientIdGenerator++;
    _isClientIdSet = true;
  }

  /**
   * automatically close if GC.
   */
  public void finalize()
  {
    // possible deadlock with the close, since it triggers
    // a rollback i.e. it's trying to do too much
    /*
    try {
      close();
    } catch (Throwable e) {
    }
    */
  }
}
