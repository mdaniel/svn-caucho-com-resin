/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.core;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.core.ControlConfig;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentClassLoader;

/**
 * Separate environment for a bean
 */
public class ResinEnv extends ControlConfig implements EnvironmentBean {
  private EnvironmentClassLoader _loader;

  private ContainerProgram _init = new ContainerProgram();

  /**
   * Instantiates the environment.
   */
  public ResinEnv()
  {
    _loader = EnvironmentClassLoader.create();
    //_loader.setOwner(this);
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _init.addProgram(program);
  }

  @PostConstruct
  public void init()
    throws Throwable
  {
    Object object = getObject();
    
    if (object != null)
      _init.configure(object);
  }
}

