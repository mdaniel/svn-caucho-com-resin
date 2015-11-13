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
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.L10N;

/**
 */
@Module
public class InjectionPointImpl<T> implements InjectionPoint, Serializable
{
  private static final L10N L = new L10N(InjectionPointImpl.class);
  
  private final CandiManager _manager;
  
  private InjectionTargetBuilder<T> _target;
  private final Bean<T> _bean;
  private final Annotated _annotated;
  private final Member _member;
  private final HashSet<Annotation> _qualifiers = new HashSet<Annotation>();
  private Type _type;

  InjectionPointImpl(CandiManager manager,
                     Bean<T> bean,
                     AnnotatedField<T> field)
  {
    this(manager, bean, field, field.getJavaMember(),
         field.getBaseType());
  }

  InjectionPointImpl(CandiManager manager,
                     InjectionTargetBuilder<T> target,
                     AnnotatedField<T> field)
  {
    this(manager, target.getBean(), field, field.getJavaMember(),
         field.getBaseType());
    
    _target = target;
  }

  public InjectionPointImpl(CandiManager manager,
                     Bean<T> bean,
                     AnnotatedParameter<?> param)
  {
    this(manager, bean, param, 
         param.getDeclaringCallable().getJavaMember(),
         param.getBaseType());
  }

  InjectionPointImpl(CandiManager manager,
                     InjectionTargetBuilder<T> target,
                     AnnotatedParameter<?> param)
  {
    this(manager, target.getBean(), 
         param, 
         param.getDeclaringCallable().getJavaMember(),
         param.getBaseType());
    
    _target = target;
  }

  public InjectionPointImpl(CandiManager manager,
                            Bean<T> bean,
                            Annotated annotated,
                            Member member,
                            Type type)
  {
    _manager = manager;
    _bean = bean;
    _annotated = annotated;
    _member = member;
    _type = type;
    
    boolean isQualifier = false;
    
    Iterable<Annotation> annIter = annotated.getAnnotations();
    
    if (annIter == null) {
      throw new IllegalArgumentException(L.l("getAnnotations() returns null for {0}", annotated));
    }

    for (Annotation ann : annIter) {
      if (_manager.isQualifier(ann.annotationType())) {
        _qualifiers.add(ann);
        
        // ioc/5006
        /*
        if (! Named.class.equals(ann.annotationType()))
          isQualifier = true;
          */
        isQualifier = true;
      }
    }

    if (! isQualifier) {
      _qualifiers.add(DefaultLiteral.DEFAULT);
    }
  }

  /**
   * Returns the declared type of the injection point, e.g. an
   * injected field's type.
   */
  @Override
  public Type getType()
  {
    if (_type != null)
      return _type;
    else
      return _annotated.getBaseType();
  }

  /**
   * Returns the declared bindings on the injection point.
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  /**
   * Returns the owning bean for the injection point.
   */
  @Override
  public Bean<?> getBean()
  {
    if (_bean != null)
      return _bean;
    else if (_target != null)
      return _target.getBean();
    else {
      return null;
    }
  }

  /**
   * Returns the Field for field injection, the Method for method injection,
   * and Constructor for constructor injection.
   */
  @Override
  public Member getMember()
  {
    return _member;
  }

  /**
   * Returns all annotations on the injection point.
   */
  @Override
  public Annotated getAnnotated()
  {
    return _annotated;
  }

  @Override
  public boolean isDelegate()
  {
    return _annotated.isAnnotationPresent(Delegate.class);
  }

  @Override
  public boolean isTransient()
  {
    int modifiers = _member.getModifiers();
    
    return Modifier.isTransient(modifiers);
  }
  
  private Object writeReplace()
  {
    return new InjectionPointImplHandle(_bean.getBeanClass().getName(),
                                        _bean.getQualifiers(),
                                        getMember(), 
                                        _qualifiers,
                                         ((Class<?>) getType()).getName());
  }
  
  @Override
  public int hashCode()
  {
    return _member.hashCode();
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o.getClass() != InjectionPointImpl.class)
      return false;
    
    InjectionPointImpl<?> ip = (InjectionPointImpl<?>) o;
    
    return _member.equals(ip._member);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getMember() + "]";
  }
}
