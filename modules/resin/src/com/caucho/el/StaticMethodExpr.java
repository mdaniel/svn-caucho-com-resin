/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.beans.*;
import java.lang.reflect.*;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.config.types.Signature;

/**
 * Represents a method call.  The expr will evaluate to a method.
 */
public class StaticMethodExpr extends Expr {
  private Method _method;
  private Marshall []_marshall;

  /**
   * Creates a new method expression.
   *
   * @param expr the expression generating the method to be called
   * @param args the arguments for the method
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
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
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
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   */
  public Object evalObject(VariableResolver env)
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
			   VariableResolver env)
    throws ELException
  {
    if (_marshall.length != args.length)
      return null;

    try {
      Object []objs = new Object[args.length];
      
      for (int i = 0; i < _marshall.length; i++)
        objs[i] = _marshall[i].marshall(args[i], env);

      return _method.invoke(null, objs);
    } catch (ELException e) {
      throw e;
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
    os.print("new com.caucho.el.StaticMethodExpr(\"");
    printType(os, _method.getReturnType());
    os.print(" ");
    os.print(_method.getDeclaringClass().getName());
    os.print(".");
    os.print(_method.getName());
    os.print("(");
    Class []parameterTypes = _method.getParameterTypes();
    
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i != 0)
        os.print(", ");
      printType(os, parameterTypes[i]);
    }
    os.print(")");
    os.print("\")");
  }

  private void printType(WriteStream os, Class cl)
    throws IOException
  {
    if (cl.isArray()) {
      printType(os, cl.getComponentType());
      os.print("[]");
    }
    else
      os.print(cl.getName());
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
}
