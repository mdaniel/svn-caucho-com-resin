package com.caucho.admin.thread.filter;

import com.caucho.admin.thread.ThreadSnapshot;

public interface ThreadSnapshotFilter
{
  public boolean isMatch(ThreadSnapshot thread);
}
