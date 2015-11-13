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

package com.caucho.v5.relaxng.program;

import com.caucho.v5.config.cf.NameCfg;

import java.util.HashSet;

/**
 * Matches names.
 */
public class NameItem extends NameClassItem {
  private final NameCfg _name;

  public NameItem(NameCfg name)
  {
    _name = name;
  }
    
  public NameCfg getQName()
  {
    return _name;
  }

  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<NameCfg> set)
  {
    set.add(_name);
  }

  /**
   * Returns true if the name matches.
   */
  public boolean matches(NameCfg name)
  {
    return name.equals(_name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(String prefix)
  {
    if (prefix.equals(""))
      return "<" + _name.getName() + ">";
    else
      return prefix + _name.getName();
  }

  public int hashCode()
  {
    return _name.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof NameItem))
      return false;

    NameItem name = (NameItem) o;

    return _name.equals(name._name);
  }

  public String toString()
  {
    return "NameItem[" + _name + "]";
  }
}

