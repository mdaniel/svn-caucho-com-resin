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

package com.caucho.env.deploy;

import java.util.logging.Logger;


/**
 * The abstract deployment instance represents a deployed service like
 * a WebApp or a Host. The instrance works with the controller to handle
 * dynamic deployment.
 */
abstract public class AbstractDeployInstance implements DeployInstance {
  private ClassLoader _classLoader;
  private Throwable _configException;
  
  protected AbstractDeployInstance()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }
  
  /**
   * Returns the deployment class loader.
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  /**
   * The deployment class loader.
   */
  protected void setClassLoader(ClassLoader classLoader)
  {
    _classLoader = classLoader;
  }

  /**
   * Returns true if the deployment is modified.
   */
  @Override
  public boolean isModified()
  {
    return isModifiedNow();
  }

  /**
   * Returns true if the deployment is modified, forcing a check.
   */
  @Override
  public boolean isModifiedNow()
  {
    return false;
  }

  /**
   * Logs the reason for modification
   */
  @Override
  public boolean logModified(Logger log)
  {
    return false;
  }

  /**
   * Returns true if the deployment is modified for the timer redeploy.
   */
  /*
  @Override
  public boolean isDeployError()
  {
    return false;
  }
  */

  /**
   * Returns true if the deployment can be removed.
   */
  @Override
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Sets the configuration exception.
   */
  @Override
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Gets the configuration exception.
   */
  @Override
  public Throwable getConfigException()
  {
    return _configException;
  }
  
  /**
   * Starts the deployment instance
   */
  @Override
  public void start()
  {
  }

  /**
   * Destroys the deployment instance
   */
  @Override
  public void destroy()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
