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
import com.caucho.quercus.expr.Expr;
import com.caucho.util.L10N;

/**
 * Represents a compiled method with 0 args
 */
@SuppressWarnings("serial")
abstract public class CompiledRefMethod_0 extends CompiledMethodRef {
  private static final L10N L = new L10N(CompiledRefMethod_0.class);

  private String _name;

  public CompiledRefMethod_0(String name)
  {
    _name = name;
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
  private Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    if (args.length != 0)
      env.warning(L.l("too many arguments"));

    return args;
  }

  @Override
  public Value callMethod(Env env,
                          QuercusClass qClass,
                          Value qThis,
                          Value []argValues)
  {
    return callMethodRef(env, qClass, qThis, argValues).copy();
  }

  /**
   * Evalautes the method.
   */
  @Override
  public Value callMethodRef(Env env, 
                             QuercusClass qClass,
                             Value qThis,
                             Value []argValues)
  {
    if (argValues.length > 0)
      env.warning(L.l("too many arguments in {0}", _name));

    return callMethodRef(env, qClass, qThis);
  }

  /**
   * Evaluates the method as a static function
   */
  @Override
  public Value callMethod(Env env, QuercusClass qClass, Value qThis)
  {
    return callMethodRef(env, qClass, qThis).copy();
  }

  /**
   * Evaluates the method as a static function
   */
  @Override
  abstract public Value callMethodRef(Env env, 
                                      QuercusClass qClass, 
                                      Value qThis);
}

