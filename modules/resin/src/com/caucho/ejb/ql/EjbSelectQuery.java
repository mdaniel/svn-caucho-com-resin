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

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Parsed expression for EJB-QL.
 */
public class EjbSelectQuery extends EjbQuery {
  private static final Logger log = Log.open(EjbSelectQuery.class);
  private static final L10N L = new L10N(EjbSelectQuery.class);

  private ArrayList<PathExpr> _fromList;
  protected Expr _selectExpr;
  protected Expr _whereExpr;
  protected IdExpr _thisExpr;

  private ArrayList<Expr> _orderByExpr;
  private ArrayList<Boolean> _orderByAscending;

  private Expr _offsetExpr;
  private Expr _limitExpr;

  private boolean _queryLoadsBean = true;
  private boolean _isDistinct = false;

  EjbSelectQuery(String ejbql)
  {
    super(ejbql);
  }

  /**
   * Sets the from list.
   */
  public void setFromList(ArrayList<PathExpr> fromList)
  {
    _fromList = fromList;
  }

  /**
   * Sets the select expr.
   */
  public void setSelectExpr(Expr selectExpr)
  {
    _selectExpr = selectExpr;
  }

  /**
   * Sets the where expr
   */
  public void setWhereExpr(Expr whereExpr)
  {
    _whereExpr = whereExpr;
  }

  /**
   * Set true if there's a "this" expression
   */
  public void setThisExpr(IdExpr thisExpr)
  {
    _thisExpr = thisExpr;
  }

  /**
   * Return true if there's a "this" expression
   */
  public IdExpr getThisExpr()
  {
    return _thisExpr;
  }

  /**
   * Sets the order by
   */
  public void setOrderBy(ArrayList<Expr> exprList,
			 ArrayList<Boolean> ascendingList)
  {
    _orderByExpr = exprList;
    _orderByAscending = ascendingList;
  }

  /**
   * Sets the limit offset.
   */
  public void setOffset(Expr offset)
  {
    _offsetExpr = offset;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getOffsetValue()
  {
    if (_offsetExpr instanceof LiteralExpr)
      return Integer.parseInt(((LiteralExpr) _offsetExpr).getValue());
    else
      return -1;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getOffsetArg()
  {
    if (_offsetExpr instanceof ArgExpr)
      return ((ArgExpr) _offsetExpr).getIndex();
    else
      return -1;
  }

  /**
   * Sets the limit max.
   */
  public void setLimit(Expr limit)
  {
    _limitExpr = limit;
  }

  /**
   * Gets the offset as an argument.
   */
  public int getLimitValue()
  {
    if (_limitExpr instanceof LiteralExpr)
      return Integer.parseInt(((LiteralExpr) _limitExpr).getValue());
    else
      return -1;
  }

  /**
   * Gets the limit as an argument.
   */
  public int getLimitArg()
  {
    if (_limitExpr instanceof ArgExpr)
      return ((ArgExpr) _limitExpr).getIndex();
    else
      return -1;
  }

  /**
   * Sets true for the distinct.
   */
  public void setDistinct(boolean isDistinct)
  {
    _isDistinct = isDistinct;
  }

  /**
   * Sets true if the query should load the bean.
   */
  public void setQueryLoadsBean(boolean queryLoadsBean)
  {
    _queryLoadsBean = queryLoadsBean;
  }

  /**
   * Returns true if the query should load the bean.
   */
  public boolean getQueryLoadsBean()
  {
    return _queryLoadsBean;
  }

  /**
   * Convert to an amber query.
   */
  public String toAmberQuery(String []args)
  {
    CharBuffer cb = new CharBuffer();

    cb.append("SELECT ");

    if (_isDistinct)
      cb.append("DISTINCT ");

    _selectExpr.generateSelect(cb);

    cb.append(" FROM ");

    for (int i = 0; i < _fromList.size(); i++) {
      PathExpr item = _fromList.get(i);

      if (i != 0)
	cb.append(", ");

      item.generateAmber(cb);
    }

    if (_whereExpr != null) {
      cb.append(" WHERE ");
      _whereExpr.generateWhere(cb);
    }

    if (_thisExpr != null) {
      if (_whereExpr == null)
	cb.append(" WHERE ");
      else
	cb.append(" AND ");

      for (int i = 0; i < _thisExpr.getComponentCount(); i++) {
	if (i != 0)
	  cb.append(" AND ");
	
	_thisExpr.generateComponent(cb, i);
	cb.append("=?" + (getMaxArg() + i + 1));
      }
    }

    if (_orderByExpr != null && _orderByExpr.size() > 0) {
      cb.append(" ORDER BY ");

      for (int i = 0; i < _orderByExpr.size(); i++) {
	if (i != 0)
	  cb.append(", ");

	Expr orderBy = _orderByExpr.get(i);;

	if (orderBy instanceof ArgExpr)
	  cb.append("\" + " + args[((ArgExpr) orderBy).getIndex() - 1] + " + \"");
	else
	  orderBy.generateSelect(cb);

	if (Boolean.FALSE.equals(_orderByAscending.get(i)))
	  cb.append(" DESC");
      }
    }
    
    return cb.toString();
  }
}
