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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.el.ELContext;
import javax.el.ELException;

import com.caucho.v5.config.types.Signature;
import com.caucho.v5.vfs.WriteStream;

/**
 * Represents a method call.  The expr will evaluate to a method.
 */
public class StaticMethodExpr extends Expr {
  private static final Object []NULL_ARGS = new Object[0];
  
  private Method _method;
  private Marshall []_marshall;
  private boolean _isVoid;

  /**
   * Creates a new method expression.
   *
   * @param method - the target method
   */
  public StaticMethodExpr(Method method)
  {
    _method = method;

    initMethod();
  }

  /**
   * Creates a new static method.
   *
   * @param signature signature
   */
  public StaticMethodExpr(String signature)
  {
    try {
      Signature sig = new Signature();
      sig.addText(signature);
      sig.init();

      _method = sig.getMethod();

      if (_method == null)
        throw new RuntimeException(L.l("'{0}' is an unknown method",
                                       sig));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
      // log.log(Level.FINE, e.toString(), e);
    }
    
    initMethod();
  }

  /**
   * Initialize the marshall arguments.
   */
  private void initMethod()
  {
    Class []param = _method.getParameterTypes();

    _marshall = new Marshall[param.length];

    for (int i = 0; i < _marshall.length; i++) {
      _marshall[i] = Marshall.create(param[i]);
    }

    _isVoid = void.class.equals(_method.getReturnType());
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
    return _method;
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   */
  public Object evalMethod(Expr []args,
                           ELContext env)
    throws ELException
  {
    if (_marshall.length != args.length) {
      // jsp/18i8
      throw new ELParseException(L.l("Arguments to '{0}' do not match expected length {1}.", _method.getName(), _marshall.length));
    }

    try {
      Object []objs;
      
      if (args.length > 0) {
        objs = new Object[args.length];
      
        for (int i = 0; i < _marshall.length; i++)
          objs[i] = _marshall[i].marshall(args[i], env);
      }
      else
        objs = NULL_ARGS;

      if (! _isVoid)
        return _method.invoke(null, objs);
      else {
        _method.invoke(null, objs);

        return null;
      }
    } catch (ELException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new ELException(e.getCause());
    } catch (Exception e) {
      throw new ELException(e);
    }
  }

  /**
   * Prints the code to create an LongLiteral.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.StaticMethodExpr(\"");
    printType(os, _method.getReturnType());
    os.print(" ");
    os.print(_method.getDeclaringClass().getName());
    os.print(".");
    os.print(_method.getName());
    os.print("(");
    Class<?> []parameterTypes = _method.getParameterTypes();
    
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i != 0)
        os.print(", ");
      printType(os, parameterTypes[i]);
    }
    os.print(")");
    os.print("\")");
  }

  private void printType(WriteStream os, Class<?> cl)
    throws IOException
  {
    if (cl.isArray()) {
      printType(os, cl.getComponentType());
      os.print("[]");
    }
    else
      os.print(cl.getName());
  }

  private Object writeReplace()
  {
    StringBuilder sig = new StringBuilder();

    addType(sig, _method.getReturnType());
    sig.append(" ");
    sig.append(_method.getDeclaringClass().getName());
    sig.append(".");
    sig.append(_method.getName());
    sig.append("(");
    Class<?> []param = _method.getParameterTypes();
    
    for (int i = 0; i < param.length; i++) {
      if (i != 0)
        sig.append(",");
      
      addType(sig, param[i]);
    }
    sig.append(")");
    
    return new Handle(sig.toString());
  }

  private void addType(StringBuilder sb, Class<?> cl)
  {
    if (cl.isArray()) {
      addType(sb, cl.getComponentType());
      sb.append("[]");
    }
    else
      sb.append(cl.getName());
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof StaticMethodExpr))
      return false;

    StaticMethodExpr expr = (StaticMethodExpr) o;

    return _method.equals(expr._method);
  }
                      
  public String toString()
  {
    return _method.getName();
  }

  static class Handle implements Serializable {
    private String _signature;

    private Handle()
    {
    }

    private Handle(String signature)
    {
      _signature = signature;
    }

    public Object readResolve()
    {
      return new StaticMethodExpr(_signature);
    }
  }
}
