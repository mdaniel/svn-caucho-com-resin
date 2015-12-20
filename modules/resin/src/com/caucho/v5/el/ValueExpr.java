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

import com.caucho.v5.vfs.WriteStream;

import javax.el.*;

import java.io.IOException;

/**
 * ValueExpression expression.
 */
public class ValueExpr extends Expr {
  // The identifier name
  private final String _name;
  
  private final ValueExpression _valExpr;

  /**
   * Creates the identifier
   */
  public ValueExpr(String name, ValueExpression valExpr)
  {
    _name = name;
    _valExpr = valExpr;
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  @Override
  public Expr createField(String field)
  {
    if (_valExpr instanceof FieldGenerator) {
      FieldGenerator gen = (FieldGenerator) _valExpr;
      
      ValueExpression fieldExpr = gen.createField(field);

      if (fieldExpr != null)
        return new ValueExpr(field, fieldExpr);
    }
    
    Expr arrayExpr = createField(new StringLiteral(field));

    return new PathExpr(arrayExpr, _name + '.' + field);
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    env.notifyBeforeEvaluation(_name);
    Object value = _valExpr.getValue(env);
    env.notifyAfterEvaluation(_name);
    return value;
  }
  
  @Override
  public ValueReference getValueReference(ELContext env)
  {
    return _valExpr.getValueReference(env);
  }

  /**
   * Sets teh value.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    _valExpr.setValue(env, value);
  }

  /**
   * Prints the code to create an IdExpr.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.ValueExpr(\"");
    printEscapedString(os, _name);
    os.print("\")");
  }
  
  /**
   * Returns true if the expression is read-only.
   */
  @Override
  public boolean isReadOnly(ELContext env)
  {
    boolean result = env.getELResolver().isReadOnly(env, null, _name);
    
    if (env.isPropertyResolved()) {
      return result;
    }

    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      if (variableMapper.resolveVariable(_name) != null) {
        return false;
      }
    }
    
    throw new PropertyNotFoundException(L.l("'{0}' not found in context '{1}'.", _name, env));
  }

  public boolean equals(Object o)
  {
    if (o == null || ! o.getClass().equals(ValueExpr.class))
      return false;

    ValueExpr expr = (ValueExpr) o;

    return _valExpr.equals(expr._valExpr);
  }

  public String toString()
  {
    return _name;
  }
}