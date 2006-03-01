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

import java.util.ArrayList;
import java.util.Collections;

import com.caucho.bytecode.JClass;

import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.IdField;

import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.ejb.cfg.CmrRelation;

/**
 * Expression representing a single-valued relation path.
 */
class RelationExpr extends PathExpr {
  // base expression
  private PathExpr _base;
  // field name
  private String _fieldName;

  // relation information
  private CmrRelation _relation;

  private String _keyTable;
  private String []_keyColumns;

  private boolean _usesField;

  /**
   * Creates a new field expression.
   *
   * @param base the base expression
   * @param field name of the field
   */
  RelationExpr(Query query, PathExpr base,
               String field, CmrRelation relation)
    throws ConfigException
  {
    super(relation.getTargetBean());
    
    _query = query;
    _base = base;
    _fieldName = field;
    _relation = relation;
    
    // base.setUsesField();

    /*
    setJavaType(relation.getJavaType());

    _keyTable = base.getKeyTable();
    _keyColumns = relation.getTargetSQLColumns();
    
    if (relation.isImplicit())
      setUsesField();
    */
  }

  /**
   * Gets the Java Type of the expression.
   */
  public JClass getJavaType()
  {
    return _relation.getTargetType();
  }

  void setUsesField()
  {
    if (_usesField)
      return;

    _usesField = true;

    /*
    _keyTable = "caucho" + _query.getUnique();

    _query.addFromItem(_keyTable, _bean.getSQLTable());
    _keyColumns = _relation.addCommonTargetLinks(_query,
						 _base.getTable(),
						 _keyTable);
    */
    //throw new UnsupportedOperationException();
  }

  String getKeyTable()
  {
    return _keyTable;
  }

  String []getKeyFields()
  {
    return _keyColumns;
  }

  String getTable()
  {
    return _keyTable;
  }

  /**
   * Returns the identifier name.
   */
  String getName()
  {
    return _keyTable;
  }

  /**
   * Returns the persistent bean this id is a member of
   */
  EjbEntityBean getBean()
  {
    return _bean;
  }

  EjbEntityBean getItemBean()
  {
    return _bean;
  }

  /**
   * Returns the EJB name.
   */
  String getReturnEJB()
  {
    return _bean.getEJBName();
  }

  int getComponentCount()
  {
    return getItemBean().getEntityType().getId().getKeyCount();
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateSelect(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  String getSelectTable(CharBuffer cb)
  {
    return getKeyTable();
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    _base.generateWhere(cb);
    cb.append(".");
    cb.append(_fieldName);
  }

  /*
  void generateComponent(CharBuffer cb, int index)
  {
    // generateWhere(cb);
    cb.append(getKeyTable());
    cb.append(".");
    cb.append(getKeyFields()[index]);
  }
  */

  void generateComponent(CharBuffer cb, int index)
  {
    EjbEntityBean bean = getItemBean();

    EntityType type = bean.getEntityType();
    
    _base.generateWhere(cb);
    cb.append(".");
    cb.append(_fieldName);
    cb.append(".");

    cb.append(keyComponent(type, index));
  }
  
  /**
   * Returns true if the two expressions are equal
   */
  public boolean equals(Object bObj)
  {
    if (! (bObj instanceof RelationExpr))
      return false;

    RelationExpr b = (RelationExpr) bObj;

    return _fieldName.equals(b._fieldName) && _base.equals(b._base);
  }

  /**
   * Returns a hash code for the expression
   */
  public int hashCode()
  {
    return 65531 * _fieldName.hashCode() + _base.hashCode();
  }

  /**
   * Printable version of the object.
   */
  public String toString()
  {
    return String.valueOf(_base) + '.' + _fieldName;
  }
}
