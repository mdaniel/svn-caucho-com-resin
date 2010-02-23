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

import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.ejb.*;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * InterceptorBean represents a Java interceptor
 */
public class InterceptorBean<X> implements Interceptor<X>
{
  private static final L10N L = new L10N(InterceptorBean.class);

  private final InjectManager _beanManager;

  private Class _type;

  private ManagedBeanImpl _bean;

  private Method _aroundInvoke;
  private Method _postConstruct;
  private Method _preDestroy;
  private Method _prePassivate;
  private Method _postActivate;

  private HashSet<Annotation> _bindings
    = new HashSet<Annotation>();

  public InterceptorBean(InjectManager beanManager,
                         Class type)
  {
    _beanManager = beanManager;

    _type = type;

    _bean = beanManager.createManagedBean(_type);

    init();
  }

  public InterceptorBean(Class type)
  {
    this(InjectManager.create(), type);
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's bindings
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _bean.getQualifiers();
  }

  /**
   * Returns the bean's stereotypes
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
  public String getName()
  {
    return _bean.getName();
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isAlternative()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  public Class<? extends Annotation> getScope()
  {
    return _bean.getScope();
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Type> getTypes()
  {
    return _bean.getTypes();
  }

  public Class getBeanClass()
  {
    return _bean.getBeanClass();
  }

  //
  // lifecycle
  //

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
  public void destroy(X instance, CreationalContext<X> env)
  {
    _bean.destroy(instance, env);
  }

  //
  // interceptor
  //

  /**
   * Returns the bean's binding types
   */
  public Set<Annotation> getInterceptorBindings()
  {
    return _bindings;
  }

  /**
   * Returns the bean's deployment type
   */
  public Method getMethod(InterceptionType type)
  {
    switch (type) {
    case AROUND_INVOKE:
      return _aroundInvoke;

    case POST_CONSTRUCT:
      return _postConstruct;

    case PRE_DESTROY:
      return _preDestroy;

    case PRE_PASSIVATE:
      return _prePassivate;

    case POST_ACTIVATE:
      return _postActivate;

    default:
      return null;
    }
  }

  /**
   * Returns the bean's deployment type
   */
  public boolean intercepts(InterceptionType type)
  {
    return getMethod(type) != null;
  }

  public Object create(CreationalContext creationalContext)
  {
    return _bean.create(creationalContext);
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _bean.getInjectionPoints();
  }

  //
  // introspection
  //

  public void init()
  {
    // _bean.init();

    introspect();
  }

  protected void introspect()
  {
    introspectQualifiers(_type.getAnnotations());

    introspectMethods();
  }

  protected void introspectMethods()
  {
    for (Method method : _type.getMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (method.isAnnotationPresent(AroundInvoke.class))
        _aroundInvoke = method;

      if (method.isAnnotationPresent(PostConstruct.class))
        _postConstruct = method;

      if (method.isAnnotationPresent(PreDestroy.class))
        _preDestroy = method;

      if (method.isAnnotationPresent(PrePassivate.class))
        _prePassivate = method;

      if (method.isAnnotationPresent(PostActivate.class))
        _postActivate = method;
    }
  }

  protected void introspectQualifiers(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
        _bindings.add(ann);
      }
    }
  }

  /**
   * Invokes the callback
   */
  public Object intercept(InterceptionType type,
                          X instance,
                          InvocationContext ctx)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Instantiate the bean.
   */
  public Object instantiate()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Inject the bean.
   */
  public void inject(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Call post-construct
   */
  public void postConstruct(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Call pre-destroy
   */
  public void preDestroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Call destroy
   */
  /*
  public void destroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof InterceptorBean))
      return false;

    InterceptorBean bean = (InterceptorBean) o;

    return _type.equals(bean._type);
  }

  @Override
  public int hashCode()
  {
    return _type.hashCode();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getSimpleName());

    sb.append("]");

    return sb.toString();
  }
}
