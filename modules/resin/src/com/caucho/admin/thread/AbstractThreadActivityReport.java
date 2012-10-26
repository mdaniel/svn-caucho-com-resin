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

import java.lang.management.*;
import java.util.*;
import java.util.logging.Logger;

import com.caucho.management.server.*;
import com.caucho.server.cluster.ServletService;
import com.caucho.util.L10N;

public abstract class AbstractThreadActivityReport
{
  private static final Logger log
    = Logger.getLogger(AbstractThreadActivityReport.class.getName());

  private static final L10N L = new L10N(AbstractThreadActivityReport.class);
  
  public ThreadActivityGroup []execute(boolean greedy)
  {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    ThreadInfo []threadInfos = threadMXBean.dumpAllThreads(true, true);
    if (threadInfos == null || threadInfos.length == 0) {
      log.fine(L.l("execute failed: ThreadMXBean.dumpAllThreads produced no results"));
      return null;
    }
    
    ThreadSnapshot []threads = createThreadSnapshots(threadInfos);
    
    ThreadActivityGroup []groups = partitionThreads(threads, greedy);
    
    return groups;
  }
  
  protected abstract ThreadActivityGroup []createGroups();
  
  protected abstract boolean assignActivityCode(ThreadSnapshot thread);
  
  public abstract Map<Character, String> getScoreboardKey();
  
  private ThreadSnapshot []createThreadSnapshots(ThreadInfo []threadInfos)
  {
    Map<Long, TcpConnectionInfo> connectionsById = getConnectionsById();
    
    ThreadSnapshot []threads = new ThreadSnapshot[threadInfos.length];
    
    for (int i=0; i<threadInfos.length; i++) {
      threads[i] = new ThreadSnapshot(threadInfos[i]);
      
      TcpConnectionInfo connectionInfo = 
        connectionsById.get(threadInfos[i].getThreadId());
      
      if (connectionInfo != null)
        threads[i].setConnectionInfo(connectionInfo);
      
      assignActivityCode(threads[i]);
    }
    
    return threads;
  }
  
  private Map<Long, TcpConnectionInfo> getConnectionsById()
  {
    Map<Long, TcpConnectionInfo> connectionInfoMap = 
      new HashMap<Long, TcpConnectionInfo>();
    
    ServletService servletService = ServletService.getCurrent();
    if (servletService != null) {
      ServerMXBean serverAdmin = servletService.getAdmin();
      
      if (serverAdmin != null) {
        PortMXBean []ports = serverAdmin.getPorts();
        
        if (ports != null && ports.length > 0) {
          for (PortMXBean port : ports) {
            TcpConnectionInfo []connectionInfos = port.connectionInfo();
            
            if (connectionInfos != null && connectionInfos.length > 0) {
              for (TcpConnectionInfo connectionInfo : connectionInfos) {
                long threadId = connectionInfo.getThreadId();
                
                connectionInfoMap.put(threadId, connectionInfo);
              }
            }
          }
        }
      }
    }
    
    return connectionInfoMap;
  }
  
  private ThreadActivityGroup []partitionThreads(ThreadSnapshot []threads, 
                                                 boolean greedy)
  {
    ThreadActivityGroup []groups = createGroups();
    
    for (ThreadSnapshot thread : threads) {
      for (ThreadActivityGroup group : groups) {
        boolean added = group.addIfMatches(thread);
        if (added && greedy)
          break;
      }
    }
    
    return groups;
  }
}
