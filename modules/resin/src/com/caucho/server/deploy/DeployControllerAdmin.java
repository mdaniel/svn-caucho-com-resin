/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.server.deploy;

import java.util.Date;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

import com.caucho.jmx.MBeanHandle;

import com.caucho.mbeans.server.DeployControllerMBean;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleListener;
import com.caucho.lifecycle.LifecycleNotification;

import com.caucho.util.Alarm;

/**
 * A deploy controller for an environment.
 */
abstract public class DeployControllerAdmin<C extends EnvironmentDeployController>
  implements DeployControllerMBean,
             NotificationEmitter,
             LifecycleListener,
             java.io.Serializable
{
  private transient final C _controller;

  // XXX: why transient?
  private transient final NotificationBroadcasterSupport _broadcaster;

  private long _sequence = 0;

  public DeployControllerAdmin(C controller)
  {
    _controller = controller;
    _broadcaster  = new NotificationBroadcasterSupport();
    controller.addLifecycleListener(this);
  }

  /**
   * Returns the controller.
   */
  protected C getController()
  {
    return _controller;
  }

  public ObjectName getObjectName()
  {
    return _controller.getObjectName();
  }

  public String getStartupMode()
  {
    return _controller.getStartupMode();
  }

  public String getRedeployMode()
  {
    return _controller.getRedeployMode();
  }

  public long getRedeployCheckInterval()
  {
    return _controller.getRedeployCheckInterval();
  }

  public String getState()
  {
    return getController().getState();
  }

  public Date getStartTime()
  {
    return new Date(getController().getStartTime());
  }

  /**
   * Starts the controller.
   */
  public void start()
    throws Exception
  {
    getController().start();
  }

  public void stop()
    throws Exception
  {
    getController().stop();
  }

  public void restart()
    throws Exception
  {
    getController().stop();
    getController().start();
  }

  public void update()
    throws Exception
  {
    getController().update();
  }

  /**
   * Returns the root directory
   */
  public String getRootDirectory()
  {
    return _controller.getRootDirectory().getNativePath();
  }

  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws IllegalArgumentException
  {
    _broadcaster.addNotificationListener(listener, filter, handback);
  }

  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    _broadcaster.removeNotificationListener(listener);
  }

  public void removeNotificationListener(NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws ListenerNotFoundException
  {
    _broadcaster.removeNotificationListener(listener, filter, handback);
  }

  public MBeanNotificationInfo[] getNotificationInfo()
  {
    // XXX: temporary hack
    MBeanNotificationInfo status = new MBeanNotificationInfo(new String[] { "jmx.attribute.change" }, "status", "status attribute changes");

    return new MBeanNotificationInfo[] { status };
  }

  synchronized public void lifecycleEvent(int oldState, int newState)
  {
    Logger log = _controller.getLog();

    long timestamp = Alarm.getCurrentTime();

    String oldValue = Lifecycle.getStateName(oldState);
    String newValue = Lifecycle.getStateName(newState);
    String message = newValue;

    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST,  toString() + " lifecycleEvent `" + newValue  + "'");

    if (newState == Lifecycle.IS_ACTIVE) {
      LifecycleNotification notif;
      notif = new LifecycleNotification(LifecycleNotification.AFTER_START,
                                        this, _sequence++, timestamp,
                                        toString () + " started");

      _broadcaster.sendNotification(notif);
    }

    if (oldState == Lifecycle.IS_ACTIVE) {
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

  public String toString()
  {
    String name =  getClass().getName();

    int i = name.lastIndexOf('.') + 1;

    if (i > 0 && i < name.length())
      name = name.substring(i);

    return name + "[" + getObjectName() + "]";
  }

}
