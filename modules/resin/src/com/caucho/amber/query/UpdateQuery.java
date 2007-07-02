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

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.TableInvalidateCompletion;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.JoinExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.table.Column;
import com.caucho.amber.type.SubEntityType;
import com.caucho.amber.type.EntityType;
import com.caucho.util.CharBuffer;

import java.sql.SQLException;
import java.util.ArrayList;


/**
 * Represents an Amber select query
 */
public class UpdateQuery extends AbstractQuery {
  private ArrayList<AmberExpr> _fieldList;
  private ArrayList<AmberExpr> _valueList;

  private String _sql;

  UpdateQuery(String query)
  {
    super(query);
  }

  /**
   * Sets the field list.
   */
  void setFieldList(ArrayList<AmberExpr> fieldList)
  {
    _fieldList = fieldList;
  }

  /**
   * Returns the field list.
   */
  public ArrayList<AmberExpr> getFieldList()
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
    throws QueryParseException
  {
    super.init();

    CharBuffer cb = new CharBuffer();

    cb.append("update ");

    FromItem item = _fromList.get(0);

    cb.append(item.getTable().getName());
    cb.append(" ");
    cb.append(item.getName());

    cb.append(" set ");

    for (int i = 0; i < _fieldList.size(); i++) {
      if (i != 0)
        cb.append(", ");

      // cb.append(_fieldList.get(i).generateSelect(null));

      AmberExpr expr = _fieldList.get(i);

      expr.generateUpdateWhere(cb);

      cb.append("=");

      _valueList.get(i).generateUpdateWhere(cb);
    }

    String updateJoin = null;

    if (_where != null) {
      cb.append(" where ");

      // jpa/1201
      if (_fromList.size() == 1) {
        _where.generateUpdateWhere(cb);
      }
      else {
        // jpa/1201
        item = _fromList.get(1);

        EntityType type = item.getEntityType();

        String relatedId = type.getId().generateSelect(item.getName());

        cb.append("exists (select ");

        cb.append(relatedId);

        cb.append(" from ");

        // jpa/114f: reorder from list for left outer join
        for (int i = 1; i < _fromList.size(); i++) {
          item = _fromList.get(i);

          if (item.isOuterJoin()) {
            JoinExpr join = item.getJoinExpr();

            if (join == null)
              continue;

            FromItem parent = join.getJoinParent();

            int index = _fromList.indexOf(parent);

            if (index < 0)
              continue;

            _fromList.remove(i);

            if (index < i)
              index++;

            _fromList.add(index, item);
          }
        }

        boolean hasJoinExpr = false;
        boolean isFirst = true;

        // 1201: skip the first from item since it is
        // available in the UPDATE clause
        for (int i = 1; i < _fromList.size(); i++) {
          item = _fromList.get(i);

          // jpa/1178
          if (getParentQuery() != null) {
            ArrayList<FromItem> fromList = getParentQuery().getFromList();
            if (fromList != null) {
              if (fromList.contains(item)) {
                hasJoinExpr = true;
                continue;
              }
            }
          }

          if (isFirst) {
            isFirst = false;
          }
          else {
            if (item.isOuterJoin())
              cb.append(" left outer join ");
            else {
              cb.append(", ");

              if (item.getJoinExpr() != null)
                hasJoinExpr = true;
            }
          }

          cb.append(item.getTable().getName());
          cb.append(" ");
          cb.append(item.getName());

          if (item.getJoinExpr() != null && item.isOuterJoin()) {
            cb.append(" on ");
            item.getJoinExpr().generateJoin(cb);
          }
        }

        // jpa/0l12
        // if (hasJoinExpr || _where != null) {

        boolean hasExpr = false;

        for (int i = 0; i < _fromList.size(); i++) {
          item = _fromList.get(i);

          AmberExpr expr = item.getJoinExpr();

          if (expr != null && ! item.isOuterJoin()) {
            if (hasExpr)
              cb.append(" and ");
            else {
              cb.append(" where ");
              hasExpr = true;
            }

            expr.generateJoin(cb);
          }

          EntityType entityType = item.getEntityType();

          // jpa/0l44
          if (entityType != null) {
            Column discriminator = entityType.getDiscriminator();

            // jpa/0l43
            if (entityType instanceof SubEntityType &&
                discriminator != null) {
              // jpa/0l12, jpa/0l4b

              if (item.getTable() == discriminator.getTable()) {
                if (hasExpr)
                  cb.append(" and ");
                else {
                  cb.append(" where ");
                  hasExpr = true;
                }

                cb.append("(" + item.getName() + "." + discriminator.getName() + " = ");
                cb.append("'" + entityType.getDiscriminatorValue() + "')");
              }
            }
          }
        }

        if (hasExpr)
          cb.append(" and ");
        else {
          cb.append(" where ");
          hasExpr = true;
        }

        _where.generateWhere(cb);
      }
    } // end if (_where != null)

    // jpa/1201
    if (_fromList.size() > 1)
      cb.append(")");

    _sql = cb.close();
  }

  /**
   * Adds any completion info.
   */
  public void prepare(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
    aConn.flushNoChecks();
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
