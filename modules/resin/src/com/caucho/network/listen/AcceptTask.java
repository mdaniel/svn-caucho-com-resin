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

import com.caucho.inject.Module;

/**
 * The thread task to handle a newly accepted request.
 */
@Module
class AcceptTask extends ConnectionReadTask {
  private static final Logger log = Logger.getLogger(AcceptTask.class.getName());
  
  AcceptTask(TcpSocketLink socketLink)
  {
    super(socketLink);
  }

  @Override
  public void run()
  {
    // SocketLinkListener listener = getListener();
    SocketLinkThreadLauncher launcher = getLauncher();

    Thread thread = Thread.currentThread();
    String threadName = thread.getName();
    thread.setName(getSocketLink().getDebugId());
    
    try {
      launcher.onChildThreadBegin();

      if (log.isLoggable(Level.FINER))
        log.finer(getSocketLink() + " starting listen thread");
      // listener.startConnection(getSocketLink());
      
      super.run();
    } finally {
      launcher.onChildThreadEnd();
      
      thread.setName(threadName);
    }
  }

  /**
   * Loop to accept new connections.
   */
  @Override
  RequestState doTask()
  throws IOException
  {
    TcpSocketLink socketLink = getSocketLink();
    TcpSocketLinkListener listener = getListener();
    
    RequestState result = RequestState.EXIT;

    while (! listener.isClosed()
           && ! socketLink.getState().isDestroyed()) {
      socketLink.toAccept();

      if (! accept()) {
        socketLink.close();

        return RequestState.EXIT;
      }

      socketLink.toStartConnection();

      if (log.isLoggable(Level.FINER)) {
        log.finer(socketLink + " accept");
      }

      boolean isKeepalive = false;
      result = socketLink.handleRequests(isKeepalive);

      if (result == RequestState.THREAD_DETACHED) {
        return result;
      }
      else if (result == RequestState.DUPLEX) {
        return socketLink.doDuplex();
      }

      socketLink.close();
    }

    return RequestState.EXIT;
  }

  private boolean accept()
  {
    SocketLinkThreadLauncher launcher = getLauncher();
    
    if (launcher.isIdleExpire())
      return false;
    
    launcher.onChildIdleBegin();
    try {
      return getListener().accept(getSocketLink().getSocket());
    } finally {
      launcher.onChildIdleEnd();
    }
  }
}
