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

package com.caucho.cloud.network;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.CurrentTime;

/**
 * Defines a member of the cluster, corresponds to <server> in the conf file.
 *
 * A {@link ServerConnector} obtained with {@link #getServerConnector} is used to actually
 * communicate with this ClusterServer when it is active in another instance of
 * Resin .
 */
public final class ServerHeartbeatState {
  private static final Logger log
    = Logger.getLogger(ServerHeartbeatState.class.getName());

  private final AtomicReference<State> _heartbeatState
    = new AtomicReference<State>(State.STOP);
  
  private final AtomicLong _stateTimestamp = new AtomicLong();
  private final AtomicLong _lastHeartbeatTime = new AtomicLong();
  
  private final ClusterServer _server;

  ServerHeartbeatState(ClusterServer server)
  {
    _server = server;
    
    _stateTimestamp.set(CurrentTime.getCurrentTime());
  }
  
  /**
   * Test if the server is active, i.e. has received an active message.
   */
  public boolean isHeartbeatActive()
  {
    return _heartbeatState.get().isActive();
  }
  
  public String getHeartbeatState()
  {
    return _heartbeatState.get().toString();
  }

  /**
   * Returns the last state change timestamp.
   */
  public long getStateTimestamp()
  {
    return _stateTimestamp.get();
  }
  
  public long getLastHeartbeatTime()
  {
    return _lastHeartbeatTime.get();
  }

  /**
   * Notify that a start event has been received.
   */
  public boolean notifyHeartbeatStart()
  {
    long now = CurrentTime.getCurrentTime();
    
    long oldHeartbeatTime = _lastHeartbeatTime.getAndSet(now);

    State oldState = _heartbeatState.getAndSet(State.ACTIVE);
    
    if (oldState == State.ACTIVE) {
      return false;
    }
    
    _stateTimestamp.set(now);

    if (oldHeartbeatTime > 0) {
      // #5173
      log.warning(this + " notify-heartbeat-start");
    }
    else if (log.isLoggable(Level.FINER)) {
      log.finer(this + " notify-heartbeat-start");
    }

    return true;
  }

  /**
   * Notify that a stop event has been received.
   */
  public boolean notifyHeartbeatStop()
  {
    _lastHeartbeatTime.set(0);
    
    State oldState = _heartbeatState.getAndSet(State.STOP);

    if (oldState == State.STOP) {
      return false;
    }
    
    _stateTimestamp.set(CurrentTime.getCurrentTime());

    return true;
  }
  
  public void updateTimeout(long timeout)
  {
    State oldState = _heartbeatState.get();
    
    if (oldState != State.ACTIVE) {
      return;
    }

    long now = CurrentTime.getCurrentTime();
    long lastTime = _lastHeartbeatTime.get();
    
    if (timeout < now - lastTime) {
      if (_heartbeatState.compareAndSet(State.ACTIVE, State.TIMEOUT)) {
        log.warning(_server + " heartbeat timeout " + (now - lastTime) + "ms");
        
        _server.onHeartbeatTimeout();
      }
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server + "," + _heartbeatState.get() + "]";
  }
  
  static enum State {
    STOP,
    
    TIMEOUT,
    
    ACTIVE {
      @Override
      public boolean isActive() { return true; }
    };
    
    public boolean isActive()
    {
      return false;
    }
  }
}
