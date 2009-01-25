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
import javax.inject.manager.Bean;
import javax.inject.manager.Manager;

/**
 * type matching the web bean
 */
abstract public class BaseType
{
  public static BaseType create(Type type, HashMap paramMap)
  {
    if (type instanceof Class)
      return new ClassType((Class) type);
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      Type []typeArgs = pType.getActualTypeArguments();
      
      BaseType []args = new BaseType[typeArgs.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = create(typeArgs[i], paramMap);
	
	if (args[i] == null)
	  return null;
      }

      return new ParamType((Class) pType.getRawType(), args);
    }
    else if (type instanceof GenericArrayType) {
      GenericArrayType aType = (GenericArrayType) type;

      BaseType baseType = create(aType.getGenericComponentType(), paramMap);
      Class rawType = Array.newInstance(baseType.getRawClass(), 0).getClass();
      
      return new ArrayType(baseType, rawType);
    }
    else {
      return null;
    }
  }
  
  abstract public Class getRawClass();

  abstract public boolean isMatch(Type type);
  
  public String toString()
  {
    return getRawClass().getName();
  }
}
