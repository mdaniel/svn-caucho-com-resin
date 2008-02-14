/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.admin;

import com.caucho.util.L10N;

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
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for the deployments.
 */
public class DeploymentManagerImpl
  implements DeploymentManager
{
  private static final L10N L = new L10N(DeploymentManagerImpl.class);
  private static final Logger log = Logger.getLogger(DeploymentManagerImpl.class.getName());

  private static final Pattern HMUX_URI_PATTERN
    = Pattern.compile("^resin://([^:]+):(\\d+)$");

  private final String _uri;
  private final String _address;
  private final int _port;

  private DeployClient _client;

  private volatile static int _taskCount;

  DeploymentManagerImpl(String uri)
    throws DeploymentManagerCreationException
  {
    _uri = uri;

    Matcher matcher;

    matcher = HMUX_URI_PATTERN.matcher(_uri);

    if (matcher.matches()) {
      _address = matcher.group(1);
      _port = Integer.parseInt(matcher.group(2));
    }
    else
      throw new DeploymentManagerCreationException(L.l("uri '{0}' is not a valid Resin deployment uri", _uri));
  }

  void connect(String user, String password)
    throws DeploymentManagerCreationException
  {
    try {
      _client = new DeployClient(_address, _port);
    }
    catch (Exception e) {
      DeploymentManagerCreationException exn
        = new DeploymentManagerCreationException(L.l("uri '{0}' is not a valid Resin deployment uri", _uri));

      exn.initCause(e);

      throw exn;
    }
  }

  private void validateConnection()
  {
    if (_client == null)
      throw new IllegalStateException(L.l("DeploymentManager is disconnected"));
  }

  public Target []getTargets()
    throws IllegalStateException
  {
    validateConnection();

    Thread thread = Thread.currentThread();

    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClass().getClassLoader());

      Target []targets = _client.getTargets();

      if (targets == null)
        return new Target[0];

      return targets;
    } catch (Throwable e) {
      log.log(Level.INFO, e.toString(), e);

      return new Target[0];
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public TargetModuleID []getRunningModules(ModuleType moduleType,
                                            Target []targetList)
    throws TargetException, IllegalStateException
  {
    validateConnection();

    throw new UnsupportedOperationException("unimplemented");
  }

  /**
   * Returns the current non-running modules.
   */
  public TargetModuleID []getNonRunningModules(ModuleType moduleType,
                                               Target []targetList)
    throws TargetException, IllegalStateException
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  /**
   * Returns all available modules.
   */
  public TargetModuleID []getAvailableModules(ModuleType moduleType,
                                              Target []targetList)
    throws TargetException, IllegalStateException
  {
    validateConnection();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());

      return _client.getAvailableModules(moduleType.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    throw new UnsupportedOperationException("unimplemented");
  }

  private ProgressObject execute(DeployTask deployTask)
  {
    deployTask.setContextClassLoader(getClass().getClassLoader());

    deployTask.start();

    return deployTask.getProgressObject();
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

    try {
      archiveIn = new FileInputStream(archive);
      ddIn = new FileInputStream(deploymentPlan);

      return distribute(targetList, archiveIn, ddIn);

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (archiveIn != null)
          archiveIn.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }

      try {
        if (ddIn != null)
          ddIn.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Deploys the object.
   */
  public ProgressObject distribute(final Target []targetList,
                                   final InputStream archive,
                                   final InputStream deploymentPlan)
    throws IllegalStateException
  {
    validateConnection();

    return execute(new DeployTask() {
      public void runImpl()
        throws Exception
      {
        if (deploymentPlan == null)
          throw new Exception(L.l("{0} is required", "deployment plan"));

        _client.distribute(targetList, deploymentPlan, archive);
      }
    });
  }

  /**
   * Starts the modules.
   */
  public ProgressObject start(final TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    validateConnection();

    return execute(new DeployTask(moduleIDList) {
      public void runImpl()
        throws Exception
      {
        _client.start(moduleIDList);
      }
    });
  }

  /**
   * Stops the modules.
   */
  public ProgressObject stop(final TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    validateConnection();

    return execute(new DeployTask(moduleIDList) {
      public void runImpl()
        throws Exception
      {
        _client.stop(moduleIDList);
      }
    });
  }

  /**
   * Undeploys the modules.
   */
  public ProgressObject undeploy(final TargetModuleID []moduleIDList)
    throws IllegalStateException
  {
    validateConnection();

    return execute(new DeployTask(moduleIDList) {
      public void runImpl()
        throws Exception
      {
        _client.undeploy(moduleIDList);
      }
    });
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
    validateConnection();

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
    validateConnection();

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

  public String toString()
  {
    return "DeploymentManagerImpl[" + _uri + "]";
  }

  abstract public class DeployTask
    extends Thread
  {
    private ProgressObjectImpl _progressObject;

    public DeployTask(TargetModuleID[] targetModuleIDs)
    {
      super(_client.getServiceName() + "-" + _taskCount++);

      _progressObject = new ProgressObjectImpl(targetModuleIDs);
    }

    public DeployTask()
    {
      this(new TargetModuleID[0]);
    }

    final public void run()
    {
      try {
        _progressObject.completed(L.l("Success"));
      }
      catch (Throwable t) {
        _progressObject.failed(t.toString());
      }
    }

    public ProgressObject getProgressObject()
    {
      return _progressObject;
    }

    /**
     * @throws Exception for any failure
     */
    abstract public void runImpl()
      throws Exception;

  }

    public ProgressObject distribute(Target[] arg0, ModuleType arg1, InputStream arg2, InputStream arg3) throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setDConfigBeanVersion(DConfigBeanVersionType arg0) throws DConfigBeanVersionUnsupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

