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

package com.caucho.v5.config.el;

import java.lang.reflect.Method;
import java.util.Map;

import javax.el.*;

/**
 * Variable resolution for webbeans variables
 */
public class CandiExpressionFactory extends ExpressionFactory {
  private final ExpressionFactory _factory;
  
  public CandiExpressionFactory(ExpressionFactory factory)
  {
    _factory = factory;
  }

  @Override
  public Object coerceToType(Object obj, Class<?> targetType)
      throws ELException
  {
    return _factory.coerceToType(obj, targetType);
  }

  @Override
  public MethodExpression createMethodExpression(ELContext context,
                                                 String expression,
                                                 Class<?> expectedReturnType,
                                                 Class<?>[] expectedParamTypes)
    throws ELException
  {
    MethodExpression expr 
      = _factory.createMethodExpression(context, expression,
                                        expectedReturnType, expectedParamTypes);
      
    return expr;
  }

  @Override
  public ValueExpression createValueExpression(ELContext context,
                                               String expression,
                                               Class<?> expectedType)
      throws ELException
  {
    ValueExpression expr 
    = _factory.createValueExpression(context, expression, expectedType);
  
    return new CandiValueExpression(expr);
  }

  @Override
  public ValueExpression createValueExpression(Object instance,
                                               Class<?> expectedType)
      throws ELException
  {
    ValueExpression expr
      = _factory.createValueExpression(instance, expectedType);
    
    return new CandiValueExpression(expr);
  }

  @Override
  public ELResolver getStreamELResolver()
  {
    return _factory.getStreamELResolver();
  }

  @Override
  public Map<String, Method> getInitFunctionMap()
  {
    return _factory.getInitFunctionMap();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _factory + "]";
  }
}
