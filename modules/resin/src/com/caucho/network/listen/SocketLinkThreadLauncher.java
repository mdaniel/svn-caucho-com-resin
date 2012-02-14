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

import com.caucho.env.thread.AbstractThreadLauncher;
import com.caucho.env.thread.ThreadPool;
import com.caucho.inject.Module;

/**
 * Represents a protocol connection.
 */
@Module
class SocketLinkThreadLauncher extends AbstractThreadLauncher
{
  private ThreadPool _threadPool = ThreadPool.getThreadPool();
  private TcpSocketLinkListener _listener;
  
  private String _threadName;

  SocketLinkThreadLauncher(TcpSocketLinkListener listener)
  {
    _listener = listener;
  }

  @Override
  protected boolean isEnable()
  {
    if (_listener.isClosed())
      return false;
    else
      return super.isEnable();
  }
  
  @Override
  protected String getThreadName()
  {
    if (_threadName == null) {
      _threadName = ("resin-port-"
          + _listener.getAddress()
          + ":" + _listener.getPort());
    }
    
    return _threadName;
  }

  @Override
  protected void launchChildThread(int id)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    TcpSocketLink startConn = null;
    
    try {
      thread.setContextClassLoader(_listener.getClassLoader());
      
      startConn = _listener.allocateConnection();

      startConn.requestAccept();
      
      startConn = null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (startConn != null)
        _listener.closeConnection(startConn);
      
      thread.setContextClassLoader(loader);
    }
  }

  @Override
  protected void startWorkerThread()
  {
    _threadPool.schedule(this);
  }
 
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listener + "]";
  }
}
