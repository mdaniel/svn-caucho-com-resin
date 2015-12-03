/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.caucho.config.gen.InterceptorException;
import com.caucho.config.reflect.AnnotatedTypeUtil;

/**
 * InterceptorBean represents a Java interceptor
 */
public class InterceptorRuntimeBean<X> extends AbstractInterceptorBean<X>
{
  private Class<X> _type;
  
  private InterceptorRuntimeBean<X> _parent;
  private InterceptorRuntimeBean<X> _child;

  private Method _aroundInvoke;
  
  private Method _postConstruct;
  private boolean _isPostConstructOverride;
  
  private Method _preDestroy;
  private Method _prePassivate;
  private Method _postActivate;

  private HashSet<Annotation> _qualifiers
    = new HashSet<Annotation>();

  public InterceptorRuntimeBean(InterceptorRuntimeBean<X> child,
                                Class<X> type)
  {
    _type = type;
    _child = child;
    
    introspectMethods(type);
    
    Class<?> parentClass = findParentInterceptor(type.getSuperclass());
    
    if (parentClass != null) {
      _parent = new InterceptorRuntimeBean(this, parentClass);
    }
    // introspectOverrideMethods(type);
  }
  
  //
  // metadata for the bean
  //

  public Bean<X> getBean()
  {
    if (_child != null)
      return _child.getBean();
    else
      return this;
  }
  
  public Class<?> getType()
  {
    return _type;
  }
  
  public InterceptorRuntimeBean<?> getParent()
  {
    return _parent;
  }
  
  public boolean isAllowParent(InterceptionType type)
  {
    if (_parent == null)
      return false;
   
    return true;
  }
  
  /**
   * Returns the bean's bindings
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  /**
   * Returns the bean's stereotypes
   */
  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return null;
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  @Override
  public String getName()
  {
    return null;
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
    return Dependent.class;
  }

  /**
   * Returns the types that the bean implements
   */
  @Override
  public Set<Type> getTypes()
  {
    return null;
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _type;
  }

  //
  // lifecycle
  //

  @Override
  public X create(CreationalContext<X> cxt)
  {
    CreationalContextImpl<?> env = (CreationalContextImpl<?>) cxt;
    
    X value = (X) CreationalContextImpl.findAny(env, getBean().getBeanClass());
    
    if (value == null) {
      throw new NullPointerException(getBean() + "\n  " + env + "\n  " + toString());
    }
    
    return value;
  }

  /**
   * Destroys a bean instance
   */
  @Override
  public void destroy(X instance, CreationalContext<X> env)
  {
  }

  //
  // interceptor
  //

  /**
   * Returns the bean's binding types
   */
  @Override
  public Set<Annotation> getInterceptorBindings()
  {
    return _qualifiers;
  }

  /**
   * Returns the bean's deployment type
   */
  @Override
  public boolean intercepts(InterceptionType type)
  {
    return getMethod(type) != null;
  }

