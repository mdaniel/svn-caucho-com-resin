package com.caucho.admin.thread;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;

import com.caucho.management.server.TcpConnectionInfo;

public class ThreadSnapshot
{
  // Based on the source, ThreadInfo is a snapshot of the thread taken at 
  // the time of the dump, so it should not change after it is obtained
  private ThreadInfo _threadInfo;
  
  private TcpConnectionInfo _connectionInfo;
  
  private char _code = '?';
  
  public ThreadSnapshot(ThreadInfo threadInfo)
  {
    _threadInfo = threadInfo;
  }
  
  public char getCode()
  {
    return _code;
  }
  
  public void setCode(char code)
  {
    _code = code;
  }
  
  public ThreadInfo getThreadInfo()
  {
    return _threadInfo;
  }
  
  public void setConnectionInfo(TcpConnectionInfo connectionInfo)
  {
    _connectionInfo = connectionInfo;
  }
  
  public TcpConnectionInfo getConnectionInfo()
  {
    return _connectionInfo;
  }
  
  public long getId()
  {
    return _threadInfo.getThreadId();
  }

  public State getState()
  {
    return _threadInfo.getThreadState();
  }
  
  public StackTraceElement []getStackElements()
  {
    return _threadInfo.getStackTrace();
  }
  
  public String getPort()
  {
    if (_connectionInfo != null)
      return _connectionInfo.getPortName();

    return null;
  }
}