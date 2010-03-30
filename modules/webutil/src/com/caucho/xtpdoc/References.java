/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import java.util.TreeSet;

import java.util.logging.Level;
import java.util.logging.Logger;


public class References {
  private final TreeSet<String> _references 
    = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

  public void addText(String text)
  {
    String []refs = text.split("[ ,]+");

    for (int i = 0; i < refs.length; i++)
      addReference(refs[i]);
  }
  
  public void addReference(String ref)
  {
    _references.add(ref);
  }

  public Iterable<String> getReferences()
  {
    return _references;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder("References[");
    
    for (String ref : _references) {
      sb.append(ref);
      sb.append(",");
    }

    sb.append("]");

    return sb.toString();
  }
}
