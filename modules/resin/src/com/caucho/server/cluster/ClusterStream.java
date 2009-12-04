/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.admin.ActiveProbe;
import com.caucho.admin.ActiveTimeProbe;
import com.caucho.bam.ActorException;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.Hessian2StreamingInput;
import com.caucho.server.hmux.HmuxRequest;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Defines a connection to the client.
 */
public class ClusterStream {
  private static final L10N L = new L10N(ClusterStream.class);

  private static final Logger log
    = Logger.getLogger(ClusterStream.class.getName());

  private ServerPool _pool;

  private ReadStream _is;
  private WriteStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2Output _out;

  private boolean _isAuthenticated;

  private ActiveProbe _connProbe;
  private ActiveTimeProbe _requestTimeProbe;
  private ActiveProbe _idleProbe;

  private long _requestStartTime;
  private boolean _isIdle;

  private long _freeTime;

  private String _debugId;

  ClusterStream(ServerPool pool, int count,
                ReadStream is, WriteStream os)
  {
    _pool = pool;
    _is = is;
    _os = os;

    _debugId = "[" + pool.getDebugId() + ":" + count + "]";

    _connProbe = pool.getConnectionProbe();
    _requestTimeProbe = pool.getRequestTimeProbe();
    _idleProbe = pool.getIdleProbe();

    _connProbe.start();

    toActive();
  }

  /**
   * Returns the owning pool
   */
  public ServerPool getPool()
  {
    return _pool;
  }

  /**
   * Returns the input stream.
   */
  public ReadStream getReadStream()
  {
    _freeTime = 0;

    return _is;
  }

  /**
   * Returns the write stream.
   */
  public WriteStream getWriteStream()
  {
    _freeTime = 0;

    return _os;
  }

  /**
   * Returns true if the stream has been authenticated
   */
  public boolean isAuthenticated()
  {
    return _isAuthenticated;
  }

  /**
   * Returns true if the stream has been authenticated
   */
  public void setAuthenticated(boolean isAuthenticated)
  {
    _isAuthenticated = isAuthenticated;
  }

  /**
   * Returns the hessian input stream
   */
  public Hessian2StreamingInput getHessianInputStream()
  {
    if (_in == null)
      _in = new Hessian2StreamingInput(_is);

    return _in;
  }

  /**
   * Returns the hessian output stream
   */
  public Hessian2Output getHessianOutputStream()
  {
    if (_out == null) {
      OutputStream os = _os;

      /*
      if (log.isLoggable(Level.FINEST)) {
        HessianDebugOutputStream hOs
          = new HessianDebugOutputStream(os, log, Level.FINEST);
        // hOs.startTop2();
        os = hOs;
      }
      */
        
      _out = new Hessian2Output(os);
    }

    return _out;
  }

  /**
   * Returns the free time, i.e. the time the connection was last idle.
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
   * Sets the free time.
   */
  public void clearFreeTime()
  {
    _freeTime = 0;
    _is.clearReadTime();
  }

  /**
   * Returns true if nearing end of free time.
   */
  public boolean isLongIdle()
  {
    long now = Alarm.getCurrentTime();

    return (_pool.getLoadBalanceIdleTime() < now - _freeTime + 2000L);
  }

  //
  // ActorStream output for HMTP
  //

  public String getJid()
  {
    return "clusterStream@admin.resin";
  }
  
  public void switchToHmtp(boolean isUnidir)
  {
    try {
      WriteStream out = getWriteStream();

      if (isUnidir)
        out.write(HmuxRequest.HMUX_TO_UNIDIR_HMTP);
      else
        out.write(HmuxRequest.HMUX_SWITCH_TO_HMTP);
             
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);
      out.flush();
    } catch (IOException e) {
      throw new ActorException(e);
    }
    
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
    _pool.clearRecycle();
  }

  /**
   * Adds the stream to the free pool.
   */
  public void free()
  {
    if (_is == null) {
      IllegalStateException exn = new IllegalStateException(L.l("{0} unexpected free of closed stream", this));
      exn.fillInStackTrace();

      log.log(Level.FINE, exn.toString(), exn);
      return;
    }

    long requestStartTime = _requestStartTime;
    _requestStartTime = 0;

    if (requestStartTime > 0)
      _requestTimeProbe.end(requestStartTime);


    // #2369 - the load balancer might set its own view of the free
    // time
    if (_freeTime <= 0) {
      _freeTime = _is.getReadTime();

      if (_freeTime <= 0) {
        // for write-only, the read time is zero
        _freeTime = Alarm.getCurrentTime();
      }
    }

    _idleProbe.start();
    _isIdle = true;

    _pool.free(this);
  }

  public void toActive()
  {
    if (_isIdle) {
      _isIdle = false;
      _idleProbe.end();
    }

    _requestStartTime = _requestTimeProbe.start();
  }
  
  public boolean isClosed()
  {
    return _is == null;
  }

  public void close()
  {
    if (_is != null)
      _pool.close(this);

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

    if (is != null) {
      _connProbe.end();

      if (_requestStartTime > 0)
        _requestTimeProbe.end(_requestStartTime);

      if (_isIdle)
        _idleProbe.end();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _debugId + "]";
  }
}
