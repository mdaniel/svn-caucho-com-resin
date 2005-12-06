/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.php.program;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;

import com.caucho.php.expr.Expr;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.php.env.Var;
import com.caucho.php.env.NullValue;

import com.caucho.php.gen.PhpWriter;

/**
 * Represents a compiled method with 1 arg
 */
abstract public class CompiledMethod_1 extends CompiledMethod {
  private static final Logger log
    = Logger.getLogger(CompiledMethod_1.class.getName());
  private static final L10N L = new L10N(CompiledMethod_1.class);

  private String _name;
  private Expr _default_0;

  public CompiledMethod_1(String name, Expr default_0)
  {
    _name = name;
    _default_0 = default_0;
  }
  
  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
    throws Exception
  {
    if (args.length != 1)
      env.warning(L.l("incorrect"));

    return args;
  }

  /**
   * Evaluates the method with the given variable arguments.
   */
  public Value evalMethod(Env env, Value obj, Value []argValues)
    throws Throwable
  {
    switch (argValues.length) {
    case 0:
      return evalMethod(env, obj, _default_0.eval(env));
    case 1:
    default:
      return evalMethod(env, obj, argValues[0]);
    }
  }

  /**
   * Evaluates the method with the given variable arguments.
   */
  public Value evalMethod(Env env, Value obj)
    throws Throwable
  {
    return evalMethod(env, obj, _default_0.eval(env));
  }

  abstract public Value evalMethod(Env env, Value obj, Value a1)
    throws Throwable;
  
  public String toString()
  {
    return "CompiledMethod_1[" + _name + "]";
  }
}

