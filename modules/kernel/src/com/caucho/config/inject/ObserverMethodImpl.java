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

import javax.enterprise.inject.spi.*;;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.event.Observer;
import javax.enterprise.inject.InjectionException;

/**
 * Internal implementation for a producer Bean
 */
public class ObserverMethodImpl<X,T> implements ObserverMethod<X,T>
{
  private BeanManager _beanManager;
  
  private Bean<X> _bean;
  private AnnotatedMethod<X> _method;

  private Type _type;
  private Set<Annotation> _bindings;
  
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

  public void notify(T event)
  {
    getListener().notify(event);
    /*
    Class<X> type = null;
    
    notify(_beanManager.getReference(getParentBean(), type), event);
    */
  }
  
  /**
   * Sends an event
   */
  public void notify(X instance, T event)
  {
    Method method = _method.getJavaMember();
    
    try {
      System.out.println("INV: " + method);
      method.invoke(instance, event);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      String loc = (method.getDeclaringClass().getSimpleName()
		    + "." + method.getName() + ": ");
      
      throw new InjectionException(loc + e.getMessage(), e.getCause());
    } catch (Exception e) {
      String loc = (method.getDeclaringClass().getSimpleName()
		    + "." + method.getName() + ": ");
      
      throw new InjectionException(loc + e.getMessage(), e.getCause());
    }
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public AnnotatedParameter<X> getEventParameter()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Listener<T> getListener()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setListener(Listener<T> listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
