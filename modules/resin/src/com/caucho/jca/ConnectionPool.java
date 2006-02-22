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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jca;
  
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.transaction.TransactionManager;
import javax.transaction.Transaction;

import javax.transaction.xa.XAResource;
  
import javax.resource.ResourceException;
import javax.resource.NotSupportedException;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;

import javax.transaction.xa.Xid;

import javax.security.auth.Subject;

import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.LifoSet;
import com.caucho.util.WeakAlarm;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;

import com.caucho.log.Log;

import com.caucho.transaction.TransactionImpl;

import com.caucho.jmx.Jmx;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.jca.mbean.ConnectionPoolMBean;

/**
 * Implementation of the connection manager.
 */
public class ConnectionPool
  implements ConnectionManager, AlarmListener, ConnectionPoolMBean {
  private static final L10N L = new L10N(ConnectionPool.class);
  private static final Logger log = Log.open(ConnectionPool.class);

  private static int _idGen;

  private String _name;
  
  private UserTransactionProxy _tm;
  
  // the maximum number of connections
  private int _maxConnections = 20;
  
  // the maximum number of overflow connections
  private int _maxOverflowConnections = 0;

  // the maximum number of connections being created
  private int _maxCreateConnections = 5;
  
  // time before an idle connection is closed (30s default)
  private long _maxIdleTime = 30000L;
  
  // time before an active connection is closed (6h default)
  private long _maxActiveTime = 6L * 60000L * 1000L;
  
  // time a connection is allowed to be used (24h default)
  private long _maxPoolTime = 24L * 60000L * 1000L;
  
  // the time to wait for a connection
  private long _connectionWaitTime = 600 * 1000L;
  
  // the time to wait for a connection
  private long _connectionWaitCount = _connectionWaitTime / 1000L;

  // True if the connector supports local transactions.
  private boolean _enableLocalTransaction = true;

  // True if the connector supports XA transactions.
  private boolean _enableXA = true;

  // True if the local transaction optimization is allowed.
  private boolean _isLocalTransactionOptimization = true;
  
    // server/3087
  private boolean _isShareable = true;

  // If true, the save a stack trace when the collection is allocated
  private boolean _saveAllocationStackTrace = false;

  private final ArrayList<PoolItem> _pool = new ArrayList<PoolItem>();

  private final LifoSet<ManagedConnection> _idlePool
    = new LifoSet<ManagedConnection>();

  // temporary connection list for the alarm callback
  private final ArrayList<PoolItem> _alarmConnections
    = new ArrayList<PoolItem>();

  private Alarm _alarm;

  // time of the last validation check
  private long _lastValidCheckTime;

  private int _idCount;

  private int _createCount;

  private final Lifecycle _lifecycle = new Lifecycle();

  ConnectionPool()
  {
  }

  /**
   * Sets the connection pool name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the connection pool name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the transaction manager.
   */
  public void setTransactionManager(UserTransactionProxy tm)
  {
    _tm = tm;
  }

  /**
   * Returns the transaction manager.
   */
  public UserTransactionProxy getTransactionManager()
  {
    return _tm;
  }

  /**
   * Returns true if shared connections are allowed.
   */
  public boolean isShareable()
  {
    return _isShareable;
  }

  /**
   * Returns true if shared connections are allowed.
   */
  public void setShareable(boolean isShareable)
  {
    _isShareable = isShareable;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public boolean isLocalTransactionOptimization()
  {
    return _isLocalTransactionOptimization;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public void setLocalTransactionOptimization(boolean enable)
  {
    _isLocalTransactionOptimization = enable;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public boolean allowLocalTransactionOptimization()
  {
    return _isLocalTransactionOptimization && _isShareable;
  }

  /**
   * Returns true if a stack trace should be shared on allocation
   */
  public boolean getSaveAllocationStackTrace()
  {
    return _saveAllocationStackTrace;
  }

  /**
   * Returns true if a stack trace should be shared on allocation
   */
  public void setSaveAllocationStackTrace(boolean save)
  {
    _saveAllocationStackTrace = save;
  }

  /**
   * Set true for local transaction support.
   */
  public void setLocalTransaction(boolean localTransaction)
  {
    _enableLocalTransaction = localTransaction;
  }

  /**
   * Set true for local transaction support.
   */
  public boolean isLocalTransaction()
  {
    return _enableLocalTransaction;
  }

  /**
   * Set true for XA transaction support.
   */
  public void setXATransaction(boolean enable)
  {
    _enableXA = enable;
  }

  /**
   * Set true for XA transaction support.
   */
  public boolean isXATransaction()
  {
    return _enableXA;
  }

  /**
   * Returns the max idle time.
   */
  public long getMaxIdleTime()
  {
    if (Long.MAX_VALUE / 2 <= _maxIdleTime)
      return -1;
    else
      return _maxIdleTime;
  }

  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long maxIdleTime)
  {
    if (maxIdleTime < 0)
      _maxIdleTime = Long.MAX_VALUE / 2;
    else
      _maxIdleTime = maxIdleTime;
  }

  /**
   * Returns the max active time.
   */
  public long getMaxActiveTime()
  {
    if (Long.MAX_VALUE / 2 <= _maxActiveTime)
      return -1;
    else
      return _maxActiveTime;
  }

  /**
   * Sets the max active time.
   */
  public void setMaxActiveTime(long maxActiveTime)
  {
    if (maxActiveTime < 0)
      _maxActiveTime = Long.MAX_VALUE / 2;
    else
      _maxActiveTime = maxActiveTime;
  }

  /**
   * Returns the max pool time.
   */
  public long getMaxPoolTime()
  {
    if (Long.MAX_VALUE / 2 <= _maxPoolTime)
      return -1;
    else
      return _maxPoolTime;
  }

  /**
   * Sets the max pool time.
   */
  public void setMaxPoolTime(long maxPoolTime)
  {
    if (maxPoolTime < 0)
      _maxPoolTime = Long.MAX_VALUE / 2;
    else
      _maxPoolTime = maxPoolTime;
  }

  /**
   * Sets the max number of connections
   */
  public void setMaxConnections(int maxConnections)
    throws ConfigException
  {
    if (maxConnections == 0)
      throw new ConfigException(L.l("max-connections '0' must be at least 1."));
    
    _maxConnections = maxConnections;
    
    if (maxConnections < 0)
      _maxConnections = Integer.MAX_VALUE / 2;
  }

  /**
   * Gets the maximum number of connections
   */
  public int getMaxConnections()
  {
    if (_maxConnections < Integer.MAX_VALUE / 2)
      return _maxConnections;
    else
      return -1;
  }

  /**
   * Sets the time to wait for connections
   */
  public void setConnectionWaitTime(Period waitTime)
  {
    _connectionWaitTime = waitTime.getPeriod();

    if (_connectionWaitTime < 0)
      _connectionWaitTime = Long.MAX_VALUE / 2;

    _connectionWaitCount = _connectionWaitTime / 1000;
  }

  /**
   * Sets the time to wait for connections
   */
  public long getConnectionWaitTime()
  {
    if (_connectionWaitTime < Long.MAX_VALUE / 2)
      return _connectionWaitTime;
    else
      return -1;
  }

  /**
   * Sets the max number of overflow connections
   */
  public void setMaxOverflowConnections(int maxOverflowConnections)
  {
    _maxOverflowConnections = maxOverflowConnections;
  }

  /**
   * Gets the max number of overflow connections
   */
  public int getMaxOverflowConnections()
  {
    return _maxOverflowConnections;
  }

  /**
   * Sets the max number of connections simultaneously creating
   */
  public void setMaxCreateConnections(int maxConnections)
    throws ConfigException
  {
    if (maxConnections == 0)
      throw new ConfigException(L.l("max-create-connections '0' must be at least 1."));
    
    _maxCreateConnections = maxConnections;
    
    if (maxConnections < 0)
      _maxCreateConnections = Integer.MAX_VALUE / 2;

  }

  /**
   * Gets the maximum number of connections simultaneously creating
   */
  public int getMaxCreateConnections()
  {
    if (_maxCreateConnections < Integer.MAX_VALUE / 2)
      return _maxCreateConnections;
    else
      return -1;
  }

  /**
   * Initialize the connection manager.
   */
  public Object init(ManagedConnectionFactory mcf)
    throws ConfigException, ResourceException
  {
    if (! _lifecycle.toInit())
      return null;
    
    if (_name == null)
      _name = "connection-pool-" + _idGen++;

    if (_tm == null)
      throw new ConfigException(L.l("the connection manager needs a transaction manager."));

    _alarm = new WeakAlarm(this);

    try {
      Jmx.register(this, "type=ConnectionPool,name=" + _name);
    } catch (Throwable e) {
      log.log(Level.CONFIG, e.toString(), e);
    }

    if (! (mcf instanceof ValidatingManagedConnectionFactory)) {
      // never check
      _lastValidCheckTime = Long.MAX_VALUE / 2;
    }

    // recover any resources on startup
    if (_enableXA) {
      Subject subject = null;
      ManagedConnection mConn = mcf.createManagedConnection(subject, null);
      
      try {
	XAResource xa = mConn.getXAResource();

	_tm.recover(xa);
      } catch (NotSupportedException e) {
	_enableXA = false;
	log.finer(e.toString());
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      } finally {
	mConn.destroy();
      }
    }

    return mcf.createConnectionFactory(this);
  }

  /**
   * start the connection manager.
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    if (0 < _maxIdleTime && _maxIdleTime < 1000)
      _alarm.queue(1000);
    else if (1000 < _maxIdleTime && _maxIdleTime < 60000)
      _alarm.queue(_maxIdleTime);
    else
      _alarm.queue(60000);
  }

  /**
   * Generates a connection id.
   */
  String generateId()
  {
    return String.valueOf(_idCount++);
  }

  /**
   * Allocates the connection.
   *
   * @return connection handle for EIS specific connection.
   */
  public Object allocateConnection(ManagedConnectionFactory mcf,
				   ConnectionRequestInfo info)
    throws ResourceException
  {
    Subject subject = null;

    return allocate(mcf, subject, info);
  }

  /**
   * Adds a connection to the idle pool.
   */
  void toIdle(PoolItem item)
  {
    if (_pool.size() <= _maxConnections && ! item.isConnectionError()) {
      ManagedConnection mConn = item.getManagedConnection();
      if (mConn != null) {
	try {
	  mConn.cleanup();

	  synchronized (_idlePool) {
	    _idlePool.add(mConn);
	  }

	  synchronized (_pool) {
	    _pool.notify();
	  }

	  return;
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }

    toDead(item);
  }
  
  /**
   * Returns the delegated pool item.
   */
  public PoolItem getDelegatePoolItem(Xid xid)
  {
    ArrayList<PoolItem> pool = _pool;

    synchronized (pool) {
      int size = pool.size();
      for (int i = 0; i < size; i++) {
        PoolItem item = pool.get(i);

        if (xid.equals(item.getXid()))
          return item;
      }
    }
    
    return null;
  }

  /**
   * Returns the total connections.
   */
  public int getConnectionCount()
  {
    return _pool.size();
  }

  /**
   * Returns the idle connections.
   */
  public int getIdleConnectionCount()
  {
    return _idlePool.size();
  }

  /**
   * Returns the active connections.
   */
  public int getActiveConnectionCount()
  {
    return _pool.size() - _idlePool.size();
  }

  /**
   * Returns the transaction.
   */
  UserTransactionImpl getTransaction()
  {
    return _tm.getTransaction();
  }

  /**
   * Allocates a connection.
   */
  private Object allocate(ManagedConnectionFactory mcf,
			  Subject subject,
			  ConnectionRequestInfo info)
    throws ResourceException
  {
    UserPoolItem userPoolItem = null;
    
    try {
      UserTransactionImpl transaction = _tm.getTransaction();

      if (transaction == null)
	return allocatePool(mcf, subject, info, null).allocateUserConnection();

      userPoolItem = transaction.allocate(mcf, subject, info);

      if (userPoolItem == null)
	userPoolItem = allocatePool(mcf, subject, info, null);

      return userPoolItem.allocateUserConnection();
    } catch (RuntimeException e) {
      if (userPoolItem != null)
	userPoolItem.close();
	
      throw e;
    } catch (ResourceException e) {
      if (userPoolItem != null)
	userPoolItem.close();
	
      throw e;
    } catch (Throwable e) {
      if (userPoolItem != null)
	userPoolItem.close();
	
      throw new ResourceException(e);
    }
  }

  /**
   * Allocates a connection.
   */
  UserPoolItem allocatePool(ManagedConnectionFactory mcf,
			    Subject subject,
			    ConnectionRequestInfo info,
			    UserPoolItem oldUserItem)
    throws ResourceException
  {
    ManagedConnection mConn;

    long timeoutCount = _connectionWaitCount;

    while (_lifecycle.isActive()) {
      UserPoolItem userPoolItem = allocateIdle(mcf, subject,
					       info, oldUserItem);

      if (userPoolItem != null)
	return userPoolItem;

      userPoolItem = create(mcf, subject, info, false, oldUserItem);

      if (userPoolItem != null)
	return userPoolItem;

      if (timeoutCount-- < 0)
	break;
    }

    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("connection pool closed"));

    UserPoolItem userPoolItem = create(mcf, subject, info, true, oldUserItem);

    if (userPoolItem == null)
      throw new NullPointerException(L.l("ConnectionPool create should not return a null PoolItem for overflow connections."));

    return userPoolItem;
  }

  /**
   * Allocates a connection from the idle pool.
   */
  private UserPoolItem allocateIdle(ManagedConnectionFactory mcf,
				    Subject subject,
				    ConnectionRequestInfo info,
				    UserPoolItem oldUserItem)
    throws ResourceException
  {
    while (_lifecycle.isActive()) {
      ManagedConnection mConn;
      
      long now = Alarm.getCurrentTime();
	
      if (_lastValidCheckTime + 1000L < now) {
	_lastValidCheckTime = now;

	if (mcf instanceof ValidatingManagedConnectionFactory) {
	  ValidatingManagedConnectionFactory vmcf;
	  vmcf = (ValidatingManagedConnectionFactory) mcf;
	  
	  validate(vmcf);
	}
      }
	    
      // asks the Driver's ManagedConnectionFactory to match an
      // idle connection
      synchronized (_idlePool) {
	mConn = mcf.matchManagedConnections(_idlePool, subject, info);

	// If there are no more idle connections, return null
	if (mConn == null)
	  return null;
	
	_idlePool.remove(mConn);
      }

      PoolItem poolItem = null;

      synchronized (_pool) {
	for (int i = _pool.size() - 1; i >= 0; i--) {
	  poolItem = _pool.get(i);

	  if (poolItem.getManagedConnection() == mConn)
	    break;
	}
      }

      if (poolItem == null)
	throw new IllegalStateException(L.l("No matching PoolItem found for {0}",
					    mConn));

      UserPoolItem userPoolItem = null;
      
      // Ensure the connection is still valid
      userPoolItem = poolItem.toActive(subject, info, oldUserItem);
      if (userPoolItem != null)
	return userPoolItem;

      toDead(poolItem);
    }

    return null;
  }

  /**
   * Validates the pool.
   */
  private void validate(ValidatingManagedConnectionFactory mcf)
  {
    Set invalid = null;

    synchronized (_idlePool) {
    }    
  }

  /**
   * Creates a new connection.
   */
  private UserPoolItem create(ManagedConnectionFactory mcf,
			      Subject subject,
			      ConnectionRequestInfo info,
			      boolean isOverflow,
			      UserPoolItem oldUserItem)
    throws ResourceException
  {
    synchronized (_pool) {
      int size = _pool.size();
      
      if (isOverflow &&
	  _maxConnections + _maxOverflowConnections <= _createCount + size) {
	throw new ResourceException(L.l("Connection pool is full.  Can't allocate connection."));
      }
      // if the pool is full, don't create, and wait
      else if (! isOverflow &&
	       (_maxConnections <= _createCount + size ||
		_maxCreateConnections <= _createCount)) {
	try {
	  _pool.wait(1000);
	} catch (Throwable e) {
	  log.log(Level.FINE, e.toString(), e);
	}
	
	return null;
      }
      
      _createCount++;
    }

    PoolItem poolItem = null;
    try {
      ManagedConnection mConn = mcf.createManagedConnection(subject, info);

      if (mConn == null)
	throw new ResourceException(L.l("'{0}' did not return a connection from createManagedConnection",
					mcf));
      
      poolItem = new PoolItem(this, mcf, mConn);

      UserPoolItem userPoolItem;

      // Ensure the connection is still valid
      userPoolItem = poolItem.toActive(subject, info, oldUserItem);
      if (userPoolItem != null)
	return userPoolItem;

      throw new IllegalStateException(L.l("Connection '{0}' was not valid on creation",
					  poolItem));
    } finally {
      synchronized (_pool) {
	_createCount--;

	if (poolItem != null)
	  _pool.add(poolItem);

	_pool.notify();
      }
    }
  }

  /**
   * Alarm listener.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      long now = Alarm.getCurrentTime();
      
      _alarmConnections.clear();

      synchronized (_pool) {
	_alarmConnections.addAll(_pool);
      }

      for (int i = _alarmConnections.size() - 1; i >= 0; i--) {
	PoolItem item = _alarmConnections.get(i);

	if (! item.isValid())
	  toDead(item);
      }
      
      _alarmConnections.clear();
    } finally {
      if (! _lifecycle.isActive()) {
      }
      else if (0 < _maxIdleTime && _maxIdleTime < 1000)
	_alarm.queue(1000);
      else if (1000 < _maxIdleTime && _maxIdleTime < 60000)
	_alarm.queue(_maxIdleTime);
      else
	_alarm.queue(60000);
    }
  }

  /**
   * Removes a connection
   */
  void toDead(PoolItem item)
  {
    synchronized (_idlePool) {
      _idlePool.remove(item.getManagedConnection());
    }
      
    synchronized (_pool) {
      _pool.remove(item);
    }

    try {
      item.destroy();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Stops the manager.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    if (_alarm != null)
      _alarm.dequeue();
  }

  /**
   * Destroys the manager.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    ArrayList<PoolItem> pool = _pool;

    synchronized (pool) {
      for (int i = 0; i < pool.size(); i++) {
	PoolItem poolItem = pool.get(i);

	try {
	  poolItem.destroy();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      pool.clear();
    }
  }

  public String toString()
  {
    return "ConnectionPool[" + getName() + "]";
  }
}
