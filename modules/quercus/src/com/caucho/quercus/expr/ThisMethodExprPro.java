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
public class ThisMethodExprPro extends ObjectMethodExprPro {
  public ThisMethodExprPro(Location location,
			       Expr objExpr,
			       String name,
			       ArrayList<Expr> args)
  {
    super(location, objExpr, name, args);
  }
}

