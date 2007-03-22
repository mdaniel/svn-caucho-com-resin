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

import com.caucho.amber.field.IdField;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.CmpField;
import com.caucho.ejb.cfg.CmrRelation;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Generated expression a relation path.
 *
 * tableA.x = tableB.y
 */
abstract class PathExpr extends Expr {
  protected EjbEntityBean _bean;

  PathExpr(EjbEntityBean bean)
  {
    _bean = bean;

    setJavaType(bean.getEJBClass());
  }

  /**
   * Returns the bean.
   */
  EjbEntityBean getBean()
  {
    return _bean;
  }

  /**
   * Gets the Java Type of the expression.
   */
  /*
  public Class getJavaType()
  {
    return _bean.getLocal();
  }
  */
  
  /**
   * Create field
   */
  Expr newField(String fieldName)
    throws ConfigException
  {
    CmpField field = _bean.getCmpField(fieldName);

    if (field != null)
      return new FieldExpr(_query, this, fieldName, field);

    CmrRelation relation = _bean.getRelation(fieldName);

    if (relation == null)
      throw error(L.l("{0}: '{1}' is an unknown cmp-field.",
		      _bean.getEJBClass().getName(),
                      fieldName));

    // _query.getPersistentBean().addBeanDepend(relation.getTargetBean().getName());

    if (relation.isCollection())
      return new CollectionExpr(_query, this, fieldName, relation);
    else
      return new RelationExpr(_query, this, fieldName, relation);
  }

  abstract String getKeyTable();
  abstract String []getKeyFields();

  abstract String getTable();

  int getComponentCount()
  {
    return getKeyFields().length;
  }

  void setUsesField()
  {
  }

  protected String generateKeyField(EntityType type, int index)
  {
    ArrayList<String> names = generateKeyFields(type);

    return names.get(index);
  }

  protected ArrayList<String> generateKeyFields(EntityType type)
  {
    ArrayList<String> names = new ArrayList<String>();

    for (IdField key : type.getId().getKeys()) {
      String name = key.getName();

      if (key.getType() instanceof EntityType) {
	ArrayList<String> subNames;
	subNames = generateKeyFields((EntityType) key.getType());

	for (String subName : subNames) {
	  names.add(name + '.' + subName);
	}
      }
      else
	names.add(name);
    }

    Collections.sort(names);

    return names;
  }

  void generateAmber(CharBuffer cb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
