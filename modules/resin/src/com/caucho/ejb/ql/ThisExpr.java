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

import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.util.CharBuffer;

/**
 * Identifier expression for EJB-QL.
 */
class ThisExpr extends PathExpr {
  // the table name
  private String _tableName;

  /**
   * Creates a new identifier expression.
   *
   * @param query the owning query
   * @param bean the mapped bean
   */
  ThisExpr(Query query, EjbEntityBean bean)
    throws ConfigException
  {
    super(bean);

    _query = query;
    
    // setJavaType(bean.getBeanClass());
  }

  /**
   * Returns the identifier name.
   */
  String getName()
  {
    return "this";
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

  String getTable()
  {
    return "this";
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
    //return _bean.getPrimaryKey().getSQLColumns().length;
    throw new UnsupportedOperationException();
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateSelect(CharBuffer cb)
  {
    if (_bean == null)
      throw new IllegalStateException("no bean for " + getName());

    /*
    PrimaryKey key = _bean.getPrimaryKey();

    String []names = key.getSQLColumns();
    for (int i = 0; i < names.length; i++) {
      if (i != 0)
        gen.print(", ");

      if (_query.getFromList().size() == 1) {
        // special case to handle strange databases
        gen.print(names[i]);
      }
      else {
        gen.print(getName());
        gen.print(".");
        gen.print(names[i]);
      }
    }
    */
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
    throw new UnsupportedOperationException();
  }

  void generateComponent(CharBuffer cb, int index)
  {
    if (_bean == null)
      throw new IllegalStateException("no bean for " + getName());

    /*
    String []names = _bean.getPrimaryKey().getSQLColumns();

    cb.append(getName());
    cb.append(".");
    cb.append(names[index]);
    */
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns true if the two expressions are equal
   */
  public boolean equals(Object bObj)
  {
    return bObj instanceof ThisExpr;
  }

  /**
   * Returns a hash code for the expression
   */
  public int hashCode()
  {
    return "this".hashCode();
  }

  /**
   * Printable version of the object.
   */
  public String toString()
  {
    return "this";
  }
}
