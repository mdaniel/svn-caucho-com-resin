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

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;

import com.caucho.management.server.TcpConnectionInfo;

public class ThreadSnapshot
{
  // Based on the source, ThreadInfo is a snapshot of the thread taken at 
  // the time of the dump, so it should not change after it is obtained
  private final ThreadInfo _threadInfo;
  
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