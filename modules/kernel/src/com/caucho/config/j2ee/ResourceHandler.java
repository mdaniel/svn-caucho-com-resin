/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.j2ee;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionPointHandler;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

/**
 * Handles the @Resource annotation for JavaEE
 */
public class ResourceHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(ResourceHandler.class);
  
  private static HashMap<Class<?>,Class<?>> _boxingMap
    = new HashMap<Class<?>,Class<?>>();
  
  public ResourceHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    Resource resource = field.getAnnotation(Resource.class);
    
    return generateContext(field, resource);
  }
  // InjectIntrospector.introspect(_injectProgramList, field);

  private ConfigProgram generateContext(AnnotatedField<?> field,
                                        Resource resource)
    throws ConfigException
  {
    String name = resource.name();
    String mappedName = resource.mappedName();
    String lookup = null; // resource.lookup();

    Field javaField = field.getJavaMember();
    
    String location = getLocation(javaField);

    Class<?> bindType = javaField.getType();
    
    ValueGenerator gen;

    if (lookup != null && ! "".equals(lookup))
      gen = new JndiValueGenerator(location, bindType, lookup);
    else
      gen = bindValueGenerator(location, bindType, name, mappedName);
      
    bindJndi(name, gen, javaField);
    
    return new FieldGeneratorProgram(javaField, gen);
  }

  private ValueGenerator bindValueGenerator(String location, 
                                            Class<?> bindType,
                                            String name,
                                            String mappedName)
  {
    Class<?> boxedType = _boxingMap.get(bindType);
  
    if (boxedType != null)
      bindType = boxedType;
  
    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
     */

    Bean<?> bean;
  
    bean = bind(location, bindType, name);
  
    if (bean == null)
      bean = bind(location, bindType, mappedName);

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @Resource.",
                                               name));
    }
    else if (! "".equals(mappedName)) {
      throw new ConfigException(location + L.l("mappedName='{0}' is an unknown @Resource.",
                                               mappedName));
    }
    else {
      throw new ConfigException(location + L.l("@Resource cannot find any matching resources with type='{0}'",
                                               bindType));
    }

    // bindJndi(location, jndiName, bean);

    // return new ComponentValueGenerator(location, (AbstractBean) bean);
  
    return new BeanValueGenerator(location, bean);
  }
  
  static {
    _boxingMap.put(boolean.class, Boolean.class);
    _boxingMap.put(char.class, Character.class);
    _boxingMap.put(byte.class, Byte.class);
    _boxingMap.put(short.class, Short.class);
    _boxingMap.put(int.class, Integer.class);
    _boxingMap.put(long.class, Long.class);
    _boxingMap.put(float.class, Float.class);
    _boxingMap.put(double.class, Double.class);
  }
}
