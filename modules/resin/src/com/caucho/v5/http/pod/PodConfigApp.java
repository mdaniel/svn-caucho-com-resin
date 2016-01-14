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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.bartender.pod.PodConfig;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.deploy.ConfigDeploy;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.PathImpl;

/**
 * Configuration for a pod.cf file
 * 
 * The pod.cf sets the replication type ("solo", "pair", "triad", "cluster"), 
 * and any explicit server assignments.
 * 
 * Any other configuration items are passed to the pod-app when it's started.
 */
@ModulePrivate
public class PodConfigApp extends ConfigDeploy
{
  private static final Logger log = Logger.getLogger(PodConfigApp.class.getName());
  
  private final String _podName;
  private String _tag;
  
  private final Set<PathImpl> _archivePaths = new TreeSet<>();
  private final Set<PathImpl> _libraryPaths = new TreeSet<>();
  
  private final ArrayList<PodAppWeb> _webList = new ArrayList<>();
  
  PodConfigApp(PodConfig config)
  {
    _podName = config.getName();
    
    Objects.requireNonNull(_podName);
    
    _tag = config.getTag();
    Objects.requireNonNull(_tag);
    
    _archivePaths.addAll(config.getArchivePaths());
    _libraryPaths.addAll(config.getLibraryPaths());
    
    _webList.addAll(config.getWebList());
    
    for (ConfigProgram program : config.getPrologue().getProgramList()) {
      getPrologue().addProgram(program);
    }
    
    getBuilderProgram().addProgram(config.getBuilderProgram());
  }
  
  public String getName()
  {
    return _podName;
  }
  
  public String getTag()
  {
    return _tag;
  }

  public Set<PathImpl> getArchivePaths()
  {
    return _archivePaths;
  }

  public Set<PathImpl> getLibraryPaths()
  {
    return _libraryPaths;
  }
  
  public List<PodAppWeb> getWebList()
  {
    return _webList;
  }

  public boolean isApplication()
  {
    if (getArchivePaths().size() > 0) {
      return true;
    }
    else if (getLibraryPaths().size() > 0) {
      return true;
    }
    else if (getName().equals("cluster")
             || getName().equals("cluster_hub")
             || getName().equals("local")) {
      // baratine/8410
      return false;
    }
    else {
      return true;
    }
  }

  public void setConfigException(Exception e)
  {
    log.log(Level.FINER, e.toString(), e);
  }
  
  public boolean isModified()
  {
    return false;
  }
  
  
  @Override
  public boolean equals(Object value)
  {
    if (! (value instanceof PodConfigApp)) {
      return false;
    }
    
    PodConfigApp config = (PodConfigApp) value;
    
    if (! getName().equals(config.getName())) {
      return false;
    }
    
    if (! getArchivePaths().equals(config.getArchivePaths())) {
      return false;
    }
    
    if (! getLibraryPaths().equals(config.getLibraryPaths())) {
      return false;
    }
    
    if (! getWebList().equals(config.getWebList())) {
      return false;
    }
    
    // baratine/6030
    if (! getBuilderProgram().equals(config.getBuilderProgram())) {
      return false;
    }
    
    if (! getPrologue().equals(config.getPrologue())) {
      return false;
    }
    
    if (! _tag.equals(config._tag)) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
