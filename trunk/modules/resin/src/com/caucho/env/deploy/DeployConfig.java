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

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.program.PropertyValueProgram;
import com.caucho.config.types.PathBuilder;
import com.caucho.config.types.Period;
import com.caucho.config.types.RawString;
import com.caucho.config.types.FileSetType;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The configuration for a deployable instance.
 */
public class DeployConfig {
  private final static Logger log
    = Logger.getLogger(DeployConfig.class.getName());

  // The deploy id
  private String _id;

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
  
  // redeploy period
  private Period _redeployCheckInterval;

  // if true, skip defaults
  private boolean _isSkipDefaultConfig;

  // The classloader program
  private ContainerProgram _classLoaderProgram = new ContainerProgram();

  // The configuration program
  private ContainerProgram _program = new ContainerProgram();

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
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
   * Sets the root directory string
   */
  public void setRootDirectory(RawString rootDirectory)
  {
    _rootDirectory = rootDirectory.getValue();
  }

  /**
   * Gets the root-directory.
   */
  public String getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the archive-path
   */
  public void setArchivePath(RawString path)
  {
    _archivePath = path.getValue();
  }

  /**
   * Gets the archive-path
   */
  public String getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Skip the defaults (for admin)
   */
  public boolean isSkipDefaultConfig()
  {
    return _isSkipDefaultConfig;
  }

  /**
   * Sets the archive auto-remove file set.
   */
  public void setExpandCleanupFileset(FileSetType fileset)
  {
    _expandCleanupFileset = fileset;
  }

  /**
   * Gets the archive auto-remove file set.
   */
  public FileSetType getExpandCleanupFileset()
  {
    return _expandCleanupFileset;
  }

  /**
   * Skip the defaults (for admin)
   */
  public void setSkipDefaultConfig(boolean isDefault)
  {
    _isSkipDefaultConfig = isDefault;
  }

  /**
   * Sets the startup-mode
   */
  public void setStartupMode(DeployMode mode)
    throws ConfigException
  {
    _startupMode = mode;
  }

  /**
   * Gets the startup mode.
   */
  public DeployMode getStartupMode()
  {
    return _startupMode;
  }

  /**
   * Sets the startup-priority
   */
  public void setStartupPriority(int priority)
    throws ConfigException
  {
    _startupPriority = priority;
  }

  /**
   * Gets the startup priority.
   */
  public int getStartupPriority()
  {
    return _startupPriority;
  }

  /**
   * Sets the redeploy-check-interval
   */
  public void setRedeployCheckInterval(Period period)
  {
    _redeployCheckInterval = period;
  }

  /**
   * Gets the redeploy check interval.
   */
  public Period getRedeployCheckInterval()
  {
    return _redeployCheckInterval;
  }

  /**
   * Sets the redeploy-mode
   */
  public void setRedeployMode(DeployMode mode)
    throws ConfigException
  {
    _redeployMode = mode;
  }

  /**
   * Gets the redeploy mode.
   */
  public DeployMode getRedeployMode()
  {
    return _redeployMode;
  }

  /**
   * Returns the prologue.
   */
  public DeployConfig getPrologue()
  {
    return null;
  }

  /**
   * Adds to the classloader
   */
  public void addClassLoader(ConfigProgram program)
  {
    _classLoaderProgram.addProgram(program);
  }
  
  public ConfigProgram getClassLoaderProgram()
  {
    return _classLoaderProgram;
  }

  /**
   * Adds to the builder program.
   */
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
  public Path calculateRootDirectory()
  {
    return calculateRootDirectory(null);
  }

  /**
   * Calculates the root directory for the config.
   */
  public Path calculateRootDirectory(Map<String,Object> varMap)
  {
    try {
      String rawPath = getRootDirectory();
      Path rootDir = null;

      if (rawPath != null)
        rootDir = PathBuilder.lookupPath(rawPath, varMap);
      
      if (rootDir != null)
        return rootDir;

      return Vfs.lookup();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
}
