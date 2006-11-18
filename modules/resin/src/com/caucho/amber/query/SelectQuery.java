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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import java.util.ArrayList;
import java.util.Map;

import java.sql.SQLException;

import com.caucho.util.CharBuffer;

import com.caucho.amber.expr.*;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.entity.AmberEntityHome;


/**
 * Represents an Amber select query
 */
public class SelectQuery extends AbstractQuery {
  private AbstractQuery _parentQuery;

  private boolean _isDistinct;

  private ArrayList<AmberExpr> _resultList;
  private AmberExpr _where;
  private AmberExpr _having;

  private ArrayList<AmberExpr> _orderList;
  private ArrayList<Boolean> _ascList;

  private ArrayList<AmberExpr> _groupList;

  private Map<AmberExpr, String> _joinFetchMap;

  private String _sql;

  // SELECT NEW
  private Class _constructorClass;

  private boolean _isTableReadOnly = false;
  private long _cacheTimeout = -1;

  private boolean _hasFrom = true;

  SelectQuery(String query)
  {
    super(query);
  }

  /**
   * Gets the (join) fetch map.
   */
  Map<AmberExpr, String> getJoinFetchMap()
  {
    return _joinFetchMap;
  }

  /**
   * Sets the constructor class for SELECT NEW.
   */
  void setConstructorClass(Class cl)
  {
    _constructorClass = cl;
  }

  /**
   * Gets the constructor class for SELECT NEW.
   */
  public Class getConstructorClass()
  {
    return _constructorClass;
  }

  /**
   * Sets whether the query has a FROM clause or not.
   */
  void setHasFrom(boolean hasFrom)
  {
    // The spec. is not clear about the FROM clause for
    // Current_Date/Time/Timestamp functions.

    _hasFrom = hasFrom;
  }

  /**
   * Sets the parent query.
   */
  void setParentQuery(AbstractQuery parent)
  {
    _parentQuery = parent;
  }

  /**
   * Gets the parent query.
   */
  public AbstractQuery getParentQuery()
  {
    return _parentQuery;
  }

  /**
   * Sets true if distinct.
   */
  void setDistinct(boolean isDistinct)
  {
    _isDistinct = isDistinct;
  }

  /**
   * Sets the result list.
   */
  void setResultList(ArrayList<AmberExpr> resultList)
  {
    _resultList = resultList;
  }

  /**
   * Returns the result list.
   */
  public ArrayList<AmberExpr> getResultList()
  {
    return _resultList;
  }

  /**
   * Returns the result type.
   */
  int getResultCount()
  {
    return _resultList.size();
  }

  /**
   * Returns the result type.
   */
  Type getResultType(int index)
  {
    AmberExpr expr = _resultList.get(index);

    return expr.getType();
  }

  /**
   * Sets the having expression
   */
  void setHaving(AmberExpr expr)
  {
    _having = expr;
  }

  /**
   * Sets the where expression
   */
  void setWhere(AmberExpr expr)
  {
    _where = expr;
  }

  /**
   * Sets the group by list.
   */
  void setGroupList(ArrayList<AmberExpr> groupList)
  {
    _groupList = groupList;
  }

  /**
   * Sets the (join) fetch map.
   */
  void setJoinFetchMap(Map<AmberExpr, String> joinFetchMap)
  {
    _joinFetchMap = joinFetchMap;
  }

  /**
   * Sets the order by list.
   */
  void setOrderList(ArrayList<AmberExpr> orderList,
                    ArrayList<Boolean> ascList)
  {
    _orderList = orderList;
    _ascList = ascList;
  }

  /**
   * Returns the id load sql
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Returns the expire time.
   */
  public long getCacheMaxAge()
  {
    return _cacheTimeout;
  }

  /**
   * Returns true for cacheable queries.
   */
  public boolean isCacheable()
  {
    return 100L <= _cacheTimeout;
  }

  /**
   * Are the tables read-only
   */
  public boolean isTableReadOnly()
  {
    return _isTableReadOnly;
  }

