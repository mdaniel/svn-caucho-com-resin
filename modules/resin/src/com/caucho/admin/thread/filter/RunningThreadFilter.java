package com.caucho.admin.thread.filter;

import java.lang.Thread.State;

import com.caucho.admin.thread.ThreadSnapshot;

public class RunningThreadFilter implements ThreadSnapshotFilter
{
  public boolean isMatch(ThreadSnapshot thread)
  {
    return thread.getState() == State.RUNNABLE;
  }
}