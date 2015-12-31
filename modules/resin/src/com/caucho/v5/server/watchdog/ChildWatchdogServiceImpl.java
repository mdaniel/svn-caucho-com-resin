/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.watchdog;

import io.baratine.service.Direct;
import io.baratine.service.OnInit;
import io.baratine.service.Result;
import io.baratine.service.ServiceRef;

import java.util.Objects;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.util.ResultList;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;

/**
 * Service for managing a child service, identified uniquely by the port.
 */
public class ChildWatchdogServiceImpl
{
  private static final L10N L = new L10N(ChildWatchdogServiceImpl.class);
  
  private final WatchdogManager _watchdog;
  private final int _port;
  
  private ChildWatchdogService _serviceSelf;
  
  private StateChildWatchdog _state = StateChildWatchdog.idle;
  
  private ResultList<String> _startResults = new ResultList<>();

  private ChildWatchdog _child;

  ChildWatchdogServiceImpl(WatchdogManager watchdog,
                           int port)
  {
    Objects.requireNonNull(watchdog);
    
    _watchdog = watchdog;
    
    if (port <= 0) {
      throw new IllegalArgumentException(L.l("Invalid port {0}", port));
    }
    
    _port = port;
  }
  
  //
  // service lifecycle
  //
  
  @OnInit
  public void onStart()
  {
    ServiceRef serviceSelfRef = ServiceRef.current();
    
    _serviceSelf = serviceSelfRef.as(ChildWatchdogService.class);
  }
  
  //
  // messages
  //
  
  @Direct
  public String getId()
  {
    if (_child != null) {
      return _child.getId();
    }
    else {
      return String.valueOf(_port);
    }
  }
  
  @Direct
  public int getPort()
  {
    return _port;
  }
  
  @Direct
  public String getState()
  {
    return _state.toString();
  }
  
  public boolean isActive()
  {
    return _state.isActive();
  }
  
  @Direct
  public String getUserName()
  {
    if (_child != null) {
      return _child.getUserName();
    }
    else {
      return null;
    }
  }
  
  @Direct
  public String getGroupName()
  {
    if (_child != null) {
      return _child.getGroupName();
    }
    else {
      return null;
    }
  }
  
  @Direct
  public int getPid()
  {
    if (_child != null) {
      return _child.getPid();
    }
    else {
      return 0;
    }
  }

  @Direct
  public String getRootDir()
  {
    ChildWatchdog child = _child;
    
    if (child != null) {
      return String.valueOf(child.getRootDir());
    }
    else {
      return null;
    }
  }
  
  @Direct
  public String getConfigPath()
  {
    ChildWatchdog child = _child;
    
    if (child != null) {
      return String.valueOf(child.getConfigPath());
    }
    else {
      return null;
    }
  }
  
  @Direct
  public String getUptimeString()
  {
    ChildWatchdog child = _child;
    
    if (child != null) {
      return child.getUptimeString();
    }
    else {
      return null;
    }
  }


  public void start(ArgsWatchdog args, 
                    ServerConfigBoot serverConfig,
                    Result<String> result)
  {
    _state = _state.toStarting();
    
    if (_state != StateChildWatchdog.starting) {
      throw new IllegalStateException(L.l("server {0} ({1}) cannot be started because a running instance already exists.  stop or restart the old server first.",
                                          serverConfig, _port));
    }
    
    _child = new ChildWatchdog(_watchdog.getSystem(), _serviceSelf, serverConfig, args);

    _child.start();
    
    waitForStart(result);
  }
  
  public void waitForStart(Result<String> result)
  {
    if (! _state.isStarting()) {
      result.ok(_state.toString());
    }
    else {
      _startResults.add(result);
    }
  }
  
  public void onStartComplete(String msg)
  {
    _state = _state.toActive();
    
    _startResults.ok(msg);
  }
  
  public void onChildExit(String msg)
  {
    _state = _state.toStop();
    
    _startResults.ok(msg);
  }
  
  public void stop(ShutdownModeAmp mode, 
                   Result<String> result)
  {
    _state = _state.toStop();
    
    ChildWatchdog child = _child;
    _child = null;
    
    if (child != null) {
      child.stop(mode, result);
    }
    else if (result != null) {
      result.ok(L.l("{0} is already stopped", this));
    }
  }
  
  public void kill(Result<String> result)
  {
    _state = _state.toStop();
    
    ChildWatchdog child = _child;
    _child = null;
    
    if (child != null) {
      child.kill(result);
    }
    else if (result != null) {
      result.ok(L.l("{0} is already stopped", this));
    }
  }


  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _port + "," + _state + "]";
  }
  
  private enum StateChildWatchdog
  {
    idle {
      @Override
      StateChildWatchdog toStarting() { return starting; }
    },
    
    starting {
      @Override
      StateChildWatchdog toStop() { return idle; }

      @Override
      StateChildWatchdog toActive() { return active; }

      @Override
      boolean isActive() { return true; }

      @Override
      boolean isStarting() { return true; }
    },
    active {
      @Override
      StateChildWatchdog toStop() { return idle; }

      @Override
      boolean isActive() { return true; }
    };
    
    StateChildWatchdog toStarting()
    {
      return this;
    }
    
    StateChildWatchdog toActive()
    {
      return this;
    }
    
    StateChildWatchdog toStop()
    {
      return this;
    }
    
    boolean isActive()
    {
      return false;
    }
    
    boolean isStarting()
    {
      return false;
    }
  }
}
