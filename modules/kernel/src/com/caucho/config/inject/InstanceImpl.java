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

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.TypeLiteral;

import com.caucho.inject.Module;

/**
 * Factory to create instances of a bean.
 */
@Module
public class InstanceImpl<T> implements Instance<T>
{
  private InjectManager _beanManager;
  private Type _type;
  private Annotation []_bindings;

  private long _version;
  private Set<Bean<?>> _beanSet;
  private Bean<T> _bean;

  InstanceImpl(InjectManager beanManager,
               Type type,
               Annotation []bindings)
  {
    _beanManager = beanManager;
    _type = type;
    _bindings = bindings;

    _beanSet = beanManager.getBeans(type, bindings);
    _version = beanManager.getVersion();
  }

  /**
   * Returns an instance of the selected bean
   */
  public T get()
  {
    if (_bean == null) {
      _bean = (Bean<T>) _beanManager.resolve(getBeanSet());
    }

    if (_bean == null)
      return null;

    CreationalContext<T> env = _beanManager.createCreationalContext(_bean);

    return (T) _beanManager.getReference(_bean, _bean.getBeanClass(), env);
  }

  /**
   * Restricts the instance given a set of bindings
   */
  public Instance<T> select(Annotation ... bindings)
  {
    return new InstanceImpl(_beanManager, _type, bindings);
  }

  /**
   * Restricts the instance to a subtype and bindings.
   */
  public <U extends T> Instance<U> select(Class<U> subtype,
                                          Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Restricts the instance to a subtype and bindings.
   */
  public <U extends T> Instance<U> select(TypeLiteral<U> subtype,
                                          Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Iterator<T> iterator()
  {
    return new InstanceIterator(_beanManager, getBeanSet().iterator());
  }

  public boolean isAmbiguous()
  {
    return getBeanSet().size() > 1;
  }

  public boolean isUnsatisfied()
  {
    return getBeanSet().size() == 0;
  }

  private Set<Bean<?>> getBeanSet()
  {
    if (_version != _beanManager.getVersion()) {
      _beanSet = _beanManager.getBeans(_type, _bindings);
      _version = _beanManager.getVersion();
    }

    return _beanSet;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }

  static class InstanceIterator<T> implements Iterator<T> {
    private final BeanManager _manager;
    private final Iterator<Bean<T>> _beanIter;

    InstanceIterator(BeanManager manager, Iterator<Bean<T>> beanIter)
    {
      _manager = manager;
      _beanIter = beanIter;
    }

    public boolean hasNext()
    {
      return _beanIter.hasNext();
    }

    public T next()
    {
      Bean<T> bean = _beanIter.next();

      CreationalContext<?> env = _manager.createCreationalContext(bean);

      return (T) _manager.getReference(bean, bean.getBeanClass(), env);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
