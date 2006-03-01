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

package com.caucho.amber.query;

import java.util.ArrayList;

import java.sql.SQLException;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.LinkColumns;

import com.caucho.amber.connection.AmberConnectionImpl;

/**
 * Represents an amber query
 */
abstract public class AbstractQuery {
  private String _sql;
  
  protected ArrayList<FromItem> _fromList = new ArrayList<FromItem>();
  
  private ArgExpr []_argList;

  AbstractQuery(String sql)
  {
    _sql = sql;
  }
  
  /**
   * Returns the query string.
   */
  public String getQueryString()
  {
    return _sql;
  }

  /**
   * Sets the from list.
   */
  FromItem createFromItem(Table table, String name)
  {
    FromItem item = new FromItem(table, name, _fromList.size());

    item.setQuery(this);

    _fromList.add(item);

    return item;
  }

  /**
   * Creates a dependent from item
   */
  FromItem createDependentFromItem(FromItem parent,
				   LinkColumns link,
				   String name)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      JoinExpr join = _fromList.get(i).getJoinExpr();

      if (join != null && join.isDependent(parent, link))
	return _fromList.get(i);
    }

    FromItem item = createFromItem(link.getSourceTable(), name);

    JoinExpr join = new ManyToOneJoinExpr(link, item, parent);

    item.setJoinExpr(join);

    return item;
  }

  /**
   * Returns the from list.
   */
  ArrayList<FromItem> getFromList()
  {
    return _fromList;
  }

  /**
   * Gets the parent query.
   */
  AbstractQuery getParentQuery()
  {
    return null;
  }

  /**
   * Returns the SQL.
   */
  public abstract String getSQL();

  /**
   * Sets the arg list.
   */
  void setArgList(ArgExpr []argList)
  {
    _argList = argList;
  }

  /**
   * Returns the arg list.
   */
  ArgExpr []getArgList()
  {
    return _argList;
  }

  /**
   * Generates update
   */
  abstract void registerUpdates(CachedQuery query);

  /**
   * Returns the expire time.
   */
  public long getCacheMaxAge()
  {
    return -1;
  }

  /**
   * Prepares before any update.
   */
  public void prepare(UserQuery userQuery, AmberConnectionImpl aConn)
    throws SQLException
  {
  }

  /**
   * Any post-sql completion
   */
  public void complete(UserQuery userQuery, AmberConnectionImpl aConn)
    throws SQLException
  {
  }
}
