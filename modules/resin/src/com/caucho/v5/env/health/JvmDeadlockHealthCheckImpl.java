/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.lang.management.ThreadInfo;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.jmx.JmxUtil;

/**
 * Configuration for management.
 */
public class JvmDeadlockHealthCheckImpl extends AbstractHealthCheck
{
  private static ObjectName _threadMBean;

  private MBeanServer _mbeanServer;

  public JvmDeadlockHealthCheckImpl()
  {
    _mbeanServer = JmxUtil.getMBeanServer();
  }

  @Override
  public HealthCheckResult checkHealth()
  {
    try {
      return checkJVMDeadlock();
    } catch (Exception e) {
      HealthCheckResult result = new HealthCheckResult(HealthStatus.UNKNOWN);
      result.setMessage("checkJVMDeadlock failed: " + e.getMessage());
      result.setException(e);
      return result;
    }
  }
  
  /**
   * Checks for a JVM deadlock
   */
  private HealthCheckResult checkJVMDeadlock()
    throws Exception
  {
    if (_mbeanServer == null)
      return new HealthCheckResult(HealthStatus.OK);

    long []deadlockedThreads =
      (long []) _mbeanServer.invoke(_threadMBean,
                                    "findMonitorDeadlockedThreads",
                                    new Object[0], new String[0]);

    if (deadlockedThreads == null)
      return new HealthCheckResult(HealthStatus.OK);
    
    // Resin.getCurrent().startFailSafeShutdown("JDK detected deadlock. Restarting Resin.");

    // logStderr is in case the logging itself is locked
    
    StringBuilder sb = new StringBuilder(); 
    sb.append("JDK detected deadlock:");
    
    for (int i = 0; i < deadlockedThreads.length; i++) {
      Long id = new Long(deadlockedThreads[i]);
      Integer maxDepth = new Integer(Integer.MAX_VALUE);

      Object objInfo = _mbeanServer.invoke(_threadMBean,
                                           "getThreadInfo",
                                           new Object[] { id, maxDepth },
                                           new String[] {"long", "int"});

      ThreadInfo info = null;

      if (objInfo instanceof ThreadInfo)
        info = (ThreadInfo) objInfo;
      else if (objInfo instanceof CompositeData)
        info = ThreadInfo.from((CompositeData) objInfo);

      if (info != null) {
        String infoString = threadInfoToString(info);
        sb.append("\n");
        sb.append(infoString);
      }
    }
    
    String msg = sb.toString();
    
    return new HealthCheckResult(HealthStatus.FATAL, msg);
  }

  /**
   * Prints the thread info.
   */
  private String threadInfoToString(ThreadInfo info)
  {
    if (info == null)
      return "null";

    return info.toString();
    /*
    StringBuilder sb = new StringBuilder();
    sb.append(info.getThreadName()
    String s = info.toString();

    String lockName = info.getLockName();
    if (lockName != null)
      s += "\n  waiting for " + lockName;

    String lockOwnerName = info.getLockOwnerName();
    if (lockOwnerName != null)
      s += "\n  owned by " + lockOwnerName;

    s += "\n" + info.toString();

    StackTraceElement[] stackTrace = info.getStackTrace();

    if (stackTrace != null) {
      for (int i = 0; i < stackTrace.length; i++) {
        s += "\n\t" + stackTrace[i];
      }
    }

    return s;
    */
  }
  
  static {
    try {
      _threadMBean = new ObjectName("java.lang:type=Threading");
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
