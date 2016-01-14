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

package com.caucho.v5.config.custom;

import java.lang.annotation.Annotation;
import java.util.Objects;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.config.reflect.BaseTypeFactory;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

/**
 * Custom bean configured by namespace
 */
public class CustomBean<T>
{
  private static final L10N L = new L10N(CustomBean.class);

  private NameCfg _name;
  private Class<T> _type;

  private TypeCustomBean<T> _configType;
  private ContainerProgram _program = new ContainerProgram();

  private boolean _isInline;

  private Object _parent;

  private Object _bean;

  public CustomBean(NameCfg name,
                    Class<T> cl,
                    Object parent)
  {
    Objects.requireNonNull(name);
    Objects.requireNonNull(cl);
    //Objects.requireNonNull(parent);
    
    _name = name;

    _type = cl;
    
    Objects.requireNonNull(cl);
    
    _parent = parent;
    
    // XXX:
    // _component = new SimpleBean(cl);
    // _component.setScopeClass(Dependent.class);
    // ioc/2601
    BaseType baseType = BaseTypeFactory.current().createForSource(cl);
    /*
    AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(baseType);
    
    _annotatedType = new AnnotatedTypeImpl<T>(annType, beanManager);
    */
      
    // _cdiManager.addConfiguredBean(cl.getName());
    
    _configType = TypeFactoryConfig.getCustomBeanType(cl);
  }

  public Object toObject()
  {
    return _bean;
  }

  public void setInlineBean(boolean isInline)
  {
    _isInline = isInline;
  }

  public void addArg(ConfigProgram argProgram)
  {
    // TODO Auto-generated method stub
    
  }

  public void init()
  {
    ConfigType<T> beanType = _configType.getBeanType();
    
    Object bean = beanType.create(_parent, _name);
    
    _program.inject(bean);
    
    beanType.init(bean);
    
    _bean = bean;
  }

  public void addAdd(ConfigProgram value)
  {
    // TODO Auto-generated method stub
    
  }

  public void addAnnotation(Annotation value)
  {
    // TODO Auto-generated method stub
    
  }

  public ConfigType<T> getConfigType()
  {
    //return TypeFactoryConfig.getType(_type);
    //return TypeFactoryConfig.getType(CustomBean.class);
    return _configType;
  }

  public void addField(ConfigCustomField value)
  {
    // TODO Auto-generated method stub
    
  }

  public void addBuilderProgram(ConfigProgram configProgram)
  {
    _program.addProgram(configProgram);
  }

  public void addMethod(ConfigCustomMethod value)
  {
    // TODO Auto-generated method stub
    
  }

  public void addInitProgram(ConfigProgram value)
  {
    if (value != null) {
      _program.addProgram(value);
    }
    //System.out.println("INIT: " + value);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type.getSimpleName() + "]";
  }
}
