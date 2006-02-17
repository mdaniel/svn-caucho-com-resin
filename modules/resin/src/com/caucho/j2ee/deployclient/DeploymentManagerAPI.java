/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.j2ee.deployclient;

import java.io.InputStream;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

import javax.enterprise.deploy.spi.status.ProgressObject;

/**
 * Interface for the deployment manager.
 */
public interface DeploymentManagerAPI {
  /**
   * Returns the targets for the server.
   */
  public TargetImpl []getTargets();
  
  /**
   * Distributes the archive.
   */
  public ProgressObject distribute(Target []targets,
				   InputStream deploymentPlan,
				   InputStream archiveIs);
  
  /**
   * Gets the current modules.
   */
  public TargetModuleID []getAvailableModules(String moduleType);
  
  /**
   * Undeploys the archive.
   */
  public ProgressObject undeploy(TargetModuleID []ids);
}