  /**
   * initializes the query.
   */
  void init()
    throws SQLException
  {
    if (_where instanceof AndExpr) {
      AndExpr and = (AndExpr) _where;

      ArrayList<AmberExpr> components = and.getComponents();

      for (int i = components.size() - 1; i >= 0; i--) {
        AmberExpr component = components.get(i);

        if (component instanceof JoinExpr) {
          JoinExpr link = (JoinExpr) component;

          if (link.bindToFromItem()) {
            components.remove(i);
          }
        }
      }

      _where = and.getSingle();
    }

    if (_having instanceof AndExpr) {
      AndExpr and = (AndExpr) _having;

      ArrayList<AmberExpr> components = and.getComponents();

      for (int i = components.size() - 1; i >= 0; i--) {
        AmberExpr component = components.get(i);

        if (component instanceof JoinExpr) {
          JoinExpr link = (JoinExpr) component;

          if (link.bindToFromItem()) {
            components.remove(i);
          }
        }
      }

      _having = and.getSingle();
    }

    // Rolls up unused from items from the left to the right.
    // It's not necessary to roll up the rightmost items because
    // they're only created if they're actually needed
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      JoinExpr join = item.getJoinExpr();

      if (join == null)
        continue;

      FromItem joinParent = join.getJoinParent();
      FromItem joinTarget = join.getJoinTarget();

      boolean isTarget = item == joinTarget;

      if (joinParent == null) {
      }
      else if (joinParent.getJoinExpr() == null &&
               joinParent == joinTarget &&
               ! usesFromData(joinParent)) {

        _fromList.remove(joinParent);

        replaceJoin(join);

        // XXX:
        item.setJoinExpr(null);
        //item.setOuterJoin(false);
        i = -1;

        AmberExpr joinWhere = join.getWhere();

        if (joinWhere != null)
          _where = AndExpr.create(_where, joinWhere);
      }
      else if (! isJoinParent(item) &&
               item == joinTarget &&
               ! usesFromData(item)) {

        // jpa/114g
        if (! item.isOuterJoin())
          continue;

        _fromList.remove(item);

        replaceJoin(join);

        i = -1;

        AmberExpr joinWhere = join.getWhere();

        if (joinWhere != null)
          _where = AndExpr.create(_where, joinWhere);
      }
    }

    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      if (item.isInnerJoin())
        continue;

      if (item.getJoinExpr() == null)
        continue;

      boolean isFromInner = isFromInnerJoin(item);

