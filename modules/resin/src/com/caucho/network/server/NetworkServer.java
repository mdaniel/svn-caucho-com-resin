/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.network.server;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Alarm;
import com.caucho.vfs.Path;

public class NetworkServer
{
  private static final Logger log
    = Logger.getLogger(NetworkServer.class.getName());

  private static final EnvironmentLocal<NetworkServer> _serverLocal
    = new EnvironmentLocal<NetworkServer>();

  private final String _id;
  private final Path _rootDirectory;
  private final Path _dataDirectory;
  private EnvironmentClassLoader _classLoader;
  
  private final ConcurrentHashMap<Class<? extends Service>,Service> _serviceMap
    = new ConcurrentHashMap<Class<? extends Service>,Service>();
  
  private InjectManager _injectManager;

  private Throwable _configException;

  private long _shutdownWaitMax = 60 * 1000;

  // private ServerAdmin _admin;

  private final Lifecycle _lifecycle;

  // stats

  private long _startTime;

  /**
   * Creates a new servlet server.
   */
  public NetworkServer(String id,
                       Path rootDirectory,
                       Path dataDirectory)
  {
    _id = id;
    _rootDirectory = rootDirectory;
    _dataDirectory = dataDirectory;
    _classLoader = EnvironmentClassLoader.create("server:" + id);

    _serverLocal.set(this, _classLoader);

    _lifecycle = new Lifecycle(log, toString(), Level.INFO);
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);
      
      _injectManager = InjectManager.create();
      
      BeanFactory<NetworkServer> beanFactory
        = _injectManager.createBeanFactory(NetworkServer.class);
      // factory.deployment(Standard.class);
      beanFactory.type(NetworkServer.class);
      _injectManager.addBean(beanFactory.singleton(this));
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
 
  /**
   * Returns the current server
   */
  public static NetworkServer getCurrent()
  {
    return _serverLocal.get();
  }
  
  /**
   * Returns the server id
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the configuration instance.
   */
  public void setConfigException(Throwable exn)
  {
    _configException = exn;
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Returns the internal data directory.
   */
  public Path getDataDirectory()
  {
    return _dataDirectory;
  }

  //
  // statistics
  //

  /**
   * Returns the time the server started in ms.
   */
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * Returns the lifecycle state
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModified()
  {
    boolean isModified = _classLoader.isModified();

    if (isModified)
      _classLoader.logModified(log);

    return isModified;
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    boolean isModified = _classLoader.isModifiedNow();

    if (isModified)
      log.fine("server is modified");

    return isModified;
  }

  /**
   * Returns true if the server is starting or active
   */
  public boolean isAfterStarting()
  {
    return _lifecycle.isAfterStarting();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopping()
  {
    return _lifecycle.isStopping();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroying()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is currently active and accepting requests
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }
  
  //
  // lifecycle operations
  //

  /**
   * Start the server.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStarting())
        return;

      _startTime = Alarm.getCurrentTime();

      if (! Alarm.isTest()) {
        log.info("");

        log.info(System.getProperty("os.name")
                 + " " + System.getProperty("os.version")
                 + " " + System.getProperty("os.arch"));

        log.info(System.getProperty("java.runtime.name")
                 + " " + System.getProperty("java.runtime.version")
                 + ", " + System.getProperty("file.encoding")
                 + ", " + System.getProperty("user.language"));

        log.info(System.getProperty("java.vm.name")
                 + " " + System.getProperty("java.vm.version")
                 + ", " + System.getProperty("sun.arch.data.model")
                 + ", " + System.getProperty("java.vm.info")
                 + ", " + System.getProperty("java.vm.vendor"));

        log.info("");

        // log.info("resin.home = " + resin.getResinHome().getNativePath());
        log.info("resin.root = " + _rootDirectory);

        log.info("");

        log.info("user.name  = " + System.getProperty("user.name"));
      }

      _lifecycle.toStarting();

      ArrayList<Service> services = new ArrayList<Service>(_serviceMap.values());
      
      // sort
      
      for (Service service : services) {
        service.start();
      }

      // _alarm.queue(ALARM_INTERVAL);

      _lifecycle.toActive();
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      throw e;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      // if the server can't start, it needs to completely fail, especially
      // for the watchdog
      throw new RuntimeException(e);

      // log.log(Level.WARNING, e.toString(), e);

      // _configException = e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Handles the alarm.
   */
  /*
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      if (isModified()) {
        // XXX: message slightly wrong
        String msg = L.l("Resin restarting due to configuration change");

        // startShutdown(msg);
        return;
      }

      try {
        ArrayList<SocketLinkListener> ports = _ports;

        for (int i = 0; i < ports.size(); i++) {
          SocketLinkListener port = ports.get(i);

          if (port.isClosed()) {
            log.severe("Resin restarting due to closed port: " + port);
            // destroy();
            //_controller.restart();
          }
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        // destroy();
        //_controller.restart();
        return;
      }
    } finally {
      alarm.queue(ALARM_INTERVAL);
    }
  }
  */

  /**
   * Closes the server.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStopping())
        return;

      // notify other servers that we've stopped
      // notifyStop();

      /*
      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();
        */

      _lifecycle.toStop();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Closes the server.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      /*
       * destroy
       */

      log.fine(this + " destroyed");

      _classLoader.destroy();
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);

      // resin.startShutdown(L.l("Resin shutdown from Server.destroy()"));
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + getId() + "]");
  }
}
