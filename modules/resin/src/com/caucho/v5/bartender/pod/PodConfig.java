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

package com.caucho.v5.bartender.pod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.bartender.pod.PodBartender.PodType;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.deploy.ConfigDeploy;
import com.caucho.v5.http.pod.PodAppWeb;
import com.caucho.v5.http.pod.ServerPodConfig;
import com.caucho.v5.loader.DependencyContainer;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

/**
 * Configuration for a pod.cf file
 * 
 * The pod.cf sets the replication type ("solo", "pair", "triad", "cluster"), 
 * and any explicit server assignments.
 * 
 * Any other configuration items are passed to the pod-app when it's started.
 */
@ModulePrivate
public class PodConfig extends ConfigDeploy
{
  private static final Logger log = Logger.getLogger(PodConfig.class.getName());
  
  private String _podName = "";
  private PodType _type;
  private int _size = -1;
  
  private String _tag = "";
  
  private ArrayList<ServerPodConfig> _servers = new ArrayList<>();
  
  private final Set<PathImpl> _archivePaths = new TreeSet<>();
  private final Set<PathImpl> _libraryPaths = new TreeSet<>();
  
  private final Set<PodAppWeb> _webList = new HashSet<>();
  
  private ArrayList<Depend> _dependList = new ArrayList<>();
  private DependencyContainer _dependContainer = new DependencyContainer();

  private Comparator<ServerPodsCost> _costComparator;

  /**
   * name: the pod name
   */
  @ConfigArg(0)
  public void setName(String podName)
  {
    _podName = podName;
  }
  
  public String getName()
  {
    return _podName;
  }

  /**
   * tag: the pod deployment tag
   */
  public void setTag(String tag)
  {
    Objects.requireNonNull(tag);
    _tag = tag;
  }
  
  public String getTag()
  {
    return _tag;
  }
  
  /**
   * The pod type: "off", "solo", "pair", "triad", "cluster"
   */
  @ConfigArg(1)
  public void setType(PodBartender.PodType type)
  {
    if (_type == null) {
      _type = type;
    }
    else if (PodBartender.PodType.auto.equals(type)) {
    }
    else {
      _type = type;
    }
  }
  
  /**
   * The pod type: "off", "solo", "pair", "triad", "cluster"
   */
  public PodBartender.PodType getType()
  {
    return _type;
  }
  
  public boolean isLazy()
  {
    return _type == null || _type == PodType.lazy;
  }
  
  public boolean isOff()
  {
    return _type == PodType.off;
  }
  
  public void setSize(int size)
  {
    _size = size;
  }
  
  public int getSize()
  {
    return _size;
  }
  
  public void addServer(ServerPodConfig server)
  {
    // seed server replaces default server
    if (! server.getAddress().isEmpty()) {
      int index = findDefaultServer(server.getPort());
      
      if (index >= 0) {
        _servers.set(index, server);
        return;
      }
    }
    
    _servers.add(server);
  }
  
  private int findDefaultServer(int port)
  {
    for (int i = 0; i < _servers.size(); i++) {
      ServerPodConfig server = _servers.get(i);
    
      if (server.getPort() == port && server.getAddress().isEmpty()) {
        return i;
      }
    }
    
    return -1;
  }
  
  public void configureServers(ArrayList<ConfigProgram> serverList)
  {
    for (int i = 0; i < _servers.size(); i++) {
      if (i < serverList.size()) {
        serverList.get(i).configure(_servers.get(i));
      }
    }
    
    for (int i = _servers.size(); i < serverList.size(); i++) {
      ServerPodConfig server = new ServerPodConfig();
      
      serverList.get(i).configure(server);
      
      _servers.add(server);
    }
  }
  
  public Iterable<ServerPodConfig> getServers()
  {
    return _servers;
  }
  
  public int getServerCount()
  {
    return _servers.size();
  }

