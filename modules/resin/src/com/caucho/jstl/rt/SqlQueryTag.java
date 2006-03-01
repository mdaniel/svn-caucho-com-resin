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

package com.caucho.jstl.rt;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javax.sql.*;
import javax.naming.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.SQLExecutionTag;

import com.caucho.log.Log;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.jstl.ResultImpl;
import com.caucho.jsp.*;

public class SqlQueryTag extends BodyTagSupport implements SQLExecutionTag {
  private static final Logger log = Log.open(SqlQueryTag.class);
  private static final L10N L = new L10N(SqlQueryTag.class);
  
  private String _sql;
  private String _var;
  private String _scope;
  private Object _dataSource;
  private int _maxRows = -1;
  private int _startRow = -1;

  private ArrayList<Object> _params;

  /**
   * Sets the SQL.
   */
  public void setSql(String sql)
  {
    _sql = sql;
  }

  /**
   * Sets the variable name.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Sets the data source.
   */
  public void setDataSource(Object dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the maximum number of rows.
   */
  public void setMaxRows(int maxRows)
  {
    _maxRows = maxRows;
  }

  /**
   * Sets the start row.
   */
  public void setStartRow(int startRow)
  {
    _startRow = startRow;
  }

  /**
   * Adds a parameter.
   */
  public void addSQLParameter(Object value)
  {
    if (_params == null)
      _params = new ArrayList<Object>();
    
    _params.add(value);
  }

  public int doEndTag() throws JspException
  {
    Connection conn = null;
    boolean isTransaction = false;
    try {
      String sql;

      if (_sql != null)
        sql = _sql;
      else
        sql = bodyContent.getString();

      conn = (Connection) pageContext.getAttribute("caucho.jstl.sql.conn");
      if (conn != null)
        isTransaction = true;

      if (! isTransaction) {
        DataSource ds;

        ds = getDataSource(pageContext, _dataSource);

        conn = ds.getConnection();
      }

      Object value = null;

      ResultSet rs;

      ArrayList params = _params;
      _params = null;
      Statement stmt;

      if (params == null) {
        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
      }
      else {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        stmt = pstmt;

        for (int i = 0; i < params.size(); i++) {
          Object paramValue = params.get(i);

          pstmt.setObject(i + 1, paramValue);
        }

        rs = pstmt.executeQuery();
      }

      int startRow = _startRow;

      while (startRow-- > 0 && rs.next()) {
      }

      Result result;
      result = new ResultImpl(rs, _maxRows);

      rs.close();
      stmt.close();

      CoreSetTag.setValue(pageContext, _var, _scope, result);
    } catch (Exception e) {
      throw new JspException(e);
    } finally {
      if (! isTransaction && conn != null) {
        try {
          conn.close();
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    return EVAL_PAGE;
  }

  public static DataSource getDataSource(PageContext pageContext,
                                         Object ds)
    throws JspException
  {
    if (ds == null)
      ds = Config.find(pageContext, Config.SQL_DATA_SOURCE);

    if (ds instanceof DataSource)
      return (DataSource) ds;
    else if (! (ds instanceof String))
      throw new JspException(L.l("`{0}' is an invalid DataSource.", ds));

    String key = (String) ds;

    try {
      String jndiName;
      
      if (key.startsWith("java:comp/"))
        jndiName = key;
      else
        jndiName = "java:comp/env/" + key;

      Object value = new InitialContext().lookup(jndiName);

      if (value instanceof DataSource)
        return (DataSource) value;
    } catch (NamingException e) {
    }
    
    throw new JspException(L.l("`{0}' is an invalid DataSource.", ds));
  }
}
