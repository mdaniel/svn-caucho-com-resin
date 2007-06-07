/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.reflection;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.Value;

public class ReflectionClass
  implements Reflector
{
  final private void __clone()
  {
    
  }
  
  public ReflectionClass __construct(String name)
  {
    return null;
  }
  
  public String __toString()
  {
    return null;
  }
  
  public static String export(Env env,
                              Value cls,
                              @Optional boolean isReturn)
  {
    return null;
  }
  
  public String getName()
  {
    return null;
  }
  
  public boolean isInternal()
  {
    return false;
  }
  
  public boolean isUserDefined()
  {
    return false;
  }
  
  public boolean isInstantiable()
  {
    return false;
  }
  
  public boolean hasConstant(String name)
  {
    return false;
  }
  
  public boolean hasMethod(String name)
  {
    return false;
  }
  
  public boolean hasProperty(String name)
  {
    return false;
  }
  
  public String getFileName()
  {
    return null;
  }
  
  public int getStartLine()
  {
    return -1;
  }
  
  public int getEndLine()
  {
    return -1;
  }
  
  public String getDocComment()
  {
    return null;
  }
  
  public ReflectionMethod getConstructor()
  {
    return null;
  }
  
  public ReflectionMethod getMethod(String name)
  {
    return null;
  }
  
  public ArrayValue getMethods()
  {
    return null;
  }
  
  public ReflectionProperty getProperty(String name)
  {
    return null;
  }
  
  public ArrayValue getProperties()
  {
    return null;
  }
  
  public ArrayValue getConstants()
  {
    return null;
  }
  
  public Value getConstant(String name)
  {
    return null;
  }
  
  public ArrayValue getInterfaces()
  {
    return null;
  }
  
  public boolean isInterface()
  {
    return false;
  }
  
  public boolean isAbstract()
  {
    return false;
  }
  
  public boolean isFinal()
  {
    return false;
  }
  
  public int getModifiers()
  {
    return -1;
  }
  
  public boolean isInstance(ObjectValue obj)
  {
    return false;
  }
  
  public Value newInstance(Value args)
  {
    return null;
  }
  
  public Value newInstanceArgs(ArrayValue args)
  {
    return null;
  }
  
  public ReflectionClass getParentClass()
  {
    return null;
  }
  
  public boolean isSubclassOf(ReflectionClass cls)
  {
    return false;
  }
  
  public ArrayValue getStaticProperties()
  {
    return null;
  }
  
  public Value getStaticPropertyValue(String name,
                                      @Optional Value defaultV)
  {
    return null;
  }
  
  public ArrayValue getDefaultProperties()
  {
    return null;
  }
  
  public boolean isIterateable()
  {
    return false;
  }
  
  public boolean implementsInterface(String name)
  {
    return false;
  }
  
  public ReflectionExtension getExtension()
  {
    return null;
  }
  
  public String getExtensionName()
  {
    return null;
  }
  
}
