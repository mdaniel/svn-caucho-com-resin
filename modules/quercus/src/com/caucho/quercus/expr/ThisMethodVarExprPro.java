/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;

import java.util.ArrayList;

/**
 * Represents a PHP method call expression from $this.
 */
public class ThisMethodVarExprPro extends ObjectMethodVarExprPro {
  public ThisMethodVarExprPro(Location location,
                              ThisExpr objExpr,
                              Expr name,
                              ArrayList<Expr> args)
  {
    super(location, objExpr, name, args);
  }
}

