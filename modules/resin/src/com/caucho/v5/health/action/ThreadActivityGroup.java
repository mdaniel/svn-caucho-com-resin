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
 */

package com.caucho.v5.health.action;

import java.util.ArrayList;
import java.util.List;

import com.caucho.v5.health.snapshot.ThreadSnapshotFilter;

public class ThreadActivityGroup
{
  private final String _name;
  
  private final List<ThreadSnapshotFilter> _filters = 
    new ArrayList<ThreadSnapshotFilter>();
  
  private final List<ThreadSnapshot> _threads = 
    new ArrayList<ThreadSnapshot>();
  
  public ThreadActivityGroup(String name)
  {
    _name = name;
  }

  public ThreadActivityGroup(String name, ThreadSnapshotFilter filter)
  {
    this(name);
    
    _filters.add(filter);
  }
  
  public void addFilter(ThreadSnapshotFilter filter)
  {
    _filters.add(filter);
  }
  
  public String getName()
  {
    return _name;
  }
  
  public boolean addIfMatches(ThreadSnapshot thread)
  {
    for (ThreadSnapshotFilter filter : _filters) {
      if (! filter.isMatch(thread))
        return false;
    }
    
    _threads.add(thread);
    return true;
  }

  public String toScoreboard()
  {
    StringBuilder sb = new StringBuilder();
    for (ThreadSnapshot thread : _threads) {
      sb.append(thread.getCode());
    }
    
    return sb.toString();
  }
}
