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

import java.math.BigDecimal;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * The Query interface.
 */
public interface Query {
  /**
   * Types.
   */
  public enum TemporalType {
    DATE,
    TIME,
    TIMESTAMP
  }

  /**
   * Execute the query and return as a List.
   */
  public List listResults();

  /**
   * Returns a single result.
   */
  public Object getSingleResult();

  /**
   * Execute an update or delete.
   */
  public int executeUpdate();

  /**
   * Sets the maximum result returned.
   */
  public Query setMaxResults(int maxResult);

  /**
   * Sets the position of the first result.
   */
  public Query setFirstResult(int startPosition);

  /**
   * Sets a hint.
   */
  public Query setHint(String hintName, Object value);

  /**
   * Sets a named parameter.
   */
  public Query setParameter(String name, Object value);

  /**
   * Sets a date parameter.
   */
  public Query setParameter(String name, Date value, TemporalType type);

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(String name, Calendar value, TemporalType type);

  /**
   * Sets an indexed parameter.
   */
  public Query setParameter(int index, Object value);

  /**
   * Sets a date parameter.
   */
  public Query setParameter(int index, Date value, TemporalType type);

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(int index, Calendar value, TemporalType type);
}
