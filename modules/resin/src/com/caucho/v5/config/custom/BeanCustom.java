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

package com.caucho.v5.config.custom;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;

import com.caucho.v5.config.candi.BeanWrapper;
import com.caucho.v5.config.candi.CreationalContextImpl;
import com.caucho.v5.config.candi.ManagedBeanImpl;
import com.caucho.v5.config.candi.ScopeAdapterBean;
import com.caucho.v5.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class BeanCustom<X> extends BeanWrapper<X>
  implements InjectionTarget<X>, ScopeAdapterBean<X>, PassivationCapable
{
  private ManagedBeanImpl<X> _bean;
  private InjectionTargetCustomBean<X> _injectionTarget;

  private ClassLoader _loader = Thread.currentThread().getContextClassLoader();

  public BeanCustom(ManagedBeanImpl<X> bean,
                 InjectionTargetCustomBean<X> injectionTarget)
  {
    super(bean.getInjectManager(), bean);
    
    _bean = bean;
    _injectionTarget = injectionTarget;
  }

  public ManagedBeanImpl<X> getBean()
  {
    return _bean;
  }

  @Override
  public Annotated getAnnotated()
  {
    return _bean.getAnnotated();
  }

  @Override
  public AnnotatedType<X> getAnnotatedType()
  {
    return _bean.getAnnotatedType();
  }

  @Override
  public InjectionTarget<X> getInjectionTarget()
  {
    return this;
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> context)
  {
    Bean<X> bean = getBean();

    if (bean instanceof ScopeAdapterBean<?>)
      return ((ScopeAdapterBean<X>) bean).getScopeAdapter(topBean, context);
    else
      return null;
  }

  @Override
  public X create(CreationalContext<X> env)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      X instance = produce(env);
      env.push(instance);
      
      inject(instance, env);
      postConstruct(instance);

      return instance;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public X produce(CreationalContext<X> context)
  {
    return _injectionTarget.produce(context);
  }

  @Override
  public void inject(X instance, CreationalContext<X> env)
  {
    _injectionTarget.inject(instance, env);
  }

  @Override
  public void postConstruct(X instance)
  {
    _injectionTarget.postConstruct(instance);
  }

  /**
   * Call destroy
   */
  public void preDestroy(X instance)
  {
    _injectionTarget.preDestroy(instance);
  }

  /**
   * Call destroy
   */
  public void dispose(X instance)
  {
    _injectionTarget.dispose(instance);
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }
}
