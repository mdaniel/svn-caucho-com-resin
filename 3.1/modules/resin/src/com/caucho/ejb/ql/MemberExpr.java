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
import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.util.CharBuffer;

import java.lang.reflect.Method;

/**
 * A 'member' expression
 */
class MemberExpr extends Expr {
  // value expression
  private PathExpr _item;
  // value expression
  private CollectionExpr _collection;
  private CollectionIdExpr _collectionId;
  // true if this is a negative member
  private boolean _isNot;

  private Query _parent;

  /**
   * Creates a like expression.
   *
   * @param value the value expression
   * @param isNot if true, this the like is negated
   */
  MemberExpr(boolean isNot, Expr item, Expr collection)
    throws ConfigException
  {
    _isNot = isNot;
    
    if (collection instanceof CollectionExpr)
      _collection = (CollectionExpr) collection;
    else if (collection instanceof CollectionIdExpr) {
      _collectionId = (CollectionIdExpr) collection;
    }
    else
      throw error(L.l("MEMBER OF needs a collection-valued field at `{0}'.",
                      collection));
    
    if (! (item instanceof PathExpr))
      throw error(L.l("MEMBER OF needs a single-valued field at `{0}'.",
                      item));
    
    _item = (PathExpr) item;

    setJavaType(boolean.class);
  }

  public String addRelation(EjbEntityBean bean, FieldExpr id)
    throws ConfigException
  {
    return null;
  }

  public Method getMethod()
  {
    //return _parent.getMethod();
    throw new UnsupportedOperationException();
  }

  public EjbEntityBean getPersistentBean()
  {
    //return _parent.getPersistentBean();
    throw new UnsupportedOperationException();
  }
  
  public void addArg(Expr arg)
  {
    //_parent.addArg(arg);
    throw new UnsupportedOperationException();
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    if (_isNot)
      cb.append("NOT ");

    _item.generateWhere(cb);

    cb.append(" MEMBER OF (");

    if (_collection != null)
      _collection.generateSelect(cb);
    else
      _collectionId.generateSelect(cb);

    cb.append(")");
    
    /*
    if (_collection != null) {
      if (_isNot)
        cb.append("NOT ");

      String tableSQL = _collection.getRelation().getSQLTable();
    
      String id = "caucho" + "666";//gen.getUnique();
    
      cb.append("EXISTS(SELECT * FROM " + tableSQL + " " + id + " ");
      cb.append("WHERE ");

      for (int i = 0; i < tableSrc.length; i++) {
        if (i != 0)
          cb.append(" AND ");
        
        cb.append(id + "." + tableSrc[i] + " = ");

        _collection.getBase().generateComponent(cb, i);
      }

      cb.append(" AND ");

      for (int i = 0; i < tableDst.length; i++) {
        if (i != 0)
          cb.append(" AND ");
        
        cb.append(id + "." + tableDst[i] + " = ");
    
        _item.generateComponent(cb, i);
      }
    
      cb.append(")");

      throw new UnsupportedOperationException();
    }
    else {
      int len = _collectionId.getKeyFields().length;

      if (_isNot)
        cb.append("NOT (");
      
      for (int i = 0; i < len; i++) {
        if (i != 0)
          cb.append(" AND ");
        
        _collectionId.generateComponent(cb, i);
        cb.append(" = ");
        _item.generateComponent(cb, i);
      }
      
      if (_isNot)
        cb.append(")");
    }
      */
  }
  
  public String toString()
  {
    String str = _item.toString();

    if (_isNot)
      return str + " NOT MEMBER OF " + _collection;
    else
      return str + " MEMBER OF " + _collection;
  }
}
