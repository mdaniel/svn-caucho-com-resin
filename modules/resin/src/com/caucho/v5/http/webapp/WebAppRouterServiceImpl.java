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

package com.caucho.v5.http.webapp;

import io.baratine.core.Cancel;
import io.baratine.core.Direct;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.files.Watch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.ServiceRefAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.deploy.DeployController;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeployInstance;
import com.caucho.v5.deploy.DeploySystem;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.util.ConcurrentArrayList.Match;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * A container of webapp deploy objects.
 */
public class WebAppRouterServiceImpl implements WebAppRouterService
{
  private static final Logger log 
    = Logger.getLogger(WebAppRouterServiceImpl.class.getName());
  private static final L10N L = new L10N(WebAppRouterServiceImpl.class);
  
  private final WebAppRouter _router;
  private final String _idPrefix;

  private final List<DeployHandle<WebApp>> _webAppList = new ArrayList<>();
  
  private HashMap<String,DeployHandle<WebApp>> _webAppMap = new HashMap<>();
  
  private Cancel _onStartCancel;

  private final Lifecycle _lifecycle = new Lifecycle();
  private DeploySystem _deploySystem;

  /**
   * Creates the deploy container.
   */
  public WebAppRouterServiceImpl(WebAppRouter router)
  {
    Objects.requireNonNull(router);
    
    _router = router;
    
    Host host = router.getHost();
    String hostIdTail = host.getIdTail();
    
    _idPrefix = "webapps/" + hostIdTail;
  }
  
  @OnInit
  public void onInit()
  {
    ServiceManagerAmp ampManager = ServiceManagerAmp.current();
    
    ServiceRefAmp onStartRef = ampManager.lookup("event:///" + OnWebAppStart.class.getName());
    
    _onStartCancel = onStartRef.subscribe((OnWebAppStart) id->onWebAppStart(id));
    
    _deploySystem = DeploySystem.getCurrent();
    Objects.requireNonNull(_deploySystem);
  }
  
  private void onWebAppStart(String id)
  {
    String contextPath = getContextPath(id);
    
    if (contextPath == null) {
      return;
    }
    
    if (_webAppMap.containsKey(contextPath)) {
      return;
    }
    
    DeployHandle<WebApp> webAppHandle = _deploySystem.getHandle(id);
    
    if (webAppHandle == null) {
      return;
    }
    
    _webAppList.add(webAppHandle);
    
    buildWebAppMap();
  }
  
  private void buildWebAppMap()
  {
    HashMap<String,DeployHandle<WebApp>> webAppMap = new HashMap<>();
    
    for (DeployHandle<WebApp> handle : _webAppList) {
      String id = handle.getId();
      
      String contextPath = getContextPath(id);
      
      webAppMap.put(contextPath, handle);
    }
    
    _webAppMap = webAppMap;
    
    _router.setWebAppMap(webAppMap);
  }

  private String getContextPath(String id)
  {
    if (! id.startsWith(_idPrefix)) {
      return null;
    }
    
    String contextPath = id.substring(_idPrefix.length());
    
    if (contextPath.length() == 1) {
      contextPath = "";
    }
    
    if (contextPath.equalsIgnoreCase("/ROOT")) {
      contextPath = "";
    }
    
    return contextPath;
  }

  /**
   * Updates the controllers
   */
  @Override
  public final void update(Result<Void> result)
  {

  }

  /**
   * Start the container.
   */
  @Override
  public void start(Result<Boolean> result)
  {
    if (! _lifecycle.toStarting()) {
      result.ok(false);
      
      return;
    }
    
    /*
    try {
      _deployListGenerator.start();

      update();
    } finally {
      _lifecycle.toActive();
    }

    ArrayList<M> controllerList = new ArrayList<M>(_controllerList);

    Collections.sort(controllerList, new StartupPriorityComparator());

    initControllers(controllerList, result);
    */
  }
  
  /*
  private void initControllers(ArrayList<M> controllerList,
                               Result<Boolean> result)
  {
    if (controllerList.size() == 0) {
      result.complete(true);
      return;
    }
    
    M controller = controllerList.remove(0);
    DeployHandle<I> handle = getHandle(controller.getId());

    handle.getService().startOnInit(result.from((x,r)->initControllers(controllerList, r)));
  }
  */

  /**
   * Returns the deployed entries.
   */
  @Direct
  public DeployHandle<WebApp> []getHandles()
  {
    //ArrayList<DeployHandle<I>> handleList = new ArrayList<>(_handleMap.values());
    
    //return handleList.toArray(new DeployHandle[handleList.size()]);
    return new DeployHandle[0];
  }
  
  /**
   * Closes the deploys.
   */
  @Override
  public void shutdown(ShutdownModeAmp mode)
  {
    /*
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
    */
    
    _onStartCancel.cancel();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _idPrefix + "]";
  }
}
