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

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;

/**
 * DeployHandle returns the currently deployed instance
 */
public interface DeployHandle<I extends DeployInstance>
{
  /**
   * Returns the controller's id, typically a tag value like
   * production/webapp/default/ROOT
   */
  String getId();
  
  DeployControllerService<I> getService();
  
  /**
   * Returns the state name.
   */
  LifecycleState getState();

  /**
   * Returns the current instance.
   */
  I getDeployInstance();

  /**
   * Returns the current instance, waiting for active.
   */
  I getActiveDeployInstance();

  /**
   * Check for modification updates, generally from an admin command when
   * using "manual" redeployment.
   */
  void update();

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  I request();

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  I subrequest();
  
  /**
   * External lifecycle listeners, so applications can detect deployment
   * and redeployment.
   */
  void addLifecycleListener(LifecycleListener listener);

  void start();

  void startOnInit();

  Throwable getConfigException();
  void stop(ShutdownModeAmp mode);
  void stopAndWait(ShutdownModeAmp mode);
  void destroy();
  
  boolean isModified();

  void alarm();
}
