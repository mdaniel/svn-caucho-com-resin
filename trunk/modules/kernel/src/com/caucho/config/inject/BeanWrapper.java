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

package com.caucho.config.inject;

import javax.enterprise.inject.spi.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Internal implementation for a Bean
 */
public class BeanWrapper<T> extends AbstractBean<T>
{
  private final Bean<T> _bean;

  public BeanWrapper(InjectManager manager, Bean<T> bean)
  {
    super(manager);

    _bean = bean;
  }

  protected Bean<T> getBean()
  {
    return _bean;
  }

  //
  // from javax.enterprise.inject.InjectionTarget
  //

  public T create(CreationalContext<T> env)
  {
    return getBean().create(env);
  }

  public void destroy(T instance, CreationalContext<T> env)
  {
    getBean().destroy(instance, env);
  }

  //
  // metadata for the bean
  //

  public Annotated getAnnotated()
  {
    Bean bean = getBean();

    if (bean instanceof AnnotatedBean)
      return ((AnnotatedBean) bean).getAnnotated();
    else
      return null;
  }

  public Set<Annotation> getQualifiers()
  {
    return getBean().getQualifiers();
  }

  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return getBean().getStereotypes();
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }

  public String getName()
  {
    return getBean().getName();
  }

  public boolean isAlternative()
  {
    return getBean().isAlternative();
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return getBean().isNullable();
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope()
  {
    return getBean().getScope();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    return getBean().getTypes();
  }

  public Class getBeanClass()
  {
    return getBean().getBeanClass();
  }

  /*
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
  */
}
