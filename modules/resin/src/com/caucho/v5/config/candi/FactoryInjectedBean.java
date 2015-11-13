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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

public class FactoryInjectedBean<T> implements Bean<T>
{
  private Bean<T> _bean;
  private InjectionTargetFactory _factory;

  public <X> FactoryInjectedBean(Bean bean, InjectionTargetFactory<X> factory)
  {
    _bean = bean;
    _factory = factory;
  }

  @Override
  public boolean isAlternative()
  {
    return _bean.isAlternative();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _bean.getBeanClass();
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _factory.createInjectionTarget(this).getInjectionPoints();
  }

  @Override
  public String getName()
  {
    return _bean.getName();
  }

  @Override
  public boolean isNullable()
  {
    return _bean.isNullable();
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return _bean.getQualifiers();
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return _bean.getScope();
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _bean.getStereotypes();
  }

  @Override
  public Set<Type> getTypes()
  {
    return _bean.getTypes();
  }

  @Override
  public T create(CreationalContext<T> creationalContext)
  {
    InjectionTarget<T> target = _factory.createInjectionTarget(this);

    T instance = target.produce(creationalContext);
    
    Objects.requireNonNull(instance);

    if (creationalContext != null) {
      creationalContext.push(instance);
    }

    target.inject(instance, creationalContext);
    target.postConstruct(instance);

    return instance;
  }

  @Override
  public void destroy(T instance, CreationalContext<T> creationalContext)
  {
    InjectionTarget target = _factory.createInjectionTarget(this);

    target.preDestroy(instance);

    if (creationalContext != null) {
      if (creationalContext instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> env
          = (CreationalContextImpl<?>) creationalContext;
        env.clearTarget();
      }

      creationalContext.release();
    }

    target.dispose(instance);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()
           + '['
           + _bean
           + ", "
           + _factory
           + ']';
  }
}
