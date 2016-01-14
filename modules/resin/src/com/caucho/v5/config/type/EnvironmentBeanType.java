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

package com.caucho.v5.config.type;

import java.util.ArrayList;

import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.custom.AttributeCustomBean;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentBean;

/**
 * Represents an introspected bean type for configuration.
 */
public class EnvironmentBeanType<X> extends InlineBeanType<X>
{
  public EnvironmentBeanType(TypeFactoryConfig typeFactory, Class<X> beanClass)
  {
    super(typeFactory, beanClass);

    setAddCustomBean(AttributeCustomBean.ATTRIBUTE);
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(ContextConfig env, Object bean)
  {
    super.beforeConfigure(env, bean);
    
    EnvironmentBean envBean = (EnvironmentBean) bean;
    ClassLoader loader = envBean.getClassLoader();
    
    Thread thread = Thread.currentThread();
    
    thread.setContextClassLoader(loader);

    // XXX: builder.setClassLoader?

    ArrayList<Dependency> dependencyList = env.getDependencyList();
    if (dependencyList != null) {
      for (Dependency depend : dependencyList) {
        EnvLoader.addDependency(depend);
      }
    }
    
    // XXX: addDependencies(builder);
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(Object bean)
  {
    super.beforeConfigure(bean);
    
    EnvironmentBean envBean = (EnvironmentBean) bean;
    ClassLoader loader = envBean.getClassLoader();
    
    Thread thread = Thread.currentThread();
    
    thread.setContextClassLoader(loader);

    // XXX: builder.setClassLoader?
    /* XXX:
    ArrayList<Dependency> dependencyList = env.getDependencyList();
    if (dependencyList != null) {
      for (Dependency depend : dependencyList) {
        Environment.addDependency(depend);
      }
    }
    */
    
    // XXX: addDependencies(builder);
  }
}
