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

import io.baratine.files.Watch;
import io.baratine.service.Cancel;
import io.baratine.service.Direct;
import io.baratine.service.OnInit;
import io.baratine.service.Result;
import io.baratine.service.ServiceRef;
import io.baratine.timer.TimerService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy2.DeployMode;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;


/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
public class DeployControllerServiceImpl<I extends DeployInstance>
{
  private static final L10N L = new L10N(DeployControllerServiceImpl.class);
  private static final Logger log
    = Logger.getLogger(DeployControllerServiceImpl.class.getName());
  
  private final Lifecycle _lifecycle;
  
  private final String _id;
  
  private DeployControllerBase<I> _controller;
  
  private DeployControllerStrategy _strategy
    = StartManualRedeployManualStrategy.STRATEGY;
  
  private HashMap<PathImpl,Cancel> _watchMap = new HashMap<>();
  
  private I _instance;
  
  private long _startTime;
  private DeployControllerService<I> _serviceSelf;
  private Logger _log;

  DeployControllerServiceImpl(String id, Logger log)
  {
    _id = id;
    
    _log = log;
    
    _lifecycle = new Lifecycle(log, id, Level.FINEST);
  }
  
  /*
  public static <J extends DeployInstance> 
  DeployControllerService<J> create(DeployControllerBase<J> controller)
  {
    DeployControllerService<J> service
      = create(controller.toString(), controller.getLog());
    
    controller.setService(service);
    
    return service;
  }
  */

  /*
  public static <J extends DeployInstance>
  DeployControllerService<J> create(String id, Logger log)
  {
    ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
    Objects.requireNonNull(ampManager);
    
    DeployControllerServiceImpl<J> serviceImpl
      = new DeployControllerServiceImpl<>(id, log);
    
    DeployControllerService<J> service
      = ampManager.service(serviceImpl).as(DeployControllerService.class);
    
    return service;
  }
    */
  
  Logger getLog()
  {
    return _log;
  }

  public void setStrategy(DeployControllerStrategy strategy)
  {
    Objects.requireNonNull(strategy);
    
    _strategy = strategy;
  }
  
  @Direct
  public DeployControllerBase<I> getController()
  {
    return _controller;
  }
  
  public Throwable getConfigException()
  {
    DeployControllerBase<I> controller = getController();
    
    /*
    if (controller != null) {
      return controller.getE.getConfigException();
    }
    else {
      return null;
    }
    */
    return null;
  }

  public void setController(DeployControllerBase<I> controller)
  {
    DeployControllerBase<I> oldController = _controller;
    
    if (controller == oldController) {
      return;
    }
    
    if (oldController != null) {
      stop(ShutdownModeAmp.GRACEFUL);
    }
    
    _instance = null;
    _controller = controller;
    
    controller.init(_serviceSelf);
    
    clearWatches();
    
    if (_lifecycle.isActive()) {
      startImpl();
    }
    /*
    startOnInit();
    start();
    */
  }
  
  private void clearWatches()
  {
    for (Map.Entry<PathImpl,Cancel> entry : _watchMap.entrySet()) {
      //entry.getKey().removeWatch(entry.getValue());
      entry.getValue().cancel();
    }
    
    _watchMap.clear();
  }
  
  @OnInit
  public void onStartup()
  {
    ServiceRef serviceRef = ServiceRef.current();
    
    _serviceSelf = serviceRef.as(DeployControllerService.class);
  }
  
  @Direct
  public I getDeployInstance()
  {
    
    /*
    if (_lifecycle.isActive() || _lifecycle.isError())
      return getDeployInstanceImpl();
    else
      return null;
      */
    
    
    return _instance;
  }
  
  public void getActiveDeployInstance(Result<I> result)
  {
    result.ok(_instance);
    /*
    _lifecycle.waitForActive(getActiveWaitTime());
    
    return getDeployInstanceImpl();
    */
  }

  public LifecycleState getState()
  {
    return _lifecycle.getState();
  }
  
  //
  // dependency/modified
  //
  
  /**
   * Returns true if the entry is modified.
   */
  public boolean isModified()
  {
    DeployInstance instance = _instance;

    if (instance == null) {
      return true;
    }
    
    if (DeployMode.MANUAL.equals(_controller.getRedeployMode())) {
      return false;
    }
    
        /* ioc/0pk0
    if (_lifecycle.isError()) {
      return true;
    }
    */
    
    if (_controller.isControllerModified()) {
      return true;
    }
    
    return instance.isModified();
  }

  /**
   * Returns true if the entry is modified.
   */
  public boolean isModifiedNow()
  {
    DeployInstance instance = getDeployInstance();
    
    if (instance == null) {
      return true;
    }
    
    if (_controller.isControllerModifiedNow()) {
      return true;
    }
    
    return instance.isModifiedNow();
  }
  
