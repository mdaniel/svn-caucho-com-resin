/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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
 * @author Scott Ferguson
 */

package com.caucho.v5.el;

import com.caucho.v5.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public class BigIntegerValueExpression extends AbstractValueExpression
{
  protected static final Logger log
    = Logger.getLogger(BigIntegerValueExpression.class.getName());
  protected static final L10N L = new L10N(BigIntegerValueExpression.class);

  private Class _expectedType;

  public BigIntegerValueExpression(Expr expr,
                                   String expressionString,
                                   Class expectedType)
  {
    super(expr, expressionString);

    _expectedType = expectedType;
  }

  public BigIntegerValueExpression(Expr expr,
                                String expressionString)
  {
    super(expr, expressionString);
  }

  public BigIntegerValueExpression(Expr expr)
  {
    super(expr);
  }

  public BigIntegerValueExpression()
  {
  }

  public Class<?> getExpectedType()
  {
    if (_expectedType != null)
      return _expectedType;
    else
      return BigInteger.class;
  }

  @Override
  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    return _expr.evalBigInteger(context);
  }
}
