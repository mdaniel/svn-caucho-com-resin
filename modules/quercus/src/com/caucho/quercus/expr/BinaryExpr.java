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

package com.caucho.php.expr;

import java.io.IOException;

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;

import com.caucho.php.program.AnalyzeInfo;

import com.caucho.php.gen.PhpWriter;

/**
 * Common analysis for a PHP binary expression.
 */
abstract public class BinaryExpr extends Expr {
  protected final Expr _left;
  protected final Expr _right;

  protected BinaryExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns the left expression.
   */
  public final Expr getLeft()
  {
    return _left;
  }

  /**
   * Returns the right expression.
   */
  public final Expr getRight()
  {
    return _right;
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    _left.analyze(info);
    _right.analyze(info);
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  public VarState getVarState(VarExpr var, VarExpr owner)
  {
    return combineBinaryVarState(_left.getVarState(var, owner),
				 _right.getVarState(var, owner));
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return _left.isVarAssigned(var) || _right.isVarAssigned(var);
  }
}

