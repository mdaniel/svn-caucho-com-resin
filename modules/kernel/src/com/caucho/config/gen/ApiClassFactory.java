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

package com.caucho.config.gen;

import com.caucho.loader.EnvironmentLocal;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;

/**
 * Factory for introspecting reflected types.
 */
public class ApiClassFactory
{
  private static EnvironmentLocal<ApiClassFactory> _current
    = new EnvironmentLocal<ApiClassFactory>();

  private WeakHashMap<Class,SoftReference<ApiClass>> _apiClassMap
    = new WeakHashMap<Class,SoftReference<ApiClass>>();

  /**
   * Returns the factory for the given loader.
   */
  public static ApiClassFactory create(ClassLoader loader)
  {
    synchronized (_current) {
      ApiClassFactory factory = _current.get(loader);

      if (factory == null) {
	factory = new ApiClassFactory();
	_current.set(factory, loader);
      }

      return factory;
    }
  }

  /**
   * Introspects a simple reflection type, i.e. a type without
   * fields and methods.
   */
  public static ApiClass introspect(Class cl)
  {
    return create(cl.getClassLoader()).introspectImpl(cl);
  }

  /**
   * Introspects the reflection type
   */
  synchronized
  private ApiClass introspectImpl(Class cl)
  {
    SoftReference<ApiClass> apiClassRef
      = _apiClassMap.get(cl);

    ApiClass apiClass = null;

    if (apiClassRef != null)
      apiClass = apiClassRef.get();

    if (apiClass == null) {
      apiClass = new ApiClass(cl, true);

      apiClassRef = new SoftReference<ApiClass>(apiClass);

      _apiClassMap.put(cl, apiClassRef);
    }

    return apiClass;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
