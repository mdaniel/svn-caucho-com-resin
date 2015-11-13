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

import javax.el.*;

import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public class ObjectValueExpression extends ValueExpression
{
  protected static final Logger log
    = Logger.getLogger(ObjectValueExpression.class.getName());
  protected static final L10N L = new L10N(Expr.class);

  private final Expr _expr;
  private final String _expressionString;
  private final Class _expectedType;

  public ObjectValueExpression(Expr expr,
                               String expressionString,
                               Class<?> expectedType)
  {
    _expr = expr;
    _expressionString = expressionString;
    _expectedType = expectedType;
  }

  public ObjectValueExpression(Expr expr, String expressionString)
  {
    _expr = expr;
    _expressionString = expressionString;
    _expectedType = Object.class;
  }

  public ObjectValueExpression(Expr expr)
  {
    _expr = expr;
    _expressionString = _expr.toString();
    _expectedType = Object.class;
  }

  /**
   * For serialization
   */
  public ObjectValueExpression()
  {
    _expr = null;
    _expressionString = null;
    _expectedType = Object.class;
  }

  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  public String getExpressionString()
  {
    return _expressionString;
  }

  public Class<?> getExpectedType()
  {
    return _expectedType;
  }

  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    return _expr.getType(context);
  }
  
  public ValueReference getValueReference(ELContext context)
  {
    //return new ValueReference(null, _expr.getExpressionString());
    return _expr.getValueReference(context);
  }

  @Override
  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");
    
    context.notifyBeforeEvaluation(_expressionString);

    Object rawValue = _expr.getValue(context);
    if (rawValue instanceof ELClass) {
      rawValue = context.getELResolver().getValue(context, 
                                                  rawValue,
                                                  _expr.getExpressionString());
    }
    
    context.notifyAfterEvaluation(_expressionString);
    
    return context.convertToType(rawValue, _expectedType);
  }

  @Override
  public boolean isReadOnly(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    return _expr.isReadOnly(context);
  }

  public void setValue(ELContext context, Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    _expr.setValue(context, value);
  }

  public int hashCode()
  {
    return _expr.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof ObjectValueExpression))
      return false;

    ObjectValueExpression expr = (ObjectValueExpression) o;

    return _expr.equals(expr._expr);
  }

  public String toString()
  {
    return "ObjectValueExpression[" + getExpressionString() + "]";
  }
}
