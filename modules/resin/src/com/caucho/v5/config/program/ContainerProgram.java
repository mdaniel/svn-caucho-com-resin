/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.v5.config.program;

import java.util.ArrayList;
import java.util.Objects;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.NoAspect;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

@NoAspect
public class ContainerProgram extends ConfigProgram {
  static final L10N L = new L10N(ContainerProgram.class);

  private ArrayList<ConfigProgram> _programList
    = new ArrayList<>();
    
  public ContainerProgram(ConfigContext config)
  {
    super(config);
  }
  
  public ContainerProgram()
  {
    super(ConfigContext.getCurrent());
  }

  public ArrayList<ConfigProgram> getProgramList()
  {
    return _programList;
  }
  
  /**
   * Adds a new program to the container
   * 
   * @param program the new program
   */
  @Override
  public void addProgram(ConfigProgram program)
  {
    Objects.requireNonNull(program);

    _programList.add(program);
  }

  /**
   * Adds a new program to the container
   * 
   * @param program the new program
   */
  public void addProgram(int index, ConfigProgram program)
  {
    Objects.requireNonNull(program);
    
    _programList.add(index, program);
  }

  /**
   * Invokes the child programs on the bean
   * 
   * @param bean the bean to configure
   * @param env the configuration environment
   * 
   * @throws com.caucho.v5.config.ConfigException
   */
  @Override
  public <T> void inject(T bean, InjectContext env)
    throws ConfigException
  {
    for (ConfigProgram program : _programList) {
      program.inject(bean, env);
    }
  }

  /**
   * Invokes the child programs on the bean
   * 
   * @param bean the bean to configure
   * @param env the configuration environment
   * 
   * @throws com.caucho.v5.config.ConfigException
   */
  @Override
  public <T> void injectTop(T bean, InjectContext env)
    throws ConfigException
  {
    for (ConfigProgram program : _programList) {
      program.injectTop(bean, env);
    }
  }

  @Override
  public <T> void inject(T bean, ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    injectChildren(bean, type, env);
  }
  
  /**
   * Invokes the child programs on the bean, given the introspected type.
   */
  public <T> void injectChildren(T bean, ConfigType<T> type, InjectContext env)
  {
    for (ConfigProgram program : _programList) {
      program.inject(bean, type, env);
    }
  }
  
  public ContainerProgram toContainer()
  {
    ContainerProgram program = new ContainerProgram();
    
    for (ConfigProgram subProgram : getProgramList()) {
      program.addProgram(subProgram);
    }
    
    return program;
  }
  
  @Override
  public boolean equals(Object value)
  {
    if (! (value instanceof ContainerProgram)) {
      return false;
    }
    
    ContainerProgram program = (ContainerProgram) value;
    
    return _programList.equals(program._programList);
  }

  public String toString()
  {
    return getClass().getSimpleName() + _programList;
  }
}
