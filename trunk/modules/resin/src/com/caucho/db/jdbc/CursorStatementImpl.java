/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
package com.caucho.db.jdbc;

import java.sql.SQLException;

import com.caucho.db.Database;
import com.caucho.db.sql.Query;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectCursor;
import com.caucho.db.xa.DbTransaction;
import com.caucho.util.L10N;


/**
 * The JDBC statement implementation.
 */
public class CursorStatementImpl extends StatementImpl {
  private static final L10N L = new L10N(CursorStatementImpl.class);
  
  CursorStatementImpl(ConnectionImpl conn)
  {
    super(conn);
  }

  @Override
  public java.sql.ResultSet executeQuery(String sql)
    throws SQLException
  {
    Database db = getDatabase();
    
    if (db == null)
      throw new SQLException(L.l("statement is closed"));

    Query query = db.parseQuery(sql);
    
    java.sql.ResultSet rs = executeQuery(query, getQueryContext());

    return rs;
  }

  private java.sql.ResultSet executeQuery(Query query, 
                                          QueryContext queryContext)
    throws SQLException
  {
    DbTransaction xa = getConnectionImpl().getTransaction();
    
    queryContext.setNonLocking();
    
    SelectCursor result = query.executeCursor(queryContext, xa);
    
    return new CursorResultSetImpl(this, queryContext, result);
  }
}
