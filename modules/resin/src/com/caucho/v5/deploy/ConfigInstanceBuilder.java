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

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.loader.ClassLoaderConfig;

/**
 * Configuration phase to configure classloaders first.
 */
public class ConfigInstanceBuilder
{
  private ContainerProgram _program = new ContainerProgram();
  
  ConfigInstanceBuilder()
  {
  }
  
  /**
   * class-loader: Configures the classloader
   */
  @Configurable
  public ClassLoaderConfig createClassLoader()
  {
    return new ClassLoaderConfig();
  }

  /**
   * Adds to the builder program.
   */
  public void addContentProgram(ConfigProgram program)
  // cloud/112j vs hudson/4000
  //public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public ConfigProgram getProgram()
  {
    return _program;
  }
}
