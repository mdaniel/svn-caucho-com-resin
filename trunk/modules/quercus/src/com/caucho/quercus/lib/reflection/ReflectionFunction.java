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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Callable;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

public class ReflectionFunction extends ReflectionFunctionAbstract
  implements Reflector
{
  public static final int IS_DEPRECATED = 1024 * 256; //262144;  //2^18

  protected static final L10N L = new L10N(ReflectionFunction.class);

  protected ReflectionFunction(Callable callable)
  {
    super(callable);
  }

  final private void __clone()
  {
  }

  public static ReflectionFunction __construct(Env env, Value nameV)
  {
    Callable callable;

    if (nameV instanceof Callable) {
      callable = (Callable) nameV;
    }
    else {
      AbstractFunction fun = env.findFunction(nameV.toStringValue(env));

      if (fun == null) {
        env.error(L.l("function '{0}' does not exist", nameV));
      }

      callable = (Callable) fun;
    }

    return new ReflectionFunction(callable);
  }

  public Value export(Env env,
                      String name,
                      @Optional boolean isReturn)
  {
    return null;
  }

  public Value invoke(Env env, Value []args)
  {
    return getCallable().call(env, args);
  }

  public Value invokeArgs(Env env, ArrayValue args)
  {
    return getCallable().call(env, args.getValueArray(env));
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
