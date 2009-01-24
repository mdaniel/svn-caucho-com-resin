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

import com.caucho.config.manager.InjectManager;
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
 * param type matching
 */
public class ParamType extends BaseType
{
  private Class _type;
  private BaseType []_param;

  public ParamType(Class type, BaseType []param)
  {
    _type = type;
    _param = param;
  }
  
  public Class getRawClass()
  {
    return _type;
  }
  
  public boolean isMatch(Type type)
  {
    if (! (type instanceof ParameterizedType))
      return false;

    ParameterizedType pType = (ParameterizedType) type;
    Type rawType = pType.getRawType();

    if (! _type.equals(rawType))
      return false;

    Type []args = pType.getActualTypeArguments();
    
    if (_param.length != args.length)
      return false;

    for (int i = 0; i < _param.length; i++) {
      if (! _param[i].isMatch(args[i]))
	return false;
    }

    return true;
  }

  public int hashCode()
  {
    return _type.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (! (o instanceof ParamType))
      return false;

    ParamType type = (ParamType) o;

    if (! _type.equals(type._type))
      return false;

    if (_param.length != type._param.length)
      return false;

    for (int i = 0; i < _param.length; i++) {
      if (! _param[i].equals(type._param[i]))
	return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getRawClass());
    sb.append("<");

    for (int i = 0; i < _param.length; i++) {
      if (i != 0)
	sb.append(",");
      
      sb.append(_param[i]);
    }
    sb.append(">");

    return sb.toString();
  }
}
