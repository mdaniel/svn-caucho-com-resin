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

package com.caucho.amber.hibernate;

import com.caucho.util.L10N;

/**
 * configuration for an column
 */
public class HibernateColumn {
  private static final L10N L = new L10N(HibernateColumn.class);

  private String _name;

  private String _sqlType;
  private int _length;
  
  private boolean _isNotNull;
  private boolean _isUnique;
  
  private String _index;
  private String _uniqueKey;

  public void setName(String name)
  {
    _name = name;
  }

  String getName()
  {
    return _name;
  }

  public void addText(String text)
  {
    _name = text.trim();
  }

  /**
   * Returns the database sql-type
   */
  public void setSQLType(String sqlType)
  {
    _sqlType = sqlType;
  }

  /**
   * Returns the database sql-type
   */
  public String getSQLType()
  {
    return _sqlType;
  }

  /**
   * Returns the database length
   */
  public void setLength(int length)
  {
    _length = length;
  }

  /**
   * Returns the database length
   */
  public int getLength()
  {
    return _length;
  }

  /**
   * Set true if the database column is not-null.
   */
  public void setNotNull(boolean isNotNull)
  {
    _isNotNull = isNotNull;
  }

  /**
   * Set true if the database column is not-null.
   */
  public boolean getNotNull()
  {
    return _isNotNull;
  }

  /**
   * Set true if the database column is unique.
   */
  public void setUnique(boolean isUnique)
  {
    _isUnique = isUnique;
  }

  /**
   * Set true if the database column is unique.
   */
  public boolean getUnique()
  {
    return _isUnique;
  }

  /**
   * Adds the column to the named unique tuple
   */
  public void setUniqueKey(String uniqueKey)
  {
    _uniqueKey = uniqueKey;
  }

  /**
   * Returns the named unique key
   */
  public String getUniqueKey()
  {
    return _uniqueKey;
  }

  /**
   * Adds the column to the named index
   */
  public void setIndex(String index)
  {
    _index = index;
  }

  /**
   * Returns the index name
   */
  public String getIndex()
  {
    return _index;
  }
}
