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

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents composite data.
 */
public class CompositeType extends OpenType {
  private TreeMap nameToDescription =
    new TreeMap();
  
  private TreeMap nameToType = new TreeMap();
  
  public CompositeType(String typeName, String description,
		       String []itemNames, String []itemDescriptions,
		       OpenType []itemTypes)
    throws OpenDataException
  {
    super(CompositeType.class.getName(), typeName, description);

    for (int i = 0; i < itemNames.length; i++) {
      this.nameToDescription.put(itemNames[i], itemDescriptions[i]);
      this.nameToType.put(itemNames[i], itemTypes[i]);
    }
  }

  /**
   * Returns true if the composite type has a matching name.
   */
  public boolean containsKey(String name)
  {
    return this.nameToDescription.keySet().contains(name);
  }

  /**
   * Returns the description for the item name.
   */
  public String getDescription(String name)
  {
    return (String) this.nameToDescription.get(name);
  }

  /**
   * Returns the type for the item name.
   */
  public OpenType getType(String name)
  {
    return (OpenType) this.nameToType.get(name);
  }

  /**
   * Returns the set view of the names.
   */
  public Set keySet()
  {
    return this.nameToDescription.keySet();
  }

  public boolean isValue(Object value)
  {
    if (! (value instanceof CompositeData))
      return false;

    CompositeData data = (CompositeData) value;

    return equals(data.getCompositeType());
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof CompositeType))
      return false;

    CompositeType type = (CompositeType) o;

    return this.nameToType.equals(type.nameToType);
  }

  public int hashCode()
  {
    return this.nameToType.hashCode();
  }

  public String toString()
  {
    return getClass().getName() + this.nameToType;
  }
}
