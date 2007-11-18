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

package javax.enterprise.deploy.shared.factories;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

/**
 * Factory for the implementation classes.
 */
public final class DeploymentFactoryManager {
  private final static DeploymentFactoryManager _manager
    = new DeploymentFactoryManager();
  
  private DeploymentFactoryManager()
  {
  }
  /**
   * Returns the manager.
   */
  public static DeploymentFactoryManager getInstance()
  {
    return _manager;
  }

  /**
   * Returns the current registered factories.
   */
  public DeploymentFactory []getDeploymentFactories()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the matchnig manager.
   */
  public DeploymentManager getDeploymentManager(String uri,
						String username,
						String password)
    throws DeploymentManagerCreationException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a factory.
   */
  public void registerDeploymentFactory(DeploymentFactory factory)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a manager.
   */
  public DeploymentManager getDisconnectedDeploymentManager(String uri)
    throws DeploymentManagerCreationException
  {
    throw new UnsupportedOperationException();
  }
}

