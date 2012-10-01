package com.caucho.admin.thread.filter;

import com.caucho.admin.thread.ThreadSnapshot;

public class CauchoThreadFilter implements ThreadSnapshotFilter
{
  public boolean isMatch(ThreadSnapshot thread)
  {
    for(StackTraceElement element : thread.getStackElements()) {
      if (element.getClassName().startsWith("com.caucho")) {
          return true;
      }
    }
  
    return false;
  }
}