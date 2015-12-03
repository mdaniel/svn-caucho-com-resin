/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.profile;

/**
 * Report of profile entries
 */
public class ProfileItem
{
  private final String _name;
  private final String _parent;
  private final long _count;
  private final long _micros;

  public ProfileItem(String name, String parent, long count, long micros)
  {
    _name = name;
    _parent = parent;
    _count = count;
    _micros = micros;
  }

  /**
   * Returns the item's function name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the item's parent name
   */
  public String getParent()
  {
    return _parent;
  }

  /**
   * Returns the item call count
   */
  public long getCount()
  {
    return _count;
  }

  /**
   * Returns the item execution time in microseconds
   */
  public long getMicros()
  {
    return _micros;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _name
            + ",parent=" + _parent
            + ",count=" + _count
            + ",micros=" + _micros
            + "]");
  }
}

