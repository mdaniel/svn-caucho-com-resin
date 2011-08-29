/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg21.CmpField;
import com.caucho.ejb.cfg21.CmpRelation;
import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.util.CharBuffer;

/**
 * Expression representing a field or a relation.
 */
class FieldExpr extends Expr {
  // base expression
  private PathExpr _base;
  // field name
  private String _name;

  // bean information
  private EjbEntityBean _bean;
  // field information
  private CmpField _field;
  // relation information
  private CmpRelation _relation;
  
  // SQL id for the results of this field
  private String _tableId;
  // base id if special
  private String _baseId;
  // sql field
  private String _sqlField;

  private int _primaryKeyFieldIndex;

  // use count
  private int _useCount;
  private boolean _hasRelation;

  /**
   * Creates a new field expression.
   *
   * @param base the base expression
   * @param field name of the field
   */
  FieldExpr(Query query, PathExpr base,
            String fieldName, CmpField field)
    throws ConfigException
  {
    _query = query;
    _base = base;
    _name = fieldName;

    _field = field;

    /*
    PrimaryKey key = base.getItemBean().getPrimaryKey();
    _primaryKeyFieldIndex = key.indexOf(field);
    if (_primaryKeyFieldIndex < 0)
      base.setUsesField();
    */

    setJavaType(field.getJavaType());
  }

  boolean hasRelation()
  {
    return _hasRelation;
  }

  void setRelation()
  {
    _hasRelation = true;
  }

  /**
   * Returns the base expression
   */
  Expr getBase()
  {
    return _base;
  }

  /**
   * Returns the field name
   */
  String getField()
  {
    return _name;
  }

  /**
   * Returns the bean type of the result object
   */
  EjbEntityBean getBean()
  {
    return _bean;
  }

  /**
   * Returns the relation
   */
  CmpRelation getRelation()
  {
    return _relation;
  }

  /**
   * Returns the SQL table identifier for the result of the field expression.
   */
  String getTableId()
  {
    return _tableId;
  }

  /**
   * Sets the SQL table identifier for the result of the field expression.
   */
  void setTableId(String tableId)
  {
    _tableId = tableId;
  }

  /**
   * Returns the SQL id for the base 
   */
  String getBaseId()
  {
    return _base.getTable();
  }

  /**
   * Returns the SQL field reference.
   */
  String getSQLField()
  {
    /*
    if (_sqlField != null)
      return _sqlField;
    else if (_field != null)
      return _field.getSQLName();
    else if (_relation != null)
      return _relation.getTargetSQLPrefix();
    else
      return PersistentBean.javaToSQLName(_name);
    */
    throw new UnsupportedOperationException();
  }

  void decrementUse()
  {
    _useCount--;
  }

  int getUseCount()
  {
    return _useCount;
  }

  /**
   * For collections, returns the underlying item bean.
   */
  EjbEntityBean getItemBean()
  {
    return _bean;
  }

  /*
  int getComponentCount()
  {
    if (isPrimaryKeyField)
      return base.getComponentCount();

    return super.getComponentCount();
  }
  */

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
    /*
  void generateSelect(CharBuffer cb)
  {
    if (_primaryKeyFieldIndex >= 0) {
      _base.printComponent(cb, _primaryKeyFieldIndex);
      return;
    }

    if (_query.getFromList().size() == 1) {
      // special case to handle strange databases
      cb.append(_field.getSQLName());
    }
    else {
      cb.append(_base.getTable());
      cb.append(".");
      cb.append(_field.getSQLName());
    }
    throw new UnsupportedOperationException();
  }
    */

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  String getSelectTable(CharBuffer cb)
    throws ConfigException
  {
    /*
    if (_query.getFromList().size() == 1) {
      // special case to handle strange databases
      return null;
    }
    else
      return _base.getTable();
    */
    return _base.getTable();
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    /*
    if (_primaryKeyFieldIndex >= 0) {
      _base.generateComponent(cb, _primaryKeyFieldIndex);
      return;
    }
    */

      /*
    if (_query.getFromList().size() == 1) {
      // special case to handle strange databases
      cb.append(_field.getSQLName());
    }
    else {
      cb.append(_base.getTable());
      cb.append(".");
      cb.append(_field.getSQLName());
    }
      */

    _base.generateWhere(cb);
    cb.append(".");
    cb.append(_name);
  }

  /*
  void generateComponent(CharBuffer cb, int i)
  {
    if (_primaryKeyFieldIndex >= 0) {
      _base.generateComponent(cb, i);
      return;
    }

    super.generateComponent(cb, i);
  }
  */

  /**
   * Returns true if the two expressions are equal
   */
  public boolean equals(Object bObj)
  {
    if (! (bObj instanceof FieldExpr))
      return false;

    FieldExpr b = (FieldExpr) bObj;

    return _field.equals(b._field) && _base.equals(b._base);
  }

  /**
   * Returns a hash code for the field
   */
  public int hashCode()
  {
    return _name.hashCode() * 65521 + _base.hashCode();
  }

  public String toString()
  {
    return _base.toString() + "." + _name;
  }
}
