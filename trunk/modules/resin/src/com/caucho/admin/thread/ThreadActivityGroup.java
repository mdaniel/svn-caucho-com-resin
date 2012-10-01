package com.caucho.admin.thread;

import java.util.*;

import com.caucho.admin.thread.filter.ThreadSnapshotFilter;

public class ThreadActivityGroup
{
  private String _name;
  
  private List<ThreadSnapshotFilter> _filters = 
    new ArrayList<ThreadSnapshotFilter>();
  
  private List<ThreadSnapshot> _threads = 
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
    for(ThreadSnapshotFilter filter : _filters) {
      if (! filter.isMatch(thread))
        return false;
    }
    
    _threads.add(thread);
    return true;
  }

  public String toScoreboard()
  {
    StringBuilder sb = new StringBuilder();
    for(ThreadSnapshot thread : _threads) {
      sb.append(thread.getCode());
    }
    
    return sb.toString();
  }
}
