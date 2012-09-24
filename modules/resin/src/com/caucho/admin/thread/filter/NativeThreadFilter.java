package com.caucho.admin.thread.filter;

import com.caucho.admin.thread.ThreadSnapshot;

public class NativeThreadFilter implements ThreadSnapshotFilter
{
  public boolean isMatch(ThreadSnapshot thread)
  {
    return thread.getThreadInfo().isInNative();
  }
}