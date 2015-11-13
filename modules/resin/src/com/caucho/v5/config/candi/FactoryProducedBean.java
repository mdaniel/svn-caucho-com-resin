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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class FactoryProducedBean<T> implements Bean<T>
{
  private Bean<T> _bean;
  private ProducerFactory _producerFactory;

  public <X> FactoryProducedBean(Bean bean, ProducerFactory<X> producerFactory)
  {
    _bean = bean;
    _producerFactory = producerFactory;
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
    return _bean.getInjectionPoints();
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
    Producer<T> producer = _producerFactory.createProducer(_bean);

    T t = producer.produce(creationalContext);

    return t;
  }

  @Override
  public void destroy(T instance, CreationalContext<T> creationalContext)
  {
    Producer<T> producer = _producerFactory.createProducer(_bean);

    producer.dispose(instance);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()
           + '['
           + _bean
           + ", "
           + _producerFactory
           + ']';
  }
}
