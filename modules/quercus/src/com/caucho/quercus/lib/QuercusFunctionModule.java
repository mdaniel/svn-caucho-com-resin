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

package com.caucho.quercus.lib;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.VariableArguments;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Callback;

/**
 * PHP function routines.
 */
public class QuercusFunctionModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusFunctionModule.class);

  private static final Logger log
    = Logger.getLogger(QuercusFunctionModule.class.getName());

  /**
   * Calls a user function
   */
  public static Value call_user_func(Env env,
                                     Callback function,
                                     Value []args)
    throws Throwable
  {
    return function.eval(env, args);
  }

  /**
   * Calls a user function
   */
  public static Value call_user_func_array(Env env,
                                           Callback function,
                                           Value arg)
    throws Throwable
  {
    ArrayValue argArray;
    
    if (arg instanceof ArrayValue)
      argArray = (ArrayValue) arg;
    else
      argArray = new ArrayValueImpl().put(arg);
	
    Value []args;

    if (argArray != null) {
      args = new Value[argArray.getSize()];

      argArray.values().toArray(args);
    }
    else
      args = new Value[0];

    return function.eval(env, args);
  }

  /**
   * Creates an anonymous function
   */
  public static Value create_function(Env env,
                                      String args,
                                      String code)
    throws Throwable
  {
    if (log.isLoggable(Level.FINER))
      log.finer(code);

    Quercus quercus = env.getPhp();

    return quercus.parseFunction(args, code);
  }

  /**
   * Returns the nth function argument.
   */
  @VariableArguments
  public static Value func_get_arg(Env env, int index)
  {
    Value []args = env.getFunctionArgs();

    if (0 <= index && index < args.length)
      return args[index];
    else {
      // XXX: warning
      return NullValue.NULL;
    }
  }

  /**
   * Returns the function arguments as an array.
   */
  @VariableArguments
  public static Value func_get_args(Env env)
  {
    Value []args = env.getFunctionArgs();

    ArrayValue result = new ArrayValueImpl();
    for (int i = 0; i < args.length; i++)
      result.append(args[i]);

    return result;
  }

  /**
   * Returns the number of arguments to the function.
   */
  @VariableArguments
  public static Value func_num_args(Env env)
  {
    Value []args = env.getFunctionArgs();

    if (args != null && args.length > 0)
      return new LongValue(args.length);
    else
      return LongValue.ZERO;
  }

  /**
   * Returns true if the function exists.
   *
   * @param env the PHP environment
   * @param name the function name
   */
  public static boolean function_exists(Env env, String name)
  {
    return env.findFunction(name) != null;
  }

  /**
   * Registers a shutdown function.
   */
  public static Value register_shutdown_function(Env env,
						 Callback fun,
						 Value []args)
  {
    env.addShutdown(fun, args);
    
    return NullValue.NULL;
  }
}
