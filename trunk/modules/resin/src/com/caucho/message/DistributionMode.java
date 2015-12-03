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

package com.caucho.message;

import com.caucho.util.L10N;

/**
 * Selects how a message should be treated when it's acquired from the node.
 *   "MOVE" is like a queue, it deletes the node.
 *   "COPY" is like a topic, it copies the node.
 */
public enum DistributionMode {
  MOVE("move"),
  COPY("copy");
  
  private static final L10N L = new L10N(DistributionMode.class);
  private String _name;
  
  DistributionMode(String name)
  {
    _name = name;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public static DistributionMode find(String name)
  {
    if (name == null)
      return null;
    else if ("move".equals(name))
      return MOVE;
    else if ("copy".equals(name))
      return COPY;
    else
      throw new IllegalArgumentException(L.l("unknown type: " + name));
  }
}
