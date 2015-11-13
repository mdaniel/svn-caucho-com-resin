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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.relaxng.pattern;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.relaxng.RelaxException;
import com.caucho.v5.relaxng.program.NameClassItem;
import com.caucho.v5.relaxng.program.NameItem;

/**
 * Relax name pattern
 */
public class NamePattern extends NameClassPattern {
  private final NameCfg _name;
  private final NameItem _item;

  /**
   * Creates a new element pattern.
   */
  public NamePattern(NameCfg name)
  {
    _name = name;
    _item = new NameItem(_name);
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "name";
  }

  /**
   * Creates the program.
   */
  public NameClassItem createNameItem()
    throws RelaxException
  {
    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return _name.getName();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof NamePattern))
      return false;

    NamePattern elt = (NamePattern) o;

    return _name.equals(elt._name);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "Name[" + _name.getName() + "]";
  }
}

