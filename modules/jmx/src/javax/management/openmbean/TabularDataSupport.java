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
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents composite data.
 */
public class TabularDataSupport
  implements TabularData, Map, Cloneable, Serializable {
  private HashMap dataMap;
  private TabularType tabularType;

  public TabularDataSupport(TabularType tabularType)
    throws OpenDataException
  {
    this(tabularType, 101, 0.75f);
  }

  public TabularDataSupport(TabularType tabularType,
			    int initialCapacity, float loadFactor)
    throws OpenDataException
  {
    this.tabularType = tabularType;
    this.dataMap = new HashMap(initialCapacity, loadFactor);
  }

  /**
   * Returns the tabular type.
   */
  public TabularType getTabularType()
  {
    return this.tabularType;
  }

  /**
   * Returns the index of the composite data.
   */
  public Object []calculateIndex(CompositeData value)
  {
    List names = getTabularType().getIndexNames();

    Object []key = new Object[names.size()];

    for (int i = 0; i < key.length; i++)
      key[i] = value.get((String) names.get(i));

    return key;
  }

  /**
   * Returns true if the key matches.
   */
  public boolean containsKey(Object key)
  {
    if (key == null || ! key.getClass().isArray())
      return false;
    else
      return containsKey((Object []) key);
  }

  /**
   * Returns true if the key matches.
   */
  public boolean containsKey(Object []key)
  {
    return this.dataMap.containsKey(createKey(key));
  }

  /**
   * Returns true if the map contains the key.
   */
  public boolean containsValue(Object value)
  {
    if (! (value instanceof CompositeData))
      return false;
    
    return this.dataMap.containsValue(value);
  }

  /**
   * Returns true if the map contains the key.
   */
  public boolean containsValue(CompositeData value)
  {
    return this.dataMap.containsValue(value);
  }

  /**
   * Returns the get value.
   */
  public Object get(Object key)
  {
    return get((Object []) key);
  }

  /**
   * Returns the get value.
   */
  public CompositeData get(Object []key)
  {
    return (CompositeData) this.dataMap.get(createKey(key));
  }

  /**
   * Puts the object in the map.
   */
  public Object put(Object key, Object value)
  {
    put((CompositeData) value);
    
    return null;
  }

  /**
   * Puts the object in the map.
   */
  public void put(CompositeData value)
  {
    this.dataMap.put(createKey(calculateIndex(value)), value);
  }

  /**
   * Puts the object in the map.
   */
  public Object remove(Object key)
  {
    return remove((Object []) key);
  }

  /**
   * Puts the object in the map.
   */
  public CompositeData remove(Object []key)
  {
    return (CompositeData) this.dataMap.remove(createKey(key));
  }

  /**
   * Adds all the values from the map.
   */
  public void putAll(Map t)
  {
    Iterator iter = t.values().iterator();

    while (iter.hasNext()) {
      CompositeData data = (CompositeData) iter.next();

      put(data);
    }
  }

  /**
   * Adds all the values from the map.
   */
  public void putAll(CompositeData []values)
  {
    for (int i = 0; i < values.length; i++)
      put(values[i]);
  }

  /**
   * Clears the map.
   */
  public void clear()
  {
    this.dataMap.clear();
  }

  /**
   * Returns the number of rows.
   */
  public int size()
  {
    return this.dataMap.size();
  }

  /**
   * Returns true if the map is empty.
   */
  public boolean isEmpty()
  {
    return this.dataMap.isEmpty();
  }

  /**
   * Returns the key set.
   */
  public Set keySet()
  {
    return this.dataMap.keySet();
  }

  /**
   * Returns the values.
   */
  public Collection values()
  {
    return this.dataMap.values();
  }

  /**
   * Entry set.
   */
  public Set entrySet()
  {
    return this.dataMap.entrySet();
  }

  /**
   * Returns a clone.
   */
  public Object clone()
  {
    return this;
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof TabularDataSupport))
      return false;

    TabularDataSupport data = (TabularDataSupport) o;

    if (! this.tabularType.equals(data.tabularType))
      return false;

    else
      return this.dataMap.equals(data.dataMap);
  }

  public int hashCode()
  {
    return this.tabularType.hashCode();
  }

  private List createKey(Object []key)
  {
    ArrayList keyList = new ArrayList();

    for (int i = 0; i < key.length; i++)
      keyList.add(key[i]);

    return keyList;
  }
}
