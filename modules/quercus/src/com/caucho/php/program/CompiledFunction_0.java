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
 * Represents a compiled function with 0 args
 */
abstract public class CompiledFunction_0 extends CompiledFunction {
  private static final Logger log
    = Logger.getLogger(CompiledFunction_0.class.getName());
  private static final L10N L = new L10N(CompiledFunction_0.class);

  private String _name;

  public CompiledFunction_0(String name)
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
    throws Exception
  {
    if (args.length != 0)
      env.warning(L.l("too many arguments"));

    return args;
  }

  public Value eval(Env env, Value []argValues)
    throws Throwable
  {
    return eval(env);
  }
  
  public String toString()
  {
    return "CompiledFunction_0[" + _name + "]";
  }
}

