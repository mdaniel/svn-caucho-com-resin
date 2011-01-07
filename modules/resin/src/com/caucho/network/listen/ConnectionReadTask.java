/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is software; you can redistribute it and/or modify
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;

/**
 * Task handling the reading from a connection.
 */
abstract class ConnectionReadTask implements Runnable {
  private static final Logger log 
    = Logger.getLogger(ConnectionReadTask.class.getName());
  
  private final TcpSocketLinkListener _listener;
  private final TcpSocketLink _socketLink;
  private SocketLinkThreadLauncher _launcher;
  
  ConnectionReadTask(TcpSocketLink socketLink)
  {
    _socketLink = socketLink;
    _listener = _socketLink.getListener();
    _launcher = _listener.getLauncher();
  }
  
  protected final TcpSocketLink getSocketLink()
  {
    return _socketLink;
  }
  
  protected final TcpSocketLinkListener getListener()
  {
    return _listener;
  }
  
  protected final SocketLinkThreadLauncher getLauncher()
  {
    return _launcher;
  }
  
  /**
   * Handles the read request for the connection
   */
  abstract RequestState doTask()
    throws IOException;

  @Override
  public void run()
  {
    runThread();
  }

  public void runThread()
  {
    Thread thread = Thread.currentThread();
    String oldThreadName = thread.getName();

    thread.setName(_socketLink.getDebugId());

    thread.setContextClassLoader(_listener.getClassLoader());

    RequestState result = null;

    _socketLink.setThread(thread);

    try {
      result = doTask();
    } catch (OutOfMemoryError e) {
      String msg = "TcpSocketLink OutOfMemory";

      ShutdownService.shutdownActive(ExitCode.MEMORY, msg); 
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _socketLink.setThread(null);
      
      thread.setName(oldThreadName);

      if (result == null)
        _socketLink.destroy();

      if (result != RequestState.THREAD_DETACHED)
        _socketLink.finishThread();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _socketLink + "]";
  }
}
