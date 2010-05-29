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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.config.bytecode.ScopeProxy;
import com.caucho.config.program.BeanArg;

/**
 * Internal implementation for a producer Bean
 */
public class ObserverMethodImpl<X, T> extends AbstractObserverMethod<T> {
  private InjectManager _beanManager;

  private Bean<X> _bean;
  private AnnotatedMethod<X> _method;

  private Type _type;
  private Set<Annotation> _qualifiers;

  private BeanArg<X> []_args;
  private boolean _isIfExists;
  
  private TransactionPhase _transactionPhase = TransactionPhase.IN_PROGRESS;

  ObserverMethodImpl(InjectManager beanManager,
                     Bean<X> bean,
                     AnnotatedMethod<X> method,
                     Type type,
                     Set<Annotation> qualifiers)
  {
    _beanManager = beanManager;
    _bean = bean;
    _method = method;
    _method.getJavaMember().setAccessible(true);
    _type = type;
    _qualifiers = qualifiers;

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
  
  @Override
  public Class<X> getBeanClass()
  {
    return (Class<X>) _bean.getBeanClass();
  }

  /**
   * Returns the observed event type
   */
  @Override
  public Type getObservedType()
  {
    return _type;
  }

  /**
   * Returns the observed event bindings
   */
  @Override
  public Set<Annotation> getObservedQualifiers()
  {
    return _qualifiers;
  }

  private void introspect()
  {
    List<AnnotatedParameter<X>> parameters = _method.getParameters();

    /*
    if (parameters.size() == 1) {
      if (parameters.get(0).isAnnotationPresent(IfExists.class)) {
        _isIfExists = true;
      }

      return;
    }
    */

    _args = new BeanArg[parameters.size()];

    for (int i = 0; i < _args.length; i++) {
      AnnotatedParameter<?> param = parameters.get(i);

      Observes observes = param.getAnnotation(Observes.class);
      
      if (observes != null) {
        _isIfExists = observes.notifyObserver() == Reception.IF_EXISTS;
        _transactionPhase = observes.during();
      }
      else {
        InjectionPoint ip = new InjectionPointImpl(_beanManager,
                                                   _bean,
                                                   param);

        _args[i] = new BeanArg<X>(_beanManager,
                                  param.getBaseType(),
                                  _beanManager.getQualifiers(param.getAnnotations()),
                                  ip);
      }
    }
  }

  /**
   * Send the event notification.
   */
  @Override
  public void notify(T event)
  {
    X instance;
    
    CreationalContextImpl<X> env = null;
    OwnerCreationalContext<X> argEnv = null;

    if (_isIfExists) {
      instance = getExistsInstance();
      
      if (instance == null)
        return;
    }
    else {
      env = new OwnerCreationalContext<X>(getParentBean());
      
      instance = getParentBean().create(env);
    }
    
    if (_args != null && _args.length > 1)
      argEnv = new OwnerCreationalContext<X>(null);
    
    Method method = _method.getJavaMember();

    try {
      Object object;
      
      if (instance instanceof ScopeProxy) {
        object = ((ScopeProxy) instance).__caucho_getDelegate();
      }
      else
        object = instance;

      method.invoke(object, getEventArguments(event, argEnv));
    } catch (IllegalArgumentException e) {
      String loc = (method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + ": ");

      throw new ObserverException(loc + e.toString(), e.getCause());
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      Throwable exn = e.getCause();
      
      if (exn instanceof RuntimeException)
        throw (RuntimeException) exn;
      
      String loc = (method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + ": ");

      throw new ObserverException(loc + exn.toString(), exn.getCause());
    } catch (Exception e) {
      String loc = (method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + ": ");

      throw new ObserverException(loc + e.toString(), e.getCause());
    } finally {
      if (argEnv != null)
        argEnv.release();
      
      if (env != null && getParentBean().getScope() == Dependent.class)
        getParentBean().destroy(instance, env);
    }
  }

  protected X getExistsInstance()
  {
    Bean<X> bean = getParentBean();
    
    Class<? extends Annotation>scopeType = bean.getScope();
    Context context = _beanManager.getContext(scopeType);

    if (context != null)
      return (X) context.get(bean);
    else
      return null;
  }

  protected Object[] getEventArguments(Object event, 
                                       CreationalContextImpl<?> parentEnv)
  {
    if (_args == null)
      return new Object[] { event };

    Object []args = new Object[_args.length];

    for (int i = 0; i < _args.length; i++) {
      BeanArg<X> arg = _args[i];

      if (arg != null)
        args[i] = arg.eval((CreationalContextImpl) parentEnv);
      else
        args[i] = event;
    }

    return args;
  }

  @Override
  public Reception getReception()
  {
    if (_isIfExists)
      return Reception.IF_EXISTS;
    else
      return Reception.ALWAYS;
  }

  @Override
  public TransactionPhase getTransactionPhase()
  {
    return _transactionPhase;
  }

  /*
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AnnotatedParameter<X> getEventParameter()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
