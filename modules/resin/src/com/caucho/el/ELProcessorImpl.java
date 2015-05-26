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

import java.lang.reflect.*;

import javax.el.*;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Signature;
import com.caucho.util.CauchoUtil;
import com.caucho.util.L10N;

public class ELProcessorImpl extends ELProcessor
{
  private static final L10N L = new L10N(ELProcessor.class);
  
  private final ELManager _elManager;
  private final ExpressionFactory _expressionFactory;
  
  public ELProcessorImpl()
  {
    _elManager = new ELManager();
    _expressionFactory = ELManager.getExpressionFactory();
  }
  
  public ELManager getELManager()
  {
    return _elManager;
  }
  
  public Object eval(String expression)
  {
    return getValue(expression, Object.class);
  }
  
  public Object getValue(String expression, Class<?> expectedType)
  {
    ELContext context = _elManager.getELContext();
    ValueExpression valueExpression = 
      _expressionFactory.createValueExpression(context, 
                                               addBrackets(expression), 
                                               expectedType);
    return valueExpression.getValue(context);
  }
  
  public void setValue(String expression, Object value)
  {
    ELContext context = _elManager.getELContext();
    ValueExpression valueExpression = 
      _expressionFactory.createValueExpression(context, 
                                               addBrackets(expression), 
                                               Object.class);
    valueExpression.setValue(context, value);
  }
  
  public void setVariable(String variable, String expression)
  {
    ELContext context = _elManager.getELContext();
    ValueExpression valueExpression = 
      _expressionFactory.createValueExpression(context, 
                                               addBrackets(expression), 
                                               Object.class);
    _elManager.setVariable(variable, valueExpression);
  }
  
  public void defineFunction(String prefix, 
                             String name, 
                             String className,
                             String methodName) 
    throws ClassNotFoundException, NoSuchMethodException
  {
    if (prefix == null || name == null || 
        className == null || methodName == null)
      throw new NullPointerException();
    
    Method method = null;
    
    if (methodName.indexOf('(') >= 0) {
      try {
        Signature methodSignature = new Signature(methodName);
        method = methodSignature.getMethod();
      } catch (ConfigException e) {
        
      }
    }
    
    if (method == null) {
      Class<?> cl = CauchoUtil.loadClass(className);
      
      Method []methods = cl.getMethods();
      for (Method m : methods) {
        if (methodName.equals(m.getName()))
          method = m;
      }
    }
    
    if (method == null) {
      throw new NoSuchMethodException(methodName);
    }
    
    defineFunction(prefix, name, method);
  }
  
  public void defineFunction(String prefix, String name, Method method)
    throws NoSuchMethodException
  {
    if (prefix == null || name == null || method == null)
      throw new NullPointerException();
    
    if (! Modifier.isStatic(method.getModifiers())) {
      throw new NoSuchMethodException(L.l("Can only map static functions: {0}",
                                          method.getName()));
    }
    
    if (name.isEmpty()) {
      name = method.getName();
    }

    _elManager.mapFunction(prefix, name, method);
  }
  
  public void defineBean(String name, Object bean)
  {
    _elManager.defineBean(name, bean);
  }
  
  private static String addBrackets(String expr)
  {
    return String.format("${%s}", expr);
  }

}
