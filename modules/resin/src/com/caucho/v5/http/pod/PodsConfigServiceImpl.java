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
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.bartender.pod.PodConfig;
import com.caucho.v5.bartender.pod.PodLocalService;
import com.caucho.v5.bartender.pod.PodsConfig;
import com.caucho.v5.vfs.PathImpl;

import io.baratine.files.BfsFileSync;
import io.baratine.service.Cancel;
import io.baratine.service.OnInit;
import io.baratine.service.Result;


/**
 * The pod deployment service manages the deployed pods for the current server.
 * 
 * Main functions:
 * 1. scan /config/pods for local pod applications.
 * 2. for each local pod node, create a PodAppController.
 */
public class PodsConfigServiceImpl
{
  private static final Logger log
    = Logger.getLogger(PodsConfigServiceImpl.class.getName());
  
  private static final int NODE_LOADER = -1;
  
  private final ServerBartender _serverSelf;
  
  private final PathImpl _expandPath;
  private BfsFileSync _configDir;
  
  private final HashMap<String,PodConfig> _podMap = new HashMap<>();
  
  private final PodsDeployService _deployService;
  
  private PodsFiles _currentFiles;
  private HashMap<String,Cancel> _watchMap = new HashMap<>();
  
  private PodContainer _podContainer;

  private PodLocalService _podService;

  private BartenderSystem _bartender;
  
  PodsConfigServiceImpl(PodContainer podContainer,
                        PodsDeployService deployService)
  {
    _podContainer = podContainer;
    _expandPath = podContainer.getPodExpandPath();

    _bartender = BartenderSystem.current();
    _serverSelf = _bartender.serverSelf();
    
    _podService = _bartender.getPodService();
    
    ServiceManagerAmp manager = AmpSystem.currentManager();
    
    /*
    _configDir = manager.service("bfs://" + PodBuilderService.CONFIG_DIR)
                      .as(BfsFileSync.class);
                      */
    
    _deployService = deployService;
    
    _currentFiles = new PodsFiles(_configDir, new String[0]);
  }
  
  private PathImpl getExpandPath()
  {
    return _expandPath;
  }
  
  @OnInit
  public void onInit()
  {
    if (_expandPath == null) {
      return;
    }
    
    //_configDir.watch(path->updateConfigDir());
    _configDir.watch(path->update(Result.ignore()));
    
    /*
    ServiceManagerAmp manager = AmpSystem.getCurrentManager();
    
    manager.lookup(PodOnUpdate.ADDRESS)
           .subscribe((PodOnUpdate) pod->updatePod(pod));
    
    manager.lookup(ServerOnUpdate.ADDRESS)
           .subscribe((ServerOnUpdate) server->updatePods());
           */
    
    update(Result.ignore());
  }
  
  public void update(Result<Void> result)
  {
    updateConfigDirSync();
    //updatePods();
    
    result.ok(null);
  }

  /**
   * Updates the deployed list for bfs configuration of the pods.
   * 
   * Any file in /config/pods named *.cf is a pod config file.
   */
  private void updateConfigDirSync()
  {
    updateConfigDir(_configDir.list());
  }
  
  /**
   * Updates the deployed list for bfs configuration of the pods.
   * 
   * Any file in /config/pods named *.cf is a pod config file.
   */
  private void updateConfigDir(String []list)
  {
    if (list == null) {
      return;
    }
    
    PodsFiles files = new PodsFiles(_configDir, list);
    
    if (files.equals(_currentFiles)) {
      return;
    }
    
    _currentFiles = files;
    
    setWatches(files);
    
    ArrayList<PodConfig> podList = new ArrayList<>();
    Arrays.sort(list);
    
    PodsConfig podsConfig = new PodsConfig();

    for (String path : list) {
      if (path.endsWith(".cf")) {
        parseConfigFile(podsConfig, path);
      }
    }
    
    // podList.addAll(podsConfig.getPods());
    ArrayList<String> podNames = new ArrayList<>();
    
    for (PodConfig pod : podList) {
      updatePod(pod);

      podNames.add(pod.getName());
    }
    
    for (PodConfig oldPod : _podMap.values()) {
      if (podNames.contains(oldPod.getName())) {
        continue;
      }
      
      if (oldPod.getType() == PodBartender.PodType.off) {
        continue;
      }
      
      PodConfig deadPod = new PodConfig();
      deadPod.setName(oldPod.getName());
      deadPod.setType(PodBartender.PodType.off);
      
      updatePod(deadPod);
    }

    /*
    for (PodNodeItem item : _podControllers) {
      if (item.isModified()) {
        //item.closeController();
        if (item.isOwner()) {
          item.open();
          item.start();
        }
        else {
          item.close();
        }
      }
      else {
        //PodControllerBase<?> controller = item.getController();
        DeployHandle<?> handle = item.getHandle();
        
        if (handle != null) {
          handle.alarm();
        }
      }
    }
    */
  }
  
  private void setWatches(PodsFiles files)
  {
    for (Cancel handle : _watchMap.values()) {
      handle.cancel();
    }
    
    _watchMap.clear();
    
    for (String name : files.getFileNames()) {
      BfsFileSync file = _configDir.lookup(name);
      
      Cancel handle = file.watch(path->update(Result.ignore()));
      
      _watchMap.put(name, handle);
    }
  }

  /*
   * Parse the /config/pods/foo.cf configuration file. The configuration
   * is merged with the running PodsConfig.
   */
  private void parseConfigFile(PodsConfig podsConfig, String path)
  {
    /*
    PathImpl configPath = Vfs.lookup("bfs://" + PodBuilderService.CONFIG_DIR + "/" + path);
    
    ConfigContext config = new ConfigContext();

    try {
      podsConfig.setCurrentDepend(new Depend(configPath));
      
      config.configure2(podsConfig, configPath);
    } catch (Exception e) {
      System.err.println("Config: [" + BartenderSystem.getCurrentSelfServer().getDisplayName() + "] " + e);
      e.printStackTrace();
      log.warning(e.toString());
      
      podsConfig.setConfigException(e);
    }
    */
  }
  
  /**
   * Callback for a changed pod.
   */
  private void updatePod(PodConfig podConfig)
  {
    String podName = podConfig.getName();

    _podMap.put(podName, podConfig);
    
    _deployService.updateConfig(podConfig);
  }
}
