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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.caucho.v5.health.snapshot.IdlePoolFilter;
import com.caucho.v5.health.snapshot.ThreadSnapshotFilter;

public class ThreadActivityReportDatabase extends ThreadActivityReportBase
{
  private final Map<ThreadSnapshotFilter, ThreadActivityCode> _codes = new 
    LinkedHashMap<ThreadSnapshotFilter, ThreadActivityCode>();
  
  private Map<Character, String> _key = new HashMap<Character, String>();
  
  public ThreadActivityReportDatabase()
  {
    // add others
    _codes.put(new IdlePoolFilter(), ThreadActivityCode.IDLE);

    // create better descriptions?
    for (ThreadActivityCode code : ThreadActivityCode.values()) {
      _key.put(code.getChar(), code.toString());
    }
    
    _key = Collections.unmodifiableMap(_key);    
  }
  
  @Override
  public Map<Character, String> getScoreboardKey()
  {
    return _key;
  }
  
  @Override
  protected boolean assignActivityCode(ThreadSnapshot thread)
  {
    for (Map.Entry<ThreadSnapshotFilter, ThreadActivityCode> entry 
      : _codes.entrySet()) {
      if (entry.getKey().isMatch(thread)) {
        thread.setCode(entry.getValue().getChar());
        return true;
      }
    }
    
    return false;
  }
  
  @Override
  protected ThreadActivityGroup[] createGroups()
  {
    List<ThreadActivityGroup> groups = new ArrayList<ThreadActivityGroup>();
    
    // create a group or each database connection pool
    
    ThreadActivityGroup []array = new ThreadActivityGroup[groups.size()];
    groups.toArray(array);
    
    return array;
  }
}