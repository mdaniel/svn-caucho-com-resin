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

package com.caucho.amber.query;

import java.util.ArrayList;

import java.sql.SQLException;

import com.caucho.util.CharBuffer;

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.TableInvalidateCompletion;

import com.caucho.amber.field.AmberField;

import com.caucho.amber.manager.AmberConnection;


/**
 * Represents an Amber select query
 */
public class UpdateQuery extends AbstractQuery {
  private ArrayList<AmberField> _fieldList;
  private ArrayList<AmberExpr> _valueList;
  private AmberExpr _where;

  private String _sql;
  
  UpdateQuery(String query)
  {
    super(query);
  }

  /**
   * Sets the field list.
   */
  void setFieldList(ArrayList<AmberField> fieldList)
  {
    _fieldList = fieldList;
  }

  /**
   * Returns the field list.
   */
  public ArrayList<AmberField> getFieldList()
  {
    return _fieldList;
  }

  /**
   * Sets the field expr list.
   */
  void setValueList(ArrayList<AmberExpr> exprList)
  {
    _valueList = exprList;
  }

  /**
   * Returns the field list.
   */
  public ArrayList<AmberExpr> getValueList()
  {
    return _valueList;
  }

  /**
   * Sets the where expression
   */
  void setWhere(AmberExpr expr)
  {
    _where = expr;
  }

  /**
   * Returns the id load sql
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Initialize
   */
  void init()
  {
    CharBuffer cb = new CharBuffer();

    cb.append("update ");

    FromItem item = _fromList.get(0);

    cb.append(item.getTable().getName());

    cb.append(" set ");

    for (int i = 0; i < _fieldList.size(); i++) {
      if (i != 0)
	cb.append(", ");

      cb.append(_fieldList.get(i).generateSelect(null));
      cb.append("=");

      _valueList.get(i).generateWhere(cb);
    }

    if (_where != null) {
      cb.append(" where ");
      _where.generateWhere(cb);
    }
    
    _sql = cb.close();
  }

  /**
   * Generates update
   */
  void registerUpdates(CachedQuery query)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      AmberEntityHome home = item.getEntityHome();

      CacheUpdate update = new TableCacheUpdate(query);

      home.addUpdate(update);
    }
  }

  /**
   * Adds any completion info.
   */
  public void prepare(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
    aConn.flush();
  }

  /**
   * Adds any completion info.
   */
  public void complete(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
    aConn.expire();
      
    FromItem item = _fromList.get(0);

    aConn.addCompletion(new TableInvalidateCompletion(item.getEntityType().getTable().getName()));
  }

  /**
   * Debug view.
   */
  public String toString()
  {
    return "UpdateQuery[" + getQueryString() + "]";
  }
}
