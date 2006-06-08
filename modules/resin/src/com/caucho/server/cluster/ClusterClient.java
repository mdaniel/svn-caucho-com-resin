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

import com.caucho.log.Log;

/**
 * Represents a connection to one of the servers in a distribution group.
 * A {@link ClusterServer} is used to define the properties of the server
 * that is connected to.
 */
public class ClusterClient {
  private static final Logger log = Log.open(ClusterClient.class);

  private ClusterServer _server;

  private String _debugId;

  private long _slowStartDoublePeriod = 10000L;
  private int _maxConnections = Integer.MAX_VALUE / 2;
  
  private ClusterStream []_free = new ClusterStream[64];
  private volatile int _freeHead;
  private volatile int _freeTail;
  private int _freeSize = 16;

  private int _streamCount;
  
  private volatile long _lastFailTime;
  private volatile long _firstConnectTime;

  private volatile int _activeCount;
  private volatile int _startingCount;
  
  private volatile long _lifetimeKeepaliveCount;
  private volatile long _lifetimeConnectionCount;

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

  public long getLifetimeConnectionCount()
  {
    return _lifetimeConnectionCount;
  }

  public long getLifetimeKeepaliveCount()
  {
    return _lifetimeKeepaliveCount;
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

    return _isEnabled && (_lastFailTime + _server.getFailRecoverTime() <= now);
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenSoft()
  {
    if (! _isEnabled)
      return false;
    
    long now = Alarm.getCurrentTime();

    if (now < _lastFailTime + _server.getFailRecoverTime())
      return false;

    long slowStartCount;

    if (_firstConnectTime <= 0)
      slowStartCount = 0;
    else
      slowStartCount = (now - _firstConnectTime) / _slowStartDoublePeriod;

    if (slowStartCount > 16)
      return true;
    else if (_activeCount + _startingCount < (1 << slowStartCount))
      return true;
    else
      return false;
  }

  /**
   * Return true if active.
   */
  public boolean isEnabled()
  {
    return _isEnabled;
  }

  /**
   * Enable the client.
   */
  public void enable()
  {
    if (! _isClosed)
      _isEnabled = true;
  }

  /**
   * Enable the client.
   */
  public void disable()
  {
    _isEnabled = false;
  }

  /**
   * Open a stream to the target server.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openSoft()
  {
    if (! _isEnabled)
      return null;

    long now = Alarm.getCurrentTime();

    ClusterStream stream = openRecycle();

    if (stream != null)
      return stream;

    if (now < _lastFailTime + _server.getFailRecoverTime())
      return null;

    long slowStartCount;

    if (_firstConnectTime <= 0)
      slowStartCount = 0;
    else
      slowStartCount = (now - _firstConnectTime) / _slowStartDoublePeriod;

    if (slowStartCount > 16)
      return connect();
    else if (_activeCount + _startingCount < (1 << slowStartCount))
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
    if (! _isEnabled)
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
    if (! _isEnabled)
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
	  _lifetimeKeepaliveCount++;
	  
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
      rs.setAttribute("timeout", new Integer((int) _server.getReadTimeout()));

      synchronized (this) {
        _activeCount++;
	_lifetimeConnectionCount++;

	if (_firstConnectTime <= 0)
	  _firstConnectTime = Alarm.getCurrentTime();
      }

      ClusterStream stream = new ClusterStream(_streamCount++, this,
					       rs, pair.getWriteStream());
      
      if (log.isLoggable(Level.FINER))
	log.finer("connect " + stream);
      
      return stream;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

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
   * Close the connection.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
      _isEnabled = false;
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
