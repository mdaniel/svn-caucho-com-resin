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

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.InputStream;

public class ResinDeploymentManager
  implements DeploymentManager
{

  public ProgressObject distribute(Target[] target, File file, File file2)
    throws IllegalStateException
  {
    return null;
  }

  public DeploymentConfiguration createConfiguration(DeployableObject deployableObject)
    throws InvalidModuleException
  {
    return new ResinConfiguration(deployableObject);
  }

  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 InputStream inputStream,
                                 InputStream inputStream2)
    throws UnsupportedOperationException, IllegalStateException
  {
    return null;
  }

  public ProgressObject distribute(Target[] target,
                                   InputStream inputStream,
                                   InputStream inputStream2)
    throws IllegalStateException
  {
    return null;
  }

  public ProgressObject undeploy(TargetModuleID[] targetModuleID)
    throws IllegalStateException
  {
    return null;
  }

  public ProgressObject stop(TargetModuleID[] targetModuleID)
    throws IllegalStateException
  {
    return null;
  }

  public ProgressObject start(TargetModuleID[] targetModuleID)
    throws IllegalStateException
  {
    return null;
  }

  public void setLocale(java.util.Locale locale)
    throws UnsupportedOperationException
  {
  }

  public boolean isLocaleSupported(java.util.Locale locale)
  {
    return false;
  }

  public TargetModuleID[] getAvailableModules(ModuleType moduleType,
                                              Target[] target)
    throws TargetException, IllegalStateException
  {
    return null;
  }

  public TargetModuleID[] getNonRunningModules(ModuleType moduleType,
                                               Target[] target)
    throws TargetException, IllegalStateException
  {
    return null;
  }

  public TargetModuleID[] getRunningModules(ModuleType moduleType,
                                            Target[] target)
    throws TargetException, IllegalStateException
  {
    return null;
  }

  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 File file,
                                 File file2)
    throws UnsupportedOperationException, IllegalStateException
  {
    return null;
  }

  public void setDConfigBeanVersion(DConfigBeanVersionType dConfigBeanVersionType)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType dConfigBeanVersionType)
  {
    return false;
  }

  public void release()
  {
  }

  public boolean isRedeploySupported()
  {
    return false;
  }

  public java.util.Locale getCurrentLocale()
  {
    return null;
  }

  public DConfigBeanVersionType getDConfigBeanVersion()
  {
    return null;
  }

  public java.util.Locale getDefaultLocale()
  {
    return null;
  }

  public java.util.Locale[] getSupportedLocales()
  {
    return null;
  }

  public Target[] getTargets()
    throws IllegalStateException
  {
    return null;
  }
}
