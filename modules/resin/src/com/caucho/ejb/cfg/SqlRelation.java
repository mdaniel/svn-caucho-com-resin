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

package com.caucho.ejb.cfg;

import com.caucho.util.*;

/**
 * Represents a single relation to SQL mapping.
 */
public class SqlRelation {
  private static L10N L = new L10N(CmpRelation.class);

  private String _fieldName;
  
  private String _references;
  private String _sqlColumn;

  public SqlRelation(String fieldName)
  {
    _fieldName = fieldName;
  }

  /**
   * Returns the name of the field.
   */
  public String getFieldName()
  {
    return _fieldName;
  }
  
  /**
   * Returns the field this relation references.
   */
  public String getReferences()
  {
    return _references;
  }
  
  /**
   * Sets the field this relation references.
   */
  public void setReferences(String references)
  {
    _references = references;
  }
  
  /**
   * Returns the sql column for the field.
   */
  public String getSQLColumn()
  {
    return _sqlColumn;
  }
  
  /**
   * Sets the sql column for the field.
   */
  public void setSQLColumn(String sqlColumn)
  {
    _sqlColumn = sqlColumn;
  }

  /**
   * Returns true if this is the same relation.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof SqlRelation))
      return false;

    SqlRelation relation = (SqlRelation) o;

    if (! _fieldName.equals(relation._fieldName))
      return false;

    if (_references == relation._references)
      return true;

    if (_references == null || relation._references == null)
      return false;

    return _references.equals(relation._references);
  }
}
