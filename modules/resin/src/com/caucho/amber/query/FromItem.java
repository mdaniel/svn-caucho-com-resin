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

import com.caucho.amber.entity.AmberEntityHome;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;

class FromItem {
  private String _name;

  private Table _table;

  private AbstractQuery _query;

  private PathExpr _collectionExpr;

  private IdExpr _idExpr;

  private int _index;

  private JoinExpr _joinExpr;

  private boolean _isUsed;

  private enum JoinSemantics { UNKNOWN, INNER, OUTER }

  private JoinSemantics _joinSemantics
    = JoinSemantics.UNKNOWN;

  FromItem(Table table, String name, int index)
  {
    _table = table;
    _name = name;
    _index = index;
  }

  /**
   * Sets the id expr.
   */
  public void setIdExpr(IdExpr idExpr)
  {
    _idExpr = idExpr;
  }

  /**
   * Gets the id expr.
   */
  public IdExpr getIdExpr()
  {
    if (_idExpr != null) {
    }
    else if (_collectionExpr != null) {
      _idExpr = new CollectionIdExpr(this, _collectionExpr);
    }
    else
      _idExpr = new IdExpr(this);

    return _idExpr;
  }

  /**
   * Sets the collection expr.
   */
  public void setCollectionExpr(PathExpr collectionExpr)
  {
    _collectionExpr = collectionExpr;
  }

  /**
   * Gets the id expr.
   */
  public PathExpr getCollectionExpr()
  {
    return _collectionExpr;
  }

  /**
   * Returns the owning query.
   */
  AbstractQuery getQuery()
  {
    return _query;
  }

  /**
   * Sets the owning query.
   */
  void setQuery(AbstractQuery query)
  {
    _query = query;
  }

  /**
   * Returns the from item's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Gets the entity class.
   */
  public EntityType getEntityType()
  {
    return (EntityType) getTableType();
  }

  /**
   * Gets the table type
   */
  public EntityType getTableType()
  {
    return _table.getType();
  }

  /**
   * Returns the table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Sets the table.
   */
  public void setTable(Table table)
  {
    _table = table;
  }

  /**
   * Sets the join expr.
   */
  void setJoinExpr(JoinExpr joinExpr)
  {
    _joinExpr = joinExpr;
  }

  /**
   * Returns true if the from is used.
   */
  boolean isUsed()
  {
    return _isUsed;
  }

  /**
   * Returns true if the from is used.
   */
  void setUsed(boolean isUsed)
  {
    _isUsed = isUsed;
  }

  /**
   * Returns true if the from has no outer join.
   */
  boolean isInnerJoin()
  {
    return _joinSemantics == JoinSemantics.INNER;
  }

  /**
   * Returns true if the from needs an outer join.
   */
  boolean isOuterJoin()
  {
    return _joinSemantics == JoinSemantics.OUTER;
  }

  /**
   * Returns true if the from needs an outer join.
   */
  void setOuterJoin(boolean isOuterJoin)
  {
    _joinSemantics = isOuterJoin ?
      JoinSemantics.OUTER : JoinSemantics.INNER;
  }

  /**
   * Gets the join expr.
   */
  JoinExpr getJoinExpr()
  {
    return _joinExpr;
  }

  /**
   * Returns the entity home.
   */
  public AmberEntityHome getEntityHome()
  {
    return ((EntityType) getTableType()).getHome();
  }

  /**
   * Gets the index within the cartesian product for the item.
   */
  public int getIndex()
  {
    return _index;
  }

  public String toString()
  {
    return "FromItem[" + _table.getName() + " AS " + getName() + "]";
  }
}
