/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Paul Cowan
 */

package com.caucho.v5.el;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.LambdaExpression;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

/**
 * A lambda expression is a ValueExpression with parameters.
 */
public class LambdaExpr extends Expr
{
  protected static final Logger log =
    Logger.getLogger(LambdaExpr.class.getName());
  protected static final L10N L = new L10N(LambdaExpr.class);

  private final LambdaParamsExpr _params;
  private final Expr _expr;

  public LambdaExpr(LambdaParamsExpr params, Expr right)
  {
    _params = params;
    _expr = right;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return (_params.isConstant() && _expr.isConstant());
  }

  public Object evalWithArgs(Expr []args, ELContext env) throws ELException
  {    
    Map<String,Object> lambdaArgs = new HashMap<>();

    if (args.length < _params.getSize()) {
      throw new ELException(L.l("expected {0} args but saw only {1} args",
                                _params.getSize(), args.length));
    }

    for (int i = 0; i <  _params.getSize(); i++) {
      Object value = args[i].getValue(env);
      lambdaArgs.put(_params.get(i), value);
    }

    env.enterLambdaScope(lambdaArgs);

    try {
      return _expr.evalObject(env);
    } finally {
      env.exitLambdaScope();
    }
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {    
    LambdaExpression expr = new LambdaExpression(_params.getParamNames(), _expr);
    expr.setELContext(env);
    
    return expr;
  }

//  public LambdaExpression toExpression(ELContext env)
//  {
//    LambdaExpression expr = new LambdaExpression(_paramNames, _expr);
//    expr.setELContext(env);
//    return expr;
//  }

  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.LambdaExpr(");
    _params.printCreate(os);
    
    os.print(", ");
    _expr.printCreate(os);
    
    os.print(")");
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof LambdaExpr)) {
      return false;
    }

    LambdaExpr expr = (LambdaExpr) o;
    return (_params.equals(expr._params) && _expr.equals(expr._expr));
  }

  @Override
  public String toString()
  {
    return String.format("%s->(%s)", _params, _expr);
  }

}
