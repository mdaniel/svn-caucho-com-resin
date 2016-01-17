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

package com.caucho.v5.http.pod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.ServerOnUpdate;
import com.caucho.v5.bartender.pod.NodePodAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.bartender.pod.PodBartender.PodType;
import com.caucho.v5.bartender.pod.PodConfig;
import com.caucho.v5.bartender.pod.PodLocalService;
import com.caucho.v5.bartender.pod.PodOnUpdate;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeploySystem;
import com.caucho.v5.deploy2.DeployHandle2;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.util.L10N;

import io.baratine.service.OnDestroy;
import io.baratine.service.OnInit;
import io.baratine.service.Result;

/**
 * Contains Baratine service pod deployments.
 */
public class PodsDeployServiceImpl
{
  private static final L10N L = new L10N(PodsDeployServiceImpl.class);
  private static final Logger log
    = Logger.getLogger(PodsDeployServiceImpl.class.getName());
  
  private final Lifecycle _lifecycle;
  
  private ConcurrentHashMap<String,DeployHandle2<PodLoader>> _podLoaderMap
    = new ConcurrentHashMap<>();
  
  private HashMap<String,PodDeploy> _podMap = new HashMap<>();
  private HashMap<String,DeployNode> _podNodeMap = new HashMap<>();
  
  private HashMap<String,List<Result<Boolean>>> _loaderWaitMap = new HashMap<>();
  private PodLocalService _podService;
  private BartenderSystem _bartender;
  private ServerBartender _serverSelf;
  private PodContainer _podContainer;
  
  /**
   * Creates the webApp with its environment loader.
   */
  public PodsDeployServiceImpl(PodContainer podContainer)
  {
    _lifecycle = new Lifecycle(log, "pod-container-service");

    _lifecycle.toActive();
    
    _bartender = BartenderSystem.current();
    _serverSelf = _bartender.serverSelf();
    
    _podService = _bartender.getPodService();
    _podContainer = podContainer;
  }
  
  //
  // service/baratine
  //
  
  @OnInit
  public void init()
  {
    ServiceManagerAmp manager = AmpSystem.currentManager();
    
    manager.service(PodOnUpdate.ADDRESS)
           .subscribe((PodOnUpdate) pod->updatePod(pod));
    
    manager.service(ServerOnUpdate.ADDRESS)
           .subscribe((ServerOnUpdate) server->updatePods());
    
    updatePods();
  }
  
  public void updateConfig(PodConfig config)
  {
    PodBartender pod = _bartender.findPod(config.getName());
    
    PodDeploy podDeploy = createPodDeploy(pod);
    
    podDeploy.updateConfig(new PodConfigApp(config));
  }
  
  public void update(Result<Void> result)
  {
    //_podContainer.getConfigService().update(result.of(x->updatePods()));
  }
  
  /**
   * Update all pods known to the server.
   */
  private Void updatePods()
  {
    for (PodBartender pod : _podService.getPods()) {
      updatePod(pod);
    }
    
    return null;
  }
  
  /**
   * returns the handle for the pod-app node
   */
  public PodAppHandle getPodAppHandle(String id)
  {
    return getPodNode(id).getPodAppHandle();
  }

  public DeployNodePodApp getPodAppNode(String id)
  {
    DeployNode deployNode = getPodNode(id);

    if (deployNode instanceof DeployNodePodApp) {
      return (DeployNodePodApp) deployNode;
    }
    else {
      throw new IllegalStateException(id);
    }

  }
   
  public DeployNode getPodNode(String id)
  {
    DeployNode nodeDeploy = _podNodeMap.get(id);
    
    if (nodeDeploy != null) {
      return nodeDeploy;
    }
    
    if (! id.startsWith("pods/")) {
      throw new IllegalArgumentException(id);
    }
    
    int p = id.indexOf('/');
    
    String name = id.substring(p + 1);
    
    p = name.indexOf('.');
    
    if (p < 0) {
      throw new IllegalArgumentException(id);
    }
    
    String podName = name.substring(0, p);
    int index = Integer.parseInt(name.substring(p + 1));
    
    PodBartender pod = _bartender.findPod(podName);
    
    PodDeploy podDeploy = createPodDeploy(pod);
    
    nodeDeploy = createPodNodeDeploy(podDeploy, index);
    
    return nodeDeploy;
  }
  
  /**
   * Update based on a changed pod. If current server should deploy a
   * pod node, deploy it.
   */
  private void updatePod(PodBartender pod)
  {
    PodDeploy podDeploy = createPodDeploy(pod);
    
    Set<Integer> nodeList = localPodNodes(pod);
    
    List<DeployNode> nodeAppList = new ArrayList<>();
    
    for (int nodeIndex : nodeList) {
      DeployNode podNodeDeploy = createPodNodeDeploy(podDeploy, nodeIndex);
      
      nodeAppList.add(podNodeDeploy);
    }

    podDeploy.update(nodeAppList);
  }
  
  private PodDeploy createPodDeploy(PodBartender pod)
  {
    String id = "pods/" + pod.getName();
    
    PodDeploy podDeploy = _podMap.get(id);
    
    if (podDeploy == null) {
      DeployHandle2<PodLoader> loaderHandle = getPodLoaderHandle(id);
      
      //podDeploy = new PodDeploy(id, pod, _podContainer, loaderHandle);
      
      _podMap.put(id, podDeploy);
    }
    
    return podDeploy;
  }
  
