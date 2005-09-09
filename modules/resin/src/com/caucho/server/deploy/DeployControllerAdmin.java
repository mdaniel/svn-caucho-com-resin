/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationEmitter;
import javax.management.MBeanOperationInfo;

import com.caucho.jmx.MBeanHandle;
import com.caucho.jmx.IntrospectionMBeanDescriptor;
import com.caucho.jmx.IntrospectionAttributeDescriptor;
import com.caucho.jmx.AdminAttributeCategory;
import com.caucho.jmx.IntrospectionOperationDescriptor;
import com.caucho.jmx.IntrospectionClosure;

import com.caucho.server.deploy.mbean.DeployControllerMBean;
import com.caucho.lifecycle.LifecycleListener;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.Alarm;

/**
 * A deploy controller for an environment.
 */
public class DeployControllerAdmin<C extends EnvironmentDeployController>
  implements DeployControllerMBean, NotificationEmitter, LifecycleListener, java.io.Serializable
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

  public void describe(IntrospectionMBeanDescriptor descriptor)
  {
  }

  /**
   * Returns the controller.
   */
  protected C getController()
  {
    return _controller;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _controller.getObjectName();
  }

  public void describeObjectName(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setIgnored(true);
  }

  /**
   * Returns the controller state.
   */
  public String getState()
  {
    return getController().getState();
  }

  public void describeState(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setSortOrder(1000);
  }

  /**
   * Returns the time of the last start
   */
  public Date getStartTime()
  {
    return new Date(getController().getStartTime());
  }

  public void describeStartTime(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setSortOrder(1010);
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    getController().start();
  }

  public void describeStart(IntrospectionOperationDescriptor descriptor)
  {
    descriptor.setImpact(MBeanOperationInfo.ACTION);

    descriptor.setSortOrder(10000);

    descriptor.setEnabled(new IntrospectionClosure() {
      public Object call()
        throws Exception
      {
        return getController().isStopped();
      }
    });
  }

  /**
   * Stops the server.
   */
  public void stop()
    throws Exception
  {
    getController().stop();
  }

  public void describeStop(IntrospectionOperationDescriptor descriptor)
  {
    descriptor.setImpact(MBeanOperationInfo.ACTION);

    descriptor.setSortOrder(10010);

    descriptor.setEnabled(new IntrospectionClosure() {
      public Object call()
        throws Exception
      {
        return getController().isActive();
      }
    });
  }

  /**
   * Restarts the server.
   */
  public void restart()
    throws Exception
  {
    getController().stop();
    getController().start();
  }

  public void describeRestart(IntrospectionOperationDescriptor descriptor)
  {
    descriptor.setImpact(MBeanOperationInfo.ACTION);

    descriptor.setSortOrder(10020);

    descriptor.setEnabled(new IntrospectionClosure() {
      public Object call()
        throws Exception
      {
        return getController().isActive();
      }
    });
  }

  /**
   * Restarts the server if changes are detected.
   */
  public void update()
    throws Exception
  {
    getController().update();
  }

  public void describeUpdate(IntrospectionOperationDescriptor descriptor)
  {
    descriptor.setImpact(MBeanOperationInfo.ACTION);

    descriptor.setSortOrder(10030);
  }

  /**
   * Returns the root directory
   */
  public String getRootDirectory()
  {
    return _controller.getRootDirectory().getNativePath();
  }

  public void describeRootDirectory(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setSortOrder(100);
  }

  /**
   * Returns the handle for serialization.
   */
  public Object writeReplace()
  {
    return new MBeanHandle(getController().getObjectName());
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
    long sequence;

    String oldValue = Lifecycle.getStateName(oldState);
    String newValue = Lifecycle.getStateName(newState);
    String message = newValue;

    sequence = _sequence++;

    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST,  toString() + " lifecycleEvent `" + newValue  + "'");

    AttributeChangeNotification notification
      = new AttributeChangeNotification(this, sequence, timestamp, message, "State", "java.lang.String", oldValue, newValue);

    _broadcaster.sendNotification(notification);
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
