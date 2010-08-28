/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.deploy;

import com.caucho.cloud.deploy.DeployNetworkService;
import com.caucho.cloud.deploy.DeployTagItem;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleListener;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.WeakAlarm;
import com.caucho.vfs.Dependency;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
abstract public class DeployController<I extends DeployInstance>
  implements DeployControllerApi<I>, Dependency, AlarmListener
{
  private static final Logger log
    = Logger.getLogger(DeployController.class.getName());
  private static final L10N L = new L10N(DeployController.class);
  
  public static final long REDEPLOY_CHECK_INTERVAL = 60000;

  private ClassLoader _parentLoader;
  
  private String _id;

  private DeployMode _startupMode = DeployMode.DEFAULT;
  private DeployMode _redeployMode = DeployMode.DEFAULT;

  private int _startupPriority = Integer.MAX_VALUE;

  private DeployControllerStrategy _strategy;

  protected final Lifecycle _lifecycle;

  private Alarm _alarm = new WeakAlarm(this);
  private long _redeployCheckInterval = REDEPLOY_CHECK_INTERVAL;
  
  private DeployTagItem _deployItem;

  private long _startTime;
  private I _deployInstance;

  protected DeployController(String id)
  {
    this(id, null);
  }

  protected DeployController(String id, ClassLoader parentLoader)
  {
    _id = id;
    
    if (parentLoader == null)
      parentLoader = Thread.currentThread().getContextClassLoader();
    
    _parentLoader = parentLoader;

    _lifecycle = new Lifecycle(getLog(), toString(), Level.FINEST);
  }

  /**
   * Creates an instance.
   */
  abstract protected I instantiateDeployInstance();

  public void addLifecycleListener(LifecycleListener listener)
  {
    _lifecycle.addListener(listener);
  }

  /**
   * Returns the controller's id.
   */
  public final String getId()
  {
    return _id;
  }

  /**
   * Returns the parent class loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }
  
  /**
   * Returns the deploy tag name.
   */
  protected String getDeployTag()
  {
    return null;
  }

  /**
   * Returns true for a versioning controller
   */
  public boolean isVersioning()
  {
    return false;
  }

  /**
   * Sets the startup mode.
   */
  public void setStartupMode(DeployMode mode)
  {
    _startupMode = mode;
  }

  /**
   * Sets the startup priority.
   */
  public void setStartupPriority(int priority)
  {
    _startupPriority = priority;
  }

  /**
   * Gets the startup priority.
   */
  @Override
  public int getStartupPriority()
  {
    return _startupPriority;
  }

  /**
   * Merges with the old controller.
   */
  protected void mergeController(DeployController<I> oldController)
  {
    _parentLoader = oldController._parentLoader = _parentLoader;

    if (oldController._startupPriority < _startupPriority)
      _startupPriority = oldController._startupPriority;
  }

  /**
   * Merge the startup mode.
   */
  public void mergeStartupMode(DeployMode mode)
  {
    if (mode == null || DeployMode.DEFAULT.equals(mode))
      return;

    _startupMode = mode;
  }

  /**
   * Returns the startup mode.
   */
  public DeployMode getStartupMode()
  {
    return _startupMode;
  }

  /**
   * Converts startup mode to code.
   */
  public static DeployMode toStartupCode(String mode)
    throws ConfigException
  {
    return DeployMode.valueOf(mode);
  }

  /**
   * Sets the redeploy mode.
   */
  public void setRedeployMode(DeployMode mode)
  {
    _redeployMode = mode;
  }

  /**
   * Merge the redeploy mode.
   */
  public void mergeRedeployMode(DeployMode mode)
  {
    if (mode == null || DeployMode.DEFAULT.equals(mode))
      return;

    _redeployMode = mode;
  }

  /**
   * Returns the redeploy mode.
   */
  public DeployMode getRedeployMode()
  {
    return _redeployMode;
  }

  /**
   * Converts redeploy mode to code.
   */
  public static DeployMode toRedeployCode(String mode)
    throws ConfigException
  {
    return DeployMode.valueOf(mode);
  }

  /**
   * Sets the redeploy-check-interval
   */
  public void mergeRedeployCheckInterval(long interval)
  {
    if (interval != REDEPLOY_CHECK_INTERVAL)
      _redeployCheckInterval = interval;
  }

  /**
   * Sets the redeploy-check-interval
   */
  public void setRedeployCheckInterval(Period period)
  {
    _redeployCheckInterval = period.getPeriod();

    if (_redeployCheckInterval < 0)
      _redeployCheckInterval = Period.INFINITE;

    if (_redeployCheckInterval < 5000)
      _redeployCheckInterval = 5000;
  }

  /**
   * Gets the redeploy-check-interval
   */
  public long getRedeployCheckInterval()
  {
    return _redeployCheckInterval;
  }

  /**
   * Returns true if
   */
  @Override
  public boolean isNameMatch(String name)
  {
    return getId().equals(name);
  }

  /**
   * Returns the start time of the entry.
   */
  public long getStartTime()
  {
    return _startTime;
  }
  
  public Throwable getConfigException()
  {
    return null;
  }

  /**
   * Returns the deploy admin.
   */
  protected DeployControllerAdmin<?> getDeployAdmin()
  {
    return null;
  }
  
  /**
   * Initialize the entry.
   */
  @Override
  public final boolean init()
  {
    if (! _lifecycle.toInitializing())
      return false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      initBegin();

      switch (_startupMode) {
      case MANUAL: {
        if (_redeployMode == DeployMode.AUTOMATIC) {
          throw new IllegalStateException(L.l("startup='manual' and redeploy='automatic' is an unsupported combination."));
        }
        else
          _strategy = StartManualRedeployManualStrategy.create();
        break;
      }

      case LAZY: {
        if (_redeployMode == DeployMode.MANUAL)
          _strategy = StartLazyRedeployManualStrategy.create();
        else
          _strategy = StartLazyRedeployAutomaticStrategy.create();
        break;
      }
      
      default: {
        if (_redeployMode == DeployMode.MANUAL)
          _strategy = StartAutoRedeployManualStrategy.create();
        else
          _strategy = StartAutoRedeployAutoStrategy.create();
      }
      }

      initEnd();

      return _lifecycle.toInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Initial calls for init.
   */
  protected void initBegin()
  {
  }

  /**
   * Final calls for init.
   */
  protected void initEnd()
  {
    DeployNetworkService deployService = DeployNetworkService.getCurrent();
    if (deployService != null && getDeployTag() != null) {
      deployService.addTag(getDeployTag());
      
      _deployItem = deployService.getTagItem(getDeployTag());
    }
  }

  protected String getMBeanTypeName()
  {
    String className = getDeployInstance().getClass().getName();
    int p = className.lastIndexOf('.');
    if (p > 0)
      className = className.substring(p + 1);

    return className;
  }

  protected String getMBeanId()
  {
    String name = getId();
    if (name == null || name.equals(""))
      name = "default";

    return name;
  }

  /**
   * Returns the state name.
   */
  @Override
  public LifecycleState getState()
  {
    return _lifecycle.getState();
    /*
    if (isDestroyed())
      return DeployControllerState.DESTROYED;
    else if (isStoppedLazy())
      return DeployControllerState.STOPPED_IDLE;
    else if (isStopped())
      return DeployControllerState.STOPPED;
    else if (isError())
      return DeployControllerState.ERROR;
    else if (isModified())
      return DeployControllerState.ACTIVE_MODIFIED;
    else
      return DeployControllerState.ACTIVE;
      */
  }

  /**
   * Returns true if the instance has been idle for longer than its timeout.
   *
   * @return true if idle
   */
  public boolean isIdleTimeout()
  {
    DeployInstance instance = getDeployInstance();

    if (instance != null)
      return instance.isDeployIdle();
    else
      return false;
  }
  
  //
  // dependency/modified

  /**
   * Returns true if the entry is modified.
   */
  @Override
  public boolean isModified()
  {
    DeployInstance instance = getDeployInstance();

    return instance == null || instance.isModified();
  }

  /**
   * Log the reason for modification
   */
  @Override
  public boolean logModified(Logger log)
  {
    DeployInstance instance = getDeployInstance();

    if (instance != null) {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(instance.getClassLoader());
      
        return instance.logModified(log);
      } finally {
        thread.setContextClassLoader(loader);
      }
    }
    else
      return false;
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    DeployInstance instance = getDeployInstance();

    return instance == null || instance.isModifiedNow();
  }

  /**
   * Returns the current instance.
   */
  @Override
  public final I getDeployInstance()
  {
    return _deployInstance;
  }

  /**
   * Returns the current instance.
   */
  protected final I createDeployInstance()
  {
    synchronized (this) {
      if (_deployInstance == null) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        try {
          thread.setContextClassLoader(_parentLoader);

          _deployInstance = instantiateDeployInstance();
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
      }

      return _deployInstance;
    }
  }

  /**
   * Redeploys the entry if it's modified.
   */
  @Override
  public void startOnInit()
  {
    if (! _lifecycle.isAfterInit())
      throw new IllegalStateException(L.l("startOnInit must be called after init (in '{0}')",
                                          _lifecycle.getStateName()));

    _strategy.startOnInit(this);
  }

  /**
   * Deploy the controller from an admin command.
   */
  @Override
  public void deploy()
  {
  }

  /**
   * Force an instance start from an admin command.
   */
  @Override
  public final void start()
  {
    _strategy.start(this);
  }

  /**
   * Stops the controller from an admin command.
   */
  @Override
  public final void stop()
  {
    _strategy.stop(this);
  }

  /**
   * Force an instance restart from an admin command.
   */
  @Override
  public final void restart()
  {
    _strategy.stop(this);
    _strategy.start(this);
  }

  /**
   * Update the controller from an admin command.
   */
  @Override
  public final void update()
  {
    _strategy.update(this);
  }

  /**
   * Updates version-specific information
   */
  public void updateVersion()
  {
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  @Override
  public final I request()
  {
    if (_lifecycle.isDestroyed())
      return null;
    else if (_strategy != null) {
      I instance = _strategy.request(this);
      
      return instance;
    }
    else
      return null;
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  @Override
  public final I subrequest()
  {
    if (_lifecycle.isDestroyed())
      return null;
    else if (_strategy != null) {
      I instance = _strategy.subrequest(this);
      
      return instance;
    }
    else
      return null;
  }

  /**
   * Restarts the instance
   *
   * @return the new instance
   */
  I restartImpl()
  {
    if (! _lifecycle.isStopped() && ! _lifecycle.isInit())
      stopImpl();

    return startImpl();
  }

  /**
   * Starts the entry.
   */
  protected I startImpl()
  {
    assert(_lifecycle.isAfterInit());

    if (DynamicClassLoader.isModified(_parentLoader)) {
      _deployInstance = null;
      return null;
    }

    I deployInstance = null;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ClassLoader loader = null;
    boolean isStarting = false;
    boolean isActive = false;

    try {
      thread.setContextClassLoader(_parentLoader);
      
      deployInstance = createDeployInstance();
      
      loader = deployInstance.getClassLoader();
      thread.setContextClassLoader(loader);

      isStarting = _lifecycle.toStarting();
      
      if (! isStarting)
        return deployInstance;
      
      expandArchive();
    
      addManifestClassPath();
      
      configureInstance(deployInstance);

      deployInstance.start();

      isActive = true;

      _startTime = Alarm.getCurrentTime();
    } catch (ConfigException e) {
      _lifecycle.toError();
      
      if (_deployItem != null)
        _deployItem.toError(e);

      if (deployInstance != null)
        deployInstance.setConfigException(e);
      else {
        log.severe(e.toString());
        log.log(Level.FINEST, e.toString(), e);
      }
    } catch (Throwable e) {
      _lifecycle.toError();
      
      if (_deployItem != null)
        _deployItem.toError(e);

      if (deployInstance != null)
        deployInstance.setConfigException(e);
      else
        log.log(Level.SEVERE, e.toString(), e);
    } finally {
      if (isActive) {
        _lifecycle.toActive();
        
        if (_deployItem != null && ! "error".equals(_deployItem.getState()))
          _deployItem.toStart();
      }
      else
        _lifecycle.toError();

      // server/
      if (loader instanceof DynamicClassLoader)
        ((DynamicClassLoader) loader).clearModified();

      if (_alarm != null)
        _alarm.queue(_redeployCheckInterval); // XXX: strategy-controlled

      thread.setContextClassLoader(oldLoader);
    }

    return deployInstance;
  }

  /**
   * Deploys the entry, e.g. archive expansion.
   */
  protected void expandArchive()
    throws Exception
  {
  }

  /**
   * Stops the current instance, putting it in the lazy state.
   */
  void stopLazyImpl()
  {
    if (! _lifecycle.isIdle()) {
      stopImpl();
    }

    _lifecycle.toIdle();
  }

  /**
   * Stops the current instance.
   */
  void stopImpl()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    DeployInstance oldInstance = _deployInstance;
    boolean isStopping = false;

    try {
      if (oldInstance != null)
        thread.setContextClassLoader(oldInstance.getClassLoader());
      
      isStopping = _lifecycle.toStopping();

      if (! isStopping)
        return;

      synchronized (this) {
        oldInstance = _deployInstance;
        _deployInstance = null;
      }

      if (oldInstance != null) {
        oldInstance.destroy();
      }
    } finally  {
      if (isStopping) {
        _lifecycle.toStop();

        if (_deployItem != null)
          _deployItem.toStop();
      }
      
      thread.setContextClassLoader(oldLoader);
    }

    return;
  }

  /**
   * Adds any manifest Class-Path
   */
  protected void addManifestClassPath()
    throws IOException
  {
  }

  /**
   * Configuration of the instance
   */
  protected void configureInstance(I deployInstance)
    throws Throwable
  {
  }

  /**
   * Handles the redeploy check alarm.
   */
  @Override
  public final void handleAlarm(Alarm alarm)
  {
    try {
      _strategy.alarm(this);
    } finally {
      if (! _lifecycle.isDestroyed())
        alarm.queue(_redeployCheckInterval);
    }
  }
  
  @Override
  public final void close()
  {
    destroy();
  }

  /**
   * Destroys the entry.
   */
  protected boolean destroy()
  {
    if (_lifecycle.isAfterInit())
      stop();
    
    if (! _lifecycle.toDestroy())
      return false;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null) {
      alarm.dequeue();
    }
    
    return true;
  }

  /**
   * Returns the appropriate log for debugging.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns the entry's debug name.
   */
  @Override
  public String toString()
  {
    String className = getClass().getName();
    int p = className.lastIndexOf('.');

    return className.substring(p + 1) + "[" + getId() + "]";
  }
}
