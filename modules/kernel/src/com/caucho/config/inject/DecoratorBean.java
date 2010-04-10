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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;

import com.caucho.config.ConfigException;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * DecoratorBean represents a Java decorator
 */
@Module
public class DecoratorBean<T> implements Decorator<T>
{
  private static final L10N L = new L10N(DecoratorBean.class);

  private Class<T> _type;

  private AbstractBean<T> _bean;

  private Field _delegateField;

  private HashSet<Annotation> _bindings
    = new HashSet<Annotation>();

  public DecoratorBean(InjectManager beanManager,
                       Class<T> type)
  {
    _type = type;

    _bean = beanManager.createManagedBean(type);

    init();
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding types
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _bindings;
  }

  /**
   * Returns the bean's deployment type
   */
  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _bean.getStereotypes();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  @Override
  public String getName()
  {
    return _bean.getName();
  }

  /**
   * Returns true if the bean can be null
   */
  @Override
  public boolean isNullable()
  {
    return false;
  }
  /**
   * Returns true if the bean can be null
   */
  @Override
  public boolean isAlternative()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return _bean.getScope();
  }

  /**
   * Returns the types that the bean implements
   */
  @Override
  public Set<Type> getTypes()
  {
    return _bean.getTypes();
  }

  /**
   * Returns the types for the decorated
   */
  @Override
  public Set<Type> getDecoratedTypes()
  {
    throw new UnsupportedOperationException();
  }

  //
  // lifecycle
  //

  @Override
  public T create(CreationalContext<T> creationalContext)
  {
    return (T) _bean.create(creationalContext);
  }

  /*
  public void destroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Returns the set of injection points, for validation.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _bean.getInjectionPoints();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _bean.getBeanClass();
  }

  /**
   * Create a new instance of the bean.
   */
  /*
  public Object create()
  {
    return _bean.create();
  }
  */

  /**
   * Destroys a bean instance
   */
  /*
  public void destroy(Object instance)
  {
  }
  */

  //
  // decorator
  //

  /**
   * Returns the type of the delegated object
   */
  @Override
  public Class<?> getDelegateType()
  {
    if (_delegateField != null)
      return _delegateField.getType();
    else
      return null;
  }

  /**
   * Returns the bindings for the delegated object
   */
  @Override
  public Set<Annotation> getDelegateQualifiers()
  {
    return _bindings;
  }

  /**
   * Sets the delegate for an object
   */
  public void setDelegate(Object instance,
                          Object delegate)
  {
    if (! _type.isAssignableFrom(instance.getClass())) {
      throw new ConfigException(L.l("{0} is an invalid @Decorator instance because it does not extend the implementation class {1}",
                                    instance.getClass().getName(),
                                    _type.getName()));
    }

    if (! getDelegateType().isAssignableFrom(delegate.getClass())) {
      throw new ConfigException(L.l("{0} is an invalid @Decorator delegate because it does not implement the delegate {1}",
                                    delegate.getClass().getName(),
                                    getDelegateType()));
    }

    try {
      _delegateField.set(instance, delegate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //
  // introspection
  //

  public void init()
  {
    // _bean.init();

    introspect();

    if (_delegateField == null)
      throw new ConfigException(L.l("{0} is missing a @Delegate field.  All @Decorators need a @Delegate field for a delegate injection",
                                    _type.getName()));
  }

  protected void introspect()
  {
    introspectDelegateField();
  }

  protected void introspectDelegateField()
  {
    if (_delegateField == null) {

      for (Field field : _type.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()))
          continue;

        if (! field.isAnnotationPresent(Delegate.class))
          continue;

        Class<?> fieldType = field.getType();

        /*
        if (! fieldType.isInterface()) {
          throw new ConfigException(L.l("{0}.{1} is an invalid @Delegate field because its type '{2}' is not an interface",
                                        _type.getName(),
                                        field.getName(),
                                        fieldType.getName()));
        }
        */

        for (Class<?> iface : _type.getInterfaces()) {
          if (Serializable.class.equals(iface))
            continue;
          
          if (! iface.isAssignableFrom(fieldType)) {
            throw new ConfigException(L.l("{0}.{1} is an invalid @Delegate field because {2} does not implement the API {3}",
                                          _type.getName(),
                                          field.getName(),
                                          fieldType.getName(),
                                          iface.getName()));
          }
        }

        if (_delegateField != null) {
          throw new ConfigException(L.l("{0}: @Decorator field '{1}' conflicts with earlier field '{2}'.  A decorator must have exactly one delegate field.",
                                        _type.getName(),
                                        _delegateField,
                                        field.getName()));
        }

        field.setAccessible(true);
        _delegateField = field;

        introspectBindingTypes(field.getAnnotations());
      }
    }
  }

  protected void introspectBindingTypes(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        _bindings.add(ann);
      }
    }

    if (_bindings.size() == 0)
      _bindings.add(CurrentLiteral.CURRENT);
  }

  /**
   * Instantiate the bean.
   */
  public T instantiate()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Inject the bean.
   */
  /*
  public void inject(T instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Call post-construct
   */
  /*
  public void postConstruct(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Call pre-destroy
   */
  /*
  public void preDestroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Call destroy
   */
  @Override
  public void destroy(T instance, CreationalContext<T> env)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getSimpleName());

    if (_delegateField != null)
      sb.append(",").append(_delegateField.getType().getSimpleName());

    sb.append("]");

    return sb.toString();
  }
}
