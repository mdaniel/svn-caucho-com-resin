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
 * Represents a compiled method with N args
 */
abstract public class CompiledMethodRef_N extends CompiledMethodRef {
  private static final Logger log
    = Logger.getLogger(CompiledMethodRef_N.class.getName());
  private static final L10N L = new L10N(CompiledMethodRef_N.class);

  private String _name;
  private Expr []_defaultArgs;

  public CompiledMethodRef_N(String name, Expr []defaultArgs)
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

  public final Value evalMethodRef(Env env, Value obj, Value []argValues)
    throws Throwable
  {
    Value []args;

    if (_defaultArgs.length <= argValues.length) {
      args = argValues;
    }
    else {
      args = new Value[_defaultArgs.length];

      System.arraycopy(argValues, 0, args, 0, argValues.length);

      for (int i = argValues.length; i < args.length; i++) {
	if (_defaultArgs[i] != null)
	  args[i] = _defaultArgs[i].eval(env);
	else
	  args[i] = NullValue.NULL;
      }
    }

    return evalMethodImpl(env, obj, args);
  }

  abstract public Value evalMethodImpl(Env env, Value obj, Value []argValues)
    throws Throwable;

  /**
   * Evaluates the method as a static function
   */
  public Value eval(Env env, Value []argValues)
    throws Throwable
  {
    env.error(L.l("can't call {0} as a static function",
		  _name));

    return NullValue.NULL;
  }
  
  public String toString()
  {
    return "CompiledMethodRef_N[" + _name + "]";
  }
}

