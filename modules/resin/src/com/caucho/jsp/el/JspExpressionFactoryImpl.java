/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package com.caucho.jsp.el;

import java.math.*;
import java.util.*;

import javax.el.*;

import com.caucho.el.*;

/**
 * Represents an EL expression factory
 */
public class JspExpressionFactoryImpl extends  ExpressionFactory {
  private final JspApplicationContextImpl _jspApplicationContext;

  JspExpressionFactoryImpl(JspApplicationContextImpl jspApplicationContext)
  {
    _jspApplicationContext = jspApplicationContext;
  }
  
  public Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    throw new UnsupportedOperationException();
  }

  public MethodExpression
    createMethodExpression(ELContext context,
			   String expression,
			   Class<?> expectedReturnType,
			   Class<?>[] expectedParamTypes)
    throws ELException
  {
    throw new UnsupportedOperationException();
  }

  public ValueExpression
    createValueExpression(ELContext context,
			  String expression,
			  Class<?> expectedType)
    throws ELException
  {
    JspELParser parser = new JspELParser(context, expression);

    Expr expr = parser.parse();

    return createValueExpression(expr, expression, expectedType);
  }

  public static ValueExpression createValueExpression(Expr expr,
						      String expression,
						      Class<?> expectedType)
  {
    if (String.class == expectedType)
      return new StringValueExpression(expr, expression);
    else if (Integer.class == expectedType || int.class == expectedType)
      return new IntegerValueExpression(expr, expression);
    else if (Double.class == expectedType || double.class == expectedType)
      return new DoubleValueExpression(expr, expression);
    else if (Long.class == expectedType || long.class == expectedType)
      return new LongValueExpression(expr, expression);
    else if (Float.class == expectedType || float.class == expectedType)
      return new FloatValueExpression(expr, expression);
    else if (Short.class == expectedType || short.class == expectedType)
      return new ShortValueExpression(expr, expression);
    else if (Byte.class == expectedType || byte.class == expectedType)
      return new ByteValueExpression(expr, expression);
    else if (BigDecimal.class == expectedType)
      return new BigDecimalValueExpression(expr, expression);
    else if (BigInteger.class == expectedType)
      return new BigIntegerValueExpression(expr, expression);
    else if (Boolean.class == expectedType)
      return new BooleanValueExpression(expr, expression);
    else if (Character.class == expectedType || char.class == expectedType)
      return new CharacterValueExpression(expr, expression);
    else
      return new ObjectValueExpression(expr, expression);
  }

  public ValueExpression
    createValueExpression(Object instance,
			  Class<?> expectedType)
    throws ELException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "JspExpressionFactoryImpl[" + _jspApplicationContext.getWebApp() + "]";
  }
}