  private DeployNode createPodNodeDeploy(PodDeploy pod, int nodeIndex)
  {
    String id = pod.getId() + "." + nodeIndex;
    
    DeployNode nodeDeploy = _podNodeMap.get(id);
    
    if (nodeDeploy == null) {
      DeploySystem deploySystem = DeploySystem.getCurrent();
      Objects.requireNonNull(deploySystem);
      
      if (pod.getPod().getType() == PodType.web) {
        String webId = "webapps/host/ROOT";
        
        /*
        DeployHandle2<WebAppBaratine> handle = deploySystem.createHandle(webId, log);
    
        nodeDeploy = new DeployNodeWebApp(pod, handle);
        */
        throw new UnsupportedOperationException();
      }
      else {
        DeployHandle2<PodApp> handle = null;//deploySystem.createHandle(id, log);
      
        nodeDeploy = new DeployNodePodApp(id, pod, nodeIndex, handle);
      }
      
      _podNodeMap.put(id, nodeDeploy);
    }
    
    return nodeDeploy;
  }

  /**
   * Find the pod nodes served by this server.
   */
  private Set<Integer> localPodNodes(PodBartender pod)
  {
    TreeSet<Integer> nodeList = new TreeSet<>();
    
    if (pod.getType() == PodType.off) {
      return nodeList;
    }
    
    if (pod.getType() == PodType.web) {
      // XXX: need to check if self is a member of that pod
      nodeList.add(0);
      
      return nodeList;
    }
    
    // -1 is used for expanding the classloader
    //nodeList.add(NODE_LOADER);
    
    for (int i = 0; i < pod.getNodeCount(); i++) {
      NodePodAmp node = pod.getNode(i);
      
      for (int j = 0; j < pod.getDepth(); j++) {
        ServerBartender server = node.getServer(j);
        
        if (server != null && server.isSameServer(_serverSelf)) {
          nodeList.add(i);
        }
      }
    }
    
    return nodeList;
  }
  
  /**
   * addPodLoaderController replaces the current controller for the pod-loader
   */
  public void addPodLoaderController(String id, PodLoaderController controller)
  {
    Objects.requireNonNull(controller);

    DeployHandle2<PodLoader> handle = getPodLoaderHandle(id);
    
    //controller.setService(controllerService);
    //handle.getService().setController(controller);
    handle.start();
    
    List<Result<Boolean>> resultWaitList = _loaderWaitMap.get(id);

    if (resultWaitList != null) {
      for (Result<Boolean> result : resultWaitList) {
        try {
          result.ok(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    
      resultWaitList.clear();
    }
  }
  
  /**
   * getPodLoaderService returns the service for the pod-loader
   */
  public DeployHandle2<PodLoader> getPodLoaderHandle(String id)
  {
    if (! id.startsWith("pods/")) {
      throw new IllegalArgumentException(id);
    }
    
    DeploySystem deploySystem = DeploySystem.getCurrent();
    Objects.requireNonNull(deploySystem);
    
    /*
    DeployHandle2<PodLoader> handle = deploySystem.createHandle(id, log);
    
    return handle;
    */
    
    throw new UnsupportedOperationException();
  }
  
  public void updateNode(String id)
  {
    getPodNode(id).update();
  }
  
  /*
  private DeployHandle<PodLoader> getPodLoader(String id)
  {
    
  }
  */
  
  public void waitForLoaderDeploy(String id, Result<Boolean> result)
  {
    if (_podLoaderMap.get(id) != null) {
      result.ok(true);
      return;
    }
    
    List<Result<Boolean>> resultList = _loaderWaitMap.get(id);
    
    if (resultList == null) {
      resultList = new ArrayList<>();
      
      _loaderWaitMap.put(id, resultList);
    }
    
    resultList.add(result);
  }
  
  public Iterable<PodAppHandle> getPodAppHandles()
  {
    ArrayList<PodAppHandle> list = new ArrayList<>();
    
    for (DeployNode node : _podNodeMap.values()) {
      
      if (node instanceof DeployNodePodApp) {
        DeployNodePodApp nodePod = (DeployNodePodApp) node;
        
        list.add(nodePod.getPodAppHandle());
      }
    }
    
    return list;
  }

  /**
   * Returns true if the webApp container has been closed.
   */
  public final boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the webApp container is active
   */
  public final boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Closes the container.
   * @param mode 
   */
  public boolean stop(ShutdownModeAmp mode)
  {
    Objects.requireNonNull(mode);
    
    if (! _lifecycle.toStop()) {
      return false;
    }
    
    for (PodDeploy pod : _podMap.values()) {
      try {
        pod.stop(mode);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    for (DeployNode deployNode : _podNodeMap.values()) {
      try {
        deployNode.shutdown(mode);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return true;
  }

  /**
   * Closes the container.
   */
  @OnDestroy
  public void destroy(ShutdownModeAmp mode)
  {
    stop(mode);
    
    if (! _lifecycle.toDestroy()) {
      return;
    }
    
    /*
    for (PodAppHandle handle : getPodAppHandles()) {
      try {
        handle.getHandle().destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    */
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
