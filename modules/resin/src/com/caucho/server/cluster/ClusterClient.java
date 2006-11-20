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

package com.caucho.server.cluster;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.lifecycle.Lifecycle;

/**
 * Represents a connection to one of the servers in a distribution group.
 * A {@link ClusterServer} is used to define the properties of the server
 * that is connected to.
 *
 * <pre>
 *  resin:name=web-a,type=ClusterServer
 * </pre>
 */
public final class ClusterClient {
  private static final Logger log
    = Logger.getLogger(ClusterClient.class.getName());

  private static final int DISABLED = 0;
  private static final int ENABLED = 1;
  private static final int ENABLED_SESSION = 2;

  private static final int ST_NEW = 0;
  private static final int ST_STANDBY = 1;
  private static final int ST_SESSION_ONLY = 2;
  // the following 5 are the active states
  private static final int ST_STARTING = 3;
  private static final int ST_WARMUP = 4;
  private static final int ST_BUSY = 5;
  private static final int ST_FAIL = 6;
  private static final int ST_ACTIVE = 7;
  private static final int ST_CLOSED = 8;

  private ServerConnector _server;

  private String _debugId;

  private int _maxConnections = Integer.MAX_VALUE / 2;

  private volatile int _enabledMode = ENABLED;
  
  private ClusterStream []_idle = new ClusterStream[64];
  private volatile int _idleHead;
  private volatile int _idleTail;
  private int _idleSize = 16;

  private int _streamCount;

  private long _failRecoverTime;
  private long _warmupTime;

  private volatile int _state = ST_NEW;

  private volatile long _lastFailTime;
  private volatile long _lastBusyTime;
  private volatile long _firstConnectTime;

  private volatile int _activeCount;
  private volatile int _startingCount;
  
  private volatile long _keepaliveCountTotal;
  private volatile long _connectCountTotal;
  private volatile long _failCountTotal;
  private volatile long _busyCountTotal;

  private volatile double _cpuLoadAvg;

  ClusterClient(ServerConnector server)
  {
    _server = server;

    Cluster cluster = Cluster.getLocal();

    String selfId = null;
    if (cluster != null)
      selfId = cluster.getId();

    if (selfId == null || selfId.equals(""))
      selfId = "default";

    String targetId = server.getId();
    if (targetId == null || targetId.equals(""))
      targetId = String.valueOf(server.getIndex());

    _debugId = selfId + "->" + targetId;

    _failRecoverTime = server.getLoadBalanceRecoverTime();
    _warmupTime = server.getLoadBalanceWarmupTime();

    _state = ST_STARTING;
  }

  /**
   * Returns the cluster server.
   */
  public ServerConnector getServer()
  {
    return _server;
  }

  /**
   * Returns the number of active connections.
   */
  public int getActiveCount()
  {
    return _activeCount;
  }

  /**
   * Returns the number of idle connections.
   */
  public int getIdleCount()
  {
    return (_idleHead - _idleTail + _idle.length) % _idle.length;
  }

  /**
   * Returns the total number of successful socket connections
   */
  public long getConnectCountTotal()
  {
    return _connectCountTotal;
  }

  /**
   * Returns the number of times a keepalive connection has been used.
   */
  public long getKeepaliveCountTotal()
  {
    return _keepaliveCountTotal;
  }

  /**
   * Returns the total number of failed connect attempts.
   */
  public long getFailCountTotal()
  {
    return _failCountTotal;
  }

  /**
   * Returns the time of the last failure.
   */
  public Date getLastFailTime()
  {
    return new Date(_lastFailTime);
  }

  /**
   * Returns the count of busy connections.
   */
  public long getBusyCountTotal()
  {
    return _busyCountTotal;
  }

  /**
   * Returns the time of the last busy.
   */
  public Date getLastBusyTime()
  {
    return new Date(_lastBusyTime);
  }

  /**
   * Sets the CPU load avg (from backend).
   */
  public void setCpuLoadAvg(double load)
  {
    _cpuLoadAvg = load;
  }

  /**
   * Gets the CPU load avg
   */
  public double getCpuLoadAvg()
  {
    return _cpuLoadAvg;
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    return _debugId;
  }

  /**
   * Returns true if the server is active.
   */
  public final boolean isActive()
  {
    switch (_state) {
    case ST_ACTIVE:
      return true;

    case ST_STANDBY:
    case ST_CLOSED:
      return false;

    case ST_FAIL:
      return (_lastFailTime + _failRecoverTime <= Alarm.getCurrentTime());
      
    default:
      return false;
    }
  }

