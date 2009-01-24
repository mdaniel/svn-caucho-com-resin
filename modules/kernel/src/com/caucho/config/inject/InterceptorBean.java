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

import com.caucho.config.inject.SimpleBean;
import com.caucho.config.manager.InjectManager;
import com.caucho.util.*;
import com.caucho.webbeans.manager.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.ejb.*;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InterceptorBindingType;
import javax.inject.manager.Interceptor;
import javax.inject.manager.InterceptionType;

/**
 * InterceptorBean represents a Java interceptor
 */
public class InterceptorBean extends Interceptor
{
  private static final L10N L = new L10N(InterceptorBean.class);
  
  private Class _type;

  private SimpleBean _bean;

  private Method _aroundInvoke;
  private Method _postConstruct;
  private Method _preDestroy;
  private Method _prePassivate;
  private Method _postActivate;

  private HashSet<Annotation> _bindings
    = new HashSet<Annotation>();
  
  public InterceptorBean(InjectManager webBeans,
			 Class type)
  {
    super(webBeans);

    _type = type;

    _bean = new SimpleBean(_type);

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
   * Returns the bean's deployment type
   */
  public Class getDeploymentType()
  {
    return _bean.getDeploymentType();
  }

  /**
   * Returns the bean's bindings
   */
  public Set<Annotation> getBindingTypes()
  {
    return _bean.getBindingTypes();
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
   * Returns true if the bean is serializable
   */
  public boolean isSerializable()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  public Class<? extends Annotation> getScopeType()
  {
    return _bean.getScopeType();
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Class<?>> getTypes()
  {
    return _bean.getTypes();
  }

  //
  // lifecycle
  //

  /**
   * Create a new instance of the bean.
   */
  public Object create()
  {
    return _bean.create();
  }

  /**
   * Destroys a bean instance
   */
  public void destroy(Object instance)
  {
    _bean.destroy(instance);
  }

  //
  // interceptor
  //

  /**
   * Returns the bean's binding types
   */
  public Set<Annotation> getInterceptorBindingTypes()
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

  //
  // introspection
  //

  public void init()
  {
    _bean.init();
    
    introspect();
  }

  protected void introspect()
  {
    introspectBindingTypes(_type.getAnnotations());
    
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

  protected void introspectBindingTypes(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBindingType.class)) {
	_bindings.add(ann);
      }
    }
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
