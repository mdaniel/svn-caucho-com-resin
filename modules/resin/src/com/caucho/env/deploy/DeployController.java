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

package com.caucho.env.deploy;

import java.util.logging.Level;
import java.util.logging.Logger;

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
  
  private final String _id;

  private DeployMode _startupMode = DeployMode.DEFAULT;
  private DeployMode _redeployMode = DeployMode.DEFAULT;

  private int _startupPriority = Integer.MAX_VALUE;

  private DeployControllerStrategy _strategy;

  protected final Lifecycle _lifecycle;

  private Alarm _alarm = new WeakAlarm(this);
  private long _redeployCheckInterval = REDEPLOY_CHECK_INTERVAL;
  
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

  /**
   * Returns the controller's id.
   */
  @Override
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
  final public long getStartTime()
  {
    return _startTime;
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
  }

  /**
   * Returns the state name.
   */
  @Override
  public final LifecycleState getState()
  {
    return _lifecycle.getState();
  }

  /**
   * Returns true if the instance has been idle for longer than its timeout.
   *
   * @return true if idle
   */
  public final boolean isIdleTimeout()
  {
    DeployInstance instance = getDeployInstance();

    if (instance != null)
      return instance.isDeployIdle();
    else
      return false;
  }
  
  //
  // dependency/modified
  //
  
  /**
   * Returns true if the entry is modified.
   */
  @Override
  public boolean isModified()
  {
    if (isControllerModified())
      return true;
    
    DeployInstance instance = getDeployInstance();

    return instance == null || instance.isModified();
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    if (isControllerModifiedNow())
      return true;
    
    DeployInstance instance = getDeployInstance();

    return instance == null || instance.isModifiedNow();
  }
  
  /**
   * Log the reason for modification
   */
  @Override
  final public boolean logModified(Logger log)
  {
    if (controllerLogModified(log)) {
      return true;
    }
    
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

  protected boolean isControllerModified()
  {
    return false;
  }
  
  protected boolean isControllerModifiedNow()
  {
    return false;
  }
  
  protected boolean controllerLogModified(Logger log)
  {
    return false;
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

  @Override
  public void addLifecycleListener(LifecycleListener listener)
  {
    _lifecycle.addListener(listener);
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
  final I restartImpl()
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

      if (deployInstance == null)
        throw new NullPointerException(getClass().getName());
      
      loader = deployInstance.getClassLoader();
      thread.setContextClassLoader(loader);

      isStarting = _lifecycle.toStarting();
      
      if (! isStarting)
        return deployInstance;
      
      configureInstance(deployInstance);

      deployInstance.start();

      isActive = true;

      _startTime = Alarm.getCurrentTime();
    } catch (ConfigException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      _lifecycle.toError();

      onError(e);

      if (deployInstance != null) {
        log.finer(e.toString());
        deployInstance.setConfigException(e);
      }
      else {
        log.severe(e.toString());
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
      
      _lifecycle.toError();
      
      onError(e);

      if (deployInstance != null) {
        log.finer(e.toString());
        deployInstance.setConfigException(e);
      }
      else {
        log.log(Level.SEVERE, e.toString(), e);
      }
    } finally {
      if (isActive) {
        _lifecycle.toActive();
        
        onActive();
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
   * Stops the current instance, putting it in the lazy state.
   */
  final void stopLazyImpl()
  {
    if (! _lifecycle.isIdle()) {
      stopImpl();
    }

    _lifecycle.toIdle();
  }

  /**
   * Stops the current instance.
   */
  final void stopImpl()
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

        onStop();
      }
      
      thread.setContextClassLoader(oldLoader);
    }

    return;
  }
  
  //
  // state callbacks
  //
  
  protected void onActive()
  {
  }
  
  protected void onError(Throwable e)
  {
  }
  
  protected void onStop()
  {
  }

  /**
   * Configuration of the instance
   */
  protected void configureInstance(I deployInstance)
    throws Exception
  {
  }

  /**
   * Handles the redeploy check alarm.
   */
  @Override
  public final void handleAlarm(Alarm alarm)
  {
    try {
      alarm();
    } finally {
      if (! _lifecycle.isDestroyed())
        alarm.queue(_redeployCheckInterval);
    }
  }
  
  @Override
  public final void alarm()
  {
    _strategy.alarm(this);
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
