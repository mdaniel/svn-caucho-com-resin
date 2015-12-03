/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.expr;

import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * A parent::bar(...) method call expression.
 */
public class TraitParentClassMethodExpr extends AbstractMethodExpr {
  protected final String _traitName;
  protected final StringValue _methodName;

  protected final int _hash;
  protected final Expr []_args;

  protected boolean _isMethod;

  public TraitParentClassMethodExpr(Location location,
                                    String traitName,
                                    StringValue methodName,
                                    ArrayList<Expr> args)
  {
    super(location);

    _traitName = traitName;
    _methodName = methodName;

    _hash = _methodName.hashCodeCaseInsensitive();

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public TraitParentClassMethodExpr(Location location,
                                    String traitName,
                                    StringValue methodName,
                                    Expr []args)
  {
    super(location);

    _traitName = traitName;
    _methodName = methodName;

    _hash = _methodName.hashCodeCaseInsensitive();

    _args = args;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    QuercusClass cls = env.getThis().getQuercusClass();

    QuercusClass parent = cls.getTraitParent(env, _traitName);

    Value []values = evalArgs(env, _args);

    Value oldThis = env.getThis();

    // XXX: 2013-03-14 nam: ugly, ugly, clean this up
    // php/09qe
    Value qThis = oldThis;
    /*
    if (oldThis.isNull()) {
      qThis = cl;
      env.setThis(qThis);
    }
    else
      qThis = oldThis;
      */
    // php/024b
    // qThis = cl;

    env.pushCall(this, parent, values);
    // QuercusClass oldClass = env.setCallingClass(cl);

    try {
      env.checkTimeout();

      return parent.callStaticMethod(env, qThis, _methodName, _hash, values);
    } finally {
      env.popCall();
      env.setThis(oldThis);
      // env.setCallingClass(oldClass);
    }
  }

  public String toString()
  {
    return "parent::" + _methodName + "()";
  }
}

