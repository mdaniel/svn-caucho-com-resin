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
import com.caucho.quercus.gen.AnalyzeInfo;

/**
 * Common analysis for a PHP binary expression.
 */
abstract public class AbstractBinaryGenerateExpr extends ExprGenerator {
  public AbstractBinaryGenerateExpr(Location location)
  {
    super(location);
  }
  
  /**
   * Returns the left expression.
   */
  abstract public ExprGenerator getLeft();

  /**
   * Returns the right expression.
   */
  abstract public ExprGenerator getRight();

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public ExprType analyze(AnalyzeInfo info)
  {
    getLeft().analyze(info);
    getRight().analyze(info);

    return ExprType.VALUE;
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  /*
  public VarState getVarState(ProVarExpr var, ProVarExpr owner)
  {
    return combineBinaryVarState(getLeft().getVarState(var, owner),
                                 getRight().getVarState(var, owner));
  }
  */

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExprPro var)
  {
    return (getLeft().isVarAssigned(var)
	    || getRight().isVarAssigned(var));
  }
}

