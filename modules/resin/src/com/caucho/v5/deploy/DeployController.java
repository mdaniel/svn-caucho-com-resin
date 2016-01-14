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

import com.caucho.v5.lifecycle.LifecycleListener;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
public interface DeployController<I extends DeployInstance>
{
  /**
   * Returns the controller's id, typically a tag value like
   * production/webapp/default/ROOT
   */
  String getId();

  /**
   * Returns true if the entry matches, used for finding a matching web-app
   * or host.
   */
  boolean isNameMatch(String name);

  /**
   * Gets the startup priority.
   */
  int getStartupPriority();
  
  /**
   * Returns the controller type
   */
  DeployControllerType getControllerType();
  
  /**
   * Merging the controller
   */
  void merge(DeployController<I> newController);

  /**
   * Initialization of the controller itself
   */
  boolean init(DeployControllerService<I> service);
  
  /**
   * Returns the state name.
   */
  // LifecycleState getState();
  
  /**
   * Returns the handle for the controller
   */
  // DeployHandle<I> getHandle();

  /**
   * Returns the current instance.
   */
  // I getDeployInstance();

  /**
   * Returns the current instance, waiting for active.
   */
  //I getActiveDeployInstance();
  
  //
  // state transition operations
  //

  /**
   * Start the controller for initialization.
   */
  //void startOnInit();
  //void startOnInit(Result<Boolean> result);

  /**
   * Force an instance start from an admin command.
   */
  //void start();

  /**
   * Stops the controller from an admin command.
   */
  //void stop();

  /**
   * Force an instance restart from an admin command.
   */
  //void restart();

  /**
   * Check for modification updates, generally from an admin command when
   * using "manual" redeployment.
   */
  //void update();

  /**
   * Internal notification for modification updates, either from a timer
   * or a repository notification. Depending on the restart mode, the
   * alarm may trigger a restart.
   */
  //void alarm();

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  //public I request();

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  //public I subrequest();
  
  /**
   * Closes the controller
   */
  public void close();
  
  /**
   * Remove the controller
   */
  public void remove();
  
  /**
   * External lifecycle listeners, so applications can detect deployment
   * and redeployment.
   */
  public void addLifecycleListener(LifecycleListener listener);
}
