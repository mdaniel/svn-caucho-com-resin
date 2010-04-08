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

import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;

/**
 * class type matching
 */
public class ArrayType extends BaseType implements GenericArrayType
{
  private BaseType _componentType;
  private Class _rawType;

  public ArrayType(BaseType componentType, Class rawType)
  {
    _componentType = componentType;
    _rawType = rawType;
  }
  
  public Class getRawClass()
  {
    return _rawType;
  }

  public Type getGenericComponentType()
  {
    return _componentType.toType();
  }

  public Type toType()
  {
    return this;
  }
  
  public boolean isMatch(Type type)
  {
    if (type instanceof GenericArrayType) {
      GenericArrayType aType = (GenericArrayType) type;

      return _componentType.equals(aType.getGenericComponentType());
    }
    else
      return false;
  }

  @Override
  public boolean isAssignableFrom(BaseType type)
  {
    return equals(type);
  }

  public int hashCode()
  {
    return 17 + 37 * _componentType.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (o instanceof GenericArrayType) {
      GenericArrayType type = (GenericArrayType) o;

      return _componentType.equals(type.getGenericComponentType());
    }
    else
      return false;
  }

  @Override
  public String getSimpleName()
  {
    return _componentType.getSimpleName() + "[]";
  }

  public String toString()
  {
    return _componentType + "[]";
  }
}
