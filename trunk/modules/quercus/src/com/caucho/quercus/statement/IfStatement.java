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

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

/**
 * Represents an if statement.
 */
public class IfStatement extends Statement {
  private final Expr _test;
  private final Statement _trueBlock;
  private final Statement _falseBlock;

  public IfStatement(Location location,
                     Expr test,
                     Statement trueBlock,
                     Statement falseBlock)
  {
    super(location);

    _test = test;
    _trueBlock = trueBlock;
    _falseBlock = falseBlock;

    if (_trueBlock != null)
      _trueBlock.setParent(this);

    if (_falseBlock != null)
      _falseBlock.setParent(this);
  }

  protected Expr getTest()
  {
    return _test;
  }

  protected Statement getTrueBlock()
  {
    return _trueBlock;
  }

  protected Statement getFalseBlock()
  {
    return _falseBlock;
  }

  /**
   * Executes the 'if' statement, returning any value.
   */
  public Value execute(Env env)
  {
    if (_test.evalBoolean(env)) {
      return _trueBlock.execute(env);
    }
    else if (_falseBlock != null) {
      return _falseBlock.execute(env);
    }
    else
      return null;
  }
}

