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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.Function;

/**
 * Represents a closure
 */
@SuppressWarnings("serial")
public class InterpretedClosure extends Closure
{
  private final Function _fun;
  private final Value []_args;

  public InterpretedClosure(Env env, Function fun, Value qThis)
  {
    super(fun.getName(), qThis);

    _fun = fun;

    Arg []args = fun.getClosureUseArgs();
    if (args != null && args.length > 0) {
      _args = new Value[args.length];

      for (int i = 0; i < args.length; i++) {
        Arg arg = args[i];

        if (arg.isReference())
          _args[i] = env.getRef(arg.getName());
        else
          _args[i] = env.getValue(arg.getName());
      }
    }
    else {
      _args = null;
    }
  }

  @Override
  public Value call(Env env, Value []args)
  {
    Value oldThis = env.setThis(getThis());
    Closure oldClosure = env.setClosure(this);

    try {
      return _fun.callImpl(env, args, false, _fun.getClosureUseArgs(), _args);
    }
    finally {
      env.setClosure(oldClosure);
      env.setThis(oldThis);
    }
  }
}
