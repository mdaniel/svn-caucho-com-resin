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
 */
public class ClusterClient {
  private static final Logger log
    = Logger.getLogger(ClusterClient.class.getName());

  private ClusterServer _server;

  private String _debugId;

  private int _maxConnections = Integer.MAX_VALUE / 2;

  private final Lifecycle _lifecycle = new Lifecycle();
  
  private ClusterStream []_free = new ClusterStream[64];
  private volatile int _freeHead;
  private volatile int _freeTail;
  private int _freeSize = 16;

  private int _streamCount;
  
  private volatile long _lastFailTime;
  private volatile long _firstConnectTime;

  private volatile int _activeCount;
  private volatile int _startingCount;
  
  private volatile long _keepaliveTotalCount;
  private volatile long _connectTotalCount;
  private volatile long _failTotalCount;

  private volatile boolean _isEnabled = true;
  private volatile boolean _isClosed;

  ClusterClient(ClusterServer server)
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

    _lifecycle.toInit();
  }

  /**
   * Returns the cluster server.
   */
  public ClusterServer getServer()
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
    return (_freeHead - _freeTail + _free.length) % _free.length;
  }

  /**
   * Returns the total number of successful socket connections
   */
  public long getConnectTotalCount()
  {
    return _connectTotalCount;
  }

  /**
   * Returns the number of times a keepalive connection has been used.
   */
  public long getKeepaliveTotalCount()
  {
    return _keepaliveTotalCount;
  }

  /**
   * Returns the total number of failed connect attempts.
   */
  public long getFailTotalCount()
  {
    return _lifecycle.getFailCount();
  }

  /**
   * Returns the time of the last failure.
   */
  public Date getLastFailTime()
  {
    return new Date(_lifecycle.getLastFailTime());
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
  public boolean isActive()
  {
    long now = Alarm.getCurrentTime();

    if (_lifecycle.isStopped())
      return false;
    else
      return (_lastFailTime + _server.getFailRecoverTime() <= now);
  }

  /**
   * Returns the lifecycle state.
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenSoft()
  {
    if (_lifecycle.isStopped())
      return false;

    long now = Alarm.getCurrentTime();

    if (now < _lastFailTime + _server.getFailRecoverTime())
      return false;

    long slowStartCount;
    long slowStartTime = _server.getSlowStartTime();

    if (_firstConnectTime <= 0)
      slowStartCount = 0;
    else if (slowStartTime <= 0)
      slowStartCount = Integer.MAX_VALUE;
    else
      slowStartCount = 16 * (now - _firstConnectTime) / slowStartTime;

    // Slow start time splits into 16 parts.  The first 4 allow 1 request
    // at a time.  After that, each segment doubles the allowed requests

    if (slowStartCount > 16) {
      _lifecycle.toActive();
      
      return true;
    }
    else if (_activeCount + _startingCount < (1 << (slowStartCount - 3)))
      return true;
    else
      return false;
  }

  /**
   * Return true if active.
   */
  public boolean isEnabled()
  {
    return ! _lifecycle.isStopped();
  }

  /**
   * Enable the client.
   */
  public void start()
  {
    _lifecycle.toStarting();
  }

  /**
   * Enable the client.
   */
  public void stop()
  {
    _lifecycle.toStop();
  }

  /**
   * Open a stream to the target server.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openSoft()
  {
    if (_lifecycle.isStopped())
      return null;

    long now = Alarm.getCurrentTime();

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    if (canOpenSoft())
      return connect();
    else
      return null;
  }

  /**
   * Open a stream to the target server.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openIfLive()
  {
    if (_lifecycle.isStopped())
      return null;

    long now = Alarm.getCurrentTime();

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    if (now < _lastFailTime + _server.getFailRecoverTime())
      return null;

    return connect();
  }

  /**
   * Open a stream to the target server, forcing a connect.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream open()
  {
    if (_lifecycle.isStopped())
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
      if (_freeHead != _freeTail) {
        stream = _free[_freeHead];
        long freeTime = stream.getFreeTime();

        _free[_freeHead] = null;
        _freeHead = (_freeHead + _free.length - 1) % _free.length;

        if (now < freeTime + _server.getMaxIdleTime()) {
          _activeCount++;
	  _keepaliveTotalCount++;
	  
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
      rs.setAttribute("timeout", new Integer((int) _server.getClientReadTimeout()));

      synchronized (this) {
        _activeCount++;
	_connectTotalCount++;

	if (_firstConnectTime <= 0) {
	  _lifecycle.toStarting();
	  _firstConnectTime = Alarm.getCurrentTime();
	}
      }

      ClusterStream stream = new ClusterStream(_streamCount++, this,
					       rs, pair.getWriteStream());
      
      if (log.isLoggable(Level.FINER))
	log.finer("connect " + stream);
      
      return stream;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      _lifecycle.toFail();
      _lastFailTime = Alarm.getCurrentTime();
      _firstConnectTime = 0;
      
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
    clearRecycle();
    _lifecycle.toStarting();
    _lastFailTime = 0;
  }

  /**
   * Frees the read/write pair for reuse.  Called only from
   * ClusterStream.free()
   */
  void free(ClusterStream stream)
  {
    synchronized (this) {
      _activeCount--;

      int size = (_freeHead - _freeTail + _free.length) % _free.length;

      if (! _isClosed && size < _freeSize) {
        _freeHead = (_freeHead + 1) % _free.length;
        _free[_freeHead] = stream;

        return;
      }
    }

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
      _freeHead = _freeTail = 0;

      for (int i = 0; i < _free.length; i++) {
        ClusterStream stream;

        stream = _free[i];
        _free[i] = null;

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
    if (! _lifecycle.toDestroy())
      return;
    
    synchronized (this) {
      _freeHead = _freeTail = 0;
    }

    for (int i = 0; i < _free.length; i++) {
      ClusterStream stream;

      synchronized (this) {
        stream = _free[i];
        _free[i] = null;
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