  public ServerPodConfig getServer(int i)
  {
    if (i < _servers.size()) {
      return _servers.get(i);
    }
    else {
      return null;
    }
  }
  
  /**
   * archive: adds a *.bar archive file for the application.
   */
  @Configurable
  public void addArchive(String path)
  {
    PathImpl libPath = Vfs.lookup("bfs://").lookup(path);
    
    _archivePaths.add(libPath);
    
    addDepend(libPath);
  }

  public Set<PathImpl> getArchivePaths()
  {
    return _archivePaths;
  }
  
  /**
   * library: adds a *.jar library file for the application.
   */
  @Configurable
  public void addLibrary(String path)
  {
    PathImpl libPath = Vfs.lookup("bfs://").lookup(path);
    
    _libraryPaths.add(libPath);
    
    addDepend(libPath);
  }

  public Set<PathImpl> getLibraryPaths()
  {
    return _libraryPaths;
  }
  
  /**
   * web: adds a static web configuration.
   */
  @Configurable
  public void addWeb(PodAppWeb web)
  {
    _webList.add(web);
  }
  
  public Set<PodAppWeb> getWebList()
  {
    return _webList;
  }

  private void addDepend(PathImpl path)
  {
    addDepend(new Depend(path));
  }

  public void addDepend(Depend depend)
  {
    if (depend != null && ! _dependList.contains(depend)) {
      _dependList.add(depend);
    }
    
    _dependContainer.add(depend);
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

  public void setCostComparator(Comparator<ServerPodsCost> costComparator)
  {
    _costComparator = costComparator;
  }

  public Comparator<ServerPodsCost> getCostComparator()
  {
    return _costComparator;
  }
  
  /*
  @Configurable
  public void setJournalMaxCount(ConfigProgram program)
  {
    getPrologue().addProgram(program);
  }
  */
  
  @Configurable
  public void setJournalDelay(ConfigProgram program)
  {
    getPrologue().addProgram(program);
  }
  
  @Configurable
  public void setDebug(ConfigProgram program)
  {
    getPrologue().addProgram(program);
    //getBuilderProgram().addProgram(program);
  }

  @Configurable
  public void setDebugQueryTimeout(ConfigProgram program)
  {
    getPrologue().addProgram(program);
  }
  
  public void preInit()
  {
    if (PodType.auto.equals(_type)) {
      _type = null;
    }
  }
  
  @PostConstruct
  public void init()
  {
    if (_type == null) {
      _type = PodType.lazy;
    }
  }

  public void setConfigException(Exception e)
  {
    log.log(Level.FINER, e.toString(), e);
  }
  
  public boolean isModified()
  {
    return _dependContainer.isModified();
  }
  
  
  @Override
  public boolean equals(Object value)
  {
    if (! (value instanceof PodConfig)) {
      return false;
    }
    
    PodConfig config = (PodConfig) value;
    
    if (! equals(_dependList, config._dependList)) {
      return false;
    }
    
    return true;
  }
  
    
  private boolean equals(ArrayList<Depend> dependListA,
                         ArrayList<Depend> dependListB)
  {
    if (dependListA.size() != dependListB.size()) {
      return false;
    }

    dependListA = new ArrayList<>(dependListA);
    dependListB = new ArrayList<>(dependListB);

    Collections.sort(dependListA, (x,y)->x.getPath().compareTo(y.getPath()));
    Collections.sort(dependListB, (x,y)->x.getPath().compareTo(y.getPath()));

    for (int i = 0; i < dependListA.size(); i++) {
      Depend dependA = dependListA.get(i);
      Depend dependB = dependListB.get(i);

      if (! dependA.getPath().equals(dependB.getPath())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public PodConfig clone()
  {
    PodConfig podConfig = new PodConfig();

    podConfig._podName = _podName;
    podConfig._type = _type;
    podConfig._size = _size;
    
    podConfig._servers.addAll(_servers);
    podConfig._costComparator = _costComparator;
    
    return podConfig;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
