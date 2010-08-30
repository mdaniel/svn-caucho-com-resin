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

import com.caucho.env.deploy.DeployControllerApi;
import com.caucho.vfs.Dependency;

/**
 * A container of deploy objects.
 */
public interface DeployContainerApi<C extends DeployControllerApi<?>>
  extends Dependency
{
  /**
   * Adds a deploy generator.
   */
  public void add(DeployGenerator<C> generator);
  
  /**
   * Removes a deploy.
   */
  public void remove(DeployGenerator<C> generator);
  
  /**
   * Start the container.
   */
  public void start();

  /**
   * Checks for updates for all controllers and generators.
   */
  public void update();

  /**
   * Stops all controllers.
   */
  public void stop();
  
  /**
   * Closes the deployment container.
   */
  public void destroy();

  /**
   * Returns the matching controller.
   */
  public C findController(String name);

  /**
   * Returns the deployed entries.
   */
  public C []getControllers();

  /**
   * Called to explicitly update a controller.
   */
  public C update(String name);

  /**
   * Called to explicitly remove a controller.
   */
  public void remove(String name);
}
