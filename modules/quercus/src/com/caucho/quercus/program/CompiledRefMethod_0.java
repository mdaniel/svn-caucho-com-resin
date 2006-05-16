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
 * Represents a compiled method with 0 args
 */
abstract public class CompiledRefMethod_0 extends CompiledMethodRef {
  private static final Logger log
    = Logger.getLogger(CompiledRefMethod_0.class.getName());
  private static final L10N L = new L10N(CompiledRefMethod_0.class);

  private String _name;

  public CompiledRefMethod_0(String name)
  {
    _name = name;
  }
  
  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    if (args.length != 0)
      env.warning(L.l("too many arguments"));

    return args;
  }

  public Value evalMethod(Env env, Value obj, Value []argValues)
  {
    return evalMethodRef(env, obj, argValues).copy();
  }

  /**
   * Evalautes the method.
   */
  public Value evalMethodRef(Env env, Value obj, Value []argValues)
  {
    if (argValues.length > 0)
      env.warning(L.l("too many arguments in {0}", _name));

    return evalMethodRef(env, obj);
  }

  /**
   * Evaluates the method as a static function
   */
  public Value eval(Env env, Value []argValues)
  {
    env.error(L.l("can't call {0} as a static function",
		  _name));

    return NullValue.NULL;
  }

  /**
   * Evaluates the method as a static function
   */
  public Value evalMethod(Env env)
  {
    return evalMethodRef(env).copy();
  }

  /**
   * Evaluates the method as a static function
   */
  abstract public Value evalMethodRef(Env env);
  
  public String toString()
  {
    return "CompiledMethod_0[" + _name + "]";
  }
}