  /**
   * Returns the bean's deployment type
   */
  // public required for QA
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
   * Returns the set of injection points, for validation.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return new HashSet<InjectionPoint>();
  }

  //
  // introspection
  //

  private void introspectMethods(Class<?> cl)
  {
    if (cl == null)
      return;
    
    Class<?> childClass = null;
    
    if (_child != null)
      childClass = _child.getType();
      
    for (Method method : cl.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;
      
      AnnotatedMethod<?> annMethod = AnnotatedOverrideMap.getMethod(method);

      // XXX:
      if (isAnnotationPresent(method, annMethod, AroundInvoke.class)) {
        Method childMethod 
        = AnnotatedTypeUtil.findDeclaredMethod(childClass, method);

        if (childMethod == null
            || Modifier.isPrivate(childMethod.getModifiers())) {
          // ioc/0cb1
          _aroundInvoke = method;
          method.setAccessible(true);
        }
      }

      if (method.isAnnotationPresent(PostConstruct.class)) {
        Method childMethod 
          = AnnotatedTypeUtil.findDeclaredMethod(childClass, method);

        if (_child != null && _child._isPostConstructOverride) {
          _isPostConstructOverride = true;
        }
        else if (childMethod == null) {
          _postConstruct = method;
          method.setAccessible(true);
        }
        else if (Modifier.isPrivate(childMethod.getModifiers())) {
          _postConstruct = method;
          method.setAccessible(true);
        }
        else if (_child != null) {
          _isPostConstructOverride = true;
        }
      }

      if (method.isAnnotationPresent(PreDestroy.class)
          && (_child == null
              || ! isMethodMatch(_child._preDestroy, method))) {
        _preDestroy = method;
        method.setAccessible(true);
      }

      if (method.isAnnotationPresent(PrePassivate.class)
          && (_child == null
              || ! isMethodMatch(_child._prePassivate, method))) {
        _prePassivate = method;
        method.setAccessible(true);
      }

      if (method.isAnnotationPresent(PostActivate.class)
          && (_child == null
              || ! isMethodMatch(_child._postActivate, method))) {
        _postActivate = method;
        method.setAccessible(true);
      }
    }
  }
  
  private boolean isAnnotationPresent(Method method,
                                      AnnotatedMethod<?> annMethod,
                                      Class<? extends Annotation> annType)
  {
    if (method.isAnnotationPresent(annType))
      return true;
    else if (annMethod != null && annMethod.isAnnotationPresent(annType))
      return true;
    else
      return false;
  }

  private boolean isMethodMatch(Method a, Method b)
  {
    if (a == b)
      return true;
    else if (a == null || b == null)
      return false;
    else if (! AnnotatedTypeUtil.isMatch(a, b))
      return false;
    else if (Modifier.isPrivate(a.getModifiers())
        || Modifier.isPrivate(b.getModifiers())) {
      return false;
    }
    else
      return true;
  }

  private Class<?> findParentInterceptor(Class<?> cl)
  {
    if (cl == null)
      return null;
    
    for (Method method : cl.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (method.isAnnotationPresent(AroundInvoke.class))
        return cl;

      if (method.isAnnotationPresent(PostConstruct.class))
        return cl;

      if (method.isAnnotationPresent(PreDestroy.class))
        return cl;

      if (method.isAnnotationPresent(PrePassivate.class))
        return cl;

      if (method.isAnnotationPresent(PostActivate.class))
        return cl;
    }
    
    return findParentInterceptor(cl.getSuperclass());
  }

  /**
   * Invokes the callback
   */
  @Override
  public Object intercept(InterceptionType type,
                          X instance,
                          InvocationContext ctx)
  {
    try {
      switch (type) {
      case AROUND_INVOKE:
        if (_aroundInvoke == null)
          throw new NullPointerException(this + " null AROUND_INVOKE");
        else if (instance == null)
          throw new NullPointerException(this + " NULL instance " + _aroundInvoke);
        
        return _aroundInvoke.invoke(instance, ctx);

      case POST_CONSTRUCT:
        return _postConstruct.invoke(instance, ctx);

      case PRE_DESTROY:
        return _preDestroy.invoke(instance, ctx);

      case PRE_PASSIVATE:
        return _prePassivate.invoke(instance, ctx);

      case POST_ACTIVATE:
        return _postActivate.invoke(instance, ctx);

      default:
        throw new UnsupportedOperationException(toString());
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new InterceptorException(e.getCause());
    } catch (Exception e) {
      throw new InterceptorException(e);
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (o == null)
      return false;
    
    if (getClass() != o.getClass())
      return false;

    InterceptorRuntimeBean<?> bean = (InterceptorRuntimeBean<?>) o;

    if (! _type.equals(bean._type))
      return false;
    
    if (_child == bean._child)
      return true;
    else if (_child == null || ! _child.equals(bean._child))
      return false;
    else
      return true;
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
