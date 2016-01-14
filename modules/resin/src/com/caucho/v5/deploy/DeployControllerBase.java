/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

import io.baratine.service.Result;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.deploy2.DeployMode;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.util.L10N;

/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
abstract public class DeployControllerBase<I extends DeployInstance>
  implements DeployController<I>, Dependency
{
  private static final Logger log
    = Logger.getLogger(DeployControllerBase.class.getName());
  private static final L10N L = new L10N(DeployControllerBase.class);
  
  public static final long REDEPLOY_CHECK_INTERVAL = 60000;

  private ClassLoader _parentLoader;
  
  private final String _id;
  private final String _idType;
  private final String _idKey;

  private DeployMode _startupMode = DeployMode.DEFAULT;
  private DeployMode _redeployMode = DeployMode.DEFAULT;

  private int _startupPriority = Integer.MAX_VALUE;
  
  private DeployControllerType _controllerType = DeployControllerType.STATIC;
  
  //private AlarmDeployController<DeployControllerBase<I>> _alarm;

  //private DeployTagItem _deployTagItem;

  private final Lifecycle _lifecycle;
  
  private long _waitForActiveTimeout = 10000L;
  private long _redeployCheckInterval = REDEPLOY_CHECK_INTERVAL;
  
  private long _startTime;
  /*
  private final AtomicReference<I> _deployInstanceRef
    = new AtomicReference<I>();
    */
  //private DeployControllerService<I> _service;
  
  // private DeployHandle<I> _handle;

  protected DeployControllerBase(String id)
  {
    this(id, null);
  }

  protected DeployControllerBase(String id, ClassLoader parentLoader)
  {
    Objects.requireNonNull(id);
    
    _id = id;
    //_handle = handle;
    
    if (parentLoader == null) {
      parentLoader = Thread.currentThread().getContextClassLoader();
    }
    
    _parentLoader = parentLoader;

    _lifecycle = new Lifecycle(getLog(), toString(), Level.FINEST);

    int p1 = _id.indexOf('/');
    _idType = _id.substring(0, p1);
    
    _idKey = _id.substring(p1 + 1);
  }

  /**
   * Returns the controller's id.
   */
  @Override
  public final String getId()
  {
    return _id;
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
  
  protected String getLifecycleState()
  {
    if (_lifecycle != null) {
      return _lifecycle.getStateName();
    }
    else {
      return null;
    }
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
  public void merge(DeployController<I> newController)
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
  public final boolean init(DeployControllerService<I> service)
  {
    if (! _lifecycle.toInitializing()) {
      return false;
    }
    
    boolean isInit = false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      initBegin();

      DeployControllerStrategy strategy;
      
      switch (_startupMode) {
      case MANUAL: {
        if (_redeployMode == DeployMode.AUTOMATIC) {
          strategy = StartManualRedeployAutoStrategy.create();
          
          //throw new IllegalStateException(L.l("startup='manual' and redeploy='automatic' is an unsupported combination."));
        }
        else
          strategy = StartManualRedeployManualStrategy.create();
        break;
      }

      case LAZY: {
        if (_redeployMode == DeployMode.MANUAL)
          strategy = StartLazyRedeployManualStrategy.create();
        else
          strategy = StartLazyRedeployAutomaticStrategy.create();
        break;
      }
      
      default: {
        if (_redeployMode == DeployMode.MANUAL)
          strategy = StartAutoRedeployManualStrategy.create();
        else
          strategy = StartAutoRedeployAutoStrategy.create();
      }
      }
      
      if (service != null) {
        service.setStrategy(strategy);
      }

      initEnd();
      
      isInit = _lifecycle.toInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
    
    initPost();
    
    return isInit;
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
   * calls after init completed
   */
  protected void initPost()
  {
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
    /*
    DeployInstance instance = getDeployInstance();

    if (instance == null) {
      return true;
    }
    */
    
    if (DeployMode.MANUAL.equals(getRedeployMode())) {
      return false;
    }
    
    if (isControllerModified()) {
      return true;
    }
    
    //return instance.isModified();
    return false;
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    /*
    DeployInstance instance = getDeployInstance();
    
    if (instance == null) {
      return true;
    }
    */
    
    if (isControllerModifiedNow()) {
      return true;
    }
    
    //return instance.isModifiedNow();
    return false;
  }
  
  /**
   * Log the reason for modification
   */
  @Override
  final public boolean logModified(Logger log)
  {
    //return getService().logModified(log);
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

  protected DeployInstanceBuilder<I> createInstanceBuilder()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Before instance configuration
   */
  protected void preConfigureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
    
  }

  /**
   * Configuration of the instance
   */
  protected void configureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
  }

  /**
   * After instance configuration
   */
  protected void postConfigureInstance(DeployInstanceBuilder<I> deployInstance)
    throws Exception
  {
    
  }


  @Override
  public void addLifecycleListener(LifecycleListener listener)
  {
    _lifecycle.addListener(listener);
  }

  protected void onStartComplete()
  {
    
  }
  
  @Override
  public final void close()
  {
    destroy();
  }
  
  @Override
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
    /*
    if (_lifecycle.isAfterInit())
      stop();
      */
    
    if (! _lifecycle.toDestroy())
      return false;

    /*
    AlarmDeployController<DeployControllerBase<I>> alarm = _alarm;
    _alarm = null;

    if (alarm != null) {
      alarm.close();
    }
    */

    // _deployTagItem.removeActionHandler(this);
    
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
    return getClass().getSimpleName() + "[" + getId() + "," + getLifecycleState() + "]";
  }

}
