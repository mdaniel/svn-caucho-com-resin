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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.deploy;

import java.util.Set;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.make.Dependency;
import com.caucho.util.L10N;
import com.caucho.util.Log;

/**
 * The generator for the deploy
 */
abstract public class DeployGenerator<E extends DeployController>
  implements Dependency, EnvironmentListener {
  private static final Logger log = Log.open(DeployGenerator.class);
  private static final L10N L = new L10N(DeployGenerator.class);

  // The owning deployment container
  private DeployContainer<E> _container;

  private ClassLoader _parentClassLoader;

  private String _startupMode = DeployController.STARTUP_AUTOMATIC;

  /**
   * Creates the deploy.
   */
  public DeployGenerator(DeployContainer<E> container)
  {
    _parentClassLoader = Thread.currentThread().getContextClassLoader();
    _container = container;
  }

  /**
   * Returns the deploy container.
   */
  public DeployContainer<E> getDeployContainer()
  {
    return _container;
  }

  /**
   * Returns the parent class loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentClassLoader;
  }

  /**
   * Sets the startup mode.
   */
  public void setStartupMode(String mode)
    throws ConfigException
  {
    _startupMode = DeployController.toStartupCode(mode);
  }

  /**
   * Gets the startup mode.
   */
  public String getStartupMode()
    throws ConfigException
  {
    return _startupMode;
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Starts the deployment.
   */
  public void start()
  {
    Environment.addEnvironmentListener(this);
  }

  /**
   * lazy-start
   */
  public void request()
  {
  }

  /**
   * Forces an update.
   */
  public void update()
  {
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
  }

  /**
   * Generates the controller.
   */
  protected E generateController(String key)
  {
    return null;
  }

  /**
   * Merges the entry with other matching entries, returning the
   * new entry.
   */
  protected E mergeController(E controller, String key)
  {
    return controller;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Stops the deploy
   */
  public void stop()
  {
  }

  /**
   * Closes the deploy
   */
  public void destroy()
  {
    _container.remove(this);
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }
}
