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

package com.caucho.v5.server.container;

import io.baratine.service.OnDestroy;
import io.baratine.service.Result;
import io.baratine.service.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.baratine.Remote;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.server.watchdog.LinkWatchdogService;
import com.caucho.v5.util.JmxUtil;
import com.caucho.v5.util.L10N;

/**
 * Actor communicating with the watchdog.
 */
@Service("public:///server")
@Remote
public class LinkChildServiceImpl
{
  private static final Logger log
    = Logger.getLogger(LinkChildServiceImpl.class.getName());

  private static final L10N L = new L10N(LinkChildServiceImpl.class);

  private ServerBase _serverContainer;
  
  private ShutdownSystem _shutdown;

  private LinkServer _linkServer;
  private LinkWatchdogService _linkWatchdog;
  
  LinkChildServiceImpl(ServerBase server)
  {
    _serverContainer = server;
    
    _shutdown = ShutdownSystem.getCurrent();
    
    if (_shutdown == null)
      throw new IllegalStateException(L.l("'{0}' requires an active {1}.",
                                          this,
                                          ShutdownSystem.class.getSimpleName()));
  }

  void initLink(LinkServer linkServer)
  {
    _linkServer = linkServer;
    
    _linkWatchdog = _linkServer.lookup("remote:///watchdog")
                               .as(LinkWatchdogService.class);
  }
  
  LinkWatchdogService getLinkWatchdog()
  {
    return _linkWatchdog;
  }
  
  //
  // messages from watchdog
  //
  
  /**
   * Startup information
   */
  public void startInfo(boolean isRestart, 
                        String restartMessage, 
                        ExitCode previousExitCode,
                        String shutdownMessage)
  {
    _serverContainer.setStartInfo(isRestart,
                                  restartMessage,
                                  previousExitCode);
  }
  
  /**
   * Query for the process pid.
   * @throws MalformedObjectNameException 
   * @throws ReflectionException 
   * @throws MBeanException 
   * @throws InstanceNotFoundException 
   * @throws AttributeNotFoundException 
   */
  public Integer queryPid()
        throws Exception
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryPid()");
    }

    MBeanServer server = JmxUtil.getMBeanServer();
    ObjectName objName = new ObjectName("java.lang:type=Runtime");

    String runtimeName = (String) JmxUtil.attribute("java.lang:type=Runtime", "Name");

    if (runtimeName == null) {
      throw new RuntimeException("null runtime name");
    }

    int p = runtimeName.indexOf('@');

    if (p > 0) {
      int pid = Integer.parseInt(runtimeName.substring(0, p));

      return pid;
    }

    throw new IllegalStateException("malformed name=" + runtimeName);
 }

  public void shutdown(ShutdownModeAmp mode,
                       Result<String> result)
  {
    log.info(_serverContainer + " stop request from watchdog");

    String msg = L.l("{0} shutdown from watchdog stop",
                     _serverContainer.programName());
    
    try {
      ShutdownSystem.shutdownActive(mode, ExitCode.OK, msg, 
                                    result.of(m->{ 
                                      _linkWatchdog.onMessage(m); 
                                      return m; 
                                    }));
    } catch (Throwable e) {
      result.fail(e);
    }
  }
  
  /**
   * Sends a warning message to the watchdog
   */
  /*
  public void sendWarning(String msg)
  {
    _linkWatchdog.onMessage(msg);
  }
  */

  @OnDestroy
  public void destroy()
  {
    String msg = L.l("{0} shutdown from unexpected watchdog exit.",
                     _serverContainer.programName());
    
    ShutdownSystem.shutdownActive(ExitCode.WATCHDOG_EXIT, msg);
  }
}
