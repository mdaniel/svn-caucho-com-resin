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

/**
 * Represents tabular data.
 */
public interface TabularData {
  public TabularType getTabularType();

  public Object []calculateIndex(CompositeData value)
    throws InvalidOpenTypeException;

  /**
   * Returns the number of rows in the TabularData.
   */
  public int size();

  /**
   * Returns true if the tabular data is empty.
   */
  public boolean isEmpty();

  /**
   * Returns true if the tabular data has the row with the key.
   */
  public boolean containsKey(Object []key);

  /**
   * Returns true if the tabular data has the value.
   */
  public boolean containsValue(CompositeData value);

  /**
   * Returns the composite data.
   */
  public CompositeData get(Object []key)
    throws InvalidKeyException;
  

  /**
   * Sets the composite data.
   */
  public void put(CompositeData value)
    throws InvalidOpenTypeException, KeyAlreadyExistsException;

  /**
   * Removes the composite data with the given key.
   */
  public CompositeData remove(Object []key)
    throws InvalidKeyException;

  /**
   * Adds an array of values.
   */
  public void putAll(CompositeData []values)
    throws InvalidOpenTypeException, KeyAlreadyExistsException;

  /**
   * Clears the data.
   */
  public void clear();

  /**
   * Returns a set of the keys.
   */
  public Set keySet();

  /**
   * Returns a collection of the values.
   */
  public Collection values();
}
