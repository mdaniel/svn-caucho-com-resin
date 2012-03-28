/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;

/**
 * Represents a PHP boolean negation
 */
abstract public class AbstractUnaryExprGenerator extends ExprGenerator {
  public AbstractUnaryExprGenerator()
  {
  }
  
  public AbstractUnaryExprGenerator(Location location)
  {
    super(location);
  }
  
  /**
   * Returns the child expression.
   */
  abstract protected ExprGenerator getExpr();

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public ExprType analyze(AnalyzeInfo info)
  {
    return getExpr().analyze(info);
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  public VarState getVarState(VarExprPro var, VarExprPro owner)
  {
    throw new UnsupportedOperationException();
    //return getExpr().getVarState(var, owner);
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return getExpr().isVarAssigned(var);
  }
}

