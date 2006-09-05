/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.el;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.beans.*;
import java.lang.reflect.*;

import javax.el.*;

import com.caucho.vfs.*;
import com.caucho.util.*;

/**
 * Represents an array reference:
 *
 * <pre>
 * a[b]
 * </pre>
 */
public class ArrayResolverExpr extends Expr {
  private Expr _left;
  private Expr _right;
  
  // cached getter method
  private Class _lastClass;
  private String _lastField;
  private Method _lastMethod;

  /**
   * Creates a new array expression.
   *
   * @param left the object expression
   * @param right the index expression.
   */
  public ArrayResolverExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns the base expression.
   */
  public Expr getExpr()
  {
    return _left;
  }

  /**
   * Returns the index expression.
   */
  public Expr getIndex()
  {
    return _right;
  }

  /**
   * Creates a method for constant arrays.
   */
  @Override
  public Expr createMethod(Expr []args)
  {
    if (! (_right instanceof StringLiteral))
      return null;

    StringLiteral literal = (StringLiteral) _right;

    return new MethodExpr(_left, literal.getValue(), args);
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the evaluated object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object aObj = _left.getValue(env);

    if (aObj == null)
      return null;

    Object fieldObj = _right.getValue(env);
    if (fieldObj == null)
      return null;

    return env.getELResolver().getValue(env, aObj, fieldObj);
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the evaluated object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    Object aObj = _left.getValue(env);

    if (aObj == null)
      throw new PropertyNotFoundException(L.l("'{0}' is null in '{1}'",
					      _left.toString(), toString()));
					      

    Object fieldObj = _right.getValue(env);
    if (fieldObj == null)
      throw new PropertyNotFoundException(L.l("'{0}' is null in '{1}'",
					      _right.toString(), toString()));

    env.getELResolver().setValue(env, aObj, fieldObj, value);
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public MethodInfo getMethodInfo(ELContext env,
				  Class<?> returnType,
				  Class<?> []argTypes)
    throws ELException
  {
    Object base = _left.getValue(env);

    if (base == null)
      throw new ELException(L.l("'{0}' is an illegal method expression.",
				toString()));

    return new MethodInfo(_right.evalString(env),
			  returnType,
			  argTypes);
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
    Object base = _left.getValue(env);

    if (base == null)
      throw new ELException(L.l("'{0}' is an illegal method expression.",
				toString()));

    String name = _right.evalString(env);

    try {
      Method method = base.getClass().getMethod(name, argTypes);

      return method.invoke(base, args);
    } catch (NoSuchMethodException e) {
      throw new ELException(e);
    } catch (IllegalAccessException e) {
      throw new ELException(e);
    } catch (InvocationTargetException e) {
      throw new ELException(e.getCause());
    }
  }

  /**
   * Prints the code to create an LongLiteral.
   *
   * @param os stream to the generated *.java code
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ArrayResolverExpr(");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ArrayResolverExpr))
      return false;

    ArrayResolverExpr expr = (ArrayResolverExpr) o;

    return (_left.equals(expr._left) && _right.equals(expr._right));
  }

  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return _left + "[" + _right + "]";
  }
}
