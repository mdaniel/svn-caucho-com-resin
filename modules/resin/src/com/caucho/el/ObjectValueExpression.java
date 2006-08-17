/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.el;

import java.util.logging.*;

import javax.el.*;

import com.caucho.util.*;

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

  public ObjectValueExpression(Expr expr, String expressionString)
  {
    _expr = expr;
    _expressionString = expressionString;
  }

  public ObjectValueExpression(Expr expr)
  {
    _expr = expr;
    _expressionString = _expr.toString();
  }

  public boolean isLiteralText()
  {
    return false;
  }

  public String getExpressionString()
  {
    return _expressionString;
  }

  public Class<?> getExpectedType()
  {
    return Object.class;
  }

  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
	   ELException
  {
    Object value = getValue(context);

    if (value == null)
      return null;
    else
      return value.getClass();
  }

  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
	   ELException
  {
    return _expr.evalObject(context);
  }

  public boolean isReadOnly(ELContext context)
    throws PropertyNotFoundException,
	   ELException
  {
    return true;
  }

  public void setValue(ELContext context, Object value)
    throws PropertyNotFoundException,
	   PropertyNotWritableException,
	   ELException
  {
    throw new PropertyNotWritableException();
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
