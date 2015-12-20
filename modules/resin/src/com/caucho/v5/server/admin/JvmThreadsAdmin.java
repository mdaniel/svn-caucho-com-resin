/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.admin;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.JvmThreadsMXBean;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;

/**
 * Statistics gathering for threads.
 */
public class JvmThreadsAdmin extends ManagedObjectBase
  implements JvmThreadsMXBean
{
  private static JvmThreadsAdmin _stat;
  
  // private long _samplePeriod = 60000L - 5000L;
  private long _samplePeriod = 5000L;
  private AtomicLong _sampleExpire = new AtomicLong(0);
  
  private ThreadMXBean _threadMXBean;
  
  private int _threadCount;
  private int _runnableCount;
  private int _nativeCount;
  private int _blockedCount;
  private int _waitingCount;
  

  private JvmThreadsAdmin()
  {
    _threadMXBean = ManagementFactory.getThreadMXBean();
    
    registerSelf();
  }

  public static JvmThreadsAdmin create()
  {
    synchronized (JvmThreadsAdmin.class) {
      if (_stat == null) {
        _stat = new JvmThreadsAdmin();
      }
    
      return _stat;
    }
  }

  public void setSamplePeriod(long period)
  {
    // add gap because the alarm period isn't exact.  This gives a bit of
    // leeway for the timer
    if (period >= 15000) {
      _samplePeriod = period - 5000;
    }
    else if (period >= 2000) {
      _samplePeriod = period - 1000;
    }
    else {
      _samplePeriod = period;
    }
  }
  
  @Override
  public String getName()
  {
    return null;
  }
  
  @Override
  public int getThreadCount()
  {
    sample();
    
    return _threadCount;
  }
  
  /**
   * Returns the number of JVM threads in the runnable state.
   */
  public int getRunnableCount()
  {
    sample();
    
    return _runnableCount;
  }
  
  /**
   * Returns the number of JVM threads running native code.
   */
  public int getNativeCount()
  {
    sample();
    
    return _nativeCount;
  }
  
  /**
   * Returns the number of JVM threads blocked.
   */
  public int getBlockedCount()
  {
    sample();
    
    return _blockedCount;
  }
  
  /**
   * Returns the number of JVM threads waiting.
   */
  public int getWaitingCount()
  {
    sample();
    
    return _waitingCount;
  }

  private void sample()
  {
    long now = CurrentTime.getCurrentTime();

    long sampleExpire = _sampleExpire.get();
    if (now < sampleExpire)
      return;
    
    long nextExpire = now + _samplePeriod;
    
    if (! _sampleExpire.compareAndSet(sampleExpire, nextExpire))
      return;
    
    long []threadIds = _threadMXBean.getAllThreadIds();
    
    if (threadIds == null)
      return;
    
    int threadCount = threadIds.length;
    
    ThreadInfo []threadInfos = _threadMXBean.getThreadInfo(threadIds);
    
    if (threadInfos == null)
      return;
    
    int runnableCount = 0;
    int nativeCount = 0;
    int waitingCount = 0;
    int blockedCount = 0;
    
    for (ThreadInfo info : threadInfos) {
      if (info == null)
        continue;
      
      Thread.State state = info.getThreadState();
      boolean isInNative = info.isInNative();
      
      if (state == null)
        continue;
      
      if (isInNative) {
        nativeCount++;
        continue;
      }
      
      switch (state) {
      case RUNNABLE:
        runnableCount++;
        break;
      case BLOCKED:
        blockedCount++;
        break;
      case WAITING:
      case TIMED_WAITING:
        waitingCount++;
        break;
      }
    }
    
    _threadCount = threadCount;
    _runnableCount = runnableCount;
    _nativeCount = nativeCount;
    _blockedCount = blockedCount;
    _waitingCount = waitingCount;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
