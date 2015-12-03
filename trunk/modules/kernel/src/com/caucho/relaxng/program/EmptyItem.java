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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.relaxng.program;

import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.HashSet;

/**
 * Generates programs from patterns.
 */
public final class EmptyItem extends Item {
  protected final static L10N L = new L10N(EmptyItem.class);

  private static final EmptyItem EMPTY = new EmptyItem();

  /**
   * Creates the empty item.
   */
  public static EmptyItem create()
  {
    return EMPTY;
  }

  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
  }
  
  /**
   * The empty item can produce empty.
   */
  @Override
  public final boolean allowEmpty()
  {
    return true;
  }
  
  /**
   * Sets an attribute.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return the program for handling the element
   */
  public Item setAttribute(QName name, String value)
  {
    return this;
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return "empty";
  }

  /**
   * True for simple syntax.
   */
  public boolean isSimpleSyntax()
  {
    return true;
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    return 37;
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    return o instanceof EmptyItem;
  }

  public String toString()
  {
    return "EmptyItem[]";
  }
}
