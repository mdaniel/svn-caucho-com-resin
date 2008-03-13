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

import com.caucho.amber.type.SelfEntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.util.CharBuffer;

/**
 * Identifier expression for EJB-QL.
 */
class IdExpr extends PathExpr {
  // identifier name
  private String _name;
  
  // the table name
  private String _tableName;

  /**
   * Creates a new identifier expression.
   *
   * @param query the owning query
   * @param name the identifier
   * @param bean the mapped bean
   */
  IdExpr(Query query, String name, EjbEntityBean bean)
    throws ConfigException
  {
    super(bean);

    _query = query;
    _name = name;

    setJavaType(bean.getEJBClass());
  }

  /**
   * Returns the identifier name.
   */
  String getName()
  {
    return _name;
  }
  
  String getKeyTable()
  {
    return getName();
  }
  
  String []getKeyFields()
  {
    /*
    PrimaryKey key = _bean.getPrimaryKey();
    
    return key.getSQLColumns();
    */
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the EJB name.
   */
  String getReturnEJB()
  {
    return getBean().getEJBName();
  }

  String getTable()
  {
    return _name;
  }

  /**
   * Returns the persistent bean this id is a member of
   */
  EjbEntityBean getBean()
  {
    return _bean;
  }
  
  /**
   * Sets the persistent bean this id is a member of
   */
  void setBean(EjbEntityBean bean)
  {
    _bean = bean;
  }

  /**
   * Returns the SQL table name for the id
   */
  String getTableName()
  {
    if (_tableName != null)
      return _tableName;
    else if (_bean != null)
      return _bean.getSQLTable();
    else
      return null;
  }

  /**
   * Sets the SQL table name for the id
   */
  void setTableName(String tableName)
  {
    _tableName = tableName;
  }

  /**
   * Returns the item bean of a collection.
   */
  EjbEntityBean getItemBean()
  {
    return _bean;
  }

  int getComponentCount()
  {
    EjbEntityBean bean = getItemBean();

    SelfEntityType type = bean.getEntityType();
    
    return type.getId().getKeyCount();
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  void printSelect(CharBuffer cb)
    throws ConfigException
  {
    if (_bean == null)
      throw new ConfigException("no bean for " + getName());

    /*
    PrimaryKey key = _bean.getPrimaryKey();

    String []names = key.getSQLColumns();
    for (int i = 0; i < names.length; i++) {
      if (i != 0)
        cb.append(", ");

      if (_query.getFromList().size() == 1) {
        // special case to handle strange databases
        cb.append(names[i]);
      }
      else {
        cb.append(getName());
        cb.append(".");
        cb.append(names[i]);
      }
    }
    */
    cb.append(getName());
  }

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
      return getName();
    */
    return getName();
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    if (_bean == null)
      throw new IllegalStateException("no bean for " + getName());

    cb.append(getName());

    /*
    String []names = _bean.getPrimaryKey().getSQLColumns();
    if (names.length != 1)
      throw new RuntimeException("multiple values need special test.");

    if (_query.getFromList().size() == 1) {
      // special case to handle strange databases
      cb.append(names[0]);
    }
    else {
      cb.append(getName());
      cb.append(".");
      cb.append(names[0]);
    }
    */
  }

  void generateComponent(CharBuffer cb, int index)
  {
    EjbEntityBean bean = getItemBean();

    SelfEntityType type = bean.getEntityType();
    
    cb.append(getName());
    cb.append(".");

    cb.append(keyComponent(type, index));
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateAmber(CharBuffer cb)
  {
    cb.append(getBean().getAbstractSchemaName());
    cb.append(' ');
    cb.append(getName());
  }
  
  /**
   * Returns true if the two expressions are equal
   */
  public boolean equals(Object bObj)
  {
    if (! (bObj instanceof IdExpr))
      return false;

    IdExpr b = (IdExpr) bObj;

    return _name.equals(b._name);
  }

  /**
   * Returns a hash code for the expression
   */
  public int hashCode()
  {
    return _name.hashCode();
  }

  /**
   * Printable version of the object.
   */
  public String toString()
  {
    return _name;
  }
}
