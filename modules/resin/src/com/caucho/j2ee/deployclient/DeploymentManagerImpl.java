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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.j2ee.deployclient;

import com.caucho.hessian.client.HessianProxyFactory;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Manager for the deployments.
 */
public class DeploymentManagerImpl implements DeploymentManager {
  private String _uri;

  private DeploymentManagerAPI _proxy;

  DeploymentManagerImpl(String uri)
  {
    int p = uri.indexOf("http");
    if (p < 0)
      throw new IllegalArgumentException(uri);

    _uri = uri.substring(p);
  }

  private void log(String message)
  {
    System.out.println(getClass().getSimpleName() + ": " + message);
  }

  /**
   * Connect to the manager.
   */
  void connect(String user, String password)
    throws DeploymentManagerCreationException
  {
    try {
      log("CONNECT:");
      
      HessianProxyFactory factory = new HessianProxyFactory();

      factory.setUser(user);
      factory.setPassword(password);
      factory.setDebug(true);
      factory.setReadTimeout(120000);

      _proxy =
	(DeploymentManagerAPI) factory.create(DeploymentManagerAPI.class, _uri);
    } catch (Exception e) {
      e.printStackTrace();
      
      DeploymentManagerCreationException exn;
      
      exn = new DeploymentManagerCreationException(e.getMessage());
      exn.initCause(e);
      throw exn;
    }
  }
  
  /**
   * Returns the targets supported by the manager.
   */
  public Target []getTargets()
    throws IllegalStateException
  {
    if (_proxy == null)
      throw new IllegalStateException("DeploymentManager is disconnected");

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());

      Target []targets = _proxy.getTargets();

      if (targets == null)
	return new Target[0];
      
      return targets;
    } catch (Throwable e) {
      log("EXCEPTION-TARGETS:");
      e.printStackTrace();
      
      return new Target[0];
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Returns the current running modules.
   */
  public TargetModuleID []getRunningModules(ModuleType moduleType,
					    Target []targetList)
    throws TargetException, IllegalStateException
  {
    log("GET-RUN-GET-RUN-MMODULES");
    return new TargetModuleID[0];
  }
  
  /**
   * Returns the current non-running modules.
   */
  public TargetModuleID []getNonRunningModules(ModuleType moduleType,
					       Target []targetList)
    throws TargetException, IllegalStateException
  {
    log("GET-NON-RUN-MODULES");
    return new TargetModuleID[0];
  }
  
  /**
   * Returns all available modules.
   */
  public TargetModuleID []getAvailableModules(ModuleType moduleType,
					      Target []targetList)
    throws TargetException, IllegalStateException
  {
    log("GET-MODULES:");
    if (_proxy == null)
      throw new IllegalStateException("DeploymentManager is disconnected");

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());
      
      return _proxy.getAvailableModules(moduleType.toString());
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Returns a configuration for the deployable object.
   */
  public DeploymentConfiguration createConfiguration(DeployableObject dObj)
    throws InvalidModuleException
  {
    log("CREATE-CONF");
    throw new UnsupportedOperationException();
  }
  
  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target []targetList,
				   File archive,
				   File deploymentPlan)
    throws IllegalStateException
  {
    InputStream archiveIn = null;
    InputStream ddIn = null;

    log("PRE-DIST:");

    try {
      archiveIn = new FileInputStream(archive);
      ddIn = new FileInputStream(deploymentPlan);

      ProgressObject progress = distribute(targetList, archiveIn, ddIn);

      log("POST-DIST:" + progress + " " + progress.getDeploymentStatus().isFailed() + " " + progress.getDeploymentStatus().getMessage());
    
      return progress;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
	archiveIn.close();
      } catch (Throwable e) {
      }
      
      try {
	ddIn.close();
      } catch (Throwable e) {
      }      
    }
  }
  
  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target []targetList,
				   InputStream archive,
				   InputStream deploymentPlan)
    throws IllegalStateException
  {
    log("DISTRIBUTE:");
    if (_proxy == null)
      throw new IllegalStateException("DeploymentManager is disconnected");

    if (deploymentPlan == null)
      return null;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());

      
      ProgressObject progress
	= _proxy.distribute(targetList, deploymentPlan, archive);

      return progress;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Starts the modules.
   */
  public ProgressObject start(TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    return new ProgressObjectImpl(moduleIDList);
  }
  
  /**
   * Stops the modules.
   */
  public ProgressObject stop(TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    return new ProgressObjectImpl(moduleIDList);
  }
  
  /**
   * Undeploys the modules.
   */
  public ProgressObject undeploy(TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    log("UNDEPLOY:");
    if (_proxy == null)
      throw new IllegalStateException("DeploymentManager is disconnected");

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());
      
      return _proxy.undeploy(moduleIDList);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns true if the redeploy is supported.
   */
  public boolean isRedeploySupported()
  {
    return false;
  }
  
  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID []targetList,
				 File archive,
				 File deploymentPlan)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID []targetList,
				 InputStream archive,
				 InputStream deploymentPlan)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Frees any resources.
   */
  public void release()
  {
  }

  /**
   * Returns the default locale.
   */
  public Locale getDefaultLocale()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the current locale.
   */
  public Locale getCurrentLocale()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the default locale.
   */
  public void setLocale(Locale locale)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the supported locales.
   */
  public Locale []getSupportedLocales()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the locale is supported.
   */
  public boolean isLocaleSupported(Locale locale)
  {
    return false;
  }

  /**
   * Returns the bean's J2EE version.
   */
  public DConfigBeanVersionType getDConfigBeanVersion()
  {
    return DConfigBeanVersionType.V1_4;
  }

  /**
   * Returns true if the given version is supported.
   */
  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType version)
  {
    return true;
  }

  /**
   * Sets true if the given version is supported.
   */
  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  /**
   * Return the debug view of the manager.
   */
  public String toString()
  {
    return "DeploymentManagerImpl[" + _uri + "]";
  }
}

