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

import javax.inject.*;
import javax.inject.manager.*;
import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Set;

/**
 * Factory to create instances of a bean.
 */
public class InstanceImpl<T> implements Instance<T>
{
  private InjectManager _beanManager;
  private Type _type;
  private Annotation []_bindings;

  private Set<Bean<?>> _beanSet;
  private Bean _bean;

  InstanceImpl(InjectManager beanManager,
	       Type type,
	       Annotation []bindings)
  {
    _beanManager = beanManager;
    _type = type;
    _bindings = bindings;

    _beanSet = beanManager.getBeans(type, bindings);

    if (_beanSet.size() == 1) {
      for (Bean bean : _beanSet) {
	_bean = bean;
      }
    }
  }
  
  /**
   * Returns an instance of the selected bean
   */
  public T get()
  {
    return (T) _beanManager.getReference(_bean);
  }

  /**
   * Restricts the instance given a set of bindings
   */
  public Instance<T> select(Annotation ... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
