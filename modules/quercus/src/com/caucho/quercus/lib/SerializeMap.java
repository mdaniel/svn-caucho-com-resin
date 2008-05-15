/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */
package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.util.IntMap;
import com.caucho.util.IntSet;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/*
 * Holds reference indexes for serialization.
 */
public final class SerializeMap {
  private HashMap<Var, Integer> _varMap
    = new HashMap<Var, Integer>();
  
  // serialization index for references
  private int _index = 1;
  
  public SerializeMap()
  {
  }
  
  /*
   * Increments the serialization index for the next reference
   */
  public void incrementIndex()
  {
    _index++;
  }
  
  /*
   * Stores reference at the current index in the serialization process.
   */
  public void put(Var var)
  {
    _varMap.put(var, new Integer(_index));
  }
  
  /*
   * Retrieves the index of this reference in the serialization, if exists.
   */
  public Integer get(Var var)
  {
    return _varMap.get(var);
  }
  
}

