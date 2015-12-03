/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

public class ReflectionObject extends ReflectionClass
{
  final private void __clone()
  {
  }

  protected ReflectionObject(QuercusClass cls)
  {
    super(cls);
  }

  public static ReflectionObject __construct(Env env, Value val)
  {
    if (! val.isObject())
      throw new ReflectionException(env, "parameter must be an object");

    ObjectValue obj = (ObjectValue) val.toObject(env);

    return new ReflectionObject(obj.getQuercusClass());
  }

  public static String export(Env env,
                              Value object,
                              @Optional boolean isReturn)
  {
    return null;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
