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
import com.caucho.quercus.expr.Expr;
import com.caucho.util.L10N;

import java.util.logging.Logger;

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
    if (default_0 == null || default_1 == null || default_2 == null ||
        default_3 == null || default_4 == null)
      Thread.dumpStack();
  }

  /**
   * Returns this function's name.
   */
  @Override
  public String getName()
  {
    return _name;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    if (args.length > 5) {
      log.fine(L.l("{0}incorrect number of arguments{1}",
                   env.getLocation().getMessagePrefix(),
                   env.getFunctionLocation()));
    }

    return args;
  }

  public Value call(Env env, Value []argValues)
  {
    switch (argValues.length) {
    case 0:
      return call(env,
                  _default_0.eval(env),
                  _default_1.eval(env),
                  _default_2.eval(env),
                  _default_3.eval(env),
                  _default_4.eval(env));

    case 1:
      return call(env,
                  argValues[0],
                  _default_1.eval(env),
                  _default_2.eval(env),
                  _default_3.eval(env),
                  _default_4.eval(env));
    case 2:
      return call(env,
                  argValues[0],
                  argValues[1],
                  _default_2.eval(env),
                  _default_3.eval(env),
                  _default_4.eval(env));
    case 3:
      return call(env,
                  argValues[0],
                  argValues[1],
                  argValues[2],
                  _default_3.eval(env),
                  _default_4.eval(env));
    case 4:
      return call(env,
                  argValues[0],
                  argValues[1],
                  argValues[2],
                  argValues[3],
                  _default_4.eval(env));
    case 5:
    default:
      return call(env,
                  argValues[0],
                  argValues[1],
                  argValues[2],
                  argValues[3],
                  argValues[4]);
    }
  }

  public Value call(Env env)
  {
    return call(env,
                _default_0.eval(env),
                _default_1.eval(env),
                _default_2.eval(env),
                _default_3.eval(env),
                _default_4.eval(env));
  }

  public Value call(Env env, Value a1)
  {
    return call(env,
                a1,
                _default_1.eval(env),
                _default_2.eval(env),
                _default_3.eval(env),
                _default_4.eval(env));
  }

  public Value call(Env env, Value a1, Value a2)
  {
    return call(env,
                a1,
                a2,
                _default_2.eval(env),
                _default_3.eval(env),
                _default_4.eval(env));
  }

  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    return call(env,
                a1,
                a2,
                a3,
                _default_3.eval(env),
                _default_4.eval(env));
  }

  public Value call(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return call(env,
                a1,
                a2,
                a3,
                a4,
                _default_4.eval(env));
  }

  /**
   * Evaluates the function with arguments
   */
  abstract public Value call(Env env, Value a1, Value a2, Value a3, Value a4,
                             Value a5);

  public String toString()
  {
    return "CompiledFunction_5[" + _name + "]";
  }
}

