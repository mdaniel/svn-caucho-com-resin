/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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

package com.caucho.v5.el;

import java.io.IOException;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.ValueReference;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

/**
 * Represents a value assignment, * A=B
 */
public class AssignExpr extends Expr
{
  protected static final Logger log = 
    Logger.getLogger(AssignExpr.class.getName());
  protected static final L10N L = new L10N(AssignExpr.class);

  private final Expr _left;
  private final Expr _right;

  public AssignExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }
  
  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return (_right.isConstant() && _right.isConstant());
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {
    //if (_left.isConstant() || _left.isReadOnly(env) || _left.isLiteralText())
    if (_left.isConstant() || _left.isLiteralText())
      throw new PropertyNotWritableException(_left.getExpressionString());
    
    Object baseA = null;
    Object propA = null;

    ValueReference valueReference = _left.getValueReference(env);
    if (valueReference != null) {
      baseA = valueReference.getBase();
      propA = valueReference.getProperty();
    }
    
    if (baseA == null && propA instanceof String) {
      // If prop-a is a Lambda parameter, throw a PropertyNotWritableException
      
      ValueExpression propAValueExpr = 
        env.getVariableMapper().resolveVariable((String)propA);
      
      if (propAValueExpr != null) {
        ValueReference propAValueReference = 
          propAValueExpr.getValueReference(env);
        
        baseA = propAValueReference.getBase();
        propA = propAValueReference.getProperty();
      }
    }
    
    Object valueB = _right.getValue(env);
    
    if (propA == null && _left instanceof Expr) {
      ((Expr)_left).setValue(env, valueB);
    } 
    else if (propA != null) {
      env.getELResolver().setValue(env, baseA, propA, valueB);
    } 
    else {
      invocationError(new IllegalStateException("Unhandled assignment " + _left.getClass().getName()));
    }
    
    return valueB;
  }
  
  /**
   * Prints the Java code to recreate the expr
   *
   * @param os the output stream to the *.java file
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.AssignExpr(");
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
    if (! (o instanceof AssignExpr))
      return false;

    AssignExpr expr = (AssignExpr) o;

    return (_left.equals(expr._left) &&
            _right.equals(expr._right));
  }

  @Override
  public String toString()
  {
    return String.format("(%s = %s)", _left, _right);
  }
}