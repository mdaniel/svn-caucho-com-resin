/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.program;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.env.Var;
import com.caucho.quercus.env.NullValue;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a compiled function
 */
abstract public class CompiledFunction extends AbstractFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunction.class.getName());
  private static final L10N L = new L10N(CompiledFunction.class);

  public Value callRef(Env env, Value []argValues)
  {
    return call(env, argValues).copyReturn();
  }

  public Value callRef(Env env, Value arg)
  {
    return call(env, arg).copyReturn();
  }

  public Value callRef(Env env, Value a1, Value a2)
  {
    return call(env, a1, a2).copyReturn();
  }

  public Value callRef(Env env, Value a1, Value a2, Value a3)
  {
    return call(env, a1, a2, a3).copyReturn();
  }

  public Value callRef(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return call(env, a1, a2, a3, a4).copyReturn();
  }

  public Value callRef(Env env, Value a1, Value a2,
		       Value a3, Value a4, Value a5)
  {
    return call(env, a1, a2, a3, a4, a5).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value []argValues)
  {
    return callMethod(env, obj, argValues).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj)
  {
    // php/37a2
    return callMethod(env, obj).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value arg)
  {
    return callMethod(env, obj, arg).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value a1, Value a2)
  {
    return callMethod(env, obj, a1, a2).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value a1, Value a2, Value a3)
  {
    return callMethod(env, obj, a1, a2, a3).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value a1,
			     Value a2, Value a3, Value a4)
  {
    return callMethod(env, obj, a1, a2, a3, a4).copyReturn();
  }

  public Value callMethodRef(Env env, Value obj, Value a1, Value a2,
			     Value a3, Value a4, Value a5)
  {
    return callMethod(env, obj, a1, a2, a3, a4, a5).copyReturn();
  }

  /**
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}

