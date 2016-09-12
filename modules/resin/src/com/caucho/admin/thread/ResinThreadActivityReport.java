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
 */

package com.caucho.admin.thread;

import java.util.*;

import com.caucho.admin.thread.filter.*;
import com.caucho.management.server.*;
import com.caucho.server.cluster.ServletService;

public class ResinThreadActivityReport extends AbstractThreadActivityReport
{
  private final Map<ThreadSnapshotFilter, ThreadActivityCode> _filters = new 
    LinkedHashMap<ThreadSnapshotFilter, ThreadActivityCode>();
  
  private Map<Character, String> _key = new TreeMap<Character, String>();
  
  public ResinThreadActivityReport()
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
    
    ServletService servletService = ServletService.getCurrent();
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