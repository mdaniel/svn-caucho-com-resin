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
import com.caucho.lifecycle.LifecycleListener;

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

  private ClassLoader _parentLoader;
  
  private String _id;

  private String _startupMode = STARTUP_DEFAULT;
  private String _redeployMode = REDEPLOY_DEFAULT;

  private DeployControllerStrategy _strategy;

  protected final Lifecycle _lifecycle;

  private Alarm _alarm = new WeakAlarm(this);
  private long _redeployCheckInterval = 60000L;

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

    _lifecycle = new Lifecycle(getLog(), toString());
  }

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
   * Merges with the old controller.
   */
  protected void mergeController(DeployController newController)
  {
  }

  /**
   * Merge the startup mode.
   */
  public void mergeStartupMode(String mode)
  {
    if (mode == null || STARTUP_DEFAULT.equals(mode))
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
    else {
      throw new ConfigException(L.l("'{0}' is an unknown startup-mode.  'automatic', 'lazy', and 'manual' are the acceptable values.",
				    mode));
    }
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
    if (mode == null || REDEPLOY_DEFAULT.equals(mode))
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
   * Returns true if the entry matches.
   */
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

  /**
   * Initialize the entry.
   */
  public final boolean init()
  {
    if (! _lifecycle.toInitializing())
      return false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      initBegin();

      if (_startupMode == STARTUP_MANUAL) {
	if (_redeployMode == REDEPLOY_AUTOMATIC) {
	  throw new IllegalStateException(L.l("startup='manual' and redeploy='automatic' is an unsupported combination."));
	}
	else
	  _strategy = StartManualRedeployManualStrategy.create();
      }
      else if (_startupMode == STARTUP_LAZY) {
	if (_redeployMode == REDEPLOY_MANUAL)
	  _strategy = StartLazyRedeployManualStrategy.create();
	else
	  _strategy = StartLazyRedeployAutomaticStrategy.create();
      }
      else {
	if (_redeployMode == STARTUP_MANUAL)
	  _strategy = StartAutoRedeployManualStrategy.create();
	else
	  _strategy = StartAutoRedeployAutoStrategy.create();
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
  public String getState()
  {
    if (isDestroyed())
      return "destroyed";
    else if (isStoppedLazy())
      return "stopped-lazy";
    else if (isStopped())
      return "stopped";
    else if (isError())
      return "error";
    else if (isModified())
      return "active-modified";
    else
      return "active";
  }

  /**
   * Returns true if the instance is in the active state.
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns true if the instance is in the stopped state.
   *
   * @return true on stopped state
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped() || _lifecycle.isInit();
  }

  /**
   * Returns true for the stop-lazy state
   */
  public boolean isStoppedLazy()
  {
    return _lifecycle.isInit();
  }

  /**
   * Returns true if the instance has been idle for longer than its timeout.
   *
   * @return true if idle
   */
  public boolean isActiveIdle()
  {
    DeployInstance instance = getDeployInstance();

    if (! _lifecycle.isActive())
      return false;
    else if (instance == null)
      return false;
    else
      return instance.isDeployIdle();
  }

  /**
   * Return true if the instance is in the error state.
   *
   * @return true for the error state.
   */
  public boolean isError()
  {
    if (_lifecycle.isError())
      return true;

    DeployInstance instance = getDeployInstance();

    return (instance != null &&
            instance.getConfigException() != null);
  }

  /**
   * Returns true if there's currently an error.
   */
  public boolean isErrorNow()
  {
    if (_lifecycle.isError())
      return true;
    
    DeployInstance instance = getDeployInstance();

    return (instance != null &&
            instance.getConfigException() != null);
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModified()
  {
    DeployInstance instance = getDeployInstance();

    return instance == null || instance.isModified();
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
  public final I getDeployInstance()
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
  public final void stop()
  {
    _strategy.stop(this);
  }

  /**
   * Force an instance restart from an admin command.
   */
  public final void restart()
  {
    _strategy.stop(this);
    _strategy.start(this);
  }

  /**
   * Update the controller from an admin command.
   */
  public final void update()
  {
    _strategy.update(this);
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  public I request()
  {
    if (_lifecycle.isDestroyed())
      return null;
    else if (_strategy != null)
      return _strategy.request(this);
    else
      return null;
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  public I subrequest()
  {
    if (_lifecycle.isDestroyed())
      return null;
    else if (_strategy != null)
      return _strategy.subrequest(this);
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

    I deployInstance = getDeployInstance();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ClassLoader loader = null;
    boolean isStarting = false;

    try {
      loader = deployInstance.getClassLoader();
      thread.setContextClassLoader(loader);

      isStarting = _lifecycle.toStarting();
      
      if (! isStarting)
	return deployInstance;
      
      expandArchive();
    
      addManifestClassPath();
      
      configureInstance(deployInstance);

      deployInstance.start();

      _startTime = Alarm.getCurrentTime();
    } catch (ConfigException e) {
      log.severe(e.toString());
      log.log(Level.FINE, e.toString(), e);

      _lifecycle.toError();

      if (deployInstance != null)
	deployInstance.setConfigException(e);
    } catch (Throwable e) {
      log.log(Level.SEVERE, e.toString(), e);

      _lifecycle.toError();

      if (deployInstance != null)
	deployInstance.setConfigException(e);
    } finally {
      if (isStarting)
	_lifecycle.toActive();

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
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    DeployInstance oldInstance = _deployInstance;
    boolean isStopping = false;

    if (oldInstance != null)
      thread.setContextClassLoader(oldInstance.getClassLoader());

    try {
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
      if (isStopping)
	_lifecycle.toStop();
      
      thread.setContextClassLoader(oldLoader);
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
  public String toString()
  {
    String className = getClass().getName();
    int p = className.lastIndexOf('.');

    return className.substring(p + 1) + "[" + getId() + "]";
  }
}
