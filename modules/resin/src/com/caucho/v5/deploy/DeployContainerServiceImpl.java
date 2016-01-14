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
import io.baratine.service.Service;
import io.baratine.service.ServiceRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.make.CachedDependency;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ConcurrentArrayList.Match;
import com.caucho.v5.vfs.PathImpl;

/**
 * A container of deploy objects.
 */
public class DeployContainerServiceImpl<I extends DeployInstance,
                             M extends DeployController<I>>
  extends CachedDependency
{
  private static final Logger log 
    = Logger.getLogger(DeployContainerServiceImpl.class.getName());
  private static final L10N L = new L10N(DeployContainerServiceImpl.class);
  
  private DeployGeneratorList<I,M> _deployListGenerator;

  private final ConcurrentArrayList<M> _controllerList;

  private final Lifecycle _lifecycle = new Lifecycle();
  
  private Class<?> _type;

  private DeployContainerService<I,M> _serviceSelf;

  private Set<String> _deployedKeys = new TreeSet<>();
  
  // private HashMap<String,DeployHandle<I>> _handleMap = new HashMap<>();

  /**
   * Creates the deploy container.
   */
  public DeployContainerServiceImpl(Class<M> type)
  {
    _type = type;
    
    _controllerList = new ConcurrentArrayList<M>(type);
    
    setCheckInterval(EnvLoader.getDependencyCheckInterval());
  }
  
  @OnInit
  public void onInit()
  {
    ServiceRef serviceSelfRef = ServiceRef.current();
    
    _serviceSelf = serviceSelfRef.as(DeployContainerService.class);
    
    _deployListGenerator = new DeployGeneratorList<I,M>(_serviceSelf);
  }
  
  //
  // service methods
  //
  
  /**
   * Adds a deploy generator.
   */
  public void add(DeployGenerator<I,M> generator)
  {
    Set<String> names = new TreeSet<>();
    generator.fillDeployedNames(names);

    _deployListGenerator.add(generator);

    if (_lifecycle.isActive()) {
      update(names);
    }
  }
  
  /**
   * Removes a deploy.
   */
  public void remove(DeployGenerator<I,M> generator)
  {
    Set<String> names = new TreeSet<String>();
    generator.fillDeployedNames(names);

    _deployListGenerator.remove(generator);

    if (_lifecycle.isActive()) {
      update(names);
    }
  }
  
  /**
   * XXX: direct and isModified() isn't quite right with service.
   * _deployListGenerator should probably be refactored.
   */
  @Direct
  public boolean isModified()
  {
    return super.isModified();
  }

  /**
   * Returns true if the deployment has modified.
   */
  @Override
  public boolean isModifiedImpl()
  {
    return _deployListGenerator.isModified();
  }

  /**
   * Logs the reason for modification.
   */
  @Override
  public boolean logModified(Logger log)
  {
    return _deployListGenerator.logModified(log);
  }

  /**
   * Updates the controllers
   */
  public final void update()
  {
    // beforeUpdate();

    Set<String> oldKeys = _deployedKeys ;
    
    TreeSet<String> newKeys = new TreeSet<>();

    _deployListGenerator.fillDeployedNames(newKeys);

    // log.warning("DeployContainerServiceImpl new keys " + newKeys);
      
    if (oldKeys.equals(newKeys)) {
      // log.warning("DeployContainerServiceImpl update: newKeys equals oldKeys");

      return;
    }

    _deployedKeys = newKeys;
      
    for (String key : oldKeys) {
      if (! newKeys.contains(key)) {
        // remove
        M controller = findDeployedController(key);

        if (controller != null) {
          removeController(controller.getId());
          
          //controller.stop();
          DeployHandle<I> handle = getHandle(controller.getId());
          
          if (handle != null) {
            handle.stop(ShutdownModeAmp.GRACEFUL);
          }
        }
        else {
          log.warning("Unknown controller for remove: " + key + " " + _controllerList);
        }
      }
    }

    for (String key : newKeys) {
      if (! oldKeys.contains(key)) {
        M controller = generateController(key);
      
        if (_lifecycle.isActive()) {
          //controller.startOnInit();
          DeployHandle<I> handle = getHandle(controller.getId());
          
          if (handle != null) {
            handle.startOnInit();
          }
        }
      }
    }
  }
  
  public void addWatch(PathImpl path, @Service Watch watch, Result<Cancel> result)
  {
    result.ok(path.watch(watch));
  }

  /**
   * Initialize the container.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit()) {
      return;
    }
  }

  /**
   * Start the container.
   */
  public void start(Result<Boolean> result)
  {
    init();

    if (! _lifecycle.toStarting()) {
      result.ok(false);
      
      return;
    }
    
    try {
      _deployListGenerator.start();

      update();
    } finally {
      _lifecycle.toActive();
    }

    ArrayList<M> controllerList = new ArrayList<M>(_controllerList);

    Collections.sort(controllerList, new StartupPriorityComparator());

    initControllers(controllerList, result);
  }
  
  private void initControllers(ArrayList<M> controllerList,
                               Result<Boolean> result)
  {
    if (controllerList.size() == 0) {
      result.ok(true);
      return;
    }
    
    M controller = controllerList.remove(0);
    DeployHandle<I> handle = getHandle(controller.getId());

    handle.getService().startOnInit(result.of((x,r)->initControllers(controllerList, r)));
  }

  /**
   * Returns the matching entry.
   */
  public DeployHandle<I> findHandle(String name)
  {
    DeployHandle<I> handle = getHandle(name);
    
    if (handle != null) {
      return handle;
    }

    //M controller = generateController(name);
    M controller = findController(name);
    
    if (controller == null) {
      return null;
    }
    else if (! controller.isNameMatch(name)) {
      // server/10tm
      return null;
    }
    
    handle = getHandle(controller.getId());
    
    return handle;
  }
  
  public DeployHandle<I> createHandle(String id)
  {
    DeploySystem deploySystem = DeploySystem.getCurrent();
    
    return deploySystem.createHandle(id, log);
  }
  
  public DeployHandle<I> getHandle(String id)
  {
    DeploySystem deploySystem = DeploySystem.getCurrent();
    
    return deploySystem.getHandle(id);
  }

  /**
   * Returns the matching entry.
   */
  public M findController(String name)
  {
    M controller = findDeployedController(name);
    if (controller != null) {
      return controller;
    }
    
    controller = generateController(name);
    if (controller == null) {
      return null;
    }
    else if (controller.isNameMatch(name)) {
      // server/10tm
      return controller;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the matching entry.
   */
  @Direct
  public M findControllerById(String name)
  {
    return findDeployedControllerById(name);
  }

  /**
   * Returns the deployed entries.
   */
  @Direct
  public M []getControllers()
  {
    return _controllerList.toArray();
  }

  /**
   * Returns the deployed entries.
   */
  @Direct
  public DeployHandle<I> []getHandles()
  {
    //ArrayList<DeployHandle<I>> handleList = new ArrayList<>(_handleMap.values());
    
    //return handleList.toArray(new DeployHandle[handleList.size()]);
    return new DeployHandle[0];
  }

  /**
   * Updates all the names.
   */
  private void update(Set<String> names)
  {
    for (String name : names) {
      DeployHandle<I> handle = findHandle(name);
      
      if (handle != null) {
        handle.update();
      }
      /*
      M controller = findController(name);
      
      if (controller != null) {
        controller.update();
      }
      */
    }
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */ 
  public M updateAndGet(String name)
  {
    update();

    /*
    if (_lifecycle.isActive() && newController != null) {
      newController.startOnInit();
    }
    */
    
    return findController(name);

    /*
    if (_lifecycle.isActive() && newController != null) {
      newController.startOnInit();
    }

    return newController;
    */
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  public M updateNoStart(String name)
  {
    update();
    
    // M newController = updateImpl(name);

    return findController(name);
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  /*
  M updateImpl(String name)
  {
    remove(name);

    // destroy must be before generate because of JMX unregistration
      
    M newController = generateController(name);

    return newController;
  }
  */

  /**
   * Generates the controller.
   */
  private M generateController(String name)
  {
    // XXX: required for watchdog
    /*
    if (! _lifecycle.isActive())
      return null;
    */

    // log.warning(L.l("generate controller {0}", name));
    
    ArrayList<M> controllerList = new ArrayList<M>();
    
    _deployListGenerator.generateController(name, controllerList);

    M bestController = null;

    for (M controller : controllerList) {
      if (bestController == null) {
        bestController = controller;
      }
      else if (controller.getControllerType().ordinal()
               < bestController.getControllerType().ordinal()) {
        bestController = controller;
      }
    }
    
    if (bestController == null) {
      return null;
    }
    
    // server/1h8j
    for (M controller : controllerList) {
      if (controller != bestController) {
        bestController.merge((DeployController) controller);
      }
    }

    // server/1h10
    _deployListGenerator.mergeController(bestController, name);
    
    return addController(bestController);
  }

  private M addController(M newController)
  {
    // server/1h00,13g4
    // generated controller might match the name, e.g.
    // when webapps deploy has an overriding explicit <web-app>
    if (newController == null) {
      return null;
    }
    
    DeployHandle<I> handle = createHandle(newController.getId());
    
    handle.getService().setController(newController);
    
    M oldController = null;
    
    // the new entry might already be generated by another thread
    oldController = findDeployedControllerById(newController.getId());
      
    if (oldController != null) {
      _controllerList.remove(oldController);
    }
    
    _controllerList.add(newController);
    
    if (oldController != null) {
      // if (controller.isVersioning())
      //   controller.updateVersion();
      // oldController.update();
      
      DeployHandle<I> oldHandle = getHandle(oldController.getId());
      
      oldHandle.update();

      return oldController;
    }
    else {
      init(newController);

      return newController;
    }
  }

  private M removeController(String id)
  {
    M oldController = null;
    
    // the new entry might already be generated by another thread
    oldController = findDeployedControllerById(id);
      
    if (oldController != null) {
      _controllerList.remove(oldController);
    }
    
    DeployHandle<I> oldHandle = findHandle(id);
    
    if (oldController != null) {
      oldHandle.stop(ShutdownModeAmp.GRACEFUL);
    }
    
    return oldController;
  }
  

  private void init(M controller)
  {
    Objects.requireNonNull(controller);
    
    DeployHandle<I> handle = createHandle(controller.getId());
    
    controller.init(handle.getService());
  }

  /**
   * Returns an already deployed entry.
   */
  @Direct
  public M findDeployedController(String name)
  {
    M controller = _controllerList.find(name, getControllerNameMatch());
    
    return controller;
  }

  /**
   * Returns an already deployed entry.
   */
  @Direct
  public M findDeployedControllerById(String id)
  {
    M controller = _controllerList.find(id, getControllerIdMatch());
    
    return controller;
  }
  
  /**
   * Closes the stop.
   */
  public void stop(ShutdownModeAmp mode)
  {
    if (! _lifecycle.toStop()) {
      return;
    }

    ArrayList<M> controllers = new ArrayList<M>(_controllerList);

    Collections.sort(controllers, new StartupPriorityComparator());

    for (int i = controllers.size() - 1; i >= 0; i--) {
      DeployHandle<I> handle = getHandle(controllers.get(i).getId());
      
      handle.stopAndWait(mode);
    }
  }
  
  /**
   * Closes the deploys.
   */
  public void destroy(ShutdownModeAmp mode)
  {
    stop(mode);
    
    if (! _lifecycle.toDestroy())
      return;
    
    _deployListGenerator.destroy();

    ArrayList<M> controllerList = new ArrayList<M>(_controllerList);
    _controllerList.clear();
    Collections.sort(controllerList, new StartupPriorityComparator());

    for (int i = controllerList.size() - 1; i >= 0; i--) {
      M controller = controllerList.get(i);
      
      DeployHandle<I> handle = getHandle(controller.getId());

      handle.stop(mode);
      //controller.stop();
    }
  }
  
  @SuppressWarnings("unchecked")
  private Match<M,String> getControllerNameMatch()
  {
    return (Match<M,String>) ControllerNameMatch.MATCH;
  }
  
  @SuppressWarnings("unchecked")
  private Match<M,String> getControllerIdMatch()
  {
    return (Match<M,String>) ControllerIdMatch.MATCH;
  }

  @Override
  public String toString()
  {
    return "DeployContainer$" + System.identityHashCode(this) + "[" + _type.getSimpleName() + "]";
  }

  public class StartupPriorityComparator
    implements Comparator<M>
  {
    @Override
    public int compare(M a, M b)
    {
      if (a.getStartupPriority() == b.getStartupPriority()) {
        return a.toString().compareTo(b.toString());
      }
      else if (a.getStartupPriority() < b.getStartupPriority()) {
        return -1;
      }
      else {
        return 1;
      }
    }
  }
  
  static class ControllerNameMatch<C extends DeployController<?>>
    implements Match<C,String>
  {
    static final ControllerNameMatch<DeployController<?>> MATCH
      = new ControllerNameMatch<DeployController<?>>();
    
    @Override
    public boolean isMatch(C controller, String name)
    {
      return controller.isNameMatch(name);
    }
  }
  
  static class ControllerIdMatch<C extends DeployController<?>>
    implements Match<C,String>
  {
    static final ControllerIdMatch<DeployController<?>> MATCH
      = new ControllerIdMatch<DeployController<?>>();
    
    @Override
    public boolean isMatch(C controller, String id)
    {
      return controller.getId().equals(id);
    }
  }
}
