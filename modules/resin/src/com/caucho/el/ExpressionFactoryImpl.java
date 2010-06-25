/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.el;

import com.caucho.jsp.el.JspApplicationContextImpl;
import com.caucho.jsp.el.JspExpressionFactoryImpl;

import javax.el.ExpressionFactory;
import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.ELContext;
import javax.el.ValueExpression;
import java.util.Properties;

public class ExpressionFactoryImpl extends ExpressionFactory {

  private ExpressionFactory _factory;

  public ExpressionFactoryImpl() {
    this(null);
  }

  public ExpressionFactoryImpl(Properties properties)
  {
    JspApplicationContextImpl jspAppContext
      = JspApplicationContextImpl.getCurrent();

    if (jspAppContext != null)
      _factory = jspAppContext.getExpressionFactory();


    if (_factory == null)
      _factory = new JspExpressionFactoryImpl(null);
  }

  @Override
  public Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    return _factory.coerceToType(obj, targetType);
  }

  @Override
  public MethodExpression
    createMethodExpression(ELContext context,
                           String expression,
                           Class<?> expectedReturnType,
                           Class<?> []expectedParamTypes)
  throws ELException
  {
    return _factory.createMethodExpression(context,
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
    return _factory.createValueExpression(context, expression, expectedType);
  }

  @Override
  public ValueExpression
    createValueExpression(Object instance,
                          Class<?> expectedType)
  throws ELException
  {
    return _factory.createValueExpression(instance, expectedType);
  }
}
