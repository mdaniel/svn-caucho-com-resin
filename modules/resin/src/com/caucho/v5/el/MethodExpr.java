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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.el.*;

import com.caucho.v5.el.stream.CollectionStream;
import com.caucho.v5.vfs.WriteStream;

/**
 * Represents a named method call on an object.
 */
@SuppressWarnings("serial")
public class MethodExpr extends Expr 
{
  private Expr _expr;
  private String _methodName;
  private Expr []_args;

  /**
   * Creates a new method expression.
   *
   * @param expr the expression generating the object on which the method 
   *        is to be called
   * @param methodName the name of the method to call
   * @param args the arguments for the method
   */
  public MethodExpr(Expr expr, String methodName, Expr []args)
  {
    _expr = expr;
    _methodName = methodName;
    _args = args;
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object base = _expr.getValue(env);
    if (base == null) {
      return null;
    }
    
    Object []params = new Object[_args.length];
    for (int i=0; i<_args.length; i++) {
      params[i] = _args[i].getValue(env);
    }
      
    return env.getELResolver().invoke(env, base, _methodName, null, params);
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public Object invoke(ELContext env, Class<?> []argTypes, Object []args)
    throws ELException
  {
    if (args != null && args.length != 0)
      throw new ELException(L.l("'{0}' is an illegal method invocation on {1}",
                                toString(), getClass().getName()));
    
    return getValue(env);
  }
  
  @Override
  public boolean isArgsProvided()
  {
    return true;
  }
  
  static Object evalArg(Class<?> cl, Expr expr, ELContext env)
    throws ELException
  {
    Marshall marshall = Marshall.create(cl);
    return marshall.marshall(expr, env);
  }
  
  /**
   * Prints the code to create an LongLiteral.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.MethodExpr(");
    _expr.printCreate(os);
    os.print(", \"");
    os.print(_methodName);
    os.print("\", new com.caucho.v5.el.Expr[] {");
    
    for (int i = 0; i < _args.length; i++) {
      if (i != 0)
        os.print(", ");
      _args[i].printCreate(os);
    }
    os.println("})");
  }
  
  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof MethodExpr))
      return false;
    
    MethodExpr expr = (MethodExpr) o;
    
    if (! _expr.equals(expr._expr))
      return false;
    
    if (! _methodName.equals(expr._methodName))
      return false;
    
    if (_args.length != expr._args.length)
      return false;
    
    for (int i = 0; i < _args.length; i++) {
      if (! _args[i].equals(expr._args[i]))
        return false;
    }
    
    return true;
  }
  
  /**
   * Returns the printed version.
   */
  public String toString()
  {
    return "MethodExpr[" + _expr + "," + _methodName + "]";
  }

}
