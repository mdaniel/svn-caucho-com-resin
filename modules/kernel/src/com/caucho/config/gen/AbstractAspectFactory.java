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
package com.caucho.config.gen;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;

/**
 * Represents a filter for invoking a method
 */
@Module
abstract public class AbstractAspectFactory<X> implements AspectFactory<X> {
  private AspectBeanFactory<X> _beanFactory;
  private AspectFactory<X> _next;
  
  protected AbstractAspectFactory(AspectBeanFactory<X> beanFactory,
                                  AspectFactory<X> next)
  {
    _beanFactory = beanFactory;
    _next = next;
  }
  
  /**
   * Returns the bean Factory.
   */
  @Override
  public AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _beanFactory;
  }
  
  /**
   * Returns the factory's bean type.
   */
  @Override
  public AnnotatedType<X> getBeanType()
  {
    return getAspectBeanFactory().getBeanType();
  }
  
  /**
   * Returns the Java class of the bean.
   */
  public Class<?> getJavaClass()
  {
    return getBeanType().getJavaClass();
  }
  
  /**
   * Returns an aspect for the method if one exists.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    return _next.create(method, isEnhanced);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getBeanType() + "]";
  }
}
