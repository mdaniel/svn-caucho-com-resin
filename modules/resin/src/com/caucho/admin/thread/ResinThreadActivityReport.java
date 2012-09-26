package com.caucho.admin.thread;

import java.util.*;

import com.caucho.admin.thread.filter.*;
import com.caucho.management.server.*;
import com.caucho.server.cluster.ServletService;

public class ResinThreadActivityReport extends AbstractThreadActivityReport
{
  public enum ThreadActivityCode
  {
    RUNNING('R'),
    BLOCKED('b'),
    NATIVE('N'),
//    LOCKING('l'),
    WAITING('w'),
    ACCEPT('_'),
    IDLE('.');

    final char _char;
    
    private ThreadActivityCode(char c)
    {
      _char = c;
    }
    
    public char getChar()
    {
      return _char;
    }
  }
  
  private Map<ThreadSnapshotFilter, ThreadActivityCode> _filters = new 
    LinkedHashMap<ThreadSnapshotFilter, ThreadActivityCode>();
  
  private Map<Character, String> _key = new HashMap<Character, String>();
  
  public ResinThreadActivityReport()
  {
    _filters.put(new AcceptThreadFilter(), ThreadActivityCode.ACCEPT);
    _filters.put(new IdlePoolFilter(), ThreadActivityCode.IDLE);
    _filters.put(new RunningThreadFilter(), ThreadActivityCode.RUNNING);
    _filters.put(new BlockedThreadFilter(), ThreadActivityCode.BLOCKED);
    _filters.put(new NativeThreadFilter(), ThreadActivityCode.NATIVE);
    _filters.put(new WaitingThreadFilter(), ThreadActivityCode.WAITING);

    // create better descriptions?
    for(ThreadActivityCode code : ThreadActivityCode.values()) {
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
    for(Map.Entry<ThreadSnapshotFilter, ThreadActivityCode> entry 
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
          for(PortMXBean port : ports) {
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