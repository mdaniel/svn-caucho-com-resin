/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
package com.caucho.netbeans.ide;

import com.caucho.netbeans.ResinDeploymentManager;
import com.caucho.netbeans.ResinStartServer;
import com.caucho.netbeans.ResinWizardProvider;

import org.netbeans.modules.j2ee.deployment.plugins.spi.FindJSPServlet;
import org.netbeans.modules.j2ee.deployment.plugins.spi.IncrementalDeployment;
import org.netbeans.modules.j2ee.deployment.plugins.spi.OptionalDeploymentManagerFactory;
import org.netbeans.modules.j2ee.deployment.plugins.spi.ServerInstanceDescriptor;
import org.netbeans.modules.j2ee.deployment.plugins.spi.StartServer;
import org.openide.WizardDescriptor.InstantiatingIterator;

import javax.enterprise.deploy.spi.DeploymentManager;
import java.util.logging.*;

public class ResinOptionalFactory
        extends OptionalDeploymentManagerFactory {

  static {
    System.setProperty("com.caucho.level", "100");
  }

  private static final Logger log = Logger.getLogger(ResinOptionalFactory.class.getName());

  public ResinOptionalFactory() {
    log.config("new ResinOptionalFactory");
  }

  public StartServer getStartServer(DeploymentManager deploymentManager) {
    return new ResinStartServer((ResinDeploymentManager) deploymentManager);
  }

  public IncrementalDeployment getIncrementalDeployment(DeploymentManager deploymentManager) {
    return null;
  }

  @Override
  public ServerInstanceDescriptor getServerInstanceDescriptor(DeploymentManager dm) {
    return super.getServerInstanceDescriptor(dm);
  }

  /*
  public TargetModuleIDResolver getTargetModuleIDResolver(DeploymentManager deploymentManager)
  {
  return new ResinTargetModuleIDResolver(deploymentManager);
  }
   */
  public FindJSPServlet getFindJSPServlet(DeploymentManager deploymentManager) {
    // return new ResinFindJSPServlet(deploymentManager);
    return null;
  }

  @Override
  public InstantiatingIterator getAddInstanceIterator() {
    return ResinWizardProvider.getInstance().getInstantiatingIterator();
  }
}
