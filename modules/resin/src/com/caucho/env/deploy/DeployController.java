/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleListener;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
abstract public class DeployController<I extends DeployInstance>
  implements DeployControllerApi<I>, Dependency, DeployActionHandler
{
  private static final Logger log
    = Logger.getLogger(DeployController.class.getName());
  private static final L10N L = new L10N(DeployController.class);
  
  public static final long REDEPLOY_CHECK_INTERVAL = 60000;

  private ClassLoader _parentLoader;
  
  private final String _id;
  private final String _idStage;
  private final String _idType;
  private final String _idKey;

  private DeployMode _startupMode = DeployMode.DEFAULT;
  private DeployMode _redeployMode = DeployMode.DEFAULT;

  private int _startupPriority = Integer.MAX_VALUE;
  
  private DeployControllerType _controllerType = DeployControllerType.STATIC;

  private DeployControllerStrategy _strategy
    = StartManualRedeployManualStrategy.STRATEGY;

  protected final Lifecycle _lifecycle;
  
  private DeployControllerAlarm<DeployController<I>> _alarm;

  private DeployTagItem _deployTagItem;
  
  private long _waitForActiveTimeout = 10000L;
  private long _redeployCheckInterval = REDEPLOY_CHECK_INTERVAL;
  
  private long _startTime;
  private final AtomicReference<I> _deployInstanceRef
    = new AtomicReference<I>();

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
    
    int p1 = id.indexOf('/');
    _idStage = id.substring(0, p1);
    
    int p2 = id.indexOf('/', p1 + 1);
    _idType = id.substring(p1 + 1, p2);
    
    _idKey = id.substring(p2 + 1);
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
  
  public final String getIdStage()
  {
    return _idStage;
  }
  
  public final String getIdType()
  {
    return _idType;
  }
  
  public final String getIdKey()
  {
    return _idKey;
  }

  /**
   * Returns the parent class loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }
  
  @Override
  public DeployControllerType getControllerType()
  {
    return _controllerType;
  }
  
  public void setControllerType(DeployControllerType type)
  {
    _controllerType = type;
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
   * Merges with the new controller information
   */
  @Override
  public void merge(DeployControllerApi<I> newController)
  {
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
    if (interval != REDEPLOY_CHECK_INTERVAL) {
      _redeployCheckInterval = interval;
    }
  }

  /**
   * Sets the redeploy-check-interval
   */
  public void setRedeployCheckInterval(Period period)
  {
    _redeployCheckInterval = period.getPeriod();

    if (_redeployCheckInterval < 0) {
      _redeployCheckInterval = Period.INFINITE;
    }

    if (_redeployCheckInterval < 5000) {
      _redeployCheckInterval = 5000;
    }
  }

  /**
   * Gets the redeploy-check-interval
   */
  public long getRedeployCheckInterval()
  {
    return _redeployCheckInterval;
  }

  /**
   * Sets the delay time waiting for a restart
   */
  public void setActiveWaitTimeMillis(long wait)
  {
    _waitForActiveTimeout = wait;
  }
  
  public long getActiveWaitTime()
  {
    return _waitForActiveTimeout;
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

      DeployControllerService deployService = 
        DeployControllerService.getCurrent();
      
      _deployTagItem = deployService.addTag(getId());

      _deployTagItem.addActionHandler(this);

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
    DeployInstance instance = getDeployInstanceImpl();

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
    DeployInstance instance = getDeployInstanceImpl();

    if (instance == null) {
      return true;
    }
    
    if (DeployMode.MANUAL.equals(getRedeployMode())) {
      return false;
    }
    
    if (isControllerModified()) {
      return true;
    }
    
    return instance.isModified();
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    DeployInstance instance = getDeployInstanceImpl();
    
    if (instance == null) {
      return true;
    }

    if (isControllerModifiedNow()) {
      return true;
    }
    
    return instance.isModifiedNow();
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
    
    DeployInstance instance = getDeployInstanceImpl();

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
  public I getDeployInstance()
  {
    if (_lifecycle.isActive() || _lifecycle.isError())
      return getDeployInstanceImpl();
    else
      return null;
  }
  
  @Override
  public I getActiveDeployInstance()
  {
    _lifecycle.waitForActive(getActiveWaitTime());
    
    return getDeployInstanceImpl();
  }

  /**
   * Returns the current instance.
   */
  public I getDeployInstanceImpl()
  {
    return _deployInstanceRef.get();
  }

  /**
   * Returns the current instance.
   */
  protected final I createDeployInstance()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      return instantiateDeployInstance();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public void addLifecycleListener(LifecycleListener listener)
  {
    _lifecycle.addListener(listener);
  }

  /**
   * Starts the entry on initialization
   */
  @Override
  public void startOnInit()
  {
    if (! _lifecycle.isAfterInit())
      throw new IllegalStateException(L.l("startOnInit must be called after init (in '{0}')",
                                          _lifecycle.getStateName()));
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      _strategy.startOnInit(this);
    
      _alarm = new DeployControllerAlarm<DeployController<I>>(this, _redeployCheckInterval);
    } finally {
      thread.setContextClassLoader(loader);
    }
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
    if (_lifecycle.isAllowStopFromRestart())
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
      I instance = _deployInstanceRef.getAndSet(null);
      
      if (instance != null)
        instance.destroy();
      
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

      if (! isStarting
          || ! _deployInstanceRef.compareAndSet(null, deployInstance)) {
        try {
          deployInstance.destroy();
        } catch (Throwable e) {
          log.log(Level.FINEST, e.toString(), e);
        }
        
        isStarting = false;
        
        return getDeployInstance();
      }
      
      
      preConfigureInstance(deployInstance);
      
      configureInstance(deployInstance);
      
      postConfigureInstance(deployInstance);

      deployInstance.start();

      _deployTagItem.onStart();

      isActive = true;

      _startTime = CurrentTime.getCurrentTime();
    } catch (ConfigException e) {
      log.log(Level.FINEST, e.toString(), e);

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
      
      onError(e);

      if (deployInstance != null) {
        log.finer(e.toString());
        deployInstance.setConfigException(e);
      }
      else {
        log.log(Level.SEVERE, e.toString(), e);
      }
    } finally {
      if (isStarting) {
        if (isActive) {
          _lifecycle.toActive();

          onActive();
        }
        else {
          _lifecycle.toError();
        }

        onStartComplete();

        // server/
        if (loader instanceof DynamicClassLoader)
          ((DynamicClassLoader) loader).clearModified();
      }

      thread.setContextClassLoader(oldLoader);
    }

    return deployInstance;
  }

  /**
   * Stops the current instance, putting it in the lazy state.
   */
  protected void stopLazyImpl()
  {
    if (! _lifecycle.isIdle()) {
      stopImpl();
    }

    _lifecycle.toIdle();
  }

  /**
   * Stops the current instance.
   */
  protected void stopImpl()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    I oldInstance = _deployInstanceRef.get();
    boolean isStopping = false;

    try {
      if (oldInstance != null)
        thread.setContextClassLoader(oldInstance.getClassLoader());
      
      isStopping = _lifecycle.toStopping();

      _lifecycle.toStop();
      
      if (! isStopping)
        return;

      if (oldInstance != null
          && _deployInstanceRef.compareAndSet(oldInstance, null)) { 
        destroyInstance(oldInstance);
      }
    } finally  {
      if (isStopping) {
        onStop();
      }
      
      thread.setContextClassLoader(oldLoader);
    }

    return;
  }
  
  protected void destroyInstance(I instance)
  {
    if (instance != null)
      instance.destroy();
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
  
  protected void onStartComplete()
  {
  }
  
  protected void onStop()
  {
  }

  /**
   * Before instance configuration
   */
  protected void preConfigureInstance(I deployInstance)
    throws Exception
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
   * After instance configuration
   */
  protected void postConfigureInstance(I deployInstance)
    throws Exception
  {
    
  }


  //
  // DeployActionHandler
  //

  @Override
  public void toStart()
  {
    start();
  }

  @Override 
  public void toStop()
  {
    stop();
  }

  @Override 
  public void toRestart()
  {
    restart();
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
  
  public final void remove()
  {
    close();
    
    onRemove();
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

    DeployControllerAlarm<DeployController<I>> alarm = _alarm;
    _alarm = null;

    if (alarm != null) {
      alarm.close();
    }

    _deployTagItem.removeActionHandler(this);
    
    onDestroy();
      
    return true;
  }
  
  protected void onDestroy()
  {
  }
  
  protected void onRemove()
  {
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