      item.setOuterJoin(! isFromInner);
    }

    _cacheTimeout = Long.MAX_VALUE / 2;
    _isTableReadOnly = true;
    for (FromItem item : _fromList) {
      EntityType type = item.getTableType();

      if (type != null) {
        long timeout = type.getCacheTimeout();

        if (timeout < _cacheTimeout)
          _cacheTimeout = timeout;

        if (! type.isReadOnly())
          _isTableReadOnly = false;
      }
      else {
        // XXX: kills the cache?
        _isTableReadOnly = false;
      }
    }

    _sql = generateLoadSQL();
  }

  boolean isJoinParent(FromItem item)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem subItem = _fromList.get(i);

      if (subItem.getJoinExpr() != null &&
          subItem.getJoinExpr().getJoinParent() == item) {
        return true;
      }
    }

    return false;
  }

  boolean isFromInnerJoin(FromItem item)
  {
    return usesFrom(item, AmberExpr.IS_INNER_JOIN);
  }

  boolean usesFromData(FromItem item)
  {
    return usesFrom(item, AmberExpr.USES_DATA);
  }

  /**
   * Returns true if the from item is used by the query.
   */
  public boolean usesFrom(FromItem item, int type)
  {
    for (int j = 0; j < _resultList.size(); j++) {
      AmberExpr result = _resultList.get(j);

      if (result.usesFrom(item, type)) {
        return true;
      }
    }

    if (_where != null && _where.usesFrom(item, type))
      return true;

    if (_having != null && _having.usesFrom(item, type))
      return true;

    if (_orderList != null) {
      for (int j = 0; j < _orderList.size(); j++) {
        AmberExpr order = _orderList.get(j);

        if (order.usesFrom(item, type)) {
          return true;
        }
      }
    }

    return false;
  }

  void replaceJoin(JoinExpr join)
  {
    for (int i = 0; i < _resultList.size(); i++) {
      AmberExpr result = _resultList.get(i);

      _resultList.set(i, result.replaceJoin(join));
    }

    if (_where != null) {
      _where = _where.replaceJoin(join);
    }

    if (_orderList != null) {
      for (int i = 0; i < _orderList.size(); i++) {
        AmberExpr order = _orderList.get(i);

        _orderList.set(i, order.replaceJoin(join));
      }
    }
  }

  public String generateLoadSQL()
  {
    return generateLoadSQL(true);
  }

  /**
   * Generates the load SQL.
   *
   * @param fullSelect true if the load entity expressions
   *                   should be fully loaded for all entity
   *                   fields. Otherwise, only the entity id
   *                   will be loaded: select o.id from ...
   *                   It is implemented to optimize the SQL
   *                   and allow for databases that only
   *                   support single columns in subqueries.
   *                   Derby is an example. An additional
   *                   condition to generate only the o.id
   *                   is the absence of group by. If there
   *                   is a group by the full select will
   *                   always be generated.
   *
   *                   See also com.caucho.amber.expr.ExistsExpr
   *
   * @return the load SQL.
   */
  public String generateLoadSQL(boolean fullSelect)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("select ");

    if (_isDistinct)
      cb.append(" distinct ");

    for (int i = 0; i < _resultList.size(); i++) {
      if (i != 0)
        cb.append(", ");

      AmberExpr expr = _resultList.get(i);

      if ((_groupList == null) && (expr instanceof LoadEntityExpr))
        ((LoadEntityExpr) expr).generateSelect(cb, fullSelect);
      else
        expr.generateSelect(cb);
    }

    if (_hasFrom)
      cb.append(" from ");

    // jpa/114f: reorder from list for left outer join
    for (int i = 1; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

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
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      if (i != 0) {
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

    if (hasJoinExpr || _where != null) {
      boolean hasExpr = false;

      cb.append(" where ");

      for (int i = 0; i < _fromList.size(); i++) {
        FromItem item = _fromList.get(i);
        AmberExpr expr = item.getJoinExpr();

        if (expr != null && ! item.isOuterJoin()) {
          if (hasExpr)
            cb.append(" and ");
          hasExpr = true;

          expr.generateJoin(cb);
        }
      }

      if (_where != null) {
        if (hasExpr)
          cb.append(" and ");
        hasExpr = true;

        _where.generateWhere(cb);
      }
    }

    if (_groupList != null) {
      cb.append(" group by ");

      for (int i = 0; i < _groupList.size(); i++) {
        if (i != 0)
          cb.append(", ");

        _groupList.get(i).generateWhere(cb);
      }
    }

    if (_having != null) {
      boolean hasExpr = false;

      cb.append(" having ");

      for (int i = 0; i < _fromList.size(); i++) {
        FromItem item = _fromList.get(i);
        AmberExpr expr = item.getJoinExpr();

        if (expr != null && ! item.isOuterJoin()) {
          if (hasExpr)
            cb.append(" and ");
          hasExpr = true;

          expr.generateJoin(cb);
        }
      }

      if (_having != null) {
        if (hasExpr)
          cb.append(" and ");
        hasExpr = true;

        _having.generateHaving(cb);
      }
    }

    if (_orderList != null) {
      cb.append(" order by ");

      for (int i = 0; i < _orderList.size(); i++) {
        if (i != 0)
          cb.append(", ");

        _orderList.get(i).generateSelect(cb);

        if (Boolean.FALSE.equals(_ascList.get(i)))
          cb.append(" desc");
      }
    }

    return cb.toString();
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
   * Returns true if modifying the given table modifies a cached query.
   */
  public boolean invalidateTable(String table)
  {
    for (int i = _fromList.size() - 1; i >= 0; i--) {
      FromItem from = _fromList.get(i);

      if (table.equals(from.getTable().getName()))
        return true;
    }

    return false;
  }

  /**
   * Debug view.
   */
  public String toString()
  {
    return "SelectQuery[" + getQueryString() + "]";
  }
}
