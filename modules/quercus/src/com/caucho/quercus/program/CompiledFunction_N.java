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
 * Represents a compiled function with N args
 */
abstract public class CompiledFunction_N extends CompiledFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunction_N.class.getName());
  private static final L10N L = new L10N(CompiledFunction_N.class);

  private final String _name;
  private final Expr []_defaultArgs;

  public CompiledFunction_N(String name, Expr []defaultArgs)
  {
    _name = name;
    _defaultArgs = defaultArgs;
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
    return args;
  }

  public final Value eval(Env env, Value []argValues)
    throws Throwable
  {
    Value []args = argValues;

    if (_defaultArgs.length != argValues.length) {
      int len = _defaultArgs.length;
      
      if (len < argValues.length)
	len = argValues.length;
	
      args = new Value[len];

      System.arraycopy(argValues, 0, args, 0, argValues.length);

      for (int i = argValues.length; i < _defaultArgs.length; i++) {
	args[i] = _defaultArgs[i].eval(env);
      }
    }

    return evalImpl(env, args);
  }

  abstract public Value evalImpl(Env env, Value []args)
    throws Throwable;
  
  public String toString()
  {
    return "CompiledFunction_N[" + _name + "]";
  }
}

