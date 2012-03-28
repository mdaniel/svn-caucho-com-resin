/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

/**
 * Represents a temp PHP variable expression.
 */
public class VarTempExprPro extends VarExprPro {
  public VarTempExprPro(InfoVarPro var)
  {
    super(var);
  }

  /**
   * Returns the java variable name.
   */
  @Override
  public String getJavaVar()
  {
    return getName().toString();
  }

  @Override
  public String toString()
  {
    return "$quercus_" + getName();
  }
}

