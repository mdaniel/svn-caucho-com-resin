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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import java.io.*;
import java.util.logging.*;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;

/**
 * Represents an equality comparison operation, either eq or ne.
 */
public class EqExpr extends AbstractBooleanExpr {
  private int _op;
  private Expr _left;
  private Expr _right;

  /**
   * Create a new equality comparison.
   *
   * @param op the lexeme code for the operation
   * @param left the left expression
   * @param right the right expression
   */
  public EqExpr(int op, Expr left, Expr right)
  {
    _op = op;
    _left = left;
    _right = right;
  }

  /**
   * Returns true if this is a constant expression.
   */
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
  }
  
  /**
   * Evaluate the expression as a boolean.
   *
   * @param env the variable environment
   */
  public boolean evalBoolean(VariableResolver env)
    throws ELException
  {
    Object aObj = _left.evalObject(env);
    Object bObj = _right.evalObject(env);

    if (aObj == bObj)
      return _op == EQ;

    else if (aObj == null || bObj == null)
      return _op == NE;

    else if (aObj.equals(bObj))
      return _op == EQ;

    try {
      if (aObj instanceof Double || aObj instanceof Float ||
	  bObj instanceof Double || bObj instanceof Float) {
	double a = toDouble(aObj, env);
	double b = toDouble(bObj, env);

	switch (_op) {
	case EQ: return a == b;
	case NE: return a != b;
	}
      }
    
      if (aObj instanceof Number || bObj instanceof Number) {
	long a = toLong(aObj, env);
	long b = toLong(bObj, env);

	switch (_op) {
	case EQ: return a == b;
	case NE: return a != b;
	}
      }

      if (aObj instanceof Boolean || bObj instanceof Boolean) {
	boolean a = toBoolean(aObj, env);
	boolean b = toBoolean(bObj, env);

	switch (_op) {
	case EQ: return a == b;
	case NE: return a != b;
	}
      }

      if (aObj instanceof String || bObj instanceof String) {
	String a = toString(aObj, env);
	String b = toString(bObj, env);

	switch (_op) {
	case EQ: return a.equals(b);
	case NE: return ! a.equals(b);
	}
      }
      
      switch (_op) {
      case EQ: return aObj.equals(bObj);
      case NE: return ! aObj.equals(bObj);
      }
    } catch (ELException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    }

    /*
    ELException e = new ELException(L.l("can't compare {0} and {1}.",
                                        aObj, bObj));

    error(e, env);
    */

    return false;
  }

  /**
   * Prints the Java code to recreate an EqExpr.
   *
   * @param os the stream to the generated *.java
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.EqExpr(");
    os.print(_op + ", ");
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
    if (! (o instanceof EqExpr))
      return false;

    EqExpr expr = (EqExpr) o;

    return (_op == expr._op &&
            _left.equals(expr._left) &&
            _right.equals(expr._right));
  }

  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    String op;

    switch (_op) {
    case EQ:
      op = " eq ";
      break;
    case NE:
      op = " ne ";
      break;
    default:
      op = " unknown(" + _op + ") ";
      break;
    }
        
    return "(" + _left + op + _right + ")";
  }
}
