/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

/**
 * Represents compilable PHP expression.
 */
public interface ExprPro {
  /**
   * Returns the expression's generator.
   */
  public ExprGenerator getGenerator();
}

