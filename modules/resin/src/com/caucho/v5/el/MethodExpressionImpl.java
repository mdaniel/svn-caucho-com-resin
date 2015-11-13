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
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotFoundException;

import java.util.logging.Logger;

/**
 * Implementation of the method expression.
 */
@SuppressWarnings("serial")
public class MethodExpressionImpl extends MethodExpression
  implements java.io.Serializable
{
  protected static final Logger log
    = Logger.getLogger(MethodExpressionImpl.class.getName());
  protected static final L10N L = new L10N(MethodExpressionImpl.class);

  private final String _expressionString;
  private final Expr _expr;
  private final Class<?> _expectedType;
  private final Class<?> []_expectedArgs;

  // XXX: for serialization
  public MethodExpressionImpl()
  {
    _expressionString = "";
    _expr = null;
    _expectedType = null;
    _expectedArgs = null;
  }

  public MethodExpressionImpl(Expr expr,
                              String expressionString,
                              Class<?> expectedType,
                              Class<?> []expectedArgs)
  {
    _expr = expr;
    _expressionString = expressionString;
    _expectedType = expectedType;
    _expectedArgs = expectedArgs;
  }

  @Override
  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  @Override
  public String getExpressionString()
  {
    return _expressionString;
  }
  
  @Override
  public MethodInfo getMethodInfo(ELContext context)
    throws PropertyNotFoundException,
           MethodNotFoundException,
           ELException
  {
    return _expr.getMethodInfo(context, _expectedType, _expectedArgs);
  }

  @Override
  public Object invoke(ELContext context,
                       Object []params)
    throws PropertyNotFoundException,
           MethodNotFoundException,
           ELException
  {
    if (! _expr.isArgsProvided()) {
      if (params == null && _expectedArgs.length != 0
          || params != null && params.length != _expectedArgs.length) {
        throw new IllegalArgumentException(L.l("'{0}' expected arguments ({1}) do not match actual arguments ({2})", _expr.toString(),
                                               _expectedArgs.length,
                                               (params != null ? params.length : 0)));
      }

      if (void.class.equals(_expectedType ) && _expr.isLiteralText()) {
        throw new ELException("String literal can not be coerced to void");
      }
    }
    
    Object value = null;
    
    context.notifyBeforeEvaluation(_expressionString);
    
    if (_expr.isArgsProvided())
      value = _expr.getValue(context);
    else
      value = _expr.invoke(context, _expectedArgs, params);
    
    context.notifyAfterEvaluation(_expressionString);
      
    return context.convertToType(value, _expectedType);
  }

  @Override
  public int hashCode()
  {
    return _expr.hashCode();
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof MethodExpressionImpl))
      return false;

    MethodExpressionImpl expr = (MethodExpressionImpl) o;

    return _expr.equals(expr._expr);
  }

  @Override
  public String toString()
  {
    return getClass().getName() + "[" + getExpressionString() + "]";
  }
}
