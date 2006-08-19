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

package com.caucho.amber.manager;

import java.lang.reflect.Constructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.FlushModeType;

import com.caucho.amber.AmberException;

import com.caucho.amber.manager.AmberConnection;

import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.SelectQuery;
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

  private AmberConnection _aConn;
  private int _firstResult;
  private int _maxResults = Integer.MAX_VALUE / 2;

  /**
   * Creates a manager instance.
   */
  QueryImpl(AbstractQuery query, AmberConnection aConn)
  {
    _query = query;
    _aConn = aConn;

    _userQuery = new UserQuery(query);
    _userQuery.setSession(_aConn);
  }

  /**
   * Execute the query and return as a List.
   */
  public List getResultList()
  {
    try {
      ArrayList results = new ArrayList();

      ResultSet rs = executeQuery();

      ResultSetMetaData metaData = null;
      int columnCount = -1;

      try {

        metaData = rs.getMetaData();

        if (metaData != null)
          columnCount = metaData.getColumnCount();

      } catch (Exception ex) {
        // Below, we work around if DB is not able
        // to retrieve result set meta data. jpa/0t00
        metaData = null;
      }

      if (columnCount < 0)
        columnCount = 10000;

      int n = 0;

      Object row[] = null;

      ArrayList columns = new ArrayList();

      Class constructorClass = null;

      if (_query instanceof SelectQuery) {
        constructorClass = ((SelectQuery) _query).getConstructorClass();
      }

      Constructor constructor = null;

      while (rs.next()) {
        Object object = null;

        if (n == 0) {
          for (int i=1; i<=columnCount; i++) {
            try {

              object = rs.getObject(i);
              columns.add(object);

            } catch (Exception ex) {

              // XXX: add this check when meta data
              // is working properly.
              //
              // if (metaData != null) {
              //   throw ex;
              // }

              // this will only happen when DB does
              // not support result set meta data (above).
              break;
            }
          }

          n = columns.size();
          row = columns.toArray();

          if (constructorClass != null) {

            try {

              Class paramTypes[] = new Class[n];

              for (int i=0; i < n; i++)
                paramTypes[i] = row[i].getClass();

              constructor = constructorClass.getConstructor(paramTypes);

            } catch (Exception ex) {
              throw error(L.l("Unable to find constructor {0}. Make sure there is a public constructor for the given arguments", constructorClass.getName()));
            }
          }
        }
        else {
          row = new Object[n];
          for (int i=1; i<=n; i++) {
            row[i-1] = rs.getObject(i);
          }
        }

        if (constructor == null) {
          if (n == 1)
            results.add(row[0]);
          else
            results.add(row);
        }
        else {
          results.add(constructor.newInstance(row));
        }
      }

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
    ArrayList<String> mapping =_query.getPreparedMapping();

    int n = mapping.size();

    for (int i=0; i < n; i++) {
      if (mapping.get(i).equals(name)) {
        setParameter(i+1, value);
      }
    }

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
    if (value instanceof Double)
      _userQuery.setDouble(index, ((Double) value).doubleValue());
    else
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

  /**
   * Sets the flush mode type.
   */
  public Query setFlushMode(FlushModeType mode)
  {
    return this;
  }

  //
  // extensions

  /**
   * Sets an indexed parameter.
   */
  public Query setDouble(int index, double value)
  {
    _userQuery.setDouble(index, value);

    return this;
  }

  /**
   * Creates an error.
   */
  private AmberException error(String msg)
  {
    msg += "\nin \"" + _query.getQueryString() + "\"";

    return new AmberException(msg);
  }
}
