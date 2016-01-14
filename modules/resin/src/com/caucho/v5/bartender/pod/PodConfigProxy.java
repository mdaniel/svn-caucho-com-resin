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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.util.ModulePrivate;

/**
 * Configuration for a pod.cf file
 * 
 * The pod.cf sets the replication type ("solo", "pair", "triad", "cluster"), 
 * and any explicit server assignments.
 * 
 * Any other configuration items are passed to the pod-app when it's started.
 */
@ModulePrivate
public class PodConfigProxy
{
  private static final Logger log = Logger.getLogger(PodConfigProxy.class.getName());
  
  private String _podName;
  
  private ContainerProgram _program = new ContainerProgram();
  
  private ArrayList<ConfigProgram> _serverList = new ArrayList<>();

  /**
   * The pod name
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
   * Adds configuration for a server
   */
  @Configurable
  public void addServer(ContainerProgram program)
  {
    _serverList.add(program);
  }
  
  public ArrayList<ConfigProgram> getServers()
  {
    return _serverList;
  }

  /**
   * Adds any other item as a config program for the PodAppConfig 
   */
  @Configurable
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  /**
   * Returns the PodApp config.
   */
  public ContainerProgram getProgram()
  {
    return _program;
  }

  public void setConfigException(Exception e)
  {
    log.log(Level.FINER, e.toString(), e);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
