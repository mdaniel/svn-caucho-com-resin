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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;
import javax.interceptor.Interceptor;

import com.caucho.config.ConfigException;
import com.caucho.config.bytecode.DecoratorAdapter;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * DecoratorBean represents a Java decorator
 */
@Module
public class DecoratorBean<T> implements Decorator<T>
{
  private static final L10N L = new L10N(DecoratorBean.class);

  private Class<T> _selfType;
  private Class<T> _adapterType;

  private Bean<T> _bean;
  
  private InjectionPoint _delegateInjectionPoint;

  private Field _delegateField;
  private Method _delegateMethod;
  private Constructor<?> _delegateConstructor;
  
  private Set<Type> _typeSet;

  private HashSet<Annotation> _qualifiers
    = new HashSet<Annotation>();

  public DecoratorBean(InjectManager beanManager,
                       Class<T> type)
  {
    _selfType = type;
    
    type = DecoratorAdapter.create(type);
    
    _adapterType = type;

    _bean = beanManager.createManagedBean(type);

    // init();
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
    if (_delegateInjectionPoint != null)
      return _delegateInjectionPoint.getQualifiers();
    else
      return _qualifiers;
    //          return _bean.getQualifiers();
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
    return true;
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
    return _typeSet;
  }

  //
  // lifecycle
  //

  @Override
  public T create(CreationalContext<T> creationalContext)
  {
    return _bean.create(creationalContext);
  }

  /**
   * Returns the set of injection points, for validation.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    if (_delegateInjectionPoint != null)
      bind();
    
    return _bean.getInjectionPoints();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _bean.getBeanClass();
  }

  //
  // decorator
  //

  /**
   * Returns the type of the delegated object
   */
  @Override
  public Type getDelegateType()
  {
    if (_delegateInjectionPoint == null)
      bind();
    
    return _delegateInjectionPoint.getType();
  }

  /**
   * Returns the bindings for the delegated object
   */
  @Override
  public Set<Annotation> getDelegateQualifiers()
  {
    if (_delegateInjectionPoint != null)
      return _delegateInjectionPoint.getQualifiers();
    else
      return _qualifiers;
  }
  
  public InjectionPoint getDelegateInjectionPoint()
  {
    if (_delegateInjectionPoint != null)
      bind();
    
    return _delegateInjectionPoint;
  }

  public void bind()
  {
    if (_delegateInjectionPoint != null)
      return;
    // _bean.init();

    introspect();

    if (_delegateField == null
        && _delegateMethod == null
        && _delegateConstructor == null)
      throw new ConfigException(L.l("{0} is missing a @Delegate field.  All @Decorators need a @Delegate field for a delegate injection",
                                    _adapterType.getName()));
    
    if (_adapterType.isAnnotationPresent(Interceptor.class))
      throw new ConfigException(L.l("{0} is an invalid @Delegate because it has an @Interceptor annotation.",
                                    _adapterType.getName()));
    
    if (Modifier.isFinal(_adapterType.getModifiers())) {
      throw new ConfigException(L.l("{0} is an invalid @Decorator because it is a final class.",
                                    _adapterType.getName()));
    }
  }

  protected void introspect()
  {
    // introspectDelegateField();

    for (InjectionPoint ip : _bean.getInjectionPoints()) {
      if (ip.isDelegate()) {
        if (_delegateInjectionPoint != null)
          throw new ConfigException(L.l("{0}: @Decorator field '{1}' conflicts with earlier field '{2}'."
                                        + " A decorator must have exactly one delegate field.",
                                        ip.getBean().getBeanClass().getName(),
                                        ip.getMember().getName(),
                                        _delegateInjectionPoint.getMember().getName()));
        
        _delegateInjectionPoint = ip;
      }
    }

    if (_delegateInjectionPoint != null) {
      if (_delegateInjectionPoint.getMember() instanceof Field) {
        _delegateField = (Field) _delegateInjectionPoint.getMember();
        _delegateField.setAccessible(true);
      }
      else if (_delegateInjectionPoint.getMember() instanceof Method) {
        _delegateMethod = (Method) _delegateInjectionPoint.getMember();
        _delegateMethod.setAccessible(true);
      }
      else if (_delegateInjectionPoint.getMember() instanceof Constructor<?>) {
        _delegateConstructor = (Constructor<?>) _delegateInjectionPoint.getMember();
        _delegateConstructor.setAccessible(true);
      }
      
      InjectManager manager = InjectManager.getCurrent();
      
      BaseType selfType = manager.createTargetBaseType(_selfType);
      BaseType delegateType 
        = manager.createSourceBaseType(_delegateInjectionPoint.getType());
            
      _typeSet = new LinkedHashSet<Type>();
      
      for (Type type : selfType.getTypeClosure(manager)) {
        BaseType baseType = manager.createSourceBaseType(type);

        if (! baseType.getRawClass().isInterface())
          continue;
        if (baseType.getRawClass().equals(Serializable.class))
          continue;

        // ioc/0i5g, ioc/0i3r
        if (baseType.isAssignableFrom(delegateType)) {
          _typeSet.add(type);
          
          for (Method method : baseType.getRawClass().getMethods()) {
            Method implMethod = AnnotatedTypeUtil.findMethod(selfType.getRawClass().getMethods(), method);
            
            if (implMethod != null && Modifier.isFinal(implMethod.getModifiers())) {
              throw new ConfigException(L.l("'{0}.{1}' is an invalid decorator method because it is final.",
                                            implMethod.getDeclaringClass().getName(),
                                            implMethod.getName()));
            }
          }
        }
        else if (isDeclaredInterface(selfType, baseType)){
          // ioc/0i5a
          // only types declared directly are errors
          throw new ConfigException(L.l("{0}: '{1}' is a Decorator type not implemented by the delegate {2}",
                                        _selfType.getName(), baseType, delegateType));
        }
        else {
          // ioc/0i3r
          _typeSet.add(type);
        }
      }
    }
  }
  
  private boolean isDeclaredInterface(BaseType selfType, BaseType baseType)
  {
    for (Class<?> iface : selfType.getRawClass().getInterfaces()) {
      if (iface.equals(baseType.getRawClass()))
        return true;
      
    }
    
    return false;
  }

  protected void introspectBindingTypes(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        _qualifiers.add(ann);
      }
    }

    if (_qualifiers.size() == 0)
      _qualifiers.add(DefaultLiteral.DEFAULT);
  }

  /**
   * Instantiate the bean.
   */
  public T instantiate()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Call destroy
   */
  @Override
  public void destroy(T instance, CreationalContext<T> env)
  {
    _bean.destroy(instance, env);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_adapterType.getSimpleName());

    if (_delegateField != null)
      sb.append(",").append(_delegateField.getType().getSimpleName());

    sb.append("]");

    return sb.toString();
  }
}
