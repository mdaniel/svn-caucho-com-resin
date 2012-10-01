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
 * @author Scott Ferguson
 */

package com.caucho.quercus.function;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a compiled function with 1 arg
 */
abstract public class CompiledFunctionRef extends AbstractFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunctionRef.class.getName());
  private static final L10N L = new L10N(CompiledFunctionRef.class);

  @Override
  public abstract String getName();

  @Override
  public Value call(Env env, Value []argValues)
  {
    return callRef(env, argValues).copy();
  }

  @Override
  public Value call(Env env)
  {
    return callRef(env).copy();
  }

  @Override
  public Value call(Env env, Value arg)
  {
    return callRef(env, arg).copy();
  }

  @Override
  public Value call(Env env, Value a1, Value a2)
  {
    return callRef(env, a1, a2).copy();
  }

  @Override
  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    return callRef(env, a1, a2, a3).copy();
  }

  @Override
  public Value call(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return callRef(env, a1, a2, a3, a4).copy();
  }

  @Override
  public Value call(Env env, Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return callRef(env, a1, a2, a3, a4, a5).copy();
  }
}

