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

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;

/**
 * Common bean introspection for Produces and ManagedBean.
 */
public class BeanSynthetic<T,X> implements Bean<T>
{
  private Class<X> _beanClass;
  private BeanAttributes<T> _attributes;
  private Producer<T> _producer;

  BeanSynthetic(BeanAttributes<T> attributes,
                Class<X> beanClass,
                ProducerFactory<X> producerFactory)
  {
    Objects.requireNonNull(attributes);
    Objects.requireNonNull(beanClass);
    Objects.requireNonNull(producerFactory);
    
    _beanClass = beanClass;
    _attributes = attributes;
    _producer = producerFactory.createProducer(this);
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _beanClass;
  }
  
  //
  // BeanAttributes methods
  //

  @Override
  public boolean isAlternative()
  {
    return _attributes.isAlternative();
  }

  @Override
  public String getName()
  {
    return _attributes.getName();
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return _attributes.getQualifiers();
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return _attributes.getScope();
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _attributes.getStereotypes();
  }

  @Override
  public Set<Type> getTypes()
  {
    return _attributes.getTypes();
  }
  
  //
  // producer-managed methods 
                
  @Override
  public T create(CreationalContext<T> env)
  {
    return _producer.produce(env);
  }

  @Override
  public void destroy(T instance, CreationalContext<T> creationalContext)
  {
    _producer.dispose(instance);
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _producer.getInjectionPoints();
  }

  @Override
  public boolean isNullable()
  {
    return false;
  }
}
