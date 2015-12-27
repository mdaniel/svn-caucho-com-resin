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

import io.baratine.core.OnDestroy;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.baratine.Remote;
import com.caucho.v5.server.container.LinkChildService;

/**
 * The link service watches the child server, sending and receiving 
 * lifecycle and heartbeat messages.
 */
@Remote
@Service("/watchdog")
public class LinkWatchdogServiceImpl
{
  private static final Logger log
    = Logger.getLogger(LinkWatchdogServiceImpl.class.getName());
  
  private ChildWatchdogProcess _child;
  
  private LinkChildService _serverLinkService;

  private ServiceManager _rampManager;

  private ArrayList<Result<String>> _shutdownResultList = new ArrayList<>();
  private boolean _isClosed;

  private ChildWatchdogService _childService;
  
  LinkWatchdogServiceImpl(ChildWatchdogProcess child,
                          ChildWatchdogService childService,
                          ServiceManager rampManager)
  {
    _rampManager = rampManager;

    /*
    _rampManager.newService()
                .service(this)
                .address("public:///watchdog")
                .build();
                */
    
    _serverLinkService = _rampManager.lookup("remote:///server")
                                     .as(LinkChildService.class);
    
    _child = child;
    _childService = childService;
  }
  
  @OnDestroy
  public void onShutdown()
  {
    linkClosed();
  }
  
  public void onStartComplete(String msg)
  {
    _childService.onStartComplete(msg);
  }
  
  public void onShutdownComplete()
  {
  }

  public void sendShutdown(ShutdownModeAmp mode,
                           Result<String> result)
  {
    if (_isClosed) {
      result.ok("Server is already closed");
      return;
    }
    
    _shutdownResultList.add(result);
    
    _serverLinkService.shutdown(mode, result);
  }

  /**
   * Failsafe when the child does not respond to the shutdown request.
   */
  public void linkClosed()
  {
    _isClosed = true;
    
    for (Result<String> result : _shutdownResultList) {
      result.ok("Watchdog detected close");
    }
  }

  public LinkChildService getServerLinkService()
  {
    return _serverLinkService;
  }
  
  public boolean ping()
  {
    log.warning("PING:");
    
    return true;
  }
  
  public boolean onMessage(String msg)
  {
    log.warning("Watchdog received message "
                + "from Server[" + _child.getId() + ",pid=" + _child.getPid() + "]:"
                + "\n  " + msg);
    
    if (msg.startsWith("Shutdown")) {
      _child.setShutdownMessage(msg);
    }
    
    return true;
  }
  
  /*
  public void destroy()
  {
    _resin.destroy();
  }
  */
}
