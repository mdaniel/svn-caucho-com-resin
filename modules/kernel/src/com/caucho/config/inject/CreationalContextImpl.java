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
 */

package com.caucho.config.inject;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.inject.Module;

/**
 * Stack of partially constructed beans.
 */
@Module
public final class CreationalContextImpl<T> implements CreationalContext<T> {
  private final CreationalContextImpl<?> _top;
  private final CreationalContextImpl<?> _parent; // parent in the creation chain
  private CreationalContextImpl<?> _next; // next in the dependent chain
  
  private final Contextual<T> _bean;
  private InjectionPoint _injectionPoint;
  private T _value;
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContext<?> parent,
                               InjectionPoint ij)
  {
    _bean = bean;
    _injectionPoint = ij;
    
    if (parent instanceof CreationalContextImpl<?>) {
      CreationalContextImpl<?> parentEnv = (CreationalContextImpl<?>) parent;
      
      _parent = parentEnv;
      _top = parentEnv._top;
      _next = _top._next;
      _top._next = this;
    }
    else {
      _top = this;
      _parent = null;
      // _next = next;
    }
  }
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContextImpl<?> parentEnv,
                               InjectionPoint ij)
  {
    _bean = bean;
    _injectionPoint = ij;
    
    _parent = parentEnv;
    
    if (parentEnv != null) {
      _top = parentEnv._top;
      _next = _top._next;
      _top._next = this;
    }
    else
      _top = this;
  }
  
  public CreationalContextImpl()
  {
    this(null, (CreationalContextImpl<?>) null, null);
  }
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContext<?> next)
  {
    this(bean, next, null);
  }
  
  public CreationalContextImpl(Contextual<T> bean,
                               CreationalContextImpl<?> next)
  {
    this(bean, next, null);
  }
  
  public CreationalContextImpl(Contextual<T> bean)
  {
    this(bean, null, null);
  }
  
  public static CreationalContextImpl<Object> create()
  {
    return new CreationalContextImpl<Object>();
  }
  
  public boolean isTop()
  {
    return this == _top;
  }
  
  public <X> X get(Contextual<X> bean)
  {
    return find(this, bean);    
  }
  
  @SuppressWarnings("unchecked")
  public
  static <X> X find(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    for (; ptr != null; ptr = ptr._parent) {
      Contextual<?> testBean = ptr._bean;
      
      if (testBean == bean && ptr._value != null) {
        return (X) ptr._value;
      }
    }
    
    return null;
  }
  
  /**
   * Find any bean, for disposers.
   */
  public <X> X getAny(Contextual<X> bean)
  {
    return findAny(_top, bean);    
  }
  
  @SuppressWarnings("unchecked")
  static <X> X findAny(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    for (; ptr != null; ptr = ptr._next) {
      Contextual<?> testBean = ptr._bean;
      
      if (testBean == bean) {
        return (X) ptr._value;
      }
    }
    
    return null;
  }
  
  public static Object findByName(CreationalContextImpl<?> ptr, String name)
  {
    for (; ptr != null; ptr = ptr._parent) {
      Contextual<?> testBean = ptr._bean;
      
      if (! (testBean instanceof Bean<?>))
        continue;
      
      Bean<?> bean = (Bean<?>) testBean;

      if (name.equals(bean.getName())) {
        return ptr._value;
      }
    }
    
    return null;
  }
  
  public void setInjectionPoint(InjectionPoint ip)
  {
    _injectionPoint = ip;
  }
  
  public InjectionPoint getInjectionPoint()
  {
    CreationalContext<?> ptr = this; 
    
    while (ptr != null) {
      if (ptr instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> env = (CreationalContextImpl<?>) ptr;
        
        if (env._injectionPoint != null)
          return env._injectionPoint;
        
        ptr = env._parent;
      }
      else
        ptr = null;
    }
    
    return null;
  }

  @Override
  public void push(T value)
  {
    _value = value;
  }
  
  public void setInjectionTarget(InjectionTarget<T> injectionTarget)
  {
  }

  @Override
  public void release()
  {
    CreationalContextImpl<?> next = _next;
    _next = null;
    _value = null;
    
    if (next != null)
      next.releaseImpl();
  }
  
  void releaseImpl()
  {
    T value = _value;
    _value = null;
    
    if (value != null)
      _bean.destroy(value, this);
  }
  
  void postConstruct()
  {
    /*
    if (_next != null)
      _next.postConstruct();
    
    T value = _value;
    // _value = null;
    
    if (value != null && _injectionTarget != null) {
      _injectionTarget.postConstruct(value);
      _injectionTarget = null;
    }
    */
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "," + _value + ",parent=" + _parent + "]";
  }
}
