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

package com.caucho.config.reflect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;


/**
 * Abstract introspected view of a Bean
 */
public class AnnotatedTypeImpl<X> extends AnnotatedElementImpl
  implements AnnotatedType<X>
{
  private Class<X> _javaClass;
  
  // private HashMap<String,BaseType> _paramMap = new HashMap<String,BaseType>();
  
  private AnnotatedType<X> _parentType;
  
  /*
  private Set<AnnotatedConstructor<X>> _constructorSet
    = new CopyOnWriteArraySet<AnnotatedConstructor<X>>();

  private Set<AnnotatedField<? super X>> _fieldSet
    = new CopyOnWriteArraySet<AnnotatedField<? super X>>();
    */

  private Set<AnnotatedMethod<? super X>> _methodSet;

  public AnnotatedTypeImpl(AnnotatedType<X> annType)
  {
    super(annType);
   
    _parentType = annType;
    
    _javaClass = annType.getJavaClass();
    
    /*
    if (getBaseTypeImpl().getParamMap() != null)
      _paramMap.putAll(getBaseTypeImpl().getParamMap());
      */
  
    // _constructorSet.addAll(annType.getConstructors());
    // _fieldSet.addAll(annType.getFields());
    // initMethodSet()
  }
  
  public static <X> AnnotatedTypeImpl<X> create(AnnotatedType<X> annType)
  {
    if (annType instanceof AnnotatedTypeImpl<?>)
      return (AnnotatedTypeImpl<X>) annType;
    else
      return new AnnotatedTypeImpl<X>(annType);
  }

  /**
   * Returns the concrete Java class
   */
  @Override
  public Class<X> getJavaClass()
  {
    return _javaClass;
  }
  
  @Override
  public HashMap<String,BaseType> getBaseTypeParamMap()
  {
    // return _paramMap;
    return getBaseTypeImpl().getParamMap();
  }
  
  private AnnotatedType<?> getParentType()
  {
    return _parentType;
  }

  /**
   * Returns the abstract introspected constructors
   */
  @Override
  public Set<AnnotatedConstructor<X>> getConstructors()
  {
    // return _constructorSet;
    
    return (Set<AnnotatedConstructor<X>>) (Set) getParentType().getConstructors();
  }

  /**
   * Returns the abstract introspected methods
   */
  @Override
  public Set<AnnotatedMethod<? super X>> getMethods()
  {
    if (_methodSet != null)
      return _methodSet;
    else
      return (Set<AnnotatedMethod<? super X>>) (Set) getParentType().getMethods();
  }

  /**
   * Returns the abstract introspected methods
   */
  public Set<AnnotatedMethod<? super X>> getMethodsForUpdate()
  {
    if (_methodSet == null) {
      initMethodSet();
    }
    
    return _methodSet;
  }
  
  /**
   * Returns the matching method, creating one if necessary.
   */
  public AnnotatedMethod<? super X> createMethod(Method method)
  {
    if (_methodSet == null) {
      initMethodSet();
    }
    
    for (AnnotatedMethod<? super X> annMethod : _methodSet) {
      if (AnnotatedMethodImpl.isMatch(annMethod.getJavaMember(), method)) {
        return annMethod;
      }
    }

    AnnotatedMethod<X> annMethod = new AnnotatedMethodImpl<X>(this, null, method);

    _methodSet.add(annMethod);

    return annMethod;
  }

  /**
   * Returns the abstract introspected fields
   */
  @Override
  public Set<AnnotatedField<? super X>> getFields()
  {
    // return _fieldSet;
    
    return (Set<AnnotatedField<? super X>>) (Set) getParentType().getFields();
  }
  
  private void initMethodSet()
  {
    synchronized (this) {
      if (_methodSet != null)
        return;
      
      _methodSet = new CopyOnWriteArraySet<AnnotatedMethod<? super X>>();

      for (AnnotatedMethod<?> annMethod : getParentType().getMethods()) {
        if (annMethod.getDeclaringType() == getParentType())
          _methodSet.add(new AnnotatedMethodImpl(this, annMethod, 
                                                 annMethod.getJavaMember()));
        else {
          _methodSet.add((AnnotatedMethod<? super X>) annMethod);
        }
      }
    }
  }
}
