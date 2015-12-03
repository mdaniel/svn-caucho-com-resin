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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import java.io.IOException;

/**
 * Represents the die expression
 */
public class DieExpr extends Expr {
  protected Expr _value;

  public DieExpr(Location location, Expr value)
  {
    super(location);
    _value = value;
  }

  public DieExpr(Location location)
  {
    super(location);
    _value = null;
  }

  public DieExpr(Expr value)
  {
    _value = value;
  }

  public DieExpr()
  {
    _value = null;
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
    if (_value != null) {
      String msg = _value.evalString(env);
          return env.die(msg);
    }
    else
      return env.die();
  }
}

