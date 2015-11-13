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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import com.caucho.v5.inject.Module;

/**
 * Factory to create instances of a bean.
 */
@Module
public final class InstanceImpl<T> implements Instance<T>, Serializable
{
  private transient CandiManager _cdiManager;
  private transient BeanManager _beanManager;
  
  private Type _type;
  private Annotation []_qualifiers;

  private transient long _version;
  private transient Set<Bean<?>> _beanSet;
  private transient ReferenceFactory<T> _factory;
  private transient InjectionPoint _injectionPoint;
  
  InstanceImpl(CandiManager cdiManager,
               BeanManager beanManager,
               Type type,
               Annotation []bindings,
               InjectionPoint injectionPoint)
  {
    _cdiManager = cdiManager;
    _beanManager = beanManager;
    
    _type = type;
    _qualifiers = bindings;

    Set<Bean<?>> beanSet = beanManager.getBeans(type, bindings);
    // XXX: beanManager.filter(beanSet, injectionPoint.getBean());
    _beanSet = beanSet;

    _version = cdiManager.getVersion();
    
    _injectionPoint = injectionPoint;
  }

  /**
   * Returns an instance of the selected bean
   */
  @Override
  public T get()
  {
    if (_factory == null) {
      Bean<?> bean = _cdiManager.resolve(_beanSet);

      if (bean != null) {
        BeanManagerBase beanManager = _cdiManager.getBeanManager(bean);
        
        _factory = (ReferenceFactory<T>) beanManager.getReferenceFactory(bean);
      }
      else
        throw _cdiManager.unsatisfiedException(_type, _qualifiers);
    }

    if (_factory != null) {
      return (T) _factory.create(null, null, _injectionPoint);
    }
    else
      return null;
  }

  /**
   * Restricts the instance given a set of bindings
   */
  @Override
  public Instance<T> select(Annotation ... bindings)
  {
    return new InstanceImpl<T>(_cdiManager, _beanManager, 
                               _type, bindings, _injectionPoint);
  }

  /**
   * Restricts the instance to a subtype and bindings.
   */
  @Override
  public <U extends T> Instance<U> select(Class<U> subtype,
                                          Annotation... bindings)
  {
    if (bindings == null || bindings.length == 0)
      bindings = _qualifiers;
    
    return new InstanceImpl<U>(_cdiManager, _beanManager, subtype, bindings, _injectionPoint);
  }

  /**
   * Restricts the instance to a subtype and bindings.
   */
  @Override
  public <U extends T> Instance<U> select(TypeLiteral<U> subtype,
                                          Annotation... bindings)
  {
    return new InstanceImpl<U>(_cdiManager,
                               _beanManager,
                               subtype.getType(), 
                               bindings, 
                               _injectionPoint);
  }

  @Override
  public Iterator<T> iterator()
  {
    return new InstanceIterator(_beanManager,
                                getBeanSet().iterator());
  }

  @Override
  public boolean isAmbiguous()
  {
    return getBeanSet().size() > 1;
  }

  @Override
  public boolean isUnsatisfied()
  {
    return getBeanSet().size() == 0;
  }

  @Override
  public void destroy(T instance) throws UnsupportedOperationException
  {
    if (_factory == null)
      return;

    _factory.getBean().destroy(instance, null);
  }

  private Set<Bean<?>> getBeanSet()
  {
    if (_version != _cdiManager.getVersion()) {
      _beanSet = _beanManager.getBeans(_type, _qualifiers);
      _version = _cdiManager.getVersion();
    }

    return _beanSet;
  }

  private Object readResolve()
  {
    CandiManager cdiManager = CandiManager.create();
    
    return new InstanceImpl(cdiManager,
                            cdiManager.getBeanManager(),
                            _type, 
                            _qualifiers,
                            _injectionPoint);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }

  static class InstanceIterator<T> implements Iterator<T> {
    private final BeanManager _beanManager;
    private final Iterator<Bean<T>> _beanIter;

    InstanceIterator(BeanManager manager, Iterator<Bean<T>> beanIter)
    {
      _beanManager = manager;
      _beanIter = beanIter;
    }

    public boolean hasNext()
    {
      return _beanIter.hasNext();
    }

    public T next()
    {
      Bean<T> bean = _beanIter.next();

      CreationalContext<?> env = _beanManager.createCreationalContext(bean);

      return (T) _beanManager.getReference(bean, bean.getBeanClass(), env);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
