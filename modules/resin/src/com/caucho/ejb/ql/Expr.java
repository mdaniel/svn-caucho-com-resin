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

package com.caucho.ejb.ql;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

import java.util.logging.Logger;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.ejb.cfg.EjbEntityBean;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyManyToOneField;

/**
 * Parsed expression for EJB-QL.
 */
public class Expr {
  static final Logger log = Log.open(Expr.class);
  static final L10N L = new L10N(Expr.class);

  protected Query _query;
  
  // The type of the expression value
  private JClass _javaType;

  /**
   * Gets the Java Type of the expression.
   */
  public JClass getJavaType()
  {
    return _javaType;
  }

  /**
   * Sets the Java Type of the expression.
   */
  void setJavaType(JClass javaType)
  {
    _javaType = javaType;
  }

  /**
   * Sets the Java Type of the expression.
   */
  void setJavaType(Class javaType)
  {
    setJavaType(JClassLoader.localForName(javaType.getName()));
  }

  /**
   * Returns the EJB name.
   */
  String getReturnEJB()
  {
    return null;
  }

  /**
   * Returns true for object values.
   */
  boolean isKey()
  {
    return getReturnEJB() != null;
  }

  /**
   * True if the type is numeric.
   */
  boolean isNumeric()
  {
    JClass type = getJavaType();

    if (type == null)
      return false;

    String typeName = type.getName();
    
    if ("java.lang.Byte".equals(typeName) ||
	"java.lang.Short".equals(typeName) ||
	"java.lang.Integer".equals(typeName) ||
	"java.lang.Long".equals(typeName) ||
	"java.lang.Float".equals(typeName) ||
	"java.lang.Double".equals(typeName))
      return true;
    else if (! type.isPrimitive())
      return false;
    else if (typeName.equals("boolean") || typeName.equals("char"))
      return false;
    else
      return true;
  }

  /**
   * True if the type is integer.
   */
  boolean isInteger()
  {
    JClass type = getJavaType();
    String typeName = type.getName();

    return ("java.lang.Byte".equals(typeName) ||
            "java.lang.Short".equals(typeName) ||
            "java.lang.Integer".equals(typeName) ||
            "java.lang.Long".equals(typeName) ||
            "byte".equals(typeName) ||
            "short".equals(typeName) ||
            "int".equals(typeName) ||
            "long".equals(typeName));
  }

  /**
   * True if the type is integer.
   */
  static boolean isInteger(JClass type)
  {
   String typeName = type.getName();

    return ("java.lang.Byte".equals(typeName) ||
            "java.lang.Short".equals(typeName) ||
            "java.lang.Integer".equals(typeName) ||
            "java.lang.Long".equals(typeName) ||
            "byte".equals(typeName) ||
            "short".equals(typeName) ||
            "int".equals(typeName) ||
            "long".equals(typeName));
  }

  int getComponentCount()
  {
    return 1;
  }

  /**
   * True if the type is a string.
   */
  boolean isString()
  {
    JClass type = getJavaType();
    String typeName = type.getName();

    return ("java.lang.String".equals(typeName) ||
            "char".equals(typeName) ||
            "java.lang.Character".equals(typeName));
  }

  /**
   * True if the type is a boolean.
   */
  boolean isBoolean()
  {
    JClass type = getJavaType();
    String typeName = type.getName();

    return ("boolean".equals(typeName) ||
	    "java.lang.Boolean".equals(typeName));
  }

  /**
   * True if the type is a date.
   */
  boolean isDate()
  {
    JClass type = getJavaType();
    String typeName = type.getName();

    return ("java.util.Date".equals(typeName) ||
            "java.sql.Timestamp".equals(typeName) ||
            "java.sql.Date".equals(typeName) ||
            "java.sql.Time".equals(typeName));
  }

  /**
   * Only args can be coerced.
   */
  boolean canCoerce()
  {
    return false;
  }

  /**
   * True if the type is a collection.
   */
  boolean isCollection()
  {
    JClass type = getJavaType();

    return type.isAssignableTo(Collection.class);
  }

  /**
   * True if the bean refers to an external entity bean.
   */
  boolean isExternal()
  {
    return false;
  }

  /**
   * Returns the item bean of a collection.
   */
  EjbEntityBean getItemBean()
  {
    return null;
  }

  /**
   * Creates a field expression from this expression.
   */
  Expr newField(String field)
    throws ConfigException
  {
    throw error(L.l("`{0}' can't have field `{1}'.  Only path expressions referring to a single bean have fields.", this, field));
  }

  /**
   * Creates an external entity reference from this expression.
   */
  FieldExpr newReference(String field)
    throws ConfigException
  {
    throw error(L.l("`{0}' can't have reference `{1}'", this, field));
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    if (getJavaType() == null)
      throw error(L.l("'{0}' has no type.", this) + getClass());
  }

  /**
   * Prints the select SQL for this expression
   */
  void generateSelect(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns the select's table
   */
  String getSelectTable(CharBuffer cb)
    throws ConfigException
  {
    throw new IllegalStateException(L.l("`{0}' can't be used in a SELECT expression", this));
  }

  /**
   * Prints the where SQL for this expression
   */
  void generateWhere(CharBuffer cb)
  {
    throw new IllegalStateException(L.l("{0}: '{1}' can't be used in a WHERE expression", getClass().getName(), this));
  }

  /**
   * Prints the where SQL for this expression
   */
  void generateWhereSubExpr(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Prints the where SQL for this expression
   */
  void generateComponent(CharBuffer cb, int i)
  {
    if (i == 0)
      generateWhereSubExpr(cb);
    else
      throw new IllegalStateException(L.l("`{0}' can't be used in a WHERE multi-component", this));
  }

  protected String keyComponent(EntityType type, int index)
  {
    ArrayList<String> names = new ArrayList<String>();

    addKeys(names, type, "");

    Collections.sort(names);

    return names.get(index);
  }

  protected void addKeys(ArrayList<String> names,
			 EntityType type,
			 String prefix)
  {
    for (IdField key : type.getId().getKeys()) {
      if (key instanceof KeyManyToOneField) {
	KeyManyToOneField manyToOne = (KeyManyToOneField) key;

	addKeys(names, manyToOne.getEntityType(),
		prefix + key.getName() + ".");
      }
      else
	names.add(prefix + key.getName());
    }
  }

  /**
   * Creates an error.
   */
  ConfigException error(String msg)
  {
    if (_query != null)
      return _query.error(msg);
    else
      return new ConfigException(msg);
  }
  
  /**
   * Creates an error.
   */
  ConfigException error(Query query, String msg)
  {
    return query.error(msg);
  }
}
