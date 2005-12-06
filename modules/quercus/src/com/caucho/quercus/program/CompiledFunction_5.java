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
 * Represents a compiled function with 5 args
 */
abstract public class CompiledFunction_5 extends CompiledFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunction_5.class.getName());
  private static final L10N L = new L10N(CompiledFunction_5.class);

  private final String _name;
  private final Expr _default_0;
  private final Expr _default_1;
  private final Expr _default_2;
  private final Expr _default_3;
  private final Expr _default_4;

  public CompiledFunction_5(String name,
			    Expr default_0,
			    Expr default_1,
			    Expr default_2,
			    Expr default_3,
			    Expr default_4)
  {
    _name = name;
    _default_0 = default_0;
    _default_1 = default_1;
    _default_2 = default_2;
    _default_3 = default_3;
    _default_4 = default_4;
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
    if (args.length != 5)
      env.warning(L.l("incorrect number of arguments"));

    return args;
  }

  public Value eval(Env env, Value []argValues)
    throws Throwable
  {
    switch (argValues.length) {
    case 0:
      return eval(env,
		  _default_0.eval(env),
		  _default_1.eval(env),
		  _default_2.eval(env),
		  _default_3.eval(env),
		  _default_4.eval(env));
      
    case 1:
      return eval(env,
		  argValues[0],
		  _default_1.eval(env),
		  _default_2.eval(env),
		  _default_3.eval(env),
		  _default_4.eval(env));
    case 2:
      return eval(env,
		  argValues[0],
		  argValues[1],
		  _default_2.eval(env),
		  _default_3.eval(env),
		  _default_4.eval(env));
    case 3:
      return eval(env,
		  argValues[0],
		  argValues[1],
		  argValues[2],
		  _default_3.eval(env),
		  _default_4.eval(env));
    case 4:
      return eval(env,
		  argValues[0],
		  argValues[1],
		  argValues[2],
		  argValues[3],
		  _default_4.eval(env));
    case 5:
    default:
      return eval(env,
		  argValues[0],
		  argValues[1],
		  argValues[2],
		  argValues[3],
		  argValues[4]);
    }
  }

  /**
   * Evaluates the function with arguments
   */
  abstract public Value eval(Env env, Value a1, Value a2, Value a3, Value a4,
			     Value a5)
    throws Throwable;
  
  public String toString()
  {
    return "CompiledFunction_5[" + _name + "]";
  }
}

