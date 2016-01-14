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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.cf;

import java.util.ArrayList;

import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.ConfigFileParser.Arg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
public class ProgramCommand extends ProgramCommandContainer
{
  private static final L10N L = new L10N(ProgramCommand.class);
  
  public ProgramCommand(String id)
  {
    super(new NameCfg(id), new ArrayList<Arg>());
  }
  
  public ProgramCommand(NameCfg id)
  {
    super(id, new ArrayList<Arg>());
  }
  
  ProgramCommand(String id, ArrayList<Arg> args)
  {
    super(new NameCfg(id), args);
  }
  
  @Override
  public <T> void inject(T parent, ConfigType<T> type, InjectContext env)
  {
    NameCfg id = getId();
    
    AttributeConfig attr = getAttribute(type);

    if (attr == null) {
      throw error("{0} is an unknown attribute of {1}", id, parent);
    }
    
    if (attr.isProgram()) {
      if (attr.getConfigType().isProgramContainer()) {
        attr.setValue(parent, id, toContainer());
      }
      else {
        attr.setValue(parent, id, this);
      }
      
      return;
    }
    
    Object childBean = configChild(attr, parent);
      
    attr.setValue(parent, id, childBean);
    // attr.setValue(bean, _id, attr.getConfigType().valueOf(_value));
  }

  public ConfigProgram toProgram()
  {
    if (getArgs().length > 0) {
      return this;
    }
    
    ArrayList<ConfigProgram> programList = getProgramList();
    
    if (programList.size() != 1) {
      return this;
    }
    
    ConfigProgram program = programList.get(0);
    /*
    if (program instanceof ProgramCommandClassName) {
      ProgramCommandClassName commandClass = (ProgramCommandClassName) program;
      
      ProgramBeanClassChild childProgram;
      childProgram = new ProgramBeanClassChild(getId(), commandClass);  
      childProgram.setLocation(commandClass.getLocation());
      
      return childProgram; 
    }
    */
    
    return this;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(getClass().getSimpleName()).append("[");
    sb.append(getId().getLocalName());
    
    for (ConfigProgram program : getProgramList()) {
      sb.append(",").append(program);
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}