  /**
   * Log the reason for modification
   */
  @Direct
  final public boolean logModified(Logger log)
  {
    if (_controller.controllerLogModified(log)) {
      return true;
    }
    
    DeployInstance instance = _instance;

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
   * Starts the entry on initialization
   */
  public boolean startOnInit()
  {
    
    DeployControllerBase<I> controller = _controller;
    
    if (controller == null) {
      return false;
    }
    
    if (! _lifecycle.toInit()) {
      return false;
    }
    
    /*
    if (! _lifecycle.isAfterInit())
      throw new IllegalStateException(L.l("startOnInit must be called after init (in '{0}')",
                                          _lifecycle.getStateName()));
                                          */
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(controller.getParentClassLoader());
      
      _strategy.startOnInit(this);

      TimerService timerService = AmpSystem.getCurrent().getTimerService();
      
      TimerController timer = new TimerController(timerService);

      timerService.runAfter(timer,
                            _controller.getRedeployCheckInterval(),
                            TimeUnit.MILLISECONDS,
                            Result.ignore());
    } finally {
      thread.setContextClassLoader(loader);
    }
    
    return true;
  }

  /**
   * Force an instance start from an admin command.
   */
  public final boolean start()
  {
    _strategy.start(this);
    
    return true;
  }

  /**
   * Stops the controller from an admin command.
   */
  public final boolean stop(ShutdownModeAmp mode)
  {
    _strategy.stop(this, mode);
    
    return true;
  }
  
  public final boolean destroy()
  {
    stop(ShutdownModeAmp.GRACEFUL);
    
    _lifecycle.toDestroy();
    
    return true;
  }

  /**
   * Force an instance restart from an admin command.
   */
  public final void restart()
  {
    _strategy.stop(this, ShutdownModeAmp.GRACEFUL);
    _strategy.start(this);
  }

  /**
   * Update the controller from an admin command.
   */
  public final boolean update()
  {
    _strategy.update(this);
    
    return true;
  }

  /**
   * Update the controller from an admin command.
   */
  public final void alarm()
  {
    _strategy.alarm(this);
  }
  
  public void addWatch(PathImpl path, Watch watch)
  {
    if (_watchMap.get(path) == null) {
      Cancel watchHandle = path.watch(watch);
      
      _watchMap.put(path, watchHandle);
    
    }
  }
  
  public void removeWatch(PathImpl path)
  {
    Cancel watchHandle = _watchMap.remove(path);
    
    if (watchHandle != null) {
      watchHandle.cancel();
    }
  }
  
