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

package com.caucho.v5.config.reflect;

import java.lang.ref.SoftReference;
import java.lang.reflect.Type;
import java.util.WeakHashMap;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.loader.EnvironmentLocal;

/**
 * Factory for introspecting reflected types.
 */
public class ReflectionAnnotatedFactory
{
  private static EnvironmentLocal<ReflectionAnnotatedFactory> _current
    = new EnvironmentLocal<>();

  private WeakHashMap<Type,SoftReference<ReflectionSimpleAnnotatedType<?>>> _simpleTypeMap
    = new WeakHashMap<Type,SoftReference<ReflectionSimpleAnnotatedType<?>>>();

  private WeakHashMap<Type,SoftReference<ReflectionAnnotatedType<?>>> _typeMap
    = new WeakHashMap<Type,SoftReference<ReflectionAnnotatedType<?>>>();

  /**
   * Returns the factory for the given loader.
   */
  private static ReflectionAnnotatedFactory create(ClassLoader loader)
  {
    synchronized (_current) {
      ReflectionAnnotatedFactory factory = _current.getLevel(loader);

      if (factory == null) {
        factory = new ReflectionAnnotatedFactory();
        _current.set(factory, loader);
      }

      return factory;
    }
  }

  /**
   * Introspects a simple reflection type, i.e. a type without
   * fields and methods.
   */
  public static <T> ReflectionSimpleAnnotatedType<T> 
  introspectSimpleType(Class<T> cl)
  {
    return create(cl.getClassLoader()).introspectSimpleTypeImpl(cl);
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private <T> ReflectionSimpleAnnotatedType<T>
  introspectSimpleTypeImpl(Type type)
  {
    SoftReference<ReflectionSimpleAnnotatedType<?>> typeRef
      = _simpleTypeMap.get(type);

    ReflectionSimpleAnnotatedType<T> annType = null;

    if (typeRef != null)
      annType = (ReflectionSimpleAnnotatedType<T>) typeRef.get();

    if (type == null) {
      CandiManager inject = CandiManager.create();
      BaseType baseType = inject.createSourceBaseType(type);
      
      annType = new ReflectionSimpleAnnotatedType(inject, baseType);

      typeRef = new SoftReference<ReflectionSimpleAnnotatedType<?>>(annType);

      _simpleTypeMap.put(type, typeRef);
    }

    return annType;
  }

  /**
   * Introspects a simple reflection type, i.e. a type without
   * fields and methods.
   */
  public static <X> ReflectionAnnotatedType<X> introspectType(Class<X> cl)
  {
    return create(cl.getClassLoader()).introspectTypeImpl(cl);
  }

  /**
   * Introspects a simple reflection type, i.e. a type without
   * fields and methods.
   */
  public static <X> ReflectionAnnotatedType<X> introspectType(BaseType type)
  {
    ClassLoader loader;
    
    if (type instanceof ClassType)
      loader = type.getRawClass().getClassLoader();
    else
      loader = Thread.currentThread().getContextClassLoader();
    
    return create(loader).introspectTypeImpl(type);
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private <X> ReflectionAnnotatedType<X> introspectTypeImpl(Type type)
  {
    SoftReference<ReflectionAnnotatedType<?>> typeRef
      = _typeMap.get(type);

    ReflectionAnnotatedType<?> annType = null;

    if (typeRef != null)
      annType = typeRef.get();

    if (annType == null) {
      BaseTypeFactory factory = BaseTypeFactory.current();
      //CandiManager inject = CandiManager.create();
      
      //BaseType baseType = inject.createSourceBaseType(type);
      BaseType baseType = factory.createForSource(type);
      
      CandiManager inject = CandiManager.getCurrent();
      
      annType = new ReflectionAnnotatedType<X>(inject, baseType);

      typeRef = new SoftReference<ReflectionAnnotatedType<?>>(annType);

      _typeMap.put(type, typeRef);
    }

    return (ReflectionAnnotatedType<X>) annType;
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private <X> ReflectionAnnotatedType<X> introspectTypeImpl(BaseType baseType)
  {
    Type type = baseType.toType();
    
    SoftReference<ReflectionAnnotatedType<?>> typeRef
      = _typeMap.get(type);

    ReflectionAnnotatedType<?> annType = null;

    if (typeRef != null)
      annType = typeRef.get();

    if (annType == null) {
      CandiManager inject = CandiManager.create();
      
      annType = new ReflectionAnnotatedType<X>(inject, baseType);

      typeRef = new SoftReference<ReflectionAnnotatedType<?>>(annType);

      _typeMap.put(type, typeRef);
    }

    return (ReflectionAnnotatedType<X>) annType;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
