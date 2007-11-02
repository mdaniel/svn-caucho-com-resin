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

package org.netbeans.modules.j2ee.resin;

import org.openide.util.NbBundle;

import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;


public class ResinDeploymentFactory
  implements DeploymentFactory
{

  public static final String URI_PREFIX = "deployer:resin"; // NOI18N
  private static DeploymentFactory instance;

  public static synchronized DeploymentFactory create()
  {
    if (instance == null) {
      instance = new ResinDeploymentFactory();
      DeploymentFactoryManager.getInstance()
        .registerDeploymentFactory(instance);
    }
    return instance;
  }

  public boolean handlesURI(String uri)
  {
    return uri != null && uri.startsWith(URI_PREFIX);
  }

  public DeploymentManager getDeploymentManager(String uri,
                                                String uname,
                                                String passwd)
    throws DeploymentManagerCreationException
  {
    if (!handlesURI(uri)) {
      throw new DeploymentManagerCreationException("Invalid URI:" +
                                                   uri); // NOI18N
    }
    return new ResinDeploymentManager();

  }

  public DeploymentManager getDisconnectedDeploymentManager(String uri)
    throws DeploymentManagerCreationException
  {
    if (!handlesURI(uri)) {
      throw new DeploymentManagerCreationException("Invalid URI:" +
                                                   uri); // NOI18N
    }
    return new ResinDeploymentManager();

  }

  public String getProductVersion()
  {
    return "0.1"; // NOI18N
  }

  public String getDisplayName()
  {
    return NbBundle.getMessage(ResinDeploymentFactory.class,
                               "TXT_DisplayName"); // NOI18N
  }
}