  /*
  public void updateConfig(Object value)
  {
    DeployControllerBase<I> controller = _controller;
    
    if (controller != null) {
      controller.updateConfigImpl(value);
    }
   
    alarm();
    
    start(); // XXX: start seems incorrect
  }
  */

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  @Direct
  public final void request(Result<I> result)
  {
    I instance = _instance;
    
    if (instance != null && _lifecycle.isActive() && ! isModified()) {
      result.ok(instance);
    }
    else {
      _serviceSelf.requestSafe(result);
    }
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  public final I requestSafe()
  {
    if (_lifecycle.isDestroyed()) {
      return null;
    }
    
    I instance = _instance;

    if (instance != null && ! isModified()) {
      return instance;
    }
    
    instance = _strategy.request(this);

    return instance;
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  @Direct
  public final void subrequest(Result<I> result)
  {
    I instance = _instance;
    
    if (instance != null) {
      result.ok(instance);
    }
    else {
      _serviceSelf.subrequestSafe(result);
    }
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  public final I subrequestSafe()
  { 
    if (_lifecycle.isDestroyed()) {
      return null;
    }
    else {
      I instance = _strategy.subrequest(this);
      
      return instance;
    }
  }
  
  //
  // strategy implementation
  //

  /**
   * Starts the entry.
   */
  public I startImpl()
  {
    DeployControllerBase<I> controller = _controller;
    
    if (controller == null) {
      return null;
    }
    
    // Thread.dumpStack();
    
    /*
    if (! _lifecycle.isAfterInit()) {
      throw new IllegalStateException(L.l("{0} has not yet been initialized", this));
    }
    */
    
    if (DynamicClassLoader.isModified(controller.getParentClassLoader())) {
      I instance = _instance;
      _instance = null;
      
      if (instance != null) {
        instance.shutdown(ShutdownModeAmp.GRACEFUL);
      }
      
      return null;
    }
    
    if (! _lifecycle.toStarting()) {
      return _instance;
    }
    

    I deployInstance = null;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ClassLoader loader = null;
    boolean isActive = false;
    
    /*
    OutboxAmp outbox = OutboxAmp.current();
    OutboxContext<MessageAmp> inbox = null;
    
    if (outbox != null) {
      inbox = outbox.getAndSetContext(null);
    }
    */

    try {
      thread.setContextClassLoader(controller.getParentClassLoader());
      
      deployInstance = createDeployInstance();

      Objects.requireNonNull(deployInstance, getClass().getName()); 
      
      loader = deployInstance.getClassLoader();
      thread.setContextClassLoader(loader);

      if (_instance != null && _instance != deployInstance) {
        throw new IllegalStateException(toString());
      }
      
      _instance = deployInstance;
      

      deployInstance.start();

      controller.onStartComplete();
      // _deployTagItem.onStart();
      // XXX: need error?
      isActive = true;

      _startTime = CurrentTime.getCurrentTime();
    } catch (ConfigException e) {
      log.log(Level.FINEST, e.toString(), e);

      onError(deployInstance, e);

/*
      if (deployInstance != null) {
        log.finer(e.toString());
        deployInstance.setConfigException(e);
      }
      else {
        log.severe(e.toString());
      }
*/
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
      
      onError(deployInstance, e);

/*
      if (deployInstance != null) {
        log.finer(e.toString());
        deployInstance.setConfigException(e);
      }
      else {
        log.log(Level.SEVERE, e.toString(), e);
      }
*/
    } finally {
      if (isActive) {
        _lifecycle.toActive();

        onActive();
      }
      else {
        _lifecycle.toError();
      }

      onStartComplete();
      
      /*
      if (outbox != null) {
        outbox.getAndSetContext(inbox);
      }
      */

      // server/
      if (loader instanceof DynamicClassLoader)
        ((DynamicClassLoader) loader).clearModified();

      thread.setContextClassLoader(oldLoader);
    }

    return deployInstance;
  }

  /**
   * Returns the current instance.
   */
  protected final I createDeployInstance()
  {
    DeployControllerBase<I> controller = _controller;
    
    if (controller == null) {
      return null;
    }
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(controller.getParentClassLoader());
      
      DeployInstanceBuilder<I> builder = controller.createInstanceBuilder();
      
      thread.setContextClassLoader(builder.getClassLoader());
      
      try {
        // I instance = builder.getInstance();
        
        controller.preConfigureInstance(builder);
      
        controller.configureInstance(builder);
      
        controller.postConfigureInstance(builder);
      } catch (Exception e) {
        builder.setConfigException(e);
        
        onError(builder.getInstance(), e);
      }

      return builder.build();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Restarts the instance
   *
   * @return the new instance
   */
  final I restartImpl()
  {
    if (_lifecycle.isError() && _startTime == CurrentTime.getCurrentTime()) {
      return _instance;
    }

    if (_lifecycle.isAllowStopFromRestart()) {
      stopImpl(ShutdownModeAmp.GRACEFUL);
    }
    
    return startImpl();
  }

  /**
   * Stops the current instance, putting it in the lazy state.
   */
  protected void stopLazyImpl()
  {
    if (! _lifecycle.isIdle()) {
      stopImpl(ShutdownModeAmp.GRACEFUL);
    }

    _lifecycle.toIdle();
  }

  /**
   * Stops the current instance.
   */
  protected void stopImpl(ShutdownModeAmp mode)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    I oldInstance = _instance;
    boolean isStopping = false;

    try {
      if (oldInstance != null) {
        thread.setContextClassLoader(oldInstance.getClassLoader());
      }
      
      isStopping = _lifecycle.toStopping();

      _lifecycle.toStop();

      if (! isStopping) {
        return;
      }

      if (oldInstance != null) {
        _instance = null;

        //_controller.destroyInstance(oldInstance);
        oldInstance.shutdown(mode);
      }
    } finally  {
      if (isStopping) {
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
    // _controller.onActive();
  }

  /**
   * @param deployInstance - WebApp or Host.
   * @param e
   */
  protected void onError(I deployInstance, Throwable e)
  {
    _lifecycle.toError();
    //_controller.onError(e);

    // server/10ki    

//    I deployInstance = _instance;

    if (deployInstance != null) {
      log.finer(e.toString());
      deployInstance.setConfigException(e);
    }
    else {
      log.log(Level.SEVERE, e.toString(), e);
    }

  }
  
  protected void onStartComplete()
  {
    /*
    DeployControllerBase<I> controller = _controller;
    
    if (controller != null) {
      controller.onStartComplete();
    }
    */
  }
  
  protected void onStop()
  {
    /*
    DeployControllerBase<I> controller = _controller;
    
    if (controller != null) {
      controller.onStop();
    }
    */
  }

  public boolean isIdleTimeout()
  {
    I instance = _instance;
    
    if (instance != null) {
      return instance.isDeployIdle();
    }
    else {
      return false;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
  
  private class TimerController implements Consumer<Cancel> {
    private TimerService _timerService;
    
    TimerController(TimerService timerService)
    {
      _timerService = timerService;
    }
    
    @Override
    public void accept(Cancel handle)
    {
      try {
        alarm();
      } finally {
        if (! _lifecycle.isDestroyed()) {
          _timerService.runAfter(this, 
                                 _controller.getRedeployCheckInterval(),
                                 TimeUnit.MILLISECONDS,
                                 Result.ignore());
        }
      }
    }
  }
}
