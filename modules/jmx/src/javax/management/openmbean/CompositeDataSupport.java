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

import java.io.Serializable;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents composite data.
 */
public class CompositeDataSupport implements CompositeData, Serializable {
  private CompositeType compositeType;
  private Map items;

  public CompositeDataSupport(CompositeType compositeType,
			      Map items)
    throws OpenDataException
  {
    this.compositeType = compositeType;
    this.items = items;
  }

  public CompositeDataSupport(CompositeType compositeType,
			      String []itemNames,
			      Object []itemValues)
    throws OpenDataException
  {
    this.compositeType = compositeType;
    this.items = new HashMap();

    for (int i = 0; i < itemNames.length; i++)
      this.items.put(itemNames[i], itemValues[i]);
  }

  public CompositeType getCompositeType()
  {
    return this.compositeType;
  }

  public Object get(String key)
  {
    return this.items.get(key);
  }

  public Object []getAll(String []keys)
  {
    Object []values = new Object[keys.length];

    for (int i = 0; i < keys.length; i++)
      values[i] = this.items.get(keys[i]);

    return values;
  }

  public boolean containsKey(String key)
  {
    return this.items.containsKey(key);
  }

  public boolean containsValue(Object value)
  {
    return this.items.containsValue(value);
  }

  public Collection values()
  {
    return this.items.values();
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof CompositeDataSupport))
      return false;

    CompositeDataSupport data = (CompositeDataSupport) o;

    return (this.compositeType.equals(data.compositeType) &&
	    this.items.equals(data.items));
  }

  public int hashCode()
  {
    return this.compositeType.hashCode();
  }
}
