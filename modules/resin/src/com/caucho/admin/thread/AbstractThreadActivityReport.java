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
    
    for(int i=0; i<threadInfos.length; i++) {
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
          for(PortMXBean port : ports) {
            TcpConnectionInfo []connectionInfos = port.connectionInfo();
            
            if (connectionInfos != null && connectionInfos.length > 0) {
              for(TcpConnectionInfo connectionInfo : connectionInfos) {
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
    
    for(ThreadSnapshot thread : threads) {
      for(ThreadActivityGroup group : groups) {
        boolean added = group.addIfMatches(thread);
        if (added && greedy)
          break;
      }
    }
    
    return groups;
  }
}
