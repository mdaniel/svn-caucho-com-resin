/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Alex Rojkov
 */

package com.caucho.el;

import com.caucho.jsp.el.JspELParser;
import com.caucho.v5.el.BigDecimalValueExpression;
import com.caucho.v5.el.BigIntegerValueExpression;
import com.caucho.v5.el.BooleanValueExpression;
import com.caucho.v5.el.ByteValueExpression;
import com.caucho.v5.el.CharacterValueExpression;
import com.caucho.v5.el.DoubleValueExpression;
import com.caucho.v5.el.Expr;
import com.caucho.v5.el.FloatValueExpression;
import com.caucho.v5.el.IntegerValueExpression;
import com.caucho.v5.el.LongValueExpression;
import com.caucho.v5.el.MethodExpressionImpl;
import com.caucho.v5.el.ObjectLiteralValueExpression;
import com.caucho.v5.el.ObjectValueExpression;
import com.caucho.v5.el.ShortValueExpression;
import com.caucho.v5.el.StringValueExpression;
import com.caucho.v5.el.stream.StreamELResolver;
import com.caucho.v5.util.L10N;

import javax.el.*;

import java.util.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

public class ExpressionFactoryImpl extends ExpressionFactory 
{
  private static final HashMap<Class<?>, CoerceType> _coerceMap
    = new HashMap<Class<?>, CoerceType>();

  protected static final L10N L = new L10N(ExpressionFactoryImpl.class);
  
  public ExpressionFactoryImpl()
  {
    
  }
  
  public ExpressionFactoryImpl(Properties properties)
  {
    // Properties are optional and can be ignored by an implementation.
    // The name of a property should start with "javax.el."
    // The following are some suggested names for properties.
    // javax.el.cacheSize
  }

  @Override
  public Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    return Expr.coerceToType(obj, targetType);
  }

  @Override
  public MethodExpression
    createMethodExpression(ELContext context,
                           String expression,
                           Class<?> expectedReturnType,
                           Class<?> []expectedParamTypes)
  throws ELException
  {
    JspELParser parser = new JspELParser(context, expression, true);

    Expr expr = parser.parse();
    
    if (! expr.isArgsProvided() && expectedParamTypes == null) { 
      throw new NullPointerException();
    }

    return new MethodExpressionImpl(expr, 
                                    expression,
                                    expectedReturnType,
                                    expectedParamTypes);
  }

  @Override
  public ValueExpression
  createValueExpression(ELContext context,
                        String expression,
                        Class<?> expectedType)
    throws ELException
  {
    if (expectedType == null)
      throw new NullPointerException(L.l("expectedType can't be null"));

    JspELParser parser = new JspELParser(context, expression);

    Expr expr = parser.parse();

    return createValueExpression(expr, expression, expectedType);
  }

  @Override
  public ValueExpression
    createValueExpression(Object instance,
                          Class<?> expectedType)
  throws ELException
  {
    if (expectedType == null)
      throw new NullPointerException(L.l("expectedType can't be null"));

    return new ObjectLiteralValueExpression(instance, expectedType);
  }

  public ValueExpression createValueExpression(Expr expr,
                                               String expression,
                                               Class<?> expectedType)
  {
    CoerceType type = _coerceMap.get(expectedType);

    if (type == null)
      return new ObjectValueExpression(expr, expression, expectedType);

    switch (type) {
      case BOOLEAN:
        return new BooleanValueExpression(expr, expression, expectedType);
      case CHARACTER:
        return new CharacterValueExpression(expr, expression, expectedType);
      case BYTE:
        return new ByteValueExpression(expr, expression, expectedType);
      case SHORT:
        return new ShortValueExpression(expr, expression, expectedType);
      case INTEGER:
        return new IntegerValueExpression(expr, expression, expectedType);
      case LONG:
        return new LongValueExpression(expr, expression, expectedType);
      case FLOAT:
        return new FloatValueExpression(expr, expression, expectedType);
      case DOUBLE:
        return new DoubleValueExpression(expr, expression, expectedType);
      case STRING:
        return new StringValueExpression(expr, expression, expectedType);
      case BIG_DECIMAL:
        return new BigDecimalValueExpression(expr, expression, expectedType);
      case BIG_INTEGER:
        return new BigIntegerValueExpression(expr, expression, expectedType);
    }

    return new ObjectValueExpression(expr, expression, expectedType);
  }
  
  @Override
  public ELResolver getStreamELResolver() 
  {
    return new StreamELResolver();
  }
  
  @Override
  public Map<String, Method> getInitFunctionMap() 
  {
    // TODO: Retrieve a function map containing a pre-configured function 
    // mapping. It must include the following functions.
    // linq:range
    // linq:repeat
    // linq:_empty

    return null;
  }

  private enum CoerceType {
    BOOLEAN,
    CHARACTER,
    STRING,
    INTEGER,
    DOUBLE,
    LONG,
    FLOAT,
    SHORT,
    BYTE,
    BIG_INTEGER,
    BIG_DECIMAL,
    VOID
  }

  static {
    _coerceMap.put(boolean.class, CoerceType.BOOLEAN);
    _coerceMap.put(Boolean.class, CoerceType.BOOLEAN);

    _coerceMap.put(byte.class, CoerceType.BYTE);
    _coerceMap.put(Byte.class, CoerceType.BYTE);

    _coerceMap.put(short.class, CoerceType.SHORT);
    _coerceMap.put(Short.class, CoerceType.SHORT);

    _coerceMap.put(int.class, CoerceType.INTEGER);
    _coerceMap.put(Integer.class, CoerceType.INTEGER);

    _coerceMap.put(long.class, CoerceType.LONG);
    _coerceMap.put(Long.class, CoerceType.LONG);

    _coerceMap.put(float.class, CoerceType.FLOAT);
    _coerceMap.put(Float.class, CoerceType.FLOAT);

    _coerceMap.put(double.class, CoerceType.DOUBLE);
    _coerceMap.put(Double.class, CoerceType.DOUBLE);

    _coerceMap.put(char.class, CoerceType.CHARACTER);
    _coerceMap.put(Character.class, CoerceType.CHARACTER);

    _coerceMap.put(String.class, CoerceType.STRING);

    _coerceMap.put(BigDecimal.class, CoerceType.BIG_DECIMAL);
    _coerceMap.put(BigInteger.class, CoerceType.BIG_INTEGER);

    _coerceMap.put(void.class, CoerceType.VOID);
  }
}
