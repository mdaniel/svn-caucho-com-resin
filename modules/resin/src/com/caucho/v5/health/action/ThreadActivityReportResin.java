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

import com.caucho.v5.health.snapshot.AcceptThreadFilter;
import com.caucho.v5.health.snapshot.AnyThreadFilter;
import com.caucho.v5.health.snapshot.BlockedThreadFilter;
import com.caucho.v5.health.snapshot.CauchoThreadFilter;
import com.caucho.v5.health.snapshot.IdlePoolFilter;
import com.caucho.v5.health.snapshot.NativeThreadFilter;
import com.caucho.v5.health.snapshot.PortThreadFilter;
import com.caucho.v5.health.snapshot.RunningThreadFilter;
import com.caucho.v5.health.snapshot.ThreadSnapshotFilter;
import com.caucho.v5.health.snapshot.WaitingThreadFilter;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.management.server.PortMXBean;
import com.caucho.v5.management.server.ServerMXBean;

public class ThreadActivityReportResin extends ThreadActivityReportBase
{
  private final Map<ThreadSnapshotFilter, ThreadActivityCode> _filters = new 
    LinkedHashMap<ThreadSnapshotFilter, ThreadActivityCode>();
  
  private Map<Character, String> _key = new HashMap<Character, String>();
  
  public ThreadActivityReportResin()
  {
    _filters.put(new AcceptThreadFilter(), ThreadActivityCode.ACCEPT);
    _filters.put(new IdlePoolFilter(), ThreadActivityCode.IDLE);
    _filters.put(new RunningThreadFilter(), ThreadActivityCode.RUNNING);
    _filters.put(new BlockedThreadFilter(), ThreadActivityCode.BLOCKED);
    _filters.put(new NativeThreadFilter(), ThreadActivityCode.NATIVE);
    _filters.put(new WaitingThreadFilter(), ThreadActivityCode.WAITING);

    // create better descriptions?
    for (ThreadActivityCode code : ThreadActivityCode.values()) {
      _key.put(code.getChar(), code.toString());
    }
    
    _key = Collections.unmodifiableMap(_key);
  }
  
  public Map<Character, String> getScoreboardKey()
  {
    return _key;
  }
  
  protected boolean assignActivityCode(ThreadSnapshot threadSnapshot)
  {
    for (Map.Entry<ThreadSnapshotFilter, ThreadActivityCode> entry 
      : _filters.entrySet()) {
      if (entry.getKey().isMatch(threadSnapshot)) {
        threadSnapshot.setCode(entry.getValue().getChar());
        return true;
      }
    }
    
    return false;
  }
  
  protected ThreadActivityGroup[] createGroups()
  {
    List<ThreadActivityGroup> groups = new ArrayList<ThreadActivityGroup>();
    
    HttpContainerServlet servletService = HttpContainerServlet.current();
    if (servletService != null) {
      ServerMXBean serverAdmin = servletService.getAdmin();
      
      if (serverAdmin != null) {
        PortMXBean []ports = serverAdmin.getPorts();
        
        if (ports != null && ports.length > 0) {
          for (PortMXBean port : ports) {
            String portName = (port.getAddress() == null ? "*" : 
              port.getAddress()) + ":" + port.getPort();
            
            String groupName = "Port " + portName + " Threads";
            
            PortThreadFilter filter = new PortThreadFilter(portName);
            ThreadActivityGroup group = new ThreadActivityGroup(groupName, filter);
            
            groups.add(group);
          }
        }
      }
    }
    
    CauchoThreadFilter cauchoFilter = new CauchoThreadFilter();
    groups.add(new ThreadActivityGroup("Resin Threads", cauchoFilter));
    
    AnyThreadFilter miscFilter = new AnyThreadFilter();
    groups.add(new ThreadActivityGroup("Other Threads", miscFilter));
    
    ThreadActivityGroup []array = new ThreadActivityGroup[groups.size()];
    groups.toArray(array);
    
    return array;
  }
}