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
 * Represents a field reference that may also be a dotted property,
 * e.g. smtp.mail.host.
 * </pre>
 */
public class PathExpr extends Expr {
  private Expr _expr;

  private String _path;

  /**
   * Creates a new path expression.
   *
   * @param expr the underlying expression
   * @param path the property name to use if null
   */
  public PathExpr(Expr expr, String path)
  {
    _expr = expr;
    _path = path;
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  @Override
  public Expr createField(String field)
  {
    Expr arrayExpr = _expr.createField(new StringLiteral(field));

    return new PathExpr(arrayExpr, _path + '.' + field);
  }

  /**
   * Creates a method call using this as the <code>obj.method</code>
   * expression
   *
   * @param args the arguments for the method
   */
  @Override
  public Expr createMethod(Expr []args)
  {
    if (_expr instanceof ArrayExpr) {
      // jsp/1b71
      
      ArrayExpr array = (ArrayExpr) _expr;

      Expr index = array.getIndex();
      
      if (index instanceof StringLiteral) {
	StringLiteral string = (StringLiteral) index;

	return new MethodExpr(array.getExpr(), string.getValue(), args);
      }
    }
    else if (_expr instanceof ArrayResolverExpr) {
      ArrayResolverExpr array = (ArrayResolverExpr) _expr;

      Expr index = array.getIndex();
      
      if (index instanceof StringLiteral) {
	StringLiteral string = (StringLiteral) index;

	return new MethodExpr(array.getExpr(), string.getValue(), args);
      }
    }
      
    return new FunctionExpr(this, args);
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
    Object value = _expr.getValue(env);

    if (value != null)
      return value;

    return env.getELResolver().getValue(env, _path, null);
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
    os.print("new com.caucho.el.PathExpr(");
    _expr.printCreate(os);
    os.print(", \"");
    os.print(_path);
    os.print("\")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof PathExpr))
      return false;

    PathExpr expr = (PathExpr) o;

    return (_expr.equals(expr._expr) && _path.equals(expr._path));
  }

  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return String.valueOf(_expr);
  }
}
