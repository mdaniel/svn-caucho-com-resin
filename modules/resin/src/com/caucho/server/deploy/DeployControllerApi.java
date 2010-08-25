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

package com.caucho.server.deploy;

import com.caucho.lifecycle.LifecycleListener;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
public interface DeployControllerApi<I extends DeployInstance>
{
  /**
   * Returns the controller's id.
   */
  public String getId();

  /**
   * Returns true if the entry matches.
   */
  public boolean isNameMatch(String name);

  /**
   * Gets the startup priority.
   */
  public int getStartupPriority();

  /**
   * Returns the start time of the entry.
   */
  public long getStartTime();

  /**
   * Returns the state name.
   */
  public DeployControllerState getState();

  /**
   * Returns true if the instance is in the active state.
   */
  public boolean isActive();

  /**
   * Returns true if the instance is in the stopped state.
   *
   * @return true on stopped state
   */
  public boolean isStopped();

  /**
   * Returns true for the stop-lazy state
   */
  public boolean isStoppedLazy();

  /**
   * Returns true if the instance has been idle for longer than its timeout.
   *
   * @return true if idle
   */
  public boolean isActiveIdle();

  /**
   * Returns true if the entry is destroyed.
   */
  public boolean isDestroyed();

  /**
   * Return true if the instance is in the error state.
   *
   * @return true for the error state.
   */
  public boolean isError();

  /**
   * Start the controller for initialization.
   */
  public void startOnInit();

  /**
   * Force an instance start from an admin command.
   */
  public void start();

  /**
   * Stops the controller from an admin command.
   */
  public void stop();

  /**
   * Force an instance restart from an admin command.
   */
  public void restart();

  /**
   * Deploy the controller from an admin command.
   */
  public void deploy();

  /**
   * Update the controller from an admin command.
   */
  public void update();

  /**
   * Returns the current instance.
   */
  public I getDeployInstance();

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  public I request();

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  public I subrequest();
  
  public void addLifecycleListener(LifecycleListener listener);
}
