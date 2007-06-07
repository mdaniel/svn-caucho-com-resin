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
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.Value;

public class ReflectionProperty
  implements Reflector
{
  final private void __clone()
  {
    
  }
  
  public Value __construct(Value cls, String name)
  {
    return null;
  }
  
  public String __toString()
  {
    return null;
  }
  
  public static String export(Env env,
                              Value cls,
                              String name,
                              @Optional boolean isReturn)
  {
    return null;
  }
  
  public String getName()
  {
    return null;
  }
  
  public boolean isPublic()
  {
    return false;
  }
  
  public boolean isPrivate()
  {
    return false;
  }
  
  public boolean isProtected()
  {
    return false;
  }
  
  public boolean isStatic()
  {
    return false;
  }
  
  public boolean isDefault()
  {
    return false;
  }
  
  public int getModifiers()
  {
    return -1;
  }
  
  public Value getValue(ObjectValue obj)
  {
    return null;
  }
  
  public void setValue(ObjectValue obj, Value value)
  {

  }
  
  public ReflectionClass getDeclaringClass()
  {
    return null;
  }
  
  public String getDocComment()
  {
    return null;
  }
}
