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

import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.program.PropertyValueProgram;
import com.caucho.v5.config.types.FileSetType;
import com.caucho.v5.config.types.PathBuilder;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.deploy2.DeployMode;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * The configuration for a deployable instance.
 */
public class ConfigDeploy {
  private final static Logger log
    = Logger.getLogger(ConfigDeploy.class.getName());

  // The deploy id
  private String _id = "";

  // The root directory
  private String _rootDirectory;

  // The archive path;
  private String _archivePath;

  // The expansion cleanup set
  private FileSetType _expandCleanupFileset;

  // startup priority
  private int _startupPriority;

  // startup mode
  private DeployMode _startupMode;

  // redeploy mode
  private DeployMode _redeployMode;
  
  // controller type
  private DeployControllerType _controllerType = DeployControllerType.STATIC;
  
  // redeploy period
  private Period _redeployCheckInterval;

  // if true, skip defaults
  private boolean _isSkipDefaultConfig;

  // The configuration prologue
  private ContainerProgram _prologue = new ContainerProgram();

  // The configuration program
  private ContainerProgram _program = new ContainerProgram();

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
    Objects.requireNonNull(id);
    
    _id = id;
  }

  /**
   * Gets the id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * archive-path: location of the *.war files
   */
  public void setArchivePath(RawString path)
  {
    _archivePath = path.getValue();
  }

  /**
   * archive-path: location of the *.war files
   */
  public String getArchivePath()
  {
    return _archivePath;
  }
  
  /**
   * controller-type: set the priority of the config
   */
  public void setControllerType(DeployControllerType type)
  {
    if (type == null) {
      type = DeployControllerType.OFF;
    }
    
    _controllerType = type;
  }
  
  /**
   * controller-type: set the priority of the config
   */
  public DeployControllerType getControllerType()
  {
    return _controllerType;
  }

  /**
   * expand-cleanup-fileset: the archive auto-remove file set.
   */
  public void setExpandCleanupFileset(FileSetType fileset)
  {
    _expandCleanupFileset = fileset;
  }

  /**
   * expand-cleanup-fileset: the archive auto-remove file set.
   */
  public FileSetType getExpandCleanupFileset()
  {
    return _expandCleanupFileset;
  }

  /**
   * redeploy-check-interval: how often to check for updates
   */
  public void setRedeployCheckInterval(Period period)
  {
    _redeployCheckInterval = period;
  }

  /**
   * redeploy-check-interval: how often to check for updates
   */
  public Period getRedeployCheckInterval()
  {
    return _redeployCheckInterval;
  }

  /**
   * redeploy-mode: which action to take when the deployment changes
   */
  public void setRedeployMode(DeployMode mode)
    throws ConfigException
  {
    _redeployMode = mode;
  }

  /**
   * redeploy-mode: which action to take when the deployment changes
   */
  public DeployMode getRedeployMode()
  {
    return _redeployMode;
  }

  /**
   * root-directory: sets the root directory for the deployment
   */
  public void setRootDirectory(RawString rootDirectory)
  {
    if (rootDirectory != null) {
      _rootDirectory = rootDirectory.getValue();
    }
  }

  /**
   * root-directory.
   */
  public String getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * skip-default-config: skip the defaults (for admin)
   */
  public boolean isSkipDefaultConfig()
  {
    return _isSkipDefaultConfig;
  }

  /**
   * skip-default-config: skip the defaults (for admin)
   */
  public void setSkipDefaultConfig(boolean isDefault)
  {
    _isSkipDefaultConfig = isDefault;
  }

  /**
   * startup-mode: deployment auto-start/auto-redeploy
   */
  public void setStartupMode(DeployMode mode)
    throws ConfigException
  {
    _startupMode = mode;
  }

  /**
   * startup-mode: deployment auto-start/auto-redeploy
   */
  public DeployMode getStartupMode()
  {
    return _startupMode;
  }

  /**
   * startup-priority: the order of the instance with its peers.
   */
  public void setStartupPriority(int priority)
    throws ConfigException
  {
    _startupPriority = priority;
  }

  /**
   * startup-priority: the order of the instance with its peers.
   */
  public int getStartupPriority()
  {
    return _startupPriority;
  }

  /**
   * Returns the prologue.
   */
  public ContainerProgram getPrologue()
  {
    return _prologue;
  }
  
  public void setPrologue(ContainerProgram program)
  {
    _prologue.addProgram(program);
  }

  public void addClassLoader(ConfigProgram program)
  {
    _prologue.addProgram(program);
  }
  
  /**
   * Adds to the builder program.
   */
  @Configurable
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public ConfigProgram getBuilderProgram()
  {
    return _program;
  }

  /**
   * Adds a program.
   */
  public void addPropertyProgram(String name, Object value)
  {
    _program.addProgram(new PropertyValueProgram(name, value));
  }

  /**
   * Calculates the root directory for the config.
   */
  public PathImpl calculateRootDirectory()
  {
    return calculateRootDirectory(null);
  }

  /**
   * Calculates the root directory for the config.
   */
  public PathImpl calculateRootDirectory(Map<String,Object> varMap)
  {
    try {
      String rawPath = getRootDirectory();
      PathImpl rootDir = null;

      if (rawPath != null) {
        rootDir = PathBuilder.lookupPath(rawPath, varMap);
      }
      
      if (rootDir != null) {
        return rootDir;
      }

      return VfsOld.lookup();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
}
