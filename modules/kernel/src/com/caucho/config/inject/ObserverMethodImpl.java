/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigContext;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.BeanArg;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observer;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Internal implementation for a producer Bean
 */
public class ObserverMethodImpl<X, T> implements Observer<T> {
  private InjectManager _beanManager;

  private Bean<X> _bean;
  private AnnotatedMethod<X> _method;

  private Type _type;
  private Set<Annotation> _bindings;
  
  private BeanArg []_args;

  ObserverMethodImpl(InjectManager beanManager,
		     Bean<X> bean,
		     AnnotatedMethod<X> method,
		     Type type,
		     Set<Annotation> bindings)
  {
    _beanManager = beanManager;
    _bean = bean;
    _method = method;
    _method.getJavaMember().setAccessible(true);
    _type = type;
    _bindings = bindings;

    introspect();
  }

  /**
   * Returns the annotated method
   */
  public AnnotatedMethod<X> getAnnotatedMethod()
  {
    return _method;
  }

  /**
   * Returns the declaring bean
   */
  public Bean<X> getParentBean()
  {
    return _bean;
  }

  /**
   * Returns the observed event type
   */
  public Type getObservedEventType()
  {
    return _type;
  }

  /**
   * Returns the observed event bindings
   */
  public Set<Annotation> getObservedEventBindings()
  {
    return _bindings;
  }

  private void introspect()
  {
    List<AnnotatedParameter<X>> parameters = _method.getParameters();

    if (parameters.size() == 1)
      return;

    _args = new BeanArg[parameters.size()];

    for (int i = 0; i < _args.length; i++) {
      AnnotatedParameter<?> param = parameters.get(i);

      if (param.isAnnotationPresent(Observes.class))
	continue;

      _args[i] = new BeanArg(param.getBaseType(),
			     _beanManager.getBindings(param.getAnnotations()));
    }
  }

  /**
   * Send the event notification.
   */
  public boolean notify(T event)
  {
    Object instance = getInstance();

    Method method = _method.getJavaMember();

    try {
      method.invoke(instance, getEventArguments(event));
    } catch (IllegalArgumentException e) {
      String loc = (method.getDeclaringClass().getSimpleName() + "."
		    + method.getName() + ": ");

      throw new InjectionException(loc + e.getMessage(), e.getCause());
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      String loc = (method.getDeclaringClass().getSimpleName() + "."
		    + method.getName() + ": ");

      throw new InjectionException(loc + e.getMessage(), e.getCause());
    } catch (Exception e) {
      String loc = (method.getDeclaringClass().getSimpleName() + "."
		    + method.getName() + ": ");

      throw new InjectionException(loc + e.getMessage(), e.getCause());
    }

    return false;
  }

  protected Object getInstance()
  {
    Class<X> type = null;

    CreationalContext env = _beanManager.createCreationalContext();
    
    return _beanManager.getReference(getParentBean(), type, env);
  }

  protected Object[] getEventArguments(Object event)
  {
    if (_args == null)
      return new Object[] { event };

    Object []args = new Object[_args.length];

    CreationalContext env = _beanManager.createCreationalContext();
    for (int i = 0; i < _args.length; i++) {
      BeanArg arg = _args[i];

      if (arg != null)
	args[i] = arg.eval((ConfigContext) env);
      else
	args[i] = event;
    }

    return args;
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AnnotatedParameter<X> getEventParameter()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}