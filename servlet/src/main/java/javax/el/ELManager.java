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

package javax.el;

import java.lang.reflect.Method;

public class ELManager
{
  private static ExpressionFactory _expressionFactory;

  private StandardELContext _elContext;
  
  public ELManager()
  {
    _elContext = new StandardELContext(getExpressionFactory());
  }
  
  public StandardELContext getELContext()
  {
    return _elContext;
  }
  
  public ELContext setELContext(ELContext context)
  {
    ELContext old = _elContext;
    _elContext = new StandardELContext(context);
    return old;
  }
  
  public void addBeanNameResolver(BeanNameResolver beanNameResolver)
  {
    _elContext.addELResolver(new BeanNameELResolver(beanNameResolver));
  }
  
  public void addELResolver(ELResolver elResolver)
  {
    _elContext.addELResolver(elResolver);
  }
  
  public void mapFunction(String prefix, String localName, Method method)
  {
    _elContext.getFunctionMapper().mapFunction(prefix, localName, method);
  }

  public void setVariable(String variable, ValueExpression expression)
  {
    _elContext.getVariableMapper().setVariable(variable, expression);
  }
  
  public void importStatic(String staticName) throws ELException
  {
    _elContext.getImportHandler().importStatic(staticName);
  }
  
  public void importClass(String className) throws ELException
  {
    _elContext.getImportHandler().importClass(className);
  }
  
  public void importPackage(String packageName)
  {
    _elContext.getImportHandler().importPackage(packageName);
  }
  
  public Object defineBean(String name, Object bean)
  {
    return _elContext.defineBean(name, bean);
  }
  
  public void addEvaluationListener(EvaluationListener evaluationListener)
  {
    _elContext.addEvaluationListener(evaluationListener);
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
  
  public static ExpressionFactory getExpressionFactory()
  {
    if (_expressionFactory == null)
      _expressionFactory = ExpressionFactory.newInstance();
    return _expressionFactory;
  }
}
