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

import javax.annotation.PostConstruct;

import com.caucho.v5.bartender.pod.PodConfig;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.PathImpl;

/**
 * The generator for the pod-deploy
 */
@ModulePrivate
public class PodDeployConfig
{
  /*
  private final EarDeployGeneratorAdmin _admin
    = new EarDeployGeneratorAdmin(this);
    */

  private PodContainer _podContainer;
  
  private ArrayList<PodConfig> _podDefaultList
    = new ArrayList<>();

  // used for QA 
  private Boolean _isDeployDirectoryEnable;

  public PodDeployConfig(PodContainer podContainer)
  {
    _podContainer = podContainer;
    
    PathImpl dataRoot = RootDirectorySystem.getCurrentDataDirectory();
    PathImpl expandPath = dataRoot.lookup("pods");
    
    if (_podContainer.getPodExpandPath() == null) {
      _podContainer.setPodExpandPath(expandPath);
    }
    
    //_podDefaultList.addAll(podContainer.getPodDefaultList());
  }

  /**
   * Returns the parent container;
   */
  PodContainer getContainer()
  {
    return _podContainer;
  }
  
  @ConfigArg(0)
  public void setPath(PathImpl path)
  {
    getContainer().setPodExpandPath(path);
  }
  
  /*
  public void setDeployDirectoryEnable(boolean isEnable)
  {
    _isDeployDirectoryEnable = isEnable;
  }
  */

  /**
   * Sets the pod default.
   */
  public void addPodDefault(PodConfig config)
  {
    _podDefaultList.add(config);
  }
  
  @PostConstruct
  public void init()
  {
    /*
    if (_isDeployDirectoryEnable != null) {
      _podContainer.setDeployDirectoryEnable(_isDeployDirectoryEnable);
    }
    */
  }
}
