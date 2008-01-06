/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.netbeans.core;

import com.caucho.netbeans.core.AddInstanceIterator;
import com.caucho.netbeans.ResinStartServer;

import org.netbeans.modules.j2ee.deployment.plugins.spi.FindJSPServlet;
import org.netbeans.modules.j2ee.deployment.plugins.spi.IncrementalDeployment;
import org.netbeans.modules.j2ee.deployment.plugins.spi.OptionalDeploymentManagerFactory;
import org.netbeans.modules.j2ee.deployment.plugins.spi.StartServer;
import org.netbeans.modules.j2ee.deployment.plugins.spi.TargetModuleIDResolver;
import org.openide.WizardDescriptor.InstantiatingIterator;

import javax.enterprise.deploy.spi.DeploymentManager;
import java.util.logging.*;

public class ResinOptionalFactory
  extends OptionalDeploymentManagerFactory
{
  private static final Logger log = Logger.getLogger(ResinOptionalFactory.class.getName());

  public StartServer getStartServer(DeploymentManager deploymentManager)
  {
    //log.info("start-server");
    //return new ResinStartServer(deploymentManager);
    return null;
  }

  public IncrementalDeployment getIncrementalDeployment(DeploymentManager deploymentManager)
  {
    //log.info("incr-server");
    return null;
  }

  /*
  public TargetModuleIDResolver getTargetModuleIDResolver(DeploymentManager deploymentManager)
  {
    log.info("foo-server");
    return new ResinTargetModuleIDResolver(deploymentManager);
  }
  */

  public FindJSPServlet getFindJSPServlet(DeploymentManager deploymentManager)
  {
    //log.info("foo1-server");
    // return new ResinFindJSPServlet(deploymentManager);

    return null;
  }

  public InstantiatingIterator getAddInstanceIterator()
  {
    //log.info("foo12-add");
    //    return new AddInstanceIterator();
    return null;
  }
}
