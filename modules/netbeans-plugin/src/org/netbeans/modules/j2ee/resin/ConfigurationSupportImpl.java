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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package org.netbeans.modules.j2ee.resin;

import java.io.File;
import java.util.logging.Logger;
import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.common.api.OriginalCMPMapping;
import org.netbeans.modules.j2ee.deployment.plugins.api.ConfigurationSupport;


public class ConfigurationSupportImpl extends ConfigurationSupport
{
  private static final Logger log = Logger.getLogger(ConfigurationSupportImpl.class.getName());

  
  public void setMappingInfo(DeploymentConfiguration config, OriginalCMPMapping[] mappings)
  {
  }
  
  public void ensureResourceDefined(DeploymentConfiguration config, DDBean bean)
  {
  }
  
  public String getWebContextRoot(DeploymentConfiguration config, DeployableObject deplObj)
  throws ConfigurationException
  {
    return ((ResinConfiguration)config).getContextPath();
  }
  
  public void setWebContextRoot(DeploymentConfiguration config, DeployableObject deplObj, String contextRoot)
  throws ConfigurationException
  {
    ((ResinConfiguration)config).setContextPath(contextRoot);
  }
  
  public void initConfiguration(DeploymentConfiguration config, File[] files,
          File resourceDir, boolean keepUpdated) throws ConfigurationException
  {
    ((ResinConfiguration)config).init(files[0]);
  }
  
  public void disposeConfiguration(DeploymentConfiguration config)
  {
  }
  
  public void updateResourceDir(DeploymentConfiguration config, File resourceDir)
  {
  }
}