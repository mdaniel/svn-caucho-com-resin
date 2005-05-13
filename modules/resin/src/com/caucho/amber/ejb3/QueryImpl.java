/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
package com.caucho.amber.ejb3;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ejb.Query;

import com.caucho.amber.connection.AmberConnectionImpl;

import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.UserQuery;

import com.caucho.util.L10N;

import com.caucho.ejb.EJBExceptionWrapper;

/**
 * The EJB query
 */
public class QueryImpl implements Query {
  private static final L10N L = new L10N(QueryImpl.class);
  
  private AbstractQuery _query;
  private UserQuery _userQuery;
    
  private AmberConnectionImpl _aConn;
  private int _firstResult;
  private int _maxResults = Integer.MAX_VALUE / 2;

  /**
   * Creates a manager instance.
   */
  QueryImpl(AbstractQuery query, AmberConnectionImpl aConn)
  {
    _query = query;
    _aConn = aConn;

    _userQuery = new UserQuery(query);
    _userQuery.setSession(_aConn);
  }

  /**
   * Execute the query and return as a List.
   */
  public List listResults()
  {
    try {
      ArrayList results = new ArrayList();
      
      ResultSet rs = executeQuery();

      while (rs.next())
	results.add(rs.getObject(1));

      rs.close();

      return results;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Returns a single result.
   */
  public Object getSingleResult()
  {
    try {
      ResultSet rs = executeQuery();

      Object value = null;

      if (rs.next())
	value = rs.getObject(1);

      rs.close();

      return value;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Execute an update or delete.
   */
  public int executeUpdate()
  {
    try {
      return _userQuery.executeUpdate();
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Executes the query returning a result set.
   */
  protected ResultSet executeQuery()
    throws SQLException
  {
    return _userQuery.executeQuery();
  }

  /**
   * Sets the maximum result returned.
   */
  public Query setMaxResults(int maxResult)
  {
    return this;
  }

  /**
   * Sets the position of the first result.
   */
  public Query setFirstResult(int startPosition)
  {
    return this;
  }

  /**
   * Sets a hint.
   */
  public Query setHint(String hintName, Object value)
  {
    return this;
  }

  /**
   * Sets a named parameter.
   */
  public Query setParameter(String name, Object value)
  {
    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(String name, Date value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(String name, Calendar value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets an indexed parameter.
   */
  public Query setParameter(int index, Object value)
  {
    _userQuery.setObject(index, value);
    
    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(int index, Date value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(int index, Calendar value, TemporalType type)
  {
    return this;
  }
}
