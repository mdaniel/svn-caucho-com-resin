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

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.ConfigFileParser.Arg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
public class ProgramCommandClassName extends ProgramCommandContainer
{
  private static final L10N L = new L10N(ProgramCommandClassName.class);
  
  public ProgramCommandClassName(NameCfg id)
  {
    this(id, new ArrayList<Arg>());
  }
  
  ProgramCommandClassName(NameCfg id, ArrayList<Arg> args)
  {
    super(id, args);
  }
  
  @Override
  public <T> void inject(T parent, 
                         ConfigType<T> parentType, 
                         InjectContext env)
  {
    NameCfg id = getId();
    
    AttributeConfig attr = getAttribute(parentType); // type

    if (attr == null) {
      ConfigType<?> envType = TypeFactoryConfig.getFactory().getEnvironmentType(id);
      
      // see XmlBean and XmlBeanConfig.initComponent
      Object bean = create(parent);

      if (bean != null) {
        return;
      }

      throw error("{0} is an unknown bean class or unknown attribute of {1}",
                  getId(), parent);
    }
    
    if (attr.isProgram()) {
      attr.setValue(parent, id, this);
      return;
    }

    Object childBean = configChild(attr, parent);
      
    attr.setValue(parent, id, childBean);
    
    // attr.setValue(bean, _id, attr.getConfigType().valueOf(_value));
  }
  
  public Class<?> getClassChild()
  {
    return TypeFactoryConfig.loadClass(getId());
  }

  @Override
  public <T> T create(ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    Object bean = type.create(null, getId());
    
    return (T) configChild(bean, (ConfigType) type);
  }

  public Object create(Object parent)
  {
    Class<?> beanClass = TypeFactoryConfig.loadClass(getId());
    
    if (beanClass == null) {
      /*
      throw error("{0} is an unknown bean class or unknown attribute of {1}",
                  getId(), parent);
                  */
      return null;
    }
    
    ConfigType type = TypeFactoryConfig.getType(beanClass);
    
    Object bean = type.create(parent, getId());
    
    return configChild(bean, type);
  }
}

