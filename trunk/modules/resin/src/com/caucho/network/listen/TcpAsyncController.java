/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.network.listen;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.inject.Module;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * Public API to control a comet connection.
 */
@Module
class TcpAsyncController extends AsyncController {
  private static final Logger log
    = Logger.getLogger(TcpAsyncController.class.getName());
  
  private TcpSocketLink _conn;

  private SocketLinkCometListener _cometHandler;

  private boolean _isCompleteRequested;
  private boolean _isTimeout;

  TcpAsyncController(TcpSocketLink conn)
  {
    _conn = conn;
  }
  
  void initHandler(SocketLinkCometListener cometHandler)
  {
    _cometHandler = cometHandler;
  }

  public TcpSocketLink getConnection()
  {
    return _conn;
  }

  /**
   * Sets the max idle time.
   */
  @Override
  public void setMaxIdleTime(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      idleTime = Long.MAX_VALUE / 2;
    
    _conn.setIdleTimeout(idleTime);
  }

  /**
   * Gets the max idle time.
   */
  @Override
  public long getMaxIdleTime()
  {
    return _conn.getIdleTimeout();
  }

  /**
   * Complete the connection
   */
  @Override
  public final void complete()
  {
    setCompleteRequested();

    try {
      wake();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  /**
   * Wakes the connection.
   */
  @Override
  public final boolean wake()
  {
    TcpSocketLink conn = _conn;

    if (conn != null) {
      conn.requestWakeComet();
      return true;
    }
    else
      return false;
  }
  
  @Override
  public boolean isAsyncStarted()
  {
    TcpSocketLink conn = _conn;

    return (conn != null && conn.isAsyncStarted());
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return false;
  }

  /**
   * Sets the timeout.
   * @return 
   */
  @Override
  public final boolean timeout()
  {
    if (_cometHandler.onTimeout()) {
      _isTimeout = true;
      // server/1lda
      // setCompleteRequested();
      
      return true;
    }

    try {
      wake();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    
    return false;
  }

  void setTimeout()
  {
    _isTimeout = true;
    // setCompleteRequested();
  }
  
  /**
   * Return true if timed out
   */
  public final boolean isTimeout()
  {
    return _isTimeout;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isActive()
  {
    return _conn != null;
  }

  final void setCompleteRequested()
  {
    _isCompleteRequested = true;
  }
  
  final boolean isCompleteRequested()
  {
    return _isCompleteRequested;
  }

  /**
   * Returns true for an active comet connection.
   */
  public boolean isComet()
  {
    TcpSocketLink conn = _conn;

    return conn != null && ! _isCompleteRequested;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isClosed()
  {
    TcpSocketLink conn = _conn;

    return conn == null || _isCompleteRequested;
  }

  /**
   * 
   */
  public void toResume()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onClose()
  {
    try {
      _cometHandler.onComplete();
    } finally {
      // _conn = null;
    }
  }
  
  @Override
  public void close()
  {
    super.close();

    _conn = null;
  }

  @Override
  public String toString()
  {
    TcpSocketLink conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[closed]";

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");

    if (CurrentTime.isTest())
      sb.append("test");
    else
      sb.append(conn.getId());

    TcpSocketLink tcpConn = null;

    if (_conn instanceof TcpSocketLink)
      tcpConn = (TcpSocketLink) _conn;

    /*
    if (tcpConn != null && tcpConn.isCometComplete())
      sb.append(",complete");
      */
    
    if (_isCompleteRequested)
      sb.append(",complete");

    if (_isTimeout)
      sb.append(",timeout");

    if (tcpConn != null)
      sb.append("," + tcpConn.getRequestState());

    sb.append("]");

    return sb.toString();
  }
}
