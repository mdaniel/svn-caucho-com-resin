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
 * Represents a compiled function with 1 arg
 */
abstract public class CompiledFunction_1 extends CompiledFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunction_1.class.getName());
  private static final L10N L = new L10N(CompiledFunction_1.class);

  private String _name;
  private Expr _default_0;

  public CompiledFunction_1(String name, Expr default_0)
  {
    _name = name;
    _default_0 = default_0;
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
    if (args.length > 1) {
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
                  _default_0.eval(env));
    case 1:
    default:
      return call(env,
                  argValues[0]);
    }
  }

  public Value call(Env env)
  {
    return call(env, _default_0.eval(env));
  }

  abstract public Value call(Env env, Value arg1);

  public String toString()
  {
    return "CompiledFunction_1[" + _name + "]";
  }
}

