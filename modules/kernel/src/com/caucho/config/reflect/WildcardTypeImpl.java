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
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import com.caucho.inject.Module;

/**
 * class type matching
 */
@Module
public class WildcardTypeImpl extends BaseType implements WildcardType
{
  private BaseType []_lowerBounds;
  private BaseType []_upperBounds;

  public WildcardTypeImpl(BaseType []lowerBounds, BaseType []upperBounds)
  {
    _lowerBounds = lowerBounds;
    _upperBounds = upperBounds;
  }

  public Type []getLowerBounds()
  {
    Type []lowerBounds = new Type[_lowerBounds.length];

    for (int i = 0; i < lowerBounds.length; i++)
      lowerBounds[i] = _lowerBounds[i].toType();
      
    return lowerBounds;
  }

  public Type []getUpperBounds()
  {
    Type []upperBounds = new Type[_upperBounds.length];

    for (int i = 0; i < upperBounds.length; i++)
      upperBounds[i] = _upperBounds[i].toType();
      
    return upperBounds;
  }
  
  public Class<?> getRawClass()
  {
    return Object.class; // technically bounds(?)
  }

  public boolean isWildcard()
  {
    return true;
  }

  public Type getGenericComponentType()
  {
    return null;
  }

  public Type toType()
  {
    return this;
  }

  public boolean isAssignableFrom(BaseType type)
  {
    for (BaseType bound : _lowerBounds) {
      if (! type.isAssignableFrom(bound))
	return false;
    }
    
    for (BaseType bound : _upperBounds) {
      if (! bound.isAssignableFrom(type))
	return false;
    }
    
    return true;
  }
  
  public boolean isMatch(Type type)
  {
    if (type instanceof WildcardType || type instanceof TypeVariable<?>)
      return true;
    else
      return false;
  }

  public int hashCode()
  {
    return 17;
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (o instanceof WildcardType) {
      return true;
    }
    else
      return false;
  }

  public String toString()
  {
    return "?";
  }
}
