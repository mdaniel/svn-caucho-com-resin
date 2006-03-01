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
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.config.types.Period;

/**
 * Defines a connection to one of the servers in a distribution group.
 */
public class ClusterClient {
  static protected final Logger log = Log.open(ClusterClient.class);

  private ClusterServer _server;

  private String _debugId;
  private int _streamCount;

  // XXX: the load balance and the tcp-session want different timeouts
  private int _timeout = 2000;

  private int _maxPoolSize = 16;

  private ClusterStream []_free = new ClusterStream[64];
  private int _freeHead;
  private int _freeTail;
  private int _freeSize = 16;

  private long _lastFailTime;

  private int _activeCount;

  private boolean _isActive = true;
  private boolean _isClosed;

  public ClusterClient(ClusterServer server)
  {
    _server = server;
    _timeout = (int) server.getReadTimeout();

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
   * Returns the socket timeout when reading and writing to the
   * target server.
   */
  public int getTimeout()
  {
    return _timeout;
  }

  /**
   * Sets the socket timeout when reading and writing to the
   * target server.
   */
  public void setTimeout(long timeout)
  {
    _timeout = (int) timeout;
  }

  /**
   * Returns the number of active connections.
   */
  public int getActiveCount()
  {
    return _activeCount;
  }

  /**
   * Sets the recycle pool size.
   */
  public void setMaxPoolSize(int size)
  {
    _maxPoolSize = size;
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    return _debugId;
  }

  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    long now = Alarm.getCurrentTime();

    return (now < _lastFailTime + _server.getDeadTime() || ! _isActive);
  }

  /**
   * Return true if active.
   */
  public boolean isActive()
  {
    return _isActive;
  }

  /**
   * Enable the client.
   */
  public void enable()
  {
    if (! _isClosed)
      _isActive = true;
  }

  /**
   * Enable the client.
   */
  public void disable()
  {
    _isActive = false;
  }

  /**
   * Open a read/write pair, trying to recycle.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream openRecycle()
  {
    if (! _isActive)
      return null;

    long now = Alarm.getCurrentTime();
    ClusterStream stream = null;

    synchronized (this) {
      if (_freeHead != _freeTail) {
        stream = _free[_freeTail];
        long freeTime = stream.getFreeTime();

        _free[_freeTail] = null;
        _freeTail = (_freeTail + 1) % _free.length;

        if (now < freeTime + _server.getLiveTime()) {
          _activeCount++;
          return stream;
        }
      }
    }

    if (stream != null)
      stream.closeImpl();

    return null;
  }

  /**
   * Open a read/write pair to the target srun connection.
   *
   * @return the socket's read/write pair.
   */
  public ClusterStream open()
  {
    if (isDead())
      return null;

    ClusterStream recycleStream = openRecycle();
    if (recycleStream != null)
      return recycleStream;

    try {
      ReadWritePair pair = _server.openTCPPair();
      ReadStream rs = pair.getReadStream();
      rs.setAttribute("timeout", new Integer((int) _server.getReadTimeout()));

      synchronized (this) {
        _activeCount++;
      }

      return new ClusterStream(_streamCount++, this,
                               rs, pair.getWriteStream());
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      _lastFailTime = Alarm.getCurrentTime();
      return null;
    }
  }

  /**
   * We now know that the server is live.
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
      int size = (_freeHead - _freeTail + _free.length) % _free.length;

      if (! _isClosed && size < _freeSize) {
        _activeCount--;

        _free[_freeHead] = stream;
        _freeHead = (_freeHead + 1) % _free.length;

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
    synchronized (this) {
      _activeCount--;
    }
  }

  /**
   * Clears the recycled connections, e.g. on detection of backend
   * server going down..
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
      _isActive = false;
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
