/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.WeakAlarm;
import com.caucho.util.AlarmListener;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.loader.DynamicClassLoader;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.make.Dependency;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
abstract public class DeployController<I extends DeployInstance>
  implements Dependency, AlarmListener {
  private static final Logger log = Log.open(DeployController.class);
  private static final L10N L = new L10N(DeployController.class);

  public static final String STARTUP_DEFAULT = "default";
  public static final String STARTUP_AUTOMATIC = "automatic";
  public static final String STARTUP_LAZY = "lazy";
  public static final String STARTUP_MANUAL = "manual";

  public static final String REDEPLOY_DEFAULT = "default";
  public static final String REDEPLOY_AUTOMATIC = "automatic";
  public static final String REDEPLOY_LAZY = "lazy";
  public static final String REDEPLOY_MANUAL = "manual";

  private DeployContainer _deployContainer;
  private ClassLoader _parentLoader;
  
  private String _name;

  private String _startupMode = STARTUP_AUTOMATIC;
  private String _redeployMode = REDEPLOY_AUTOMATIC;

  private DeployControllerStrategy _strategy;

  protected final Lifecycle _lifecycle = new Lifecycle(getLog());

  private Alarm _alarm = new WeakAlarm(this);
  private long _redeployCheckInterval = 60000L;

  private long _startTime;
  private I _deployInstance;

  protected DeployController()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  protected DeployController(ClassLoader parentLoader)
  {
    if (parentLoader == null)
      parentLoader = Thread.currentThread().getContextClassLoader();
    
    _parentLoader = parentLoader;
  }

  /**
   * Sets the deploy container.
   */
  public void setDeployContainer(DeployContainer deploy)
  {
    _deployContainer = deploy;
  }

  /**
   * Returns the parent class loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the entry's canonical name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the entry's canonical name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the startup mode.
   */
  public void setStartupMode(String mode)
  {
    try {
      _startupMode = toStartupCode(mode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Merge the startup mode.
   */
  public void mergeStartupMode(String mode)
  {
    if (STARTUP_DEFAULT.equals(mode))
      return;

    _startupMode = mode;
  }

  /**
   * Returns the startup mode.
   */
  public String getStartupMode()
  {
    return _startupMode;
  }

  /**
   * Converts startup mode to code.
   */
  public static String toStartupCode(String mode)
    throws ConfigException
  {
    if ("automatic".equals(mode))
      return STARTUP_AUTOMATIC;
    else if ("lazy".equals(mode))
      return STARTUP_LAZY;
    else if ("manual".equals(mode))
      return STARTUP_MANUAL;
    else
      throw new ConfigException(L.l("'{0}' is an unknown startup-mode.  'automatic', 'lazy', and 'manual' are the acceptable values.",
				    mode));
  }

  /**
   * Returns true for automatic startup.
   */
  public boolean isStartupAutomatic()
  {
    return (_startupMode == STARTUP_AUTOMATIC ||
	    _startupMode == STARTUP_DEFAULT);
  }

  /**
   * Returns true for lazy startup.
   */
  public boolean isStartupLazy()
  {
    return _startupMode == STARTUP_LAZY;
  }

  /**
   * Returns true for manual startup.
   */
  public boolean isStartupManual()
  {
    return _startupMode == STARTUP_MANUAL;
  }

  /**
   * Sets the redeploy mode.
   */
  public void setRedeployMode(String mode)
  {
    try {
      _redeployMode = toRedeployCode(mode);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Merge the redeploy mode.
   */
  public void mergeRedeployMode(String mode)
  {
    if (REDEPLOY_DEFAULT.equals(mode))
      return;

    _redeployMode = mode;
  }

  /**
   * Returns the redeploy mode.
   */
  public String getRedeployMode()
  {
    return _redeployMode;
  }

  /**
   * Converts redeploy mode to code.
   */
  public static String toRedeployCode(String mode)
    throws ConfigException
  {
    if ("automatic".equals(mode))
      return REDEPLOY_AUTOMATIC;
    else if ("lazy".equals(mode))
      return REDEPLOY_LAZY;
    else if ("manual".equals(mode))
      return REDEPLOY_MANUAL;
    else
      throw new ConfigException(L.l("'{0}' is an unknown redeploy-mode.  'automatic', 'lazy', and 'manual' are the acceptable values.",
				    mode));
  }

  /**
   * Returns true for automatic redeploy.
   */
  public boolean isRedeployAutomatic()
  {
    return (_redeployMode == REDEPLOY_AUTOMATIC ||
	    _redeployMode == REDEPLOY_DEFAULT);
  }

  /**
   * Returns true for lazy redeploy.
   */
  public boolean isRedeployLazy()
  {
    return _redeployMode == REDEPLOY_LAZY;
  }

  /**
   * Returns true for manual redeploy.
   */
  public boolean isRedeployManual()
  {
    return (_redeployMode == REDEPLOY_MANUAL ||
	    _startupMode == STARTUP_MANUAL);
  }

  /**
   * Returns true if the entry matches.
   */
  public boolean isNameMatch(String name)
  {
    return name.equals(_name);
  }

  /**
   * Returns the start time of the entry.
   */
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * Initialize the entry.
   */
  public boolean init()
  {
    _lifecycle.setName(toString());

    if (_parentLoader != Thread.currentThread().getContextClassLoader())
      throw new IllegalStateException(L.l("init must be called from parent class loader"));

    if (_redeployMode == REDEPLOY_MANUAL)
      _strategy = StartManualRedeployManualStrategy.create();
    else {
      if (_startupMode == STARTUP_LAZY)
        _strategy = StartLazyRedeployAutomaticStrategy.create();
      else
        _strategy = StartAutoRedeployAutoStrategy.create();
    }

    return _lifecycle.toInit();
  }

  /**
   * Deploys the entry, e.g. archive expansion.
   */
  protected void deploy()
    throws Exception
  {
  }

  /**
   * Deploys the entry, e.g. for JMX registration..
   */
  protected void deployEntry()
  {
  }

  /**
   * Returns the state name.
   */
  public String getState()
  {
    if (isDestroyed())
      return "destroyed";
    else if (isStopped())
      return "stopped";
    else if (isStoppedLazy())
      return "stopped-lazy";
    else if (isError())
      return "error";
    else if (isModified())
      return "active-modified";
    else
      return "active";
  }

  /**
   * Returns true if the instance is in the stopped state.
   *
   * @return true on stopped state
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped();
  }

  /**
   * Returns true for the stop-lazy state
   */
  public boolean isStoppedLazy()
  {
    return _deployInstance == null;
  }

  /**
   * Returns true if the instance has been idle for longer than its timeout.
   *
   * @return true if idle
   */
  public boolean isActiveIdle()
  {
    DeployInstance instance = _deployInstance;

    return instance != null && instance.isDeployIdle();
  }

  /**
   * Return true if the instance is in the error state.
   *
   * @return true for the error state.
   */
  public boolean isError()
  {
    DeployInstance instance = _deployInstance;

    return (instance != null &&
            instance.getConfigException() != null);
  }

  /**
   * Returns true if there's currently an error.
   */
  public boolean isErrorNow()
  {
    DeployInstance instance = _deployInstance;

    return (instance != null &&
            instance.getConfigException() != null);
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModified()
  {
    DeployInstance instance = _deployInstance;
    
    return instance == null || instance.isModified();
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    DeployInstance instance = _deployInstance;

    return instance == null || instance.isModifiedNow();
  }

  /**
   * Returns the current instance, returning null if no active instance
   * exists.
   */
  public I getDeployInstance()
  {
    // Can't wait for active because of init dependencies.

    return _deployInstance;
  }

  /**
   * Redeploys the entry if it's modified.
   */
  public void startOnInit()
  {
    if (! _lifecycle.isAfterInit())
      throw new IllegalStateException(L.l("startOnInit must be called after init (in '{0}')",
                                          _lifecycle.getStateName()));

    _strategy.startOnInit(this);

  }

  /**
   * Force an instance start from an admin command.
   */
  public final void start()
  {
    _strategy.start(this);
  }

  /**
   * Stops the controller from an admin command.
   */
  protected final void stop()
  {
    _strategy.stop(this);
  }

  /**
   * Update the controller from an admin command.
   */
  protected final void update()
  {
    _strategy.update(this);
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  public I request()
  {
    return _strategy.request(this);
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  public I subrequest()
  {
    return _strategy.subrequest(this);
  }

  /**
   * Redeploys the entry if it's modified.
   */
  public final void redeployIfModified()
  {
    redeployIfModifiedImpl(false, true);
  }
    
  /**
   * Redeploys the entry if it's modified.
   */
  protected I redeployIfModifiedImpl(boolean lazyRequest,
				     boolean enableRedeploy)
  {
    Thread.dumpStack();
    I oldInstance = _deployInstance;

    if (_startupMode == STARTUP_MANUAL)
      return oldInstance;
    else if (oldInstance == null) {
      if (! lazyRequest && _startupMode != STARTUP_AUTOMATIC)
	return null;
    }
    else if (_redeployMode == REDEPLOY_MANUAL || ! enableRedeploy)
      return oldInstance;
    else if ((_startupMode != STARTUP_LAZY || lazyRequest) &&
	     ! oldInstance.isModified() &&
	     ! oldInstance.isDeployError())
      return oldInstance;
    else if (_startupMode == STARTUP_LAZY && ! lazyRequest &&
	     ! oldInstance.isModified() &&
	     ! oldInstance.isDeployIdle())
      return oldInstance;
    else if (! _lifecycle.toStopping())
      return oldInstance;

    _deployInstance = null;

    if (oldInstance != null) {
      try {
	oldInstance.destroy();
      } finally {
	_lifecycle.toStop();
      }
    }

    // automatically undeploy idle lazy webapps
    if (_startupMode == STARTUP_LAZY && ! lazyRequest &&
	(oldInstance == null ||
	 oldInstance.isDeployError() ||
	 oldInstance.isDeployIdle())) {
      _deployContainer.update(_name);

      return null;
    }

    /*
    if (_deploy != null) {
      DeployController<I> newEntry = _deploy.update(_name);

      if (newEntry != this)
	return newEntry.startImpl();
    }
    */

    return startImpl();
  }
  /**
   * Restarts the instance
   *
   * @return the new instance
   */
  I restartImpl()
  {
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

    if (! _lifecycle.toStarting())
      return _deployInstance;

    I deployInstance = null;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      deploy();
    
      deployInstance = instantiateDeployInstance();
      _deployInstance = deployInstance;

      ClassLoader loader = deployInstance.getClassLoader();
      thread.setContextClassLoader(loader);

      addManifestClassPath();
      
      configureInstance(deployInstance);

      deployInstance.start();

      _startTime = Alarm.getCurrentTime();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e.toString());
      
      deployInstance.setConfigException(e);
    } finally {
      _lifecycle.toActive();

      _alarm.queue(_redeployCheckInterval); // XXX: strategy-controlled

      thread.setContextClassLoader(oldLoader);
    }

    return deployInstance;
  }

  /**
   * Stops the current instance, putting it in the lazy state.
   */
  void stopLazyImpl()
  {
    if (_lifecycle.isInit()) 
      return;

    stopImpl();

    _lifecycle.toPostInit();
  }

  /**
   * Stops the current instance.
   */
  void stopImpl()
  {
    if (! _lifecycle.toStopping())
      return;

    try {
      DeployInstance oldInstance = null;

      synchronized (this) {
        oldInstance = _deployInstance;
        _deployInstance = null;
      }

      if (oldInstance != null) {
          oldInstance.destroy();
      }
    } finally  {
      _lifecycle.toStop();
    }

    return;
  }

  /**
   * Creates an instance.
   */
  abstract protected I instantiateDeployInstance();

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
  public void handleAlarm(Alarm alarm)
  {
    try {
      _strategy.alarm(this);
    } finally {
      if (! _lifecycle.isDestroyed())
	alarm.queue(_redeployCheckInterval);
    }
  }

  /**
   * Returns true if the entry is destroyed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Destroys the entry.
   */
  protected boolean destroy()
  {
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
  public String toString()
  {
    String className = getClass().getName();
    int p = className.lastIndexOf('.');

    return className.substring(p + 1) + "[" + _name + "]";
  }
}
