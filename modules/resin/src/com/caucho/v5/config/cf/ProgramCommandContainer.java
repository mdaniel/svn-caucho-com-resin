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
import java.util.Objects;

import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.ConfigFileParser.Arg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.inject.impl.InjectContextImpl;

/**
 * Program to assign parameters.
 */
abstract public class ProgramCommandContainer extends ProgramContainerBase
{
  private final NameCfg _id;
  private final ConfigProgram []_args;
  
  ProgramCommandContainer(NameCfg id, ArrayList<Arg> args)
  {
    _id = id;
    _args = new ConfigProgram[args.size()];
    
    for (int i = 0; i < _args.length; i++) {
      _args[i] = args.get(i).toProgram();
    }
  }
  
  protected NameCfg getId()
  {
    return _id;
  }
  
  protected ConfigProgram []getArgs()
  {
    return _args;
  }
  
  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    try {
      //ConfigType type = TypeFactoryConfig.getType(bean);
      ConfigType type = TypeFactoryConfig.getType(bean.getClass());

      inject(bean, type, env);

      // type.init(bean);
    } catch (RuntimeException e) {
      throw error(e);
    } catch (Exception e) {
      throw error(e);
    }
  }
  
  protected <T> Object configChild(AttributeConfig attr, Object parent)
  {
    T childBean = createChildInline(attr, parent);

    if (childBean != null) {
      return childBean;
    }
    
    childBean = createChild(attr, parent);

    if (childBean == null) {
      // special case to handle text
      if (_args.length == 0 && getProgramList().size() == 0) {
        // T value = (T) attr.getConfigType().valueOf(Boolean.TRUE);
        if (attr.getConfigType().getType().equals(boolean.class)) {
          return (T) attr.getConfigType().valueOf(Boolean.TRUE);
        }
        else {
          T value = (T) attr.getConfigType().valueOf("");
          return value;
        }
      }
      
      ConfigProgram arg = null;
      
      if (_args.length == 1 && getProgramList().size() == 0) {
        arg = _args[0];
      }
      else if (_args.length == 0 && getProgramList().size() == 1) {
        arg = getProgramList().get(0);
      }
      
      if (arg instanceof ProgramPropertyString) {
        ProgramPropertyString programString = (ProgramPropertyString) arg;
        
        if (programString.getId().getLocalName().equals("#text")) {
          T value = (T) attr.getConfigType().valueOf(programString.getValue());
          
          return value;
        }
      }

      throw error("'{0}' cannot create a child bean for {1}", 
                  getId().getCanonicalName(), parent);
    }
    
    ConfigType<T> subType = (ConfigType) attr.getType(childBean);
    
    Objects.requireNonNull(subType);
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      subType.beforeConfigure(childBean);
      
      subType.setLocation(childBean, getLocation());
    
      return configChild(childBean, subType);
    } catch (Exception e) {
      throw error(e);
    } finally {
      thread.setContextClassLoader(loader);
      
      subType.afterConfigure(childBean);
    }
  }
  
  /**
   * Creates an inline child.
   * 
   * <pre>
   * child { qa:MyChild; }
   * </pre>
   * 
   * The inline child requires a single child element that is assignable
   * to the attribute.
   */
  protected <T> T createChildInline(AttributeConfig attr, Object parent)
  {
    if (! attr.isAllowInline()) {
      return null;
    }
    
    if (getArgs() != null && getArgs().length > 0) {
      return null;
    }
    
    ArrayList<ConfigProgram> childPrograms = getProgramList();
    
    if (childPrograms.size() != 1) {
      return null;
    }
    
    ConfigProgram programChild = childPrograms.get(0);
    
    if (programChild instanceof ProgramCommandClassName) {
      ProgramCommandClassName programChildClass
        = (ProgramCommandClassName) programChild;

      ConfigType<?> configType = attr.getConfigType();
      
      if (configType == null || configType.getType() == null) {
        return null;
      }
      
      if (configType.getAddAttribute(programChildClass.getClassChild()) != null) {
        return null;
      }
      
      TypeFactoryConfig typeFactory = TypeFactoryConfig.getFactory();
      
      ConfigType<?> envType = typeFactory.getEnvironmentType(programChildClass.getId());
      
      if (envType != null && envType.isFlow()) {
        return null;
      }
      
      // config/231d
      // return (T) programChildClass.create(parent);

      Class<?> classChild = programChildClass.getClassChild();

      if (classChild == null) {
        return null;
      }

      if (attr.isAssignableFrom(classChild)) {
        return (T) programChildClass.create(parent);
      }
      
      ConfigType<?> childType = typeFactory.getType(classChild);
      
      if (childType.isReplace()) {
        // check for readResolve()
        return (T) programChildClass.create(parent);
      }
      
      
      /*
      ConfigType configType = attr.getConfigType();

      if (configType == null) {
        return null;
      }
      else if (configType.getType() == null) {
        return null;
      }
      else if (configType.getType().isAssignableFrom(classChild)) {
      }
      */
    }
    
    return null;
  }
  
  protected <T> T createChild(AttributeConfig attr, Object parent)
  {
    T childBean = (T) attr.create(parent, _id);

    
    return childBean;
  }
  
  protected <T> Object configChild(T childBean, ConfigType<T> type)
  {
    try {
      //CreationalContext<T> childEnv = new OwnerCreationalContext<T>(null);
      InjectContext env = InjectContextImpl.CONTEXT;
    
      for (ConfigProgram arg : _args) {
        arg.inject(childBean, type, env);
      }
    
      injectChildren(childBean, type, env);

      type.init(childBean);

      return type.replaceObject(childBean);
    } catch (Exception e) {
      throw error(e);
    }
  }
  
  protected AttributeConfig getAttribute(ConfigType<?> type)
  {
    NameCfg id = getId();
    
    return ProgramIdBase.getAttribute(id, type);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}

