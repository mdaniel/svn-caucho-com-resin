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
 * Identifier expression.
 */
public class IdExpr extends Expr 
{
  // The identifier name
  private String _id;

  /**
   * Creates the identifier
   */
  public IdExpr(String id)
  {
    _id = id;
  }
  
  protected String getId()
  {
    return _id;
  }
  
  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  @Override
  public Expr createField(String field)
  {
    Expr arrayExpr = createField(new StringLiteral(field));

    return new PathExpr(arrayExpr, _id + '.' + field);
  }
  
  public ValueReference getValueReference(ELContext env) 
    throws ELException 
  {
    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      ValueExpression valueExpression = variableMapper.resolveVariable(_id);
      if (valueExpression != null) {
        return valueExpression.getValueReference(env);
      }
    }
    
    return new ValueReference(null, _id);
  }

  /**
   * Returns true if the expression is read-only.
   */
  @Override
  public boolean isReadOnly(ELContext env)
  {
    if (env.isLambdaArgument(_id)) {
      return true;
    }
    
    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      ValueExpression valueExpression = variableMapper.resolveVariable(_id);
      if (valueExpression != null) {
        return valueExpression.isReadOnly(env);
      }
    }

    env.setPropertyResolved(false);
    
    boolean result = env.getELResolver().isReadOnly(env, null, _id);
    if (env.isPropertyResolved())
      return result;
    
    throw new PropertyNotFoundException(L.l("'{0}' not found in context '{1}'.",
                                            _id, 
                                            env));
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Class<?> getType(ELContext env)
    throws ELException
  {
    if (env.isLambdaArgument(_id)) {
      return Object.class;
    }
    
    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      ValueExpression valueExpression = variableMapper.resolveVariable(_id);
      if (valueExpression != null) {
        return valueExpression.getType(env);
      }
    }
    
    env.setPropertyResolved(false);

    Class<?> type = env.getELResolver().getType(env, null, _id);
    if (env.isPropertyResolved())
      return type;
    
    throw new PropertyNotFoundException(L.l("'{0}' not found in context '{1}'.",
                                            _id, 
                                            env));
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
    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      ValueExpression valueExpression = variableMapper.resolveVariable(_id);
      
      if (valueExpression != null) {
        return valueExpression.getValue(env);
      }
    }
    
    if (ELUtil.isJavaee7()) {
      Object value = getImport(env);
      
      if (value != null) {
        return value;
      }
    }
    
    env.setPropertyResolved(false);

    Object value = env.getELResolver().getValue(env, null, _id);
    
    if (env.isPropertyResolved()) {
      return value;
    }

    return null;
//    throw new PropertyNotFoundException(L.l("'{0}' not found in context '{1}'.",
//                                            _id, 
//                                            env));
  }
  
  private Object getImport(ELContext env)
  {
    ImportHandler importHandler = env.getImportHandler();
    
    if (importHandler != null) {
      Class<?> cl = importHandler.resolveClass(_id);
      
      if (cl != null) {
        return new ELClass(cl);
      }
      
      cl = importHandler.resolveStatic(_id);
      
      if (cl != null) {
        return new ELClass(cl);
      }
    }
    
    return null;
  }

  /**
   * Evaluates the expression, setting an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    if (env.isLambdaArgument(_id)) {
      throw new PropertyNotWritableException(
        L.l("lambda parameter '{0}' is read-only", _id));
    }
    
    VariableMapper variableMapper = env.getVariableMapper();
    if (variableMapper != null) {
      ValueExpression valueExpression = variableMapper.resolveVariable(_id);
      if (valueExpression != null) {
        valueExpression.setValue(env, value);
      }
    }
    
    env.setPropertyResolved(false);
    env.getELResolver().setValue(env, null, _id, value);
    if (! env.isPropertyResolved()) {
      throw new PropertyNotFoundException(L.l("'{0}' not found in context '{1}'.",
                                              _id, 
                                              env));
    }
  }

  /**
   * Prints the code to create an IdExpr.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.IdExpr(\"");
    printEscapedString(os, _id);
    os.print("\")");
  }

  public boolean equals(Object o)
  {
    if (o == null || ! o.getClass().equals(IdExpr.class))
      return false;

    IdExpr expr = (IdExpr) o;

    return _id.equals(expr._id);
  }

  public String toString()
  {
    return _id;
  }
}
