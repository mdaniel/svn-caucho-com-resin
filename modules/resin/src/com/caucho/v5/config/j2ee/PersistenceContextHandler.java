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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.j2ee;

import javax.enterprise.inject.spi.Bean;
import javax.persistence.PersistenceContext;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.BeanValueGenerator;
import com.caucho.v5.config.program.ValueGenerator;
import com.caucho.v5.inject.InjectManagerAmp;
import com.caucho.v5.util.L10N;

/**
 * Handles the @PersistenceContext annotation for JavaEE
 */
public class PersistenceContextHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(PersistenceContextHandler.class);

  public PersistenceContextHandler(InjectManagerAmp manager)
  {
    super(manager);
  }
  
  /*
  @Override
  public ConfigProgram introspectType(AnnotatedType<?> type)
  {
    PersistenceContext pContext = type.getAnnotation(PersistenceContext.class);
    
    String location = type.getJavaClass().getName() + ": ";

    String jndiName = null;
    
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();

    Bean<?> bean = bindEntityManager(location, pContext);
    
    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);
    
    if (jndiName != null)
      bindJndi(jndiName, gen, null);
    
    return new NullProgram();
  }
  */
  
  /*
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    PersistenceContext pContext = field.getAnnotation(PersistenceContext.class);
    
    PersistenceContextType type = pContext.type();
    
    Field javaField = field.getJavaMember();
    String location = getLocation(javaField);
    
    if (! javaField.getType().isAssignableFrom(EntityManager.class)) {
      throw new DefinitionException(L.l("{0}: @PersistenceContext field must be assignable from EntityManager.",
                                        getLocation(javaField)));
    }
    
    ValueGenerator gen;
    
    if (PersistenceContextType.EXTENDED.equals(type))
      gen = generateExtendedContext(location, pContext);
    else
      gen = generateTransactionContext(location, pContext);
    
    return new FieldGeneratorProgram(Config.getCurrent(), javaField, gen);
  }
  */
  // InjectIntrospector.introspect(_injectProgramList, field);
  
  /*
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    PersistenceContext pContext = method.getAnnotation(PersistenceContext.class);
    
    Method javaMethod= method.getJavaMember();
    String location = getLocation(javaMethod);
    
    Class<?> param = javaMethod.getParameterTypes()[0];
    
    if (! param.isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(L.l("{0}: @PersistenceContext method must be assignable from EntityManager.",
                                    getLocation(javaMethod)));
    }
    
    BeanValueGenerator gen;
    
    gen = generateTransactionContext(location, pContext);
    
    return new MethodGeneratorProgram(Config.getCurrent(), javaMethod, gen);
  }
  */
  // InjectIntrospector.introspect(_injectProgramList, field);

  private BeanValueGenerator 
  generateTransactionContext(String location,
                             PersistenceContext pContext)
    throws ConfigException
  {
    /*
    Bean<?> bean = bindEntityManager(location, pContext);

    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);
    
    return gen;
    */
    
    return null;
  }

  private ValueGenerator generateExtendedContext(String location,
                                                 PersistenceContext pContext)
  {
    /*
    Bean<?> bean = bindExtendedEntityManager(location, pContext);

    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);
    
    return gen;
    */
    
    return null;
  }
  
  private Bean<?> bindEntityManager(String location, 
                                    PersistenceContext pContext)
  {
    String name = pContext.name();
    String unitName = pContext.unitName();

    Bean<?> bean = null;
    
    /*
    bean = bind(location, EntityManager.class, unitName);
    
    if (bean == null)
      bean = bind(location, EntityManager.class, name);
*/
    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(unitName)) {
      throw new ConfigException(location + L.l("unitName='{0}' is an unknown @PersistenceContext.",
                                               unitName));
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @PersistenceContext.",
                                               name));

    }
    else {
      throw new ConfigException(location + L.l("@PersistenceContext cannot find any persistence contexts.  No JPA persistence-units have been deployed"));
    }
    
    return bean;
  }
  
  private Bean<?> bindExtendedEntityManager(String location, 
                                            PersistenceContext pContext)
  {
    String name = pContext.name();
    String unitName = pContext.unitName();

    Bean<?> bean = null;
  
    /*
    bean = bind(location, EntityManager.class, unitName);
    
    if (bean == null && "".equals(unitName))
      bean = bind(location, EntityManager.class, name);
      */

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(unitName)) {
      throw new ConfigException(location + L.l("unitName='{0}' is an unknown @PersistenceContext.",
                                               unitName));
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @PersistenceContext.",
                                               name));

    }
    else {
      throw new ConfigException(location + L.l("@PersistenceContext cannot find any persistence contexts.  No JPA persistence-units have been deployed"));
    }
    
    return bean;
  }
}
