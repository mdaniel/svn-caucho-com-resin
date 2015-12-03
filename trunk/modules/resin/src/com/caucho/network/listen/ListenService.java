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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.*;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;

/**
 * The socket listen service, which accepts sockets and dispatches them to
 * protocols. 
 */
public class ListenService extends AbstractResinSubSystem
{
  public static final int START_PRIORITY_LISTEN = 2000;
  public static final int START_PRIORITY_CLUSTER = 2100;

  private static final L10N L = new L10N(ListenService.class);
  private static final Logger log = 
    Logger.getLogger(ListenService.class.getName());
  
  private final ResinSystem _server;
  
  private final ArrayList<TcpPort> _listeners
    = new ArrayList<TcpPort>();
  
  private final ContainerProgram _listenDefaults
    = new ContainerProgram();

  private final Lifecycle _lifecycle = new Lifecycle();
  private AtomicBoolean _isStartedListeners = new AtomicBoolean();
  
  private ListenService()
  {
    _server = ResinSystem.getCurrent();
  }

  public static ListenService createAndAddService()
  {
    ResinSystem system = preCreate(ListenService.class);

    ListenService service = new ListenService();
    system.addService(ListenService.class, service);
    
    return service;
  }
  
  public static ListenService getCurrent()
  {
    return ResinSystem.getCurrentService(ListenService.class);
  }

  /**
   * Creates a listener with the defaults applied.
   * The listener will not be registered until addNotificationListener is called.
   */
  public TcpPort createListener()
  {
    TcpPort listener = new TcpPort();
  
    applyListenerDefaults(listener);
    
    return listener;
  }

  /**
   * Registers a listener with the service.
   */
  public void addListener(TcpPort listener)
  {
    try {
      if (_listeners.contains(listener))
        throw new IllegalStateException(L.l("listener '{0}' has already been registered", listener));
      
      _listeners.add(listener);
    
      if (_lifecycle.getState().isAfterStarting()) {
        // server/1e00
        listener.bind();
        listener.start();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the {@link TcpPort}s for this server.
   */
  public Collection<TcpPort> getListeners()
  {
    return Collections.unmodifiableList(_listeners);
  }

  private void applyListenerDefaults(TcpPort port)
  {
    _listenDefaults.configure(port);
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_LISTEN;
  }
  
  @Override
  public void start()
    throws Exception
  {
    bindListeners();
    startListeners();
  }

  @Override
  public void stop()
    throws Exception
  {
    ArrayList<TcpPort> listeners = _listeners;
    for (int i = 0; i < listeners.size(); i++) {
      TcpPort listener = listeners.get(i);

      try {
        listener.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Bind the ports.
   */
  private void bindListeners()
    throws Exception
  {
    if (_isStartedListeners.getAndSet(true))
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_server.getClassLoader());

      ArrayList<TcpPort> listeners = _listeners;
      boolean isFirst = true;

      for (int i = 0; i < listeners.size(); i++) {
        TcpPort listener = listeners.get(i);
          
        if (listener.isAfterBind())
          continue;
          
        if (isFirst) {
          log.info("");
          isFirst = false;
        }

        listener.bind();
      }

      if (! isFirst)
        log.info("");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Start the listeners
   */
  private void startListeners()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_server.getClassLoader());

      ArrayList<TcpPort> listeners = _listeners;
      for (int i = 0; i < listeners.size(); i++) {
        TcpPort listener = listeners.get(i);

        listener.start();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
