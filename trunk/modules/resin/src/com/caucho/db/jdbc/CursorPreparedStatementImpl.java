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

import java.io.InputStream;
import java.io.Reader;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import com.caucho.db.Database;
import com.caucho.db.sql.Query;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectCursor;
import com.caucho.db.xa.DbTransaction;
import com.caucho.util.L10N;

/**
 * The JDBC statement implementation.
 */
public class CursorPreparedStatementImpl extends PreparedStatementImpl
  implements PreparedStatement {
  private static final L10N L = new L10N(CursorPreparedStatementImpl.class);

  CursorPreparedStatementImpl(ConnectionImpl conn, Query query)
  {
    super(conn, query);
  }

  @Override
  public boolean execute()
    throws SQLException
  {
    Database db = getDatabase();
    
    if (db == null)
      throw new SQLException(L.l("statement is closed"));

    setResultSet(executeQuery(getQuery(), getQueryContext()));

    return true;
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
