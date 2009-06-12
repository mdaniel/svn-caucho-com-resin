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

package com.caucho.config.inject;

import com.caucho.config.ConfigContext;
import com.caucho.config.inject.HandleAware;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.ApplicationScope;

import java.io.Closeable;
import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.*;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
public class BeanFactory<T>
{
  private ManagedBeanImpl<T> _managedBean;
  
  private Object _value;

  private Set<Type> _types;
  private AnnotatedElementImpl _annotated;
  private Set<Annotation> _bindings;
  private Set<Annotation> _stereotypes;
  private String _name;
  private Class<? extends Annotation> _scopeType;

  public BeanFactory(ManagedBeanImpl managedBean)
  {
    _managedBean = managedBean;
  }

  public BeanFactory name(String name)
  {
    _name = name;
    
    return this;
  }

  public BeanFactory binding(Annotation ann)
  {
    if (_bindings == null)
      _bindings = new LinkedHashSet<Annotation>();

    _bindings.add(ann);
    
    return this;
  }

  public BeanFactory binding(Collection<Annotation> list)
  {
    if (_bindings == null)
      _bindings = new LinkedHashSet<Annotation>();

    _bindings.addAll(list);
    
    return this;
  }

  public BeanFactory stereotype(Annotation ann)
  {
    if (_stereotypes == null)
      _stereotypes = new LinkedHashSet<Annotation>();

    _stereotypes.add(ann);
    
    return this;
  }

  public BeanFactory stereotype(Collection<Annotation> list)
  {
    if (_stereotypes == null)
      _stereotypes = new LinkedHashSet<Annotation>();

    _stereotypes.addAll(list);
    
    return this;
  }

  public BeanFactory annotation(Annotation ann)
  {
    if (_annotated == null)
      _annotated = new AnnotatedElementImpl(_managedBean.getAnnotated());

    _annotated.addAnnotation(ann);
    
    return this;
  }

  public BeanFactory annotation(Collection<Annotation> list)
  {
    if (_annotated == null)
      _annotated = new AnnotatedElementImpl(_managedBean.getAnnotated());

    for (Annotation ann : list) {
      _annotated.addAnnotation(ann);
    }
    
    return this;
  }

  public BeanFactory scope(Class<? extends Annotation> scopeType)
  {
    _scopeType = scopeType;

    return this;
  }

  public BeanFactory type(Type ...types)
  {
    if (_types == null)
      _types = new LinkedHashSet<Type>();

    if (types != null) {
      for (Type type : types) {
	_types.add(type);
      }
    }
    
    return this;
  }

  public Bean singleton(Object value)
  {
    return new SingletonBean(_managedBean,
			     _types,
			     _annotated,
			     _bindings,
			     _stereotypes,
			     _scopeType,
			     _name,
			     value);
  }

  public Bean injection(InjectionTarget injection)
  {
    return new InjectionBean(_managedBean,
			     _types,
			     _annotated,
			     _bindings,
			     _stereotypes,
			     _scopeType,
			     _name,
			     injection);
  }

  public Bean bean()
  {
    return new ManagedSingletonBean(_managedBean,
				    _types,
				    _annotated,
				    _bindings,
				    _stereotypes,
				    _scopeType,
				    _name);
  }
}
