/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.lang.management.ThreadInfo;

import com.caucho.bartender.network.NetworkSystem;
import com.caucho.config.ConfigException;
import com.caucho.http.protocol.RequestHttpBase;
import com.caucho.network.listen.ConnectionTcp;
import com.caucho.util.ThreadDump;

public class ProThreadDump extends ThreadDump
{
  public ProThreadDump()
  {
  }
  
  protected void buildThread(StringBuilder sb, ThreadInfo info)
  {
    sb.append("\n\"");
    sb.append(info.getThreadName());
    sb.append("\" id=" + info.getThreadId());
    sb.append(" " + info.getThreadState());

    if (info.isInNative())
      sb.append(" (in native)");

    if (info.isSuspended())
      sb.append(" (suspended)");

    String lockName = info.getLockName();
    if (lockName != null) {
      sb.append("\n    waiting on ");
      sb.append(lockName);

      if (info.getLockOwnerName() != null) {
        sb.append("\n    owned by \"");
        sb.append(info.getLockOwnerName());
        sb.append("\"");
      }
    }

    sb.append("\n");

    NetworkSystem networkSystem = NetworkSystem.getCurrent();

    if (networkSystem != null) {
      ConnectionTcp conn
        = networkSystem.findConnectionByThreadId(info.getThreadId());

      if (conn != null && conn.getRequest() instanceof RequestHttpBase) {
        RequestHttpBase req = (RequestHttpBase) conn.getRequest();

        if (req.getRequestURI() != null) {
          sb.append("   ").append(req.getRequestURI()).append("\n");
        }
      }
    }

    StackTraceElement []stackList = info.getStackTrace();
    if (stackList == null)
      return;

    for (StackTraceElement stack : stackList) {
      sb.append("  at ");
      sb.append(stack.getClassName());
      sb.append(".");
      sb.append(stack.getMethodName());

      if (stack.getFileName() != null) {
        sb.append(" (");
        sb.append(stack.getFileName());
        if (stack.getLineNumber() > 0) {
          sb.append(":");
          sb.append(stack.getLineNumber());
        }
        sb.append(")");
      }

      if (stack.isNativeMethod())
        sb.append(" (native)");

      sb.append("\n");
    }
  }
}
