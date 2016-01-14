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

import io.baratine.files.Watch;
import io.baratine.service.Cancel;
import io.baratine.service.Result;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.vfs.PathImpl;

/**
 * A container of deploy objects.
 */
public interface DeployContainerService<I extends DeployInstance,
                                 M extends DeployController<I>>
  extends Dependency
{
  /**
   * Adds a deploy generator.
   */
  void add(DeployGenerator<I,M> generator);
  
  /**
   * Removes a deploy.
   */
  void remove(DeployGenerator<I,M> generator);
  
  /**
   * Start the container.
   */
  boolean start();

  /**
   * Checks for updates for all controllers and generators.
   */
  void update();

  /**
   * Stops all controllers.
   */
  Void stop(ShutdownModeAmp mode);
  
  /**
   * Closes the deployment container.
   */
  Void destroy(ShutdownModeAmp mode);
  
  /**
   * Returns the matching controller if it already exists.
   */
  M findDeployedController(String name);

  /**
   * Returns the matching controller, creating if necessary
   */
  //M findController(String name);

  /**
   * Returns the matching controller by the id.
   */
  M findControllerById(String tag);

  /**
   * Returns the deployed entries.
   */
  M []getControllers();

  /**
   * Returns the deployed entries.
   */
  DeployHandle<I> []getHandles();

  /**
   * Returns the matching handle, creating a controller if necessary.
   */
  DeployHandle<I> findHandle(String name);

  /**
   * Creates the matching handle based on the id.
   */
  DeployHandle<I> createHandle(String id);

  /**
   * Called to explicitly update a controller.
   */
  M update(String name);
  
  void addWatch(PathImpl path, Watch watch, Result<Cancel> result);

  /**
   * Called to explicitly remove a controller.
   */
  // void remove(String name);
}
