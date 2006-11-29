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

import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.CmrRelation;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.util.CharBuffer;

/**
 * Expression representing the a collection specified in the FROM field,
 * e.g. FROM l in o.list
 */
class CollectionExpr extends Expr {
  // base expression
  private PathExpr _base;
  // field name
  private String _fieldName;

  // relation information
  private CmrRelation _relation;

  private CollectionIdExpr _id;
  private boolean _usesField;

  /**
   * Creates a new field expression.
   *
   * @param base the base expression
   * @param field name of the field
   */
  CollectionExpr(Query query, PathExpr base,
                 String field, CmrRelation relation)
    throws ConfigException
  {
    _query = query;
    _base = base;
    _fieldName = field;
    _relation = relation;

    base.setUsesField();
    
    // setJavaType(relation.getJavaType());
  }

  void setId(CollectionIdExpr id)
  {
    if (_id != null)
      throw new RuntimeException();
    
    _id = id;
  }

  CollectionIdExpr getId()
  {
    return _id;
  }

  void setUsesField()
  {
    if (! _usesField) {
      _usesField = true;
    }
  }

  PathExpr getBase()
  {
    return _base;
  }

  EjbEntityBean getItemBean()
  {
    return _relation.getTargetBean();
  }

  int getComponentCount()
  {
    EjbEntityBean bean = getItemBean();

    EntityType type = bean.getEntityType();
    
    return type.getId().getKeyCount();
  }

  /**
   * Returns the EJB name.
   */
  String getReturnEJB()
  {
    return getItemBean().getEJBName();
  }

  CmrRelation getRelation()
  {
    return _relation;
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param cb the java code generator
   */
  void generateSelect(CharBuffer cb)
  {
    _base.generateSelect(cb);
    cb.append('.');
    cb.append(_fieldName);

    // _id.generateSelect(cb);
  }

  public String toString()
  {
    return String.valueOf(_base) + "." + _fieldName;
  }
}
