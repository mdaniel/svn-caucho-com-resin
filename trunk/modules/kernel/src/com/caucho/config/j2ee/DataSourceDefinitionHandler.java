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

package com.caucho.config.j2ee;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.sql.DataSource;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.MethodGeneratorProgram;
import com.caucho.config.program.NullProgram;
import com.caucho.config.program.SingletonValueGenerator;
import com.caucho.config.program.ValueGenerator;
import com.caucho.env.jdbc.DatabaseFactory;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Handles the @EJB annotation for JavaEE
 */
public class DataSourceDefinitionHandler extends JavaeeInjectionHandler {
  private static final Logger log
    = Logger.getLogger(DataSourceDefinitionHandler.class.getName());
  
  private static final L10N L = new L10N(DataSourceDefinitionHandler.class);
  
  public DataSourceDefinitionHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    DataSourceDefinition db = field.getAnnotation(DataSourceDefinition.class);
    
    return generateProgram(field, db);
  }
  
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    DataSourceDefinition db = method.getAnnotation(DataSourceDefinition.class);
    
    return generateProgram(method, db);
  }

  @Override
  public ConfigProgram introspectType(AnnotatedType<?> type)
  {
    // ejb/123j
    for (Class<?> parentClass = type.getJavaClass().getSuperclass();
         parentClass != null;
         parentClass = parentClass.getSuperclass()) {
      DataSourceDefinitions dbs
        = parentClass.getAnnotation(DataSourceDefinitions.class);

      if (dbs != null) {
        for (DataSourceDefinition db : dbs.value()) {
          introspectClass(getClass().getName(), db);
        }
      }

      DataSourceDefinition db
        = parentClass.getAnnotation(DataSourceDefinition.class);

      if (db != null)
        introspectClass(getClass().getName(), db);
    }
    
    DataSourceDefinitions dbs = type.getAnnotation(DataSourceDefinitions.class);

    if (dbs != null) {
      for (DataSourceDefinition db : dbs.value()) {
        introspectClass(getClass().getName(), db);
      }
    }

    DataSourceDefinition db = type.getAnnotation(DataSourceDefinition.class);

    if (db != null)
      introspectClass(getClass().getName(), db);

    return new NullProgram();
  }

  private void introspectClass(String location, DataSourceDefinition def)
  {
    String name = def.name();
    
    ValueGenerator gen = bindGenerator(location, def);

    if (name != null && ! "".equals(name)) {
      bindJndi(name, gen, name);
    }
  }

  private ConfigProgram generateProgram(AnnotatedField<?> field,
                                        DataSourceDefinition def)
    throws ConfigException
  {
    String name = def.name();

    Field javaField = field.getJavaMember();
    
    String location = getLocation(javaField);

    Class<?> bindType = javaField.getType();
    
    ValueGenerator gen = bindGenerator(location, def);

    if (name != null && ! "".equals(name))
      bindJndi(name, gen, name);
    
    bindJndi(javaField, gen);
    
    return new FieldGeneratorProgram(javaField, gen);
  }
  

  private ConfigProgram generateProgram(AnnotatedMethod<?> method,
                                        DataSourceDefinition def)
    throws ConfigException
  {
    String name = def.name();

    Method javaMethod = method.getJavaMember();
    
    String location = getLocation(javaMethod);

    ValueGenerator gen = bindGenerator(location, def);

    if (name != null && ! "".equals(name))
      bindJndi(name, gen, name);
    
    bindJndi(javaMethod, gen);
    
    return new MethodGeneratorProgram(javaMethod, gen);
  }
  
  protected ValueGenerator bindGenerator(String location,
                                         DataSourceDefinition def)
  {
    DataSource db = createDatabase(location, def);
    
    /*
    InjectManager cdiManager = getManager();
    
    BeanBuilder<?> builder = cdiManager.createBeanFactory(DataSource.class);
    builder.name(def.name());
    
    Bean<?> bean = builder.singleton(db);
    
    cdiManager.addBean(bean);
    */
    
    return new SingletonValueGenerator(db);
  }
  
  private DataSource createDatabase(String location,
                                    DataSourceDefinition def)
  {
    DataSource db = null;
    
    String name = def.name();
    
    try {
      db = (DataSource) Jndi.lookup(name);
      
      if (db != null)
        return db;
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }

    String className = def.className();
  
    if ("".equals(name))
      throw new ConfigException(L.l("{0}: @{1} name() attribute is required.",
                                    location, 
                                    DataSourceDefinition.class.getSimpleName()));
  
    if ("".equals(className)) {
      throw new ConfigException(L.l("{0}: @{1} beanInterface() attribute is required.",
                                    location,
                                    DataSourceDefinition.class.getSimpleName()));
    }
    
    Class<?> driverClass = null;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      driverClass = Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      throw new ConfigException(L.l("{0}: @{1} className() {2} is not a valid class.\n  {3}",
                                    location,
                                    DataSourceDefinition.class.getSimpleName(),
                                    className));
    }
    
    DatabaseFactory factory = DatabaseFactory.createBuilder();
    factory.setName(name);
    factory.setDriverClass(driverClass);
    
    if (! "".equals(def.url()))
      factory.setUrl(def.url());
    
    if (! "".equals(def.databaseName()))
      factory.setDatabaseName(def.databaseName());
    
    if (! "".equals(def.user()))
      factory.setUser(def.user());
    
    if (! "".equals(def.password()))
      factory.setPassword(def.password());
    
    return factory.create();
  }
}
