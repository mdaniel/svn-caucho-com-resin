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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * Represents a PHP __CLASS__ expression for traits.
 * If within a trait scope, __CLASS__ needs to look up the current class.
 */
public class ConstClassExpr extends Expr {
  protected final StringValue _funName;

  public ConstClassExpr(Location location, StringValue funName)
  {
    super(location);

    _funName = funName;
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
    return evalStatic(env, env.getThis(), _funName);
  }

  /**
   * Called by pro version.
   */
  public static StringValue evalStatic(Env env,
                                       Value qThis, StringValue funName)
  {
    QuercusClass cls = qThis.getQuercusClass();

    if (cls == null) {
      return env.getEmptyString();
    }

    String bindingClassName = cls.getTraitMethodBindingClassName(funName);

    if (bindingClassName == null) {
      return env.getEmptyString();
    }
    else {
      return env.createString(bindingClassName);
    }
  }

  @Override
  public String toString()
  {
    return "__CLASS__";
  }
}

