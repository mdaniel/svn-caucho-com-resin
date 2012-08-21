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
 * @author Alex Rojkov
 */

package com.caucho.tomcat;

import com.caucho.config.inject.InjectManager;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.NamingException;
import java.lang.reflect.InvocationTargetException;

public class InstanceManager implements org.apache.tomcat.InstanceManager
{
  public void destroyInstance(Object instance)
    throws IllegalAccessException, InvocationTargetException
  {
    BeanManager beanManager = InjectManager.create();

    Class instanceClass = instance.getClass();

    AnnotatedType annotatedType
      = beanManager.createAnnotatedType(instanceClass);

    InjectionTarget injectionTarget
      = beanManager.createInjectionTarget(annotatedType);

    injectionTarget.preDestroy(instance);

    injectionTarget.dispose(instance);
  }

  public void newInstance(final Object instance)
    throws IllegalAccessException, InvocationTargetException, NamingException
  {
    Class instanceClass = instance.getClass();

    newInstance(instance, instanceClass);
  }

  public Object newInstance(String className)
    throws IllegalAccessException, InvocationTargetException, NamingException,
    InstantiationException, ClassNotFoundException
  {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    Class instanceClass = classLoader.loadClass(className);

    Object instance = newInstance(null, instanceClass);

    return instance;
  }

  public Object newInstance(String className, ClassLoader classLoader)
    throws IllegalAccessException, InvocationTargetException, NamingException,
    InstantiationException, ClassNotFoundException
  {
    Class instanceClass = classLoader.loadClass(className);

    Object instance = newInstance(null, instanceClass);

    return instance;
  }

  private Object newInstance(Object instance, Class instanceClass)
  {
    BeanManager beanManager = InjectManager.create();

    AnnotatedType annotatedType
      = beanManager.createAnnotatedType(instanceClass);

    InjectionTarget injectionTarget
      = beanManager.createInjectionTarget(annotatedType);

    CreationalContext context = beanManager.createCreationalContext(null);

    if (instance == null) {
      instance = injectionTarget.produce(context);
    }

    injectionTarget.inject(instance, context);

    injectionTarget.postConstruct(instance);

    return instance;
  }

  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}