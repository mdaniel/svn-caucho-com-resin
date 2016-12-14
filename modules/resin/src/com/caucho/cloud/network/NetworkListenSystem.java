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

import java.util.*;
import java.util.logging.*;

import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.env.service.*;
import com.caucho.network.listen.*;
import com.caucho.util.*;
import com.caucho.vfs.QServerSocket;

public class NetworkListenSystem extends AbstractResinSubSystem 
  implements AlarmListener
{
  public static final int START_PRIORITY_AT_BEGIN = 50;
  public static final int START_PRIORITY_AT_END = 90;

  private static final L10N L = new L10N(NetworkListenSystem.class);
  private static final Logger log
    = Logger.getLogger(NetworkListenSystem.class.getName());
  
  private static final long ALARM_TIMEOUT = 120 * 1000L;
  
  private final CloudServer _cloudServer;
  
  private TcpPort _clusterListener;
  
  private final ArrayList<TcpPort> _listeners
    = new ArrayList<TcpPort>();

  private boolean _isBindPortsAtEnd = true;
  
  private Alarm _alarm;
  
  private NetworkListenSystem(CloudServer cloudServer)
  {
    _cloudServer = cloudServer;
    
    NetworkClusterSystem clusterService = NetworkClusterSystem.getCurrent();
    
    if (clusterService != null)
      _clusterListener = clusterService.getClusterListener();
    
    if (_clusterListener != null) {
      _listeners.add(_clusterListener);
    }
    
    NetworkListenStopSystem stopSystem = new NetworkListenStopSystem(this);
    ResinSystem system = ResinSystem.getCurrent();
    
    system.addService(stopSystem);

    NetworkServerConfig config = new NetworkServerConfig(this);
   
    configure(_cloudServer, config);
  }
  
  public static NetworkListenSystem 
    createAndAddService(CloudServer cloudServer)
  {
    ResinSystem system = preCreate(NetworkListenSystem.class);
    
    NetworkListenSystem service = new NetworkListenSystem(cloudServer);
    system.addService(NetworkListenSystem.class, service);
    
    return service;
  }
  
  public static NetworkListenSystem getCurrent()
  {
    return ResinSystem.getCurrentService(NetworkListenSystem.class);
  }
  
  /**
   * Returns the cluster listener, if in a clustered environment.
   */
  public TcpPort getClusterListener()
  {
   return _clusterListener;
  }

  public void addListener(TcpPort listener)
  {
    try {
      if (! _listeners.contains(listener))
        _listeners.add(listener);
    
      /*
      if (_lifecycle.isAfterStarting()) {
        // server/1e00
        port.bind();
        port.start();
      }
      */
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * If true, ports are bound at end.
   */
  public void setBindPortsAfterStart(boolean bindAtEnd)
  {
    _isBindPortsAtEnd = bindAtEnd;
  }

  /**
   * If true, ports are bound at end.
   */
  public boolean isBindPortsAfterStart()
  {
    return _isBindPortsAtEnd;
  }

  /**
   * Returns the {@link TcpPort}s for this server.
   */
  public Collection<TcpPort> getListeners()
  {
    return Collections.unmodifiableList(_listeners);
  }

  public void bind(String address, int port, QServerSocket ss)
    throws Exception
  {
    if ("null".equals(address))
      address = null;

    for (int i = 0; i < _listeners.size(); i++) {
      TcpPort serverPort = _listeners.get(i);

      if (port != serverPort.getPort())
        continue;

      if ((address == null) != (serverPort.getAddress() == null))
        continue;
      else if (address == null || address.equals(serverPort.getAddress())) {
        serverPort.bind(ss);

        return;
      }
    }

    throw new IllegalStateException(L.l("No matching port for {0}:{1}",
                                        address, port));
  }

  /**
   * Finds the TcpConnection given the threadId
   */
  public TcpSocketLink findConnectionByThreadId(long threadId)
  {
    for (TcpPort listener : getListeners()) {
      TcpSocketLink conn = listener.findConnectionByThreadId(threadId);

      if (conn != null)
        return conn;
    }

    return null;
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    if (_isBindPortsAtEnd)
      return START_PRIORITY_AT_END;
    else
      return START_PRIORITY_AT_BEGIN;
  }
  
  /**
   * Bind the ports.
   */
  @Override
  public void start()
    throws Exception
  {
    boolean isFirst = true;

    for (TcpPort listener : _listeners) {
      if (listener == _clusterListener)
        continue;

      if (isFirst)
        log.info("");

      isFirst = false;

      listener.bind();
      
      listener.start();
    }

    if (! isFirst)
      log.info("");
    
    _alarm = new Alarm(this);
    _alarm.queue(ALARM_TIMEOUT);
  }
  
  private void configure(CloudServer server, Object config)
  {
    NetworkClusterSystem.configServer(config, server);
  }

  /**
   * Handles the alarm.
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      for (TcpPort listener : _listeners) {
        if (listener.isClosed()) {
          log.severe("Resin restarting due to closed listener: " + listener);
          // destroy();
          //_controller.restart();
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      // destroy();
      //_controller.restart();
    } finally {
      alarm = _alarm;
      
      if (alarm != null)
        alarm.queue(ALARM_TIMEOUT);
    }
  }

  /**
   * Closes the server.
   */
  @Override
  public void stop()
  {
    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null) {
      alarm.dequeue();
    }

    for (TcpPort listener : _listeners) {
      try {
        if (listener != _clusterListener) {
          listener.closeBind();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