  /**
   * Returns the lifecycle state.
   */
  public String getState()
  {
    switch (_state) {
    case ST_NEW:
      return "init";
    case ST_STANDBY:
      return "standby";
    case ST_SESSION_ONLY:
      return "session-only";
    case ST_STARTING:
      return "starting";
    case ST_WARMUP:
      return "warmup";
    case ST_BUSY:
      return "busy";
    case ST_FAIL:
      return "fail";
    case ST_ACTIVE:
      return "active";
    case ST_CLOSED:
      return "closed";
    default:
      return "unknown(" + _state + ")";
    }
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenSoft()
  {
    int state = _state;
    
    if (state == ST_ACTIVE)
      return true;
    else if (ST_STARTING <= state && state < ST_ACTIVE) {
      long now = Alarm.getCurrentTime();

      if (now < _lastFailTime + _failRecoverTime)
	return false;
      if (now < _lastBusyTime + _failRecoverTime)
	return false;

      long warmupCount;
      long warmupTime = _warmupTime;

      if (_firstConnectTime <= 0) {
	toWarmup();
	warmupCount = 0;
      }
      else if (warmupTime <= 0)
	warmupCount = Integer.MAX_VALUE;
      else
	warmupCount = 16 * (now - _firstConnectTime) / warmupTime;

      // Warmup time splits into 16 parts.  The first 4 allow 1 request
      // at a time.  After that, each segment doubles the allowed requests

      if (warmupCount > 16) {
	toActive();
      
	return true;
      }

      int idleCount = getIdleCount();
      int totalCount = _activeCount + _startingCount + idleCount;
      
      if (totalCount <= 1)
	return true;
      else if (warmupCount < 8)
	return false;
      else if (totalCount < (1 << (warmupCount - 8))) {
	return true;
      }
      else
	return false;
    }
    else {
      return false;
    }
  }

  /**
   * Return true if active.
   */
  public boolean isEnabled()
  {
    int state = _state;
    
    return ST_STARTING <= state && state <= ST_ACTIVE;
  }
  
  private void toActive()
  {
    synchronized (this) {
      if (_state < ST_CLOSED)
	_state = ST_ACTIVE;
    }
  }
  
  private void toWarmup()
  {
    synchronized (this) {
      if (ST_STARTING <= _state && _state < ST_CLOSED) {
	_state = ST_WARMUP;
	_firstConnectTime = Alarm.getCurrentTime();
      }
    }
  }
  
  public void toBusy()
  {
    _lastBusyTime = Alarm.getCurrentTime();
    _firstConnectTime = 0;
    
    synchronized (this) {
      _busyCountTotal++;
      
      if (_state < ST_CLOSED)
	_state = ST_BUSY;
    }
  }
  
  public void toFail()
  {
    _lastFailTime = Alarm.getCurrentTime();
    _firstConnectTime = 0;
    
    synchronized (this) {
      _failCountTotal++;
      
      if (_state < ST_CLOSED)
	_state = ST_FAIL;
    }

    clearRecycle();
  }

  /**
   * Enable the client.
   */
  public void start()
  {
    synchronized (this) {
      if (_state == ST_ACTIVE) {
      }
      else if (_state < ST_CLOSED)
	_state = ST_STARTING;
    }
  }

  /**
   * Disable the client.
   */
  public void stop()
  {
    synchronized (this) {
      if (_state < ST_CLOSED)
	_state = ST_STANDBY;
    }
  }

  /**
   * Session only
   */
  public void enableSessionOnly()
  {
    synchronized (this) {
      if (_state < ST_CLOSED && _state != ST_STANDBY)
	_state = ST_SESSION_ONLY;
    }
  }

  /**
   * Open a stream to the target server.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openSoft()
  {
    int state = _state;

    if (! (ST_STARTING <= state && state <= ST_ACTIVE))
      return null;

    long now = Alarm.getCurrentTime();

    if (now < _lastFailTime + _failRecoverTime)
      return null;
    if (now < _lastBusyTime + _failRecoverTime)
      return null;

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    if (canOpenSoft()) {
      return connect();
    }
    else
      return null;
  }

  /**
   * Open a stream to the target server object persistence.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openIfLive()
  {
    if (_state == ST_CLOSED)
      return null;

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    long now = Alarm.getCurrentTime();

    if (now < _lastFailTime + _failRecoverTime)
      return null;

    return connect();
  }

  /**
   * Open a stream to the target server for a session.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openForSession()
  {
    int state = _state;
    if (! (ST_SESSION_ONLY <= state && state < ST_CLOSED)) {
      return null;
    }

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    long now = Alarm.getCurrentTime();

    if (now < _lastFailTime + _failRecoverTime) {
      return null;
    }
    
    if (now < _lastBusyTime + _failRecoverTime) {
      return null;
    }

    return connect();
  }

  /**
   * Open a stream to the target server for the load balancer.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream open()
  {
    int state = _state;
    if (! (ST_STARTING <= state && state < ST_CLOSED))
      return null;

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    return connect();
  }

  /**
   * Returns a valid recycled stream from the idle pool to the backend.
   *
   * If the stream has been in the pool for too long (> live_time),
   * close it instead.
   *
   * @return the socket's read/write pair.
   */
  private ClusterStream openRecycle()
  {
    long now = Alarm.getCurrentTime();
    ClusterStream stream = null;

    synchronized (this) {
      if (_idleHead != _idleTail) {
        stream = _idle[_idleHead];
        long freeTime = stream.getFreeTime();

        _idle[_idleHead] = null;
        _idleHead = (_idleHead + _idle.length - 1) % _idle.length;

        if (now < freeTime + _server.getLoadBalanceIdleTime()) {
          _activeCount++;
	  _keepaliveCountTotal++;

          return stream;
        }
      }
    }

    if (stream != null)
      stream.closeImpl();

    return null;
  }

  /**
   * Connect to the backend server.
   *
   * @return the socket's read/write pair.
   */
  private ClusterStream connect()
  {
    synchronized (this) {
      if (_maxConnections <= _activeCount + _startingCount)
	return null;
      
      _startingCount++;
    }
	  
    try {
      ReadWritePair pair = _server.openTCPPair();
      ReadStream rs = pair.getReadStream();
      rs.setAttribute("timeout", new Integer((int) _server.getSocketTimeout()));

      synchronized (this) {
        _activeCount++;
	_connectCountTotal++;

	if (_firstConnectTime <= 0) {
	  if (ST_STARTING <= _state && _state < ST_ACTIVE) {
	    _state = ST_WARMUP;
	    _firstConnectTime = Alarm.getCurrentTime();
	  }
	}
      }

      ClusterStream stream = new ClusterStream(_streamCount++, this,
					       rs, pair.getWriteStream());
      
      if (log.isLoggable(Level.FINER))
	log.finer("connect " + stream);
      
      return stream;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      toFail();
      
      return null;
    } finally {
      synchronized (this) {
	_startingCount--;
      }
    }
  }

  /**
   * We now know that the server is live, e.g. if a sibling has
   * contacted us.
   */
  public void wake()
  {
    synchronized (this) {
      _lastFailTime = 0;
      if (_state == ST_FAIL) {
	_state = ST_STARTING;
      }
    }
  }

  /**
   * Free the read/write pair for reuse.  Called only from
   * ClusterStream.free()
   */
  void free(ClusterStream stream)
  {
    synchronized (this) {
      _activeCount--;

      int size = (_idleHead - _idleTail + _idle.length) % _idle.length;

      if (_state != ST_CLOSED && size < _idleSize) {
        _idleHead = (_idleHead + 1) % _idle.length;
        _idle[_idleHead] = stream;

	stream = null;
      }
    }

    long now = Alarm.getCurrentTime();
    long maxIdleTime = _server.getLoadBalanceIdleTime();
    ClusterStream oldStream = null;
    
    do {
      oldStream = null;

      synchronized (this) {
	if (_idleHead != _idleTail) {
	  int nextTail = (_idleTail + 1) % _idle.length;
	  
	  oldStream = _idle[nextTail];

	  if (oldStream != null
	      && oldStream.getFreeTime() + maxIdleTime < now) {
	    _idle[nextTail] = null;
	    _idleTail = nextTail;
	  }
	  else
	    oldStream = null;
	}
      }

      if (oldStream != null)
	oldStream.closeImpl();
    } while (oldStream != null);

    if (stream != null)
      stream.closeImpl();
  }

  /**
   * Closes the read/write pair for reuse.  Called only
   * from ClusterStream.close().
   */
  void close(ClusterStream stream)
  {
    if (log.isLoggable(Level.FINER))
      log.finer("close " + stream);
    
    synchronized (this) {
      _activeCount--;
    }
  }

  /**
   * Clears the recycled connections, e.g. on detection of backend
   * server going down.
   */
  public void clearRecycle()
  {
    ArrayList<ClusterStream> recycleList = null;

    synchronized (this) {
      _idleHead = _idleTail = 0;

      for (int i = 0; i < _idle.length; i++) {
        ClusterStream stream;

        stream = _idle[i];
        _idle[i] = null;

        if (stream != null) {
          if (recycleList == null)
            recycleList = new ArrayList<ClusterStream>();

          recycleList.add(stream);
        }
      }
    }

    if (recycleList != null) {
      for (ClusterStream stream : recycleList) {
        stream.closeImpl();
      }
    }
  }

  /**
   * Close the client
   */
  public void close()
  {
    synchronized (this) {
      if (_state == ST_CLOSED)
	return;

      _state = ST_CLOSED;
    }
    
    synchronized (this) {
      _idleHead = _idleTail = 0;
    }

    for (int i = 0; i < _idle.length; i++) {
      ClusterStream stream;

      synchronized (this) {
        stream = _idle[i];
        _idle[i] = null;
      }

      if (stream != null)
        stream.closeImpl();
    }
  }

  public String toString()
  {
    return ("ClusterClient[" + _server + "]");
  }
}
