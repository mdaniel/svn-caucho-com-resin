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
 * @author Paul Cowan
 */

package javax.el;

import java.lang.reflect.Method;

public class ELProcessor
{
  private ELProcessor _delegate;
  
  public ELProcessor()
  {
  }
  
  public ELProcessor getDelegate()
  {
    if (_delegate == null) {
      _delegate = (ELProcessor) 
        DelegateFactory.newInstance(ELProcessor.class, 
                                    "com.caucho.v5.el.ELProcessorImpl",
                                    null);
    }
    
    return _delegate;
  }
  
  public ELManager getELManager()
  {
    return getDelegate().getELManager();
  }
  
  public Object eval(String expression)
  {
    return getDelegate().eval(expression);
  }
  
  public Object getValue(String expression, Class<?> expectedType)
  {
    return getDelegate().getValue(expression, expectedType);
  }
  
  public void setValue(String expression, Object value)
  {
    getDelegate().setValue(expression, value);
  }
  
  public void setVariable(String variable, String expression)
  {
    getDelegate().setVariable(variable, expression);
  }
  
  public void defineFunction(String prefix, 
                             String function, 
                             String className,
                             String methodName) 
    throws ClassNotFoundException, NoSuchMethodException
  {
    getDelegate().defineFunction(prefix, function, className, methodName);
  }
  
  public void defineFunction(String prefix, String function, Method method)
    throws NoSuchMethodException
  {
    getDelegate().defineFunction(prefix, function, method);
  }
  
  public void defineBean(String name, Object bean)
  {
    getDelegate().defineBean(name, bean);
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
