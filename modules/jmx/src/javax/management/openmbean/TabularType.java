/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package javax.management.openmbean;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents composite data.
 */
public class TabularType extends OpenType {
  private CompositeType rowType;
  private ArrayList indexNames = new ArrayList();
  
  public TabularType(String typeName, String description,
		     CompositeType rowType, String []indexNames)
    throws OpenDataException
  {
    super(TabularType.class.getName(), typeName, description);

    this.rowType = rowType;

    for (int i = 0; i < indexNames.length; i++)
      this.indexNames.add(indexNames[i]);
  }

  /**
   * Returns the index names.
   */
  public List getIndexNames()
  {
    return this.indexNames;
  }

  /**
   * Returns the row type.
   */
  public CompositeType getRowType()
  {
    return this.rowType;
  }

  /**
   * Returns true if the obj could be tabular.
   */
  public boolean isValue(Object obj)
  {
    if (! (obj instanceof TabularData))
      return false;

    TabularData data = (TabularData) obj;

    return equals(data.getTabularType());
  }

  /**
   * Returns true for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof TabularType))
      return false;

    TabularType type = (TabularType) o;
    
    return (this.rowType.equals(type.rowType) &&
	    this.indexNames.equals(type.indexNames));
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return this.rowType.hashCode();
  }

  public String toString()
  {
    return (getClass().getName() + "[" + this.rowType + "," +
	    this.indexNames + "]");
  }
}
