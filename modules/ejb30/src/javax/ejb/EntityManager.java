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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.ejb;

import java.sql.Connection;

/**
 * The Entity manager
 */
public interface EntityManager {
  /**
   * Makes the instance managed.
   */
  public void persist(Object entity);
  
  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entity);
  
  /**
   * Remove the instance.
   */
  public void remove(Object entity);
  
  /**
   * Find by the primary key.
   */
  public Object find(String entityName, Object primaryKey);
  
  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass, Object primaryKey);

  /**
   * Synchronize with the database.
   */
  public void flush();

  /**
   * Creates a query.
   */
  public Query createQuery(String sql);

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String sql);

  /**
   * Creates an instance of the native query
   */
  public Query createNativeQuery(String sql);

  /**
   * Creates an instance of the native query
   */
  public Query createNativeQuery(String sql, Class resultType);

  /**
   * Creates an instance of the native query
   */
  public Query createNativeQuery(String sql, String resultSetMapping);

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity);

  /**
   * Returns true if the entity belongs to the current context.
   */
  public boolean contains(Object entity);
}
