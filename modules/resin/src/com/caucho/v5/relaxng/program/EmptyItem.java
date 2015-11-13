/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.relaxng.program;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.L10N;

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
  public void firstSet(HashSet<NameCfg> set)
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
  public Item setAttribute(NameCfg name, String value)
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
