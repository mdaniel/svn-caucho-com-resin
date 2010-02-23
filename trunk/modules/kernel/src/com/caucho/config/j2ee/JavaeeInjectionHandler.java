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
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionPointHandler;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ValueGenerator;
import com.caucho.naming.Jndi;

/**
 * Common JavaEE injection handler
 */
abstract public class JavaeeInjectionHandler extends InjectionPointHandler {
  private InjectManager _manager;
  
  protected JavaeeInjectionHandler(InjectManager manager)
  {
    _manager = manager;
  }

  protected InjectManager getManager()
  {
    return _manager;
  }
  
  protected Bean<?> bind(String location, Class<?> type, String name)
  {
    InjectManager injectManager = getManager();

    Set<Bean<?>> beans = null;
    
    if ("".equals(name))
      name = null;

    if (name != null)
      beans = injectManager.getBeans(type, Names.create(name));
    else
      beans = injectManager.getBeans(type, CurrentLiteral.CURRENT);

    if (beans != null && beans.size() != 0)
      return injectManager.resolve(beans);

    beans = injectManager.getBeans(type, new AnnotationLiteral<Any>() {});

    if (beans == null || beans.size() == 0)
      return null;

    for (Bean<?> bean : beans) {
      // XXX: dup

      if (name == null || name.equals(bean.getName()))
        return bean;
    }

    return null;
  }
  
  protected void bindJndi(String name, ValueGenerator gen, String fullJndiName)
  {
    if (name == null || "".equals(name)) {
      name = fullJndiName;
    }
    
    if (! name.startsWith("java:")) {
      name = "java:comp/env/" + name;
    }
    
    try {
      Jndi.bindDeep(name, gen);
    } catch (NamingException e) {
      throw ConfigException.create(e);
    }
  }

  protected String getLocation(Field javaField)
  {
    return (javaField.getDeclaringClass().getName() 
            + "." + javaField.getName() + " ");
  }

  protected String getLocation(Method javaMethod)
  {
    return (javaMethod.getDeclaringClass().getName() 
            + "." + javaMethod.getName() + " ");
  }
}
