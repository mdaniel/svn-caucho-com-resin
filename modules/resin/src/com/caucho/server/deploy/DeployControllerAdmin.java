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

import java.io.IOException;

import javax.management.ObjectName;

import com.caucho.jmx.MBeanHandle;

import com.caucho.server.deploy.mbean.DeployControllerMBean;

/**
 * A deploy controller for an environment.
 */
public class DeployControllerAdmin<C extends EnvironmentDeployController>
  implements DeployControllerMBean, java.io.Serializable {
  private transient C _controller;

  public DeployControllerAdmin(C controller)
  {
    _controller = controller;
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

  /**
   * Returns the controller state.
   */
  public String getState()
  {
    return getController().getState();
  }
  
  /**
   * Returns the time of the last start
   */
  public Date getStartTime()
  {
    return new Date(getController().getStartTime());
  }

  /**
   * Stops the server.
   */
  public void stop()
    throws Exception
  {
    getController().stop();
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    getController().start();
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

  /**
   * Restarts the server if changes are detected.
   */
  public void update()
    throws Exception
  {
    getController().update();
  }

  /**
   * Returns the handle for serialization.
   */
  public Object writeReplace()
  {
    return new MBeanHandle(getController().getObjectName());
  }
}
