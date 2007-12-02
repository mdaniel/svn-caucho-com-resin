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

import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines a connection to the client.
 */
public class ClusterStream {
  static protected final Logger log = Log.open(ClusterStream.class);

  private ServerConnector _srun;

  private ReadStream _is;
  private WriteStream _os;

  private long _freeTime;

  private String _debugId;

  ClusterStream(int count, ServerConnector client,
		ReadStream is, WriteStream os)
  {
    _srun = client;
    _is = is;
    _os = os;

    _debugId = "[" + client.getDebugId() + ":" + count + "]";
  }

  /**
   * Returns the cluster server.
   */
  public ServerConnector getServer()
  {
    return _srun;
  }

  /**
   * Returns the input stream.
   */
  public ReadStream getReadStream()
  {
    return _is;
  }

  /**
   * Returns the write stream.
   */
  public WriteStream getWriteStream()
  {
    return _os;
  }

  /**
   * Returns the free time.
   */
  public long getFreeTime()
  {
    return _freeTime;
  }

  /**
   * Sets the free time.
   */
  public void setFreeTime(long freeTime)
  {
    _freeTime = freeTime;
  }

  /**
   * Returns true if nearing end of free time.
   */
  public boolean isLongIdle()
  {
    return (_srun.getLoadBalanceIdleTime()
	    < Alarm.getCurrentTime() - _freeTime + 2000L);
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    return _debugId;
  }

  /**
   * Clears the recycled connections.
   */
  public void clearRecycle()
  {
    _srun.clearRecycle();
  }

  /**
   * Recycles.
   */
  public void free()
  {
    if (_is != null)
      _freeTime = _is.getReadTime();

    _srun.free(this);
  }

  public void close()
  {
    if (_is != null)
      _srun.close(this);

    closeImpl();
  }
  
  /**
   * closes the stream.
   */
  void closeImpl()
  {
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;
    
    try {
      if (is != null)
	is.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      if (os != null)
	os.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public String toString()
  {
    return "ClusterStream[" + _debugId + "]";
  }
}
