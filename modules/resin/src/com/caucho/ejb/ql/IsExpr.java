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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.ql;

import com.caucho.bytecode.JClass;

import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.ejb.cfg.EjbEntityBean;

/**
 * An 'is' expression
 */
class IsExpr extends Expr {
  // value expression
  private Expr _value;
  // operation
  private int _op;
  // true if this is a negative between
  private boolean _isNot;

  /**
   * Creates a like expression.
   *
   * @param value the value expression
   * @param op the operation
   * @param isNot if true, this the like is negated
   */
  IsExpr(Query query, Expr value, int op, boolean isNot)
    throws ConfigException
  {
    _query = query;
    
    _value = value;
    _op = op;
    _isNot = isNot;

    evalTypes();
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    if (getJavaType() != null)
      return;

    if (_op == Query.EMPTY && ! _value.isCollection())
      throw error(L.l("IS EMPTY requires a collection at `{0}'",
                      _value));
    else if (_op == Query.NULL && _value.isCollection())
      throw error(L.l("IS NULL requires a single value at `{0}'",
                      _value));

    setJavaType(boolean.class);
  }
  

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    _value.generateWhereSubExpr(cb);

    JClass valueType = _value.getJavaType();
      /*
    if (javax.ejb.EJBLocalObject.class.isAssignableFrom(valueType)) {
      EjbEntityBean bean = _query.getBeanByType(valueType);

      valueType = bean.getPrimaryKey().getJavaType();
      
      if (byte.class.equals(valueType) ||
          short.class.equals(valueType) ||
          int.class.equals(valueType) ||
          long.class.equals(valueType)) {
        if (_op == Query.NULL) {
          if (_isNot)
            cb.append(" <> 0");
          else
            cb.append(" = 0");
          return;
        }
      }
      throw new UnsupportedOperationException();
    }
      */

    if (_isNot)
      cb.append(" IS NOT ");
    else
      cb.append(" IS ");

    switch (_op) {
    case Query.NULL:
      cb.append("NULL");
      break;
    case Query.EMPTY:
      cb.append("EMPTY");
      break;
    }
  }

  
  public String toString()
  {
    String str = _value.toString();

    if (_isNot)
      str += " IS NOT ";
    else
      str += " IS ";

    switch (_op) {
    case Query.NULL:
      return str + "NULL";
    case Query.EMPTY:
      return str + "EMPTY";
    default:
      return super.toString();
    }
  }
}
