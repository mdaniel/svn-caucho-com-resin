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

package com.caucho.config.reflect;

import java.lang.reflect.Type;
import java.util.HashMap;

import com.caucho.inject.Module;
import com.caucho.util.LruCache;

/**
 * type matching the web bean
 */
@Module
public class BaseTypeFactory
{
  private LruCache<Type,BaseType> _cache
    = new LruCache<Type,BaseType>(128);
  
  private LruCache<Class<?>,BaseType> _classCache
    = new LruCache<Class<?>,BaseType>(128);

  public BaseType create(Type type)
  {
    BaseType baseType = _cache.get(type);

    if (baseType == null) {
      baseType = BaseType.create(type, new HashMap<String,BaseType>());

      if (baseType == null)
	throw new NullPointerException("unsupported BaseType: " + type + " " + type.getClass());

      _cache.put(type, baseType);
    }

    return baseType;
  }

  public BaseType createClass(Class<?> type)
  {
    BaseType baseType = _classCache.get(type);

    if (baseType == null) {
      baseType = BaseType.createClass(type);

      if (baseType == null)
	throw new NullPointerException("unsupported BaseType: " + type + " " + type.getClass());

      _classCache.put(type, baseType);
    }

    return baseType;
  }

  public BaseType create(Type type, HashMap<String,BaseType> paramMap)
  {
    return BaseType.create(type, paramMap);
  }
}
