/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.types;

import com.caucho.config.ConfigContext;
import com.caucho.config.inject.BeanWrapper;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ScopeAdapterBean;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.L10N;
import javax.enterprise.inject.spi.*;
import javax.enterprise.inject.spi.InjectionTarget;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.PassivationCapable;

/**
 * Internal implementation for a Bean
 */
public class XmlBean<X> extends BeanWrapper<X>
  implements InjectionTarget<X>, ScopeAdapterBean, PassivationCapable
{
  private static final L10N L = new L10N(XmlBean.class);

  ManagedBeanImpl _bean;
  private Constructor _ctor;
  private Arg []_newProgram;
  private ConfigProgram []_injectProgram;

  private ClassLoader _loader = Thread.currentThread().getContextClassLoader();
  
  public XmlBean(ManagedBeanImpl bean,
		 Constructor ctor,
		 Arg []newProgram,
		 ConfigProgram []injectProgram)
  {
    super(bean.getBeanManager(), bean);

    _bean = bean;
    _ctor = ctor;
    _newProgram = newProgram;

    _injectProgram = injectProgram;
  }

  public ManagedBeanImpl getBean()
  {
    return _bean;
  }

  @Override
  public Annotated getAnnotated()
  {
    return _bean.getAnnotated();
  }

  @Override
  public AnnotatedType getAnnotatedType()
  {
    return _bean.getAnnotatedType();
  }

  public InjectionTarget<X> getInjectionTarget()
  {
    return this;
  }

  public Object getScopeAdapter(CreationalContext context)
  {
    Bean bean = getBean();
    
    if (bean instanceof ScopeAdapterBean)
      return ((ScopeAdapterBean) bean).getScopeAdapter(context);
    else
      return null;
  }

  @Override
  public X create(CreationalContext context)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
      
      X instance = produce(context);
      inject(instance, context);
      postConstruct(instance);

      return instance;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public X produce(CreationalContext context)
  {
    ConfigContext env = (ConfigContext) context;
    
    if (_ctor == null)
      return (X) getBean().getInjectionTarget().produce(env);
    else {
      Object []args = new Object[_newProgram.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = _newProgram[i].eval(env);
      }

      try {
	return (X) _ctor.newInstance(args);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	// XXX: clean up exception type
	throw new RuntimeException(e);
      }
    }
  }

  public void inject(X instance, CreationalContext<X> env)
  {
    getBean().getInjectionTarget().inject(instance, env);

    if (_injectProgram.length > 0) {
      for (ConfigProgram program : _injectProgram) {
	program.inject(instance, (ConfigContext) env);
      }
    }
  }

  public void postConstruct(X instance)
  {
    getBean().getInjectionTarget().postConstruct(instance);

    /*
    if (_initProgram.length > 0) {
      for (ConfigProgram program : _initProgram) {
	program.inject(instance, (ConfigContext) env);
      }
    }
    */
  }
  
  /**
   * Call destroy
   */
  public void preDestroy(X instance)
  {
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }
}
