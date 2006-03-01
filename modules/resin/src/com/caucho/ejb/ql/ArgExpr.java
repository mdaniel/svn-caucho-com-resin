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

package com.caucho.ejb.ql;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JField;
import com.caucho.bytecode.JMethod;

import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.ejb.cfg.EjbEntityBean;

import com.caucho.amber.type.EntityType;

/**
 * Expression representing a select method argument.
 */
class ArgExpr extends Expr {
  // method index
  private int _index;

  private Class _coerceType;

  /**
   * Creates a new argument expression.
   *
   * @param index the method index
   */
  ArgExpr(Query query, int index)
    throws ConfigException
  {
    _query = query;
    _index = index;

    evalTypes();
  }

  /**
   * Returns the method index.
   */
  int getIndex()
  {
    return _query.getArgIndex(_index);
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    if (getJavaType() != null)
      return;

    JMethod method = _query.getMethod();

    JClass []args = method.getParameterTypes();

    if (args.length <= _index - 1)
      throw error(L.l("`{0}' exceeds number of arguments", "?" + _index));

    setJavaType(args[_index - 1]);

    _query.setArgSize(_index, getComponentCount());
  }

  void setCoerceType(Class javaType)
  {
    _coerceType = javaType;
  }

  /**
   * Only args can be coerced.
   */
  boolean canCoerce()
  {
    return true;
  }

  int getComponentCount()
  {
    JClass javaType = getJavaType();

    if (javaType.isPrimitive() ||
        javaType.isArray() ||
        javaType.getName().startsWith("java."))
      return 1;

    if (javaType.isAssignableTo(javax.ejb.EJBLocalObject.class)) {
      EjbEntityBean bean = _query.getConfig().findEntityByLocal(javaType);

      EntityType type = bean.getEntityType();
    
      return type.getId().getKeys().size();
    }

    JField []fields = javaType.getFields();
    return fields.length;
  }
  
  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    cb.append("?" + getIndex());
  }

  void generateComponent(CharBuffer cb, int index)
  {
    cb.append("?" + (getIndex() + index));
  }

  /**
   * Printable version of the object.
   */
  public String toString()
  {
    return "?" + _index;
  }
}
