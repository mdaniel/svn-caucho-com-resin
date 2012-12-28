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
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

/**
 * Represents a compiled method with 0 args
 */
@SuppressWarnings("serial")
abstract public class CompiledMethod extends AbstractFunction {
  @Override
  public Value call(Env env, Value []args)
  {
    throw new IllegalStateException(getClass().getName());
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value []args)
  {
    return callMethod(env, qClass, qThis, args).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis)
  {
    // php/37a2
    return callMethod(env, qClass, qThis).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value a1)
  {
    return callMethod(env, qClass, qThis, a1).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value a1, Value a2)
  {
    return callMethod(env, qClass, qThis, a1, a2).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value a1, Value a2, Value a3)
  {
    return callMethod(env, qClass, qThis, a1, a2, a3).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value a1, Value a2, Value a3, Value a4)
  {
    return callMethod(env, qClass, qThis, a1, a2, a3, a4).copyReturn();
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return callMethod(env, qClass, qThis, a1, a2, a3, a4, a5).copyReturn();
  }
}

