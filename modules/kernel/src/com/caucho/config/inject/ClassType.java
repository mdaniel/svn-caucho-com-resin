/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;

/**
 * class type matching
 */
public class ClassType extends BaseType
{
  private static final HashMap<Class,Class> _boxTypeMap
    = new HashMap<Class,Class>();
    
  private Class _type;

  public ClassType(Class type)
  {
    Class boxType = _boxTypeMap.get(type);

    if (boxType != null)
      type = boxType;
    
    _type = type;
  }
  
  public Class getRawClass()
  {
    return _type;
  }
  
  public boolean isMatch(Type type)
  {
    return _type.equals(type);
  }

  public int hashCode()
  {
    return _type.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (! (o instanceof ClassType))
      return false;

    ClassType type = (ClassType) o;

    return _type.equals(type._type);
  }

  public String toString()
  {
    return getRawClass().toString();
  }

  static {
    _boxTypeMap.put(boolean.class, Boolean.class);
    _boxTypeMap.put(byte.class, Byte.class);
    _boxTypeMap.put(short.class, Short.class);
    _boxTypeMap.put(int.class, Integer.class);
    _boxTypeMap.put(long.class, Long.class);
    _boxTypeMap.put(float.class, Float.class);
    _boxTypeMap.put(double.class, Double.class);
    _boxTypeMap.put(char.class, Character.class);
  }
}
