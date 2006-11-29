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
class CollectionIdExpr extends PathExpr {
  // identifier name
  private String _name;
  
  // the persistent bean this id is a member of
  private CollectionExpr _path;

  private String _keyTable;
  private String []_keyColumns;

  private boolean _usesField;

  /**
   * Creates a new identifier expression.
   *
   * @param query the owning query
   * @param name the identifier
   * @param bean the mapped bean
   */
  CollectionIdExpr(Query query, String name, CollectionExpr path)
    throws ConfigException
  {
    super(path.getItemBean());
    
    _query = query;
    _name = name;
    _path = path;

    path.setId(this);

    if (getBean() == null)
      throw new NullPointerException("unknown bean for " + name + " " + query);
    
    // setJavaType(getBean().getJavaType());

    /*
    PersistentRelation relation = path.getRelation();

    if (relation.hasMiddleTable()) {
      _keyTable = "caucho" + query.getUnique();
      PrimaryKey key = relation.getTargetBean().getPrimaryKey();
      _keyColumns = relation.getTargetSQLColumns();

      query.addFromItem(_keyTable, relation.getSQLTable());
      _keyColumns = relation.addTargetLinks(query,
                                            path.getBase().getTable(),
                                            _keyTable);
    }
    else {
      _usesField = true;
      path.setUsesField();
      
      _keyTable = name;
      PrimaryKey key = relation.getTargetBean().getPrimaryKey();
      
      query.addFromItem(_keyTable, relation.getSQLTable());
      _keyColumns = relation.addTargetLinks(query, path.getBase().getTable(),
                                            _keyTable);
    }
    */
  }

  CollectionExpr getPath()
  {
    return _path;
  }

  void setUsesField()
  {
    if (_usesField)
      return;

    _usesField = true;

    /*
    _query.addFromItem(_name, _bean.getSQLTable());
    _keyColumns = _path.getRelation().addSourceLinks(_query, _keyTable, _name);
    _keyTable = _name;
    */
  }

  String getKeyTable()
  {
    return _keyTable;
  }

  String []getKeyFields()
  {
    return _keyColumns;
  }

  int getComponentCount()
  {
    return _path.getComponentCount();
  }

  String getTable()
  {
    return _name;
  }

  /**
   * Returns the identifier name.
   */
  String getName()
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

  EjbEntityBean getItemBean()
  {
    return _bean;
  }

  String getReturnEJB()
  {
    return getItemBean().getEJBName();
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  void printSelect(CharBuffer cb)
    throws ConfigException
  {
    String []names = getKeyFields();

    for (int i = 0; i < names.length; i++) {
      if (i != 0)
        cb.append(", ");
      
      cb.append(getKeyTable());
      cb.append(".");
      cb.append(names[i]);
    }
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  String getSelectTable(CharBuffer cb)
    throws ConfigException
  {
    return getKeyTable();
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void printWhere(CharBuffer cb)
    throws ConfigException
  {
    String []names = getKeyFields();
    if (names.length != 1)
      throw new RuntimeException();
    
    cb.append(getKeyTable());
    cb.append(".");
    cb.append(names[0]);
  }

  void printComponent(CharBuffer cb, int index)
    throws ConfigException
  {
    cb.append(getKeyTable());
    cb.append(".");
    cb.append(getKeyFields()[index]);
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
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateComponent(CharBuffer cb, int i)
  {
    if (_bean == null)
      throw new IllegalStateException("no bean for " + getName());

    cb.append(getName());
    cb.append('.');
    cb.append(generateKeyField(getItemBean().getEntityType(), i));
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateAmber(CharBuffer cb)
  {
    cb.append("IN(");
    _path.generateSelect(cb);
    cb.append(") ");
    cb.append(getName());
  }
  
  /**
   * Returns true if the two expressions are equal
   */
  public boolean equals(Object bObj)
  {
    if (! (bObj instanceof CollectionIdExpr))
      return false;

    CollectionIdExpr b = (CollectionIdExpr) bObj;

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
