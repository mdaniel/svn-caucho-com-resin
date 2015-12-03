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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.make.CachedDependency;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.ConcurrentArrayList.Match;
import com.caucho.vfs.Dependency;

/**
 * A container of deploy objects.
 */
public class DeployContainer<C extends DeployControllerApi<?>>
  extends CachedDependency
  implements DeployContainerApi<C>, Dependency
{
  private final DeployListGenerator<C> _deployListGenerator
    = new DeployListGenerator<C>(this);

  private final ConcurrentArrayList<C> _controllerList;

  private final Lifecycle _lifecycle = new Lifecycle();
  
  private Class<?> _type;

  /**
   * Creates the deploy container.
   */
  public DeployContainer(Class<C> type)
  {
    _type = type;
    
    _controllerList = new ConcurrentArrayList<C>(type);
    setCheckInterval(Environment.getDependencyCheckInterval());
  }
  
  /**
   * Adds a deploy generator.
   */
  @Override
  public void add(DeployGenerator<C> generator)
  {
    Set<String> names = new TreeSet<String>();
    generator.fillDeployedNames(names);

    _deployListGenerator.add(generator);

    if (_lifecycle.isActive())
      update(names);
  }
  
  /**
   * Removes a deploy.
   */
  @Override
  public void remove(DeployGenerator<C> generator)
  {
    Set<String> names = new TreeSet<String>();
    generator.fillDeployedNames(names);

    _deployListGenerator.remove(generator);

    if (_lifecycle.isActive())
      update(names);
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
   * Forces updates.
   */
  @Override
  public void update()
  {
    _deployListGenerator.update();
  }

  /**
   * Initialize the container.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit())
      return;
  }

  /**
   * Start the container.
   */
  @Override
  public void start()
  {
    init();

    if (! _lifecycle.toActive()) {
      return;
    }

    _deployListGenerator.start();

    HashSet<String> keys = new LinkedHashSet<String>();

    _deployListGenerator.fillDeployedNames(keys);

    for (String key : keys) {
      updateImpl(key);
    }

    ArrayList<C> controllerList = new ArrayList<C>(_controllerList);

    Collections.sort(controllerList, new StartupPriorityComparator());

    for (int i = 0; i < controllerList.size(); i++) {
      C controller = controllerList.get(i);

      controller.startOnInit();
    }
  }

  /**
   * Returns the matching entry.
   */
  @Override
  public C findController(String name)
  {
    C controller = findDeployedController(name);

    if (controller != null) {
      return controller;
    }

    controller = generateController(name);

    if (controller == null)
      return null;
    // server/10tm
    else if (controller.isNameMatch(name)) {
      return controller;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the matching entry.
   */
  public C findControllerById(String name)
  {
    return findDeployedControllerById(name);
  }

  /**
   * Returns the deployed entries.
   */
  public C []getControllers()
  {
    return _controllerList.toArray();
  }

  /**
   * Updates all the names.
   */
  private void update(Set<String> names)
  {
    Iterator<String> iter = names.iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      update(name);
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
  @Override
  public C update(String name)
  {
    C newController = updateImpl(name);
    
    if (_lifecycle.isActive() && newController != null) {
      newController.startOnInit();
    }

    return newController;
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  public C updateNoStart(String name)
  {
    C newController = updateImpl(name);

    return newController;
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  C updateImpl(String name)
  {
    remove(name);

    // destroy must be before generate because of JMX unregistration
      
    C newController = generateController(name);

    return newController;
  }

  /**
   * Called to explicitly remove an entry from the cache.
   */
  @Override
  public void remove(String name)
  {
    C oldController = _controllerList.remove(name, getControllerNameMatch());

    if (oldController != null) {
      // oldController.close();
      oldController.remove();
    }
  }

  /**
   * Generates the controller.
   */
  private C generateController(String name)
  {
    // XXX: required for watchdog
    /*
    if (! _lifecycle.isActive())
      return null;
    */
    
    ArrayList<C> controllerList = new ArrayList<C>();
    
    _deployListGenerator.generateController(name, controllerList);
    
    C bestController = null;

    for (C controller : controllerList) {
      if (bestController == null)
        bestController = controller;
      else if (controller.getControllerType().ordinal()
               < bestController.getControllerType().ordinal()) {
        bestController = controller;
      }
    }
    
    if (bestController == null)
      return null;
    
    // server/1h8j
    for (C controller : controllerList) {
      if (controller != bestController) {
        bestController.merge((DeployControllerApi) controller);
      }
    }

    // server/1h10
    _deployListGenerator.mergeController(bestController, name);
    
    return addController(bestController);
  }

  private C addController(C newController)
  {
    // server/1h00,13g4
    // generated controller might match the name, e.g.
    // when webapps deploy has an overriding explicit <web-app>
    if (newController == null) {
      return null;
    }
    
    C oldController = null;
    
    // the new entry might already be generated by another thread
    synchronized (_controllerList) {
      oldController = findDeployedControllerById(newController.getId());
      
      if (oldController == null) {
        _controllerList.add(newController);
      }
    }
    
    if (oldController != null) {
      // if (controller.isVersioning())
      //   controller.updateVersion();
      oldController.update();

      return oldController;
    }
    else {
      init(newController);

      return newController;
    }
  }
  

  private void init(C controller)
  {
    controller.init();
  }

  /**
   * Returns an already deployed entry.
   */
  private C findDeployedController(String name)
  {
    return _controllerList.find(name, getControllerNameMatch());
  }

  /**
   * Returns an already deployed entry.
   */
  private C findDeployedControllerById(String id)
  {
    return _controllerList.find(id, getControllerIdMatch());
  }
  
  /**
   * Closes the stop.
   */
  @Override
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    ArrayList<C> controllers = new ArrayList<C>(_controllerList);

    Collections.sort(controllers, new StartupPriorityComparator());

    for (int i = controllers.size() - 1; i >= 0; i--)
      controllers.get(i).stop();
  }
  
  /**
   * Closes the deploys.
   */
  public void destroy()
  {
    stop();
    
    if (! _lifecycle.toDestroy())
      return;
    
    _deployListGenerator.destroy();

    ArrayList<C> controllerList = new ArrayList<C>(_controllerList);
    _controllerList.clear();
    Collections.sort(controllerList, new StartupPriorityComparator());

    for (int i = controllerList.size() - 1; i >= 0; i--) {
      C controller = controllerList.get(i);

      controller.close();
    }
  }
  
  @SuppressWarnings("unchecked")
  private Match<C,String> getControllerNameMatch()
  {
    return (Match<C,String>) ControllerNameMatch.MATCH;
  }
  
  @SuppressWarnings("unchecked")
  private Match<C,String> getControllerIdMatch()
  {
    return (Match<C,String>) ControllerIdMatch.MATCH;
  }

  @Override
  public String toString()
  {
    return "DeployContainer$" + System.identityHashCode(this) + "[" + _type.getSimpleName() + "]";
  }

  public class StartupPriorityComparator
    implements Comparator<C>
  {
    public int compare(C a, C b)
    {
      if (a.getStartupPriority() == b.getStartupPriority())
        return 0;
      else if (a.getStartupPriority() < b.getStartupPriority())
        return -1;
      else
        return 1;
    }
  }
  
  static class ControllerNameMatch<C extends DeployControllerApi<?>>
    implements Match<C,String>
  {
    static final ControllerNameMatch<DeployControllerApi<?>> MATCH
      = new ControllerNameMatch<DeployControllerApi<?>>();
    
    @Override
    public boolean isMatch(C controller, String name)
    {
      return controller.isNameMatch(name);
    }
  }
  
  static class ControllerIdMatch<C extends DeployControllerApi<?>>
    implements Match<C,String>
  {
    static final ControllerIdMatch<DeployControllerApi<?>> MATCH
      = new ControllerIdMatch<DeployControllerApi<?>>();
    
    @Override
    public boolean isMatch(C controller, String id)
    {
      return controller.getId().equals(id);
    }
  }
}
