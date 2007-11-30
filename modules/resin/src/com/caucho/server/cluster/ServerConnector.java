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

import com.caucho.management.server.ServerConnectorMXBean;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.vfs.*;
import com.caucho.server.resin.*;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines a member of the cluster.
 *
 * A {@link ClusterClient} obtained with {@link #getClient} is used to actually
 * communicate with this ClusterServer when it is active in another instance of
 * Resin .
 */
public class ServerConnector
{
  private static final Logger log
    = Logger.getLogger(ServerConnector.class.getName());
  private static final L10N L = new L10N(ServerConnector.class);

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

  // number of chunks in the throttling
  private static final int WARMUP_MAX = 16;
  private static final int WARMUP_MIN = -16;
  private static final int []WARMUP_CONNECTION_MAX
    = new int[] { 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 8, 8, 16, 32, 64, 128 };

  private ClusterServer _server;
  private ClusterPort _port;

  private ObjectName _objectName;

  private Cluster _cluster;

  private int _srunIndex;
  private Path _tcpPath;

  private ServerConnectorAdmin _admin;

  private String _debugId;

  private int _maxConnections = Integer.MAX_VALUE / 2;

  private volatile int _enabledMode = ENABLED;
  
  private ClusterStream []_idle = new ClusterStream[64];
  private volatile int _idleHead;
  private volatile int _idleTail;
  private int _idleSize = 16;

  private int _streamCount;

  private long _warmupTime;
  private long _warmupChunkTime;
  
  private long _failRecoverTime;
  private long _failChunkTime;

  private volatile int _state = ST_NEW;

  // current connection count
  private volatile int _activeCount;
  private volatile int _startingCount;

  private volatile int _loadBalanceAllocateCount;

  // numeric value representing the throttle state
  private volatile int _warmupState;
  
  // load management data
  private volatile long _lastFailConnectTime;
  private volatile long _dynamicFailRecoverTime = 1000L;
  
  private volatile long _lastFailTime;
  private volatile long _lastBusyTime;

  private volatile long _failTime;
  private volatile long _firstSuccessTime;
  private volatile long _lastSuccessTime;
  private volatile long _prevSuccessTime;
  private volatile double _latencyFactor;

  // statistics
  private volatile long _keepaliveCountTotal;
  private volatile long _connectCountTotal;
  private volatile long _failCountTotal;
  private volatile long _busyCountTotal;

  private volatile double _cpuLoadAvg;
  private volatile long _cpuSetTime;

  public ServerConnector(ClusterServer server)
  {
    _server = server;
    _cluster = _server.getCluster();
    _port = server.getClusterPort();

    _warmupTime = server.getLoadBalanceWarmupTime();
    _warmupChunkTime = _warmupTime / WARMUP_MAX;
    if (_warmupChunkTime <= 0)
      _warmupChunkTime = 1;
      
    _failRecoverTime = server.getLoadBalanceRecoverTime();
    _failChunkTime = _failRecoverTime / WARMUP_MAX;
    if (_failChunkTime <= 0)
      _failChunkTime = 1;

    _state = ST_STARTING;
  }

  /**
   * Gets the owning cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName;
  }

  /**
   * Returns the admin.
   */
  public ServerConnectorMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Gets the cluster port.
   */
  public ClusterPort getClusterPort()
  {
    return _port;
  }

  /**
   * Returns the user-readable id of the target server.
   */
  public String getId()
  {
    return _server.getId();
  }

  /**
   * Returns the index of this connection in the connection group.
   */
  public int getIndex()
  {
    return _server.getIndex();
  }

  /**
   * Returns the hostname of the target server.
   */
  public String getAddress()
  {
    return _port.getAddress();
  }

  /**
   * Gets the port of the target server.
   */
  public int getPort()
  {
    return _port.getPort();
  }

  /**
   * Returns the time in milliseconds for the slow start throttling.
   */
  public long getLoadBalanceWarmupTime()
  {
    return _server.getLoadBalanceWarmupTime();
  }

  /**
   * Returns the socket timeout when connecting to the
   * target server.
   */
  public long getLoadBalanceConnectTimeout()
  {
    return _server.getLoadBalanceConnectTimeout();
  }

  /**
   * Returns the socket timeout when reading from the
   * target server.
   */
  public long getLoadBalanceSocketTimeout()
  {
    return _server.getLoadBalanceSocketTimeout();
  }

  /**
   * Returns how long the connection can be cached in the free pool.
   */
  public long getLoadBalanceIdleTime()
  {
    return _server.getLoadBalanceIdleTime();
  }

  /**
   * Returns how long the connection will be treated as dead.
   */
  public long getLoadBalanceRecoverTime()
  {
    return _server.getLoadBalanceRecoverTime();
  }

  /**
   * Returns the load balance weight.
   */
  public int getLoadBalanceWeight()
  {
    return _server.getLoadBalanceWeight();
  }

  /**
   * Initialize
   */
  public void init()
    throws Exception
  {
    String address = getAddress();

    if (address == null)
      address = "localhost";

    HashMap<String,Object> attr = new HashMap<String,Object>();
    attr.put("connect-timeout", new Long(getLoadBalanceConnectTimeout()));

    if (_port.isSSL())
      _tcpPath = Vfs.lookup("tcps://" + address + ":" + getPort(), attr);
    else
      _tcpPath = Vfs.lookup("tcp://" + address + ":" + getPort(), attr);

    _admin = new ServerConnectorAdmin(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      Resin resin = Resin.getLocal();

      if (resin != null)
	thread.setContextClassLoader(resin.getClassLoader());
      
      String name = getId();

      if (name == null)
        name = "";

      _admin.register();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
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
   * Returns the number of load balance allocations
   */
  public int getLoadBalanceAllocateCount()
  {
    return _loadBalanceAllocateCount;
  }

  /**
   * Allocate a connection for load balancing.
   */
  public void allocateLoadBalance()
  {
    synchronized (this) {
      _loadBalanceAllocateCount++;
    }
  }

  /**
   * Free a connection for load balancing.
   */
  public void freeLoadBalance()
  {
    synchronized (this) {
      _loadBalanceAllocateCount--;
    }
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
   * Returns the time of the last failure.
   */
  public Date getLastFailConnectTime()
  {
    return new Date(_lastFailConnectTime);
  }

  /**
   * Returns the time of the last failure.
   */
  public long getLastSuccessTime()
  {
    return _lastSuccessTime;
  }

  /**
   * Returns the latency factory
   */
  public double getLatencyFactor()
  {
    return _latencyFactor;
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
    _cpuSetTime = Alarm.getCurrentTime();
    _cpuLoadAvg = load;
  }

  /**
   * Gets the CPU load avg
   */
  public double getCpuLoadAvg()
  {
    double avg = _cpuLoadAvg;
    long time = _cpuSetTime;

    long now = Alarm.getCurrentTime();

    if (now - time < 10000L)
      return avg;
    else
      return avg * 10000L / (now - time);
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    if (_debugId == null) {
      String selfId = null;
      Cluster localCluster = Cluster.getLocal();
      if (localCluster != null)
	selfId = localCluster.getId();

      if (selfId == null || selfId.equals(""))
	selfId = "default";

      String targetId = _server.getId();
      if (targetId == null || targetId.equals(""))
	targetId = String.valueOf(_server.getIndex());

      _debugId = selfId + "->" + targetId;
    }
    
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
      return (_failTime + _failRecoverTime <= Alarm.getCurrentTime());
      
    default:
      return false;
    }
  }
  
  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    return ! isActive();
  }

  /**
   * Enable the client
   */
  public void enable()
  {
    start();
  }

  /**
   * Disable the client
   */
  public void disable()
  {
    stop();
  }

  /**
   * Returns the lifecycle state.
   */
  public String getState()
  {
    updateWarmup();
    
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
  public boolean canOpenSoftOrRecycle()
  {
    return getIdleCount() > 0 || canOpenSoft();
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

      if (now < _lastFailConnectTime + _dynamicFailRecoverTime) {
	return false;
      }

      int warmupState = _warmupState;

      if (warmupState < 0) {
	return (_failTime - warmupState * _failChunkTime < now);
      }
      else if (WARMUP_MAX <= warmupState)
	return true;

      int connectionMax = WARMUP_CONNECTION_MAX[warmupState];

      int idleCount = getIdleCount();
      int activeCount = _activeCount + _startingCount;
      int totalCount = activeCount + idleCount;
      
      return totalCount < connectionMax;
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
  
  public void toBusy()
  {
    _lastBusyTime = Alarm.getCurrentTime();
    _firstSuccessTime = 0;
    
    synchronized (this) {
      _busyCountTotal++;
      
      if (_state < ST_CLOSED)
	_state = ST_BUSY;
    }
  }
  
  public void toFail()
  {
    _failTime = Alarm.getCurrentTime();
    _lastFailTime = _failTime;
    _firstSuccessTime = 0;
    
    synchronized (this) {
      _failCountTotal++;
      
      if (_state < ST_CLOSED)
	_state = ST_FAIL;
    }

    clearRecycle();
  }

  /**
   * Called when the socket read/write fails.
   */
  public void failSocket()
  {
    synchronized (this) {
      _failCountTotal++;
      
      long now = Alarm.getCurrentTime();
      _firstSuccessTime = 0;

      // only degrade one per 100ms
      if (now - _failTime >= 100) {
	_warmupState--;
	_failTime = now;
	_lastFailTime = _failTime;
      }

      if (_warmupState < WARMUP_MIN)
	_warmupState = WARMUP_MIN;
      
      if (_state < ST_CLOSED)
	_state = ST_FAIL;
    }
  }

  /**
   * Called when the socket read/write fails.
   */
  public void failConnect()
  {
    synchronized (this) {
      _failCountTotal++;
      
      _firstSuccessTime = 0;

      // only degrade one per 100ms
      _warmupState--;
      long now = Alarm.getCurrentTime();
      _failTime = now;
      _lastFailTime = _failTime;
      _lastFailConnectTime = now;
      _dynamicFailRecoverTime *= 2;
      if (_failRecoverTime < _dynamicFailRecoverTime)
	_dynamicFailRecoverTime = _failRecoverTime;

      if (_warmupState < WARMUP_MIN)
	_warmupState = WARMUP_MIN;
      
      if (_state < ST_CLOSED)
	_state = ST_FAIL;
    }
  }

  /**
   * Called when the server responds with "busy", e.g. HTTP 503
   */
  public void busy()
  {
    synchronized (this) {
      _lastBusyTime = Alarm.getCurrentTime();
      _firstSuccessTime = 0;
      
      _warmupState--;
      if (_warmupState < 0)
	_warmupState = 0;
    
      _busyCountTotal++;
      
      if (_state < ST_CLOSED)
	_state = ST_BUSY;
    }
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

    if (! (ST_STARTING <= state && state <= ST_ACTIVE)) {
      return null;
    }

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    if (canOpenSoft()) {
      return connect();
    }
    else {
      return null;
    }
  }

  /**
   * Open a stream to the target server object persistence.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openIfLive()
  {
    if (_state == ST_CLOSED) {
      return null;
    }

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    long now = Alarm.getCurrentTime();

    if (now < _failTime + _failRecoverTime) {
      return null;
    }

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

    if (now < _failTime + _failRecoverTime) {
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
      ReadWritePair pair = openTCPPair();
      ReadStream rs = pair.getReadStream();
      rs.setAttribute("timeout", new Integer((int) getLoadBalanceSocketTimeout()));

      synchronized (this) {
        _activeCount++;
	_connectCountTotal++;
      }

      ClusterStream stream = new ClusterStream(_streamCount++, this,
					       rs, pair.getWriteStream());
      
      if (log.isLoggable(Level.FINER))
	log.finer("connect " + stream);
      
      return stream;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      failConnect();
      
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
      if (_state == ST_FAIL) {
	_state = ST_STARTING;
      }

      _failTime = 0;
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

      long now = Alarm.getCurrentTime();

      long prevSuccessTime = _prevSuccessTime;

      if (prevSuccessTime > 0) {
	_latencyFactor = (0.95 * _latencyFactor
			  + 0.05 * (now - prevSuccessTime));
      }

      if (_activeCount > 0)
	_prevSuccessTime = now;
      else
	_prevSuccessTime = 0;
	
      _lastSuccessTime = now;

      if (_firstSuccessTime <= 0) {
	if (ST_STARTING <= _state && _state < ST_ACTIVE) {
	  _state = ST_WARMUP;
	  _firstSuccessTime = now;
	}

	if (_warmupState < 0)
	  _warmupState = 0;
      }
    }
    
    updateWarmup();

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

  private void updateWarmup()
  {
    synchronized (this) {
      if (! isEnabled())
	return;
    
      long now = Alarm.getCurrentTime();
      int warmupState = _warmupState;
    
      if (warmupState >= 0 && _firstSuccessTime > 0) {
	warmupState = (int) ((now - _firstSuccessTime) / _warmupChunkTime);

	// reset the connection fail recover time
	_dynamicFailRecoverTime = 1000L;

	if (WARMUP_MAX <= warmupState) {
	  warmupState = WARMUP_MAX;
	  toActive();
	}
      }

      _warmupState = warmupState;
    }
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

  /**
   * Open a read/write pair to the target srun connection.
   *
   * @return the socket's read/write pair.
   */
  ReadWritePair openTCPPair()
    throws IOException
  {
    return _tcpPath.openReadWrite();
  }

  /**
   * Returns true if can connect to the client.
   */
  public boolean canConnect()
  {
    try {
      wake();

      ClusterStream stream = open();

      if (stream != null) {
        stream.free();

        return true;
      }

      return false;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  public String toString()
  {
    return ("ServerConnector[id=" + getId() +
            " index=" + _port.getIndex() +
            " address=" + _port.getAddress() + ":" + _port.getPort() +
            " cluster=" + _cluster.getId() + "]");
  }

}
