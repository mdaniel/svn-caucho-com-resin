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

package com.caucho.netbeans;

import com.caucho.netbeans.PluginL10N;
import com.caucho.netbeans.core.ResinTarget;

import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;

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
import java.util.Locale;
import java.util.logging.*;

public final class ResinDeploymentManager
  implements DeploymentManager
{
  private static final Logger log
    = Logger.getLogger(ResinDeploymentManager.class.getName());
  
  private static final PluginL10N L = new PluginL10N(ResinStartServer.class);

  private final String _uri;
  private final ResinConfiguration _resinConfiguration;
  private final ResinProcess _resinProcess;

  private ResinPlatformImpl _j2eePlatform;

  public ResinDeploymentManager(String uri, boolean connected)
  {
    _uri = uri;

    
    // XXX: what is connected for?

    InstanceProperties instanceProperties = InstanceProperties.getInstanceProperties(_uri);

    _resinConfiguration = new ResinConfiguration(instanceProperties);

    ResinProcess resinProcess = new ResinProcess(_uri, _resinConfiguration);

    resinProcess.init();

    _resinProcess = resinProcess;
  }

  public ResinConfiguration getResinConfiguration()
  {
    return _resinConfiguration;
  }

  public ResinProcess getResinProcess()
  {
    return _resinProcess;
  }

  public Target[] getTargets()
    throws IllegalStateException
  {
    log.info("get targets");
    return new ResinTarget[]{
      new ResinTarget(_uri, _resinConfiguration.getDisplayName())
    };
  }

  public TargetModuleID[] getRunningModules(ModuleType moduleType,
                                            Target[] target)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  public TargetModuleID[] getNonRunningModules(ModuleType moduleType,
                                               Target[] target)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  public TargetModuleID[] getAvailableModules(ModuleType moduleType,
                                              Target[] target)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  public DeploymentConfiguration createConfiguration(DeployableObject deployableObject)
    throws InvalidModuleException
  {
    log.info("createConfiguration");
    ModuleType type = deployableObject.getType();

    if (type == ModuleType.WAR)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EAR)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EJB)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else {
      throw new InvalidModuleException(L.l("Unsupported module type ''{0}''", type));
    }
  }

  public ProgressObject distribute(Target[] target, File file, File file0)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject distribute(Target[] target,
                                   InputStream inputStream,
                                   InputStream inputStream0)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject distribute(Target[] target,
				   ModuleType type,
                                   InputStream inputStream,
                                   InputStream inputStream0)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject start(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject stop(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject undeploy(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public boolean isRedeploySupported()
  {
    return false;
  }

  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 File file,
                                 File file0)
    throws UnsupportedOperationException, IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 InputStream inputStream,
                                 InputStream inputStream0)
    throws UnsupportedOperationException, IllegalStateException
  {
    throw new UnsupportedOperationException("XXX: unimplemented");
  }

  public void release()
  {
  }

  public Locale getDefaultLocale()
  {
    return null;
  }

  public Locale getCurrentLocale()
  {
    return null;
  }

  public void setLocale(Locale locale)
    throws UnsupportedOperationException
  {
  }

  public Locale[] getSupportedLocales()
  {
    return null;
  }

  public boolean isLocaleSupported(Locale locale)
  {
    return false;
  }

  public DConfigBeanVersionType getDConfigBeanVersion()
  {
    return null;
  }

  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType dConfigBeanVersionType)
  {
    return false;
  }

  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  public void setDConfigBeanVersion(DConfigBeanVersionType dConfigBeanVersionType)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  public synchronized ResinPlatformImpl getJ2eePlatform()
  {
    log.info("GET j2ee-platform");
    
    if (_j2eePlatform == null)
      _j2eePlatform = new ResinPlatformImpl(_resinConfiguration);

    return _j2eePlatform;
  }

  public String getUri()
  {
    return _uri;
  }
}
