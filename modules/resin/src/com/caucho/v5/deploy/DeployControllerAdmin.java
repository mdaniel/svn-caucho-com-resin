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

package com.caucho.v5.deploy;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleNotification;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.management.server.DeployControllerMXBean;
import com.caucho.v5.util.CurrentTime;

/**
 * A deploy controller for an environment.
 */
abstract public class DeployControllerAdmin<I extends DeployInstanceEnvironment,
                                            C extends DeployControllerEnvironment<I,?>>
  extends ManagedObjectBase
  implements DeployControllerMXBean,
             NotificationEmitter,
             LifecycleListener,
             java.io.Serializable
{
  // private transient final C _controller;
  private transient final DeployHandle<I> _handle;

  // XXX: why transient?
  private transient final NotificationBroadcasterSupport _broadcaster;

  private long _sequence = 0;

  public DeployControllerAdmin(DeployHandle<I> handle)
  {
    // _controller = controller;
    _handle = handle;
    _broadcaster  = new NotificationBroadcasterSupport();
    
    // XXX: controller.addLifecycleListener(this);
  }

  /**
   * Returns the controller.
   */
  protected C getController()
  {
    return (C) getHandle().getService().getController();
  }

  /**
   * Returns the handle.
   */
  protected DeployHandle<I> getHandle()
  {
    return _handle;
  }

  protected void register()
  {
    registerSelf();
  }

  protected void unregister()
  {
    unregisterSelf();
  }
  
  @Override
  public String getId()
  {
    return getHandle().getId();
  }

  @Override
  public String getName()
  {
    return getController().getMBeanId();
  }

  @Override
  public String getStartupMode()
  {
    return getController().getStartupMode().toString();
  }

  @Override
  public String getRedeployMode()
  {
    return getController().getRedeployMode().toString();
  }

  @Override
  public long getRedeployCheckInterval()
  {
    return getController().getRedeployCheckInterval();
  }

  @Override
  public String getState()
  {
    DeployHandle<I> handle = getHandle();
    
    LifecycleState state = handle.getState();
    
    if (state.isActive() && handle.isModified())
      return state.toString() + "_MODIFIED";
    else
      return state.toString();
  }

  @Override
  public String getErrorMessage()
  {
    return getController().getErrorMessage();
  }

  @Override
  public Date getStartTime()
  {
    return new Date(getController().getStartTime());
  }
  
  @Override
  public Map<String,String> getRepositoryMetaData()
  {
    return getController().getRepositoryMetaData();
  }
  
  @Override
  public String []getClassPath()
  {
    //return getHandle().getDeployInstance().getClassPath();
    return null;
  }
  
  //
  // Lifecycle
  //

  /**
   * Starts the controller.
   */
  @Override
  public void start()
    throws Exception
  {
    getHandle().start();
  }

  @Override
  public void stop()
    throws Exception
  {
    getHandle().stop(ShutdownModeAmp.GRACEFUL);
  }

  @Override
  public void restart()
    throws Exception
  {
    getHandle().stop(ShutdownModeAmp.GRACEFUL);
    getHandle().start();
  }

  @Override
  public void update()
    throws Exception
  {
    getHandle().update();
  }

  @Override
  public boolean destroy()
    throws Exception
  {
    return getController().destroy();
  }

  /**
   * Returns the root directory
   */
  @Override
  public String getRootDirectory()
  {
    return getController().getRootDirectory().getNativePath();
  }

  @Override
  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws IllegalArgumentException
  {
    _broadcaster.addNotificationListener(listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    _broadcaster.removeNotificationListener(listener);
  }

  @Override
  public void removeNotificationListener(NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws ListenerNotFoundException
  {
    _broadcaster.removeNotificationListener(listener, filter, handback);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo()
  {
    // XXX: temporary hack
    MBeanNotificationInfo status = new MBeanNotificationInfo(new String[] { "jmx.attribute.change" }, "status", "status attribute changes");

    return new MBeanNotificationInfo[] { status };
  }

  @Override
  public void lifecycleEvent(LifecycleState oldState, LifecycleState newState)
  {
    Logger log = getController().getLog();

    long timestamp = CurrentTime.getCurrentTime();

    String oldValue = oldState.toString();
    String newValue = newState.toString();

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " lifecycleEvent " + oldValue + " -> " + newValue);

    if (newState.isActive()) {
      LifecycleNotification notif;
      notif = new LifecycleNotification(LifecycleNotification.AFTER_START,
                                        this, _sequence++, timestamp,
                                        toString () + " started");

      _broadcaster.sendNotification(notif);
    }

    if (oldState.isActive()) {
      LifecycleNotification notif;
      notif = new LifecycleNotification(LifecycleNotification.BEFORE_STOP,
                                        this, _sequence++, timestamp,
                                        toString() + " stopping");

      _broadcaster.sendNotification(notif);
    }

    /*
    AttributeChangeNotification notification
      = new AttributeChangeNotification(this, _sequence++,
                                        timestamp, message, "State", "java.lang.String", oldValue, newValue);

    _broadcaster.sendNotification(notification);
    */
  }

  @Override
  public String toString()
  {
    String name =  getClass().getName();

    int i = name.lastIndexOf('.') + 1;

    if (i > 0 && i < name.length()) {
      name = name.substring(i);
    }

    return name + "[" + getObjectName() + "]";
  }
}
