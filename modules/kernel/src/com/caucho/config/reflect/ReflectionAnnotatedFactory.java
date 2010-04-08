/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

import com.caucho.loader.EnvironmentLocal;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;

/**
 * Factory for introspecting reflected types.
 */
public class ReflectionAnnotatedFactory
{
  private static EnvironmentLocal<ReflectionAnnotatedFactory> _current
    = new EnvironmentLocal<ReflectionAnnotatedFactory>();

  private WeakHashMap<Class,SoftReference<ReflectionSimpleAnnotatedType>> _simpleTypeMap
    = new WeakHashMap<Class,SoftReference<ReflectionSimpleAnnotatedType>>();

  private WeakHashMap<Class,SoftReference<ReflectionAnnotatedType>> _typeMap
    = new WeakHashMap<Class,SoftReference<ReflectionAnnotatedType>>();

  /**
   * Returns the factory for the given loader.
   */
  public static ReflectionAnnotatedFactory create(ClassLoader loader)
  {
    synchronized (_current) {
      ReflectionAnnotatedFactory factory = _current.get(loader);

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
  public static ReflectionSimpleAnnotatedType introspectSimpleType(Class cl)
  {
    return create(cl.getClassLoader()).introspectSimpleTypeImpl(cl);
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private ReflectionSimpleAnnotatedType introspectSimpleTypeImpl(Class cl)
  {
    SoftReference<ReflectionSimpleAnnotatedType> typeRef
      = _simpleTypeMap.get(cl);

    ReflectionSimpleAnnotatedType type = null;

    if (typeRef != null)
      type = typeRef.get();

    if (type == null) {
      type = new ReflectionSimpleAnnotatedType(cl);

      typeRef = new SoftReference<ReflectionSimpleAnnotatedType>(type);

      _simpleTypeMap.put(cl, typeRef);
    }

    return type;
  }

  /**
   * Introspects a simple reflection type, i.e. a type without
   * fields and methods.
   */
  public static ReflectionAnnotatedType introspectType(Class cl)
  {
    return create(cl.getClassLoader()).introspectTypeImpl(cl);
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private ReflectionAnnotatedType introspectTypeImpl(Class cl)
  {
    SoftReference<ReflectionAnnotatedType> typeRef
      = _typeMap.get(cl);

    ReflectionAnnotatedType type = null;

    if (typeRef != null)
      type = typeRef.get();

    if (type == null) {
      type = new ReflectionAnnotatedType(cl);

      typeRef = new SoftReference<ReflectionAnnotatedType>(type);

      _typeMap.put(cl, typeRef);
    }

    return type;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
