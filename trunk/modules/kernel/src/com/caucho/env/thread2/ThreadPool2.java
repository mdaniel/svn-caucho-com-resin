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
 * @author Scott Ferguson
 */

package com.caucho.env.thread2;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.health.HealthSystemFacade;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.Friend;
import com.caucho.util.L10N;
import com.caucho.util.RingValueQueue;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public class ThreadPool2 implements Executor {
  private static final L10N L = new L10N(ThreadPool2.class);
  
  private static final Logger log
    = Logger.getLogger(ThreadPool2.class.getName());
  
  public static final String THREAD_FULL_EVENT = "caucho.thread.schedule.full";

  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;
  
  private static final long PRIORITY_TIMEOUT = 10L;
  
  private static final int PRIORITY_IDLE_MIN = 2;
  private static final int THREAD_IDLE_MIN = 16;
  private static final int THREAD_IDLE_MAX = 1024;
  
  private static final int THREAD_THROTTLE_LIMIT = 100;
  private static final long THREAD_THROTTLE_SLEEP = 10;
  
  private static final AtomicReference<ThreadPool2> _globalThreadPool
    = new AtomicReference<ThreadPool2>();
  
  private final String _name;
  
  private final ThreadLauncher2 _launcher;
  private final Lifecycle _lifecycle = new Lifecycle();
  
  private final ThreadScheduleWorker _scheduleWorker
    = new ThreadScheduleWorker();
  
  // configuration items
  
  private int _idleMin = THREAD_IDLE_MIN;
  private int _idleMax = THREAD_IDLE_MAX;
  
  private int _priorityIdleMin = PRIORITY_IDLE_MIN;
  
  
  //
  // lifecycle count to drain on environment change
  //

  private final AtomicLong _resetCount = new AtomicLong();

  //
  // thread max and thread lifetime counts
  //
  private final AtomicLong _overflowCount = new AtomicLong();
  
  //
  // the idle stack
  //
  
  private final RingValueQueue<ResinThread2> _idleThreadRing
    = new RingValueQueue<ResinThread2>(8192);

  //
  // task/priority overflow queues
  //

  private final ThreadTaskRing2 _taskQueue = new ThreadTaskRing2();
  
  private final ThreadTaskRing2 _priorityQueue = new ThreadTaskRing2();
  
  private final RingValueQueue<Thread> _unparkQueue
    = new RingValueQueue<Thread>(1024);

  private int _waitCount;

  public ThreadPool2()
  {
    this("system");
  }

  public ThreadPool2(String name)
  {
    _name = name;
    
    _launcher = new ThreadLauncher2(this);
    _launcher.setIdleMax(THREAD_IDLE_MAX + PRIORITY_IDLE_MIN);
    _launcher.setIdleMin(THREAD_IDLE_MIN + PRIORITY_IDLE_MIN);
    
    _launcher.setThrottleLimit(THREAD_THROTTLE_LIMIT);
    _launcher.setThrottleSleepTime(THREAD_THROTTLE_SLEEP);
    // initialize default values
    init();
  }

  public static ThreadPool2 getCurrent()
  {
    ThreadPool2 threadPool = _globalThreadPool.get();
    
    if (threadPool != null)
      throw new IllegalStateException();
    
    return threadPool;
  }

  protected void setAsGlobal(ThreadPool2 pool)
  {
    _globalThreadPool.set(pool);
  }

  //
  // Configuration properties
  //

  /**
   * Sets the maximum number of threads.
   */
  public void setThreadMax(int max)
  {
    _launcher.setThreadMax(max);
  }

  /**
   * Gets the maximum number of threads.
   */
  public int getThreadMax()
  {
    return _launcher.getThreadMax();
  }

  /**
   * Sets the minimum number of idle threads.
   */
  public void setIdleMin(int min)
  {
    if (min < 1)
      throw new IllegalArgumentException(L.l("idle-min must be greater than zero."));
    
    if (_idleMax <= min) {
      throw new IllegalArgumentException(L.l("idle-min '{0}' must be less than idle-max '{1}'.",
                                             min, _idleMax));
    }
    
    _idleMin = min;
    
    _launcher.setIdleMin(_idleMin + _priorityIdleMin);
  }

  /**
   * Gets the minimum number of idle threads.
   */
  public int getIdleMin()
  {
    return _idleMin;
  }
  
  /**
   * Returns the thread idle max.
   */
  public int getIdleMax()
  {
    return _idleMax;
  }
  
  /**
   * Returns the thread idle max.
   */
  public void setIdleMax(int idleMax)
  {
    if (idleMax <= _idleMin) {
      throw new IllegalArgumentException(L.l("idle-max '{0}' must be greater than idle-min '{1}'.",
                                             idleMax, _idleMin));
    }
    
    _launcher.setIdleMax(_idleMax + _priorityIdleMin);
  }

  /**
   * Sets the minimum number of free threads reserved for priority tasks.
   */
  public void setPriorityIdleMin(int min)
  {
    if (min < 0) {
      throw new IllegalArgumentException(L.l("priority-idle-min '{0}' must be greater than zero.",
                                             min));
    }
    
    _priorityIdleMin = min;
    
    _launcher.setIdleMax(_idleMax + _priorityIdleMin);
    _launcher.setIdleMin(_idleMin + _priorityIdleMin);
  }

  public int getPriorityIdleMin()
  {
    return _priorityIdleMin;
  }
    
  /**
   * Sets the idle timeout
   */
  public void setIdleTimeout(long timeout)
  {
    _launcher.setIdleTimeout(timeout);
  }
  
  /**
   * Returns the idle timeout.
   */
  public long getIdleTimeout()
  {
    return _launcher.getIdleTimeout();
  }

  //
  // launcher throttle configuration
  //
  
  
  /**
   * Sets the throttle period.
   */
  public void setThrottlePeriod(long period)
  {
    _launcher.setThrottlePeriod(period);
  }
  
  /**
   * Sets the throttle limit.
   */
  public void setThrottleLimit(int limit)
  {
    _launcher.setThrottleLimit(limit);
  }
  
  /**
   * Sets the throttle sleep time.
   */
  public void setThrottleSleepTime(long period)
  {
    _launcher.setThrottleSleepTime(period);
  }


  //
  // statistics
  //

  /**
   * Returns the total thread count.
   */
  public int getThreadCount()
  {
    return _launcher.getThreadCount();
  }

  /**
   * Returns the active thread count.
   */
  public int getThreadActiveCount()
  {
    return (getThreadCount()
            - getThreadIdleCount());

    /*
    return (getThreadCount()
            - getThreadIdleCount()
            - getPriorityIdleCount());
            */
  }

  /**
   * Returns the starting thread count.
   */
  public int getThreadStartingCount()
  {
    return _launcher.getStartingCount();
  }

  /**
   * Returns the idle thread count.
   */
  public int getThreadIdleCount()
  {
    return _launcher.getIdleCount();
  }

  /**
   * Returns the priority idle thread count.
   */
  /*
  public int getPriorityIdleCount()
  {
    return _launcher.getPriorityIdleCount();
  }
  */

  /**
   * Returns the waiting thread count.
   */
  public int getThreadWaitCount()
  {
    return _waitCount;
  }

  /**
   * Returns the free thread count.
   */
  public int getFreeThreadCount()
  {
    return getThreadMax() - getThreadCount() - _launcher.getStartingCount();
  }

  /**
   * Returns the total created thread count.
   */
  public long getThreadCreateCountTotal()
  {
    return _launcher.getCreateCountTotal();
  }

  /**
   * Returns the total created overflow thread count.
   */
  public long getThreadOverflowCountTotal()
  {
    return _overflowCount.get();
  }

  /**
   * Returns priority queue size
   */
  public int getThreadPriorityQueueSize()
  {
    return _priorityQueue.getSize();
  }

  /**
   * Returns task queue size
   */
  public int getThreadTaskQueueSize()
  {
    return _taskQueue.getSize();
  }

  //
  // initialization
  //

  private void init()
  {
    update();
  }

  private void update()
  {
    _launcher.update();
  }
  
  public void start()
  {
    _launcher.start();
  }

  //
  // Scheduling methods
  //

  @Override
  public void execute(Runnable task)
  {
    schedule(task);
  }

  /**
   * Schedules a new task.
   */
  public boolean schedule(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;
    boolean isWake = true;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
  }

  /**
   * Schedules a new task.
   */
  public boolean schedule(Runnable task, ClassLoader loader)
  {
    boolean isPriority = false;
    boolean isQueue = true;
    boolean isWake = true;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
  }

  /**
   * Adds a new task.
   */
  public boolean schedule(Runnable task, long timeout)
  {
    long expire;

    if (timeout < 0 || MAX_EXPIRE < timeout)
      expire = MAX_EXPIRE;
    else
      expire = CurrentTime.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;
    boolean isWake = true;

    return scheduleImpl(task, loader, expire, isPriority, isQueue, isWake);
  }

  /**
   * Adds a new task.
   */
  public void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = CurrentTime.getCurrentTimeActual() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;
    boolean isWake = true;

    if (! scheduleImpl(task, loader, expire, isPriority, isQueue, isWake)) {
      String msg = (this + " unable to schedule priority thread " + task
                    + " pri-min=" + getPriorityIdleMin()
                    + " thread=" + getThreadCount()
                    + " idle=" + getThreadIdleCount()
                    // + " pri-idle=" + getPriorityIdleCount()
                    + " starting=" + getThreadStartingCount()
                    + " max=" + getThreadMax());
      
      log.warning(msg);

      OverflowThread item = new OverflowThread(task);
      item.start();
      
      HealthSystemFacade.fireEvent(THREAD_FULL_EVENT, msg);
    }
  }

  /**
   * Adds a new task.
   */
  public boolean start(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = false;
    boolean isWake = true;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
  }

  /**
   * Adds a new task.
   */
  public boolean start(Runnable task, long timeout)
  {
    long expire;

    if (timeout < 0 || timeout > MAX_EXPIRE)
      expire = MAX_EXPIRE;
    else
      expire = CurrentTime.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = false;
    boolean isWake = true;

    return scheduleImpl(task, loader, expire, isPriority, isQueue, isWake);
  }

  /**
   * Adds a new task.
   */
  public void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = CurrentTime.getCurrentTimeActual() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;
    boolean isWake = true;

    if (! scheduleImpl(task, loader, expire, isPriority, isQueue, isWake)) {
      String msg = (this + " unable to start priority thread " + task
                    + " pri-min=" + getPriorityIdleMin()
                    + " thread=" + getThreadCount()
                    + " idle=" + getThreadIdleCount()
                    // + " pri-idle=" + getPriorityIdleCount()
                    + " starting=" + getThreadStartingCount()
                    + " max=" + getThreadMax());
      
      log.warning(msg);

      HealthSystemFacade.fireEvent(THREAD_FULL_EVENT, msg);
      
      OverflowThread item = new OverflowThread(task);
      item.start();
    }
  }

  /**
   * Adds a new task.
   */
  public boolean startPriority(Runnable task, long timeout)
  {
    long expire;

    if (timeout < 0 || timeout > MAX_EXPIRE)
      expire = MAX_EXPIRE;
    else
      expire = CurrentTime.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = true;
    boolean isQueue = false;
    boolean isWake = true;

    return scheduleImpl(task, loader, expire, isPriority, isQueue, isWake);
  }

  /**
   * Submit a task, but do not wake the scheduler
   */
  public boolean submitNoWake(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;
    boolean isWake = false;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
  }

  /**
   * Submit a task, but do not wake the scheduler
   */
  public boolean submitNoWake(Runnable task, ClassLoader loader)
  {
    boolean isPriority = false;
    boolean isQueue = true;
    boolean isWake = false;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue, isWake);
  }
  
  /**
   * main scheduling implementation class.
   */
  protected boolean scheduleImpl(Runnable task,
                               ClassLoader loader,
                               long expireTime,
                               boolean isPriority,
                               boolean isQueueIfFull,
                               boolean isWakeScheduler)
  {
    if (isPriority) {
      if (! _priorityQueue.offer(task, loader)) {
        System.out.println("PRIORITY_FULL");
        return false;
      }
    }
    else {
      if (! _taskQueue.offer(task, loader)) {
        System.out.println("TASK_FULL");
        return false;
      }
    }
    
    if (isWakeScheduler) {
      wakeScheduler();
    }

    return true;
  }
  
  //
  // task methods
  //

  @Friend(ResinThread2.class)
  boolean execute(ResinThread2 thread)
  {
    if (_priorityQueue.takeAndSchedule(thread)) {
      return true;
    }
    else if (getPriorityIdleMin() < _launcher.getIdleCount() 
             && _taskQueue.takeAndSchedule(thread)) {
      return true;
    }
    
    if (! _idleThreadRing.offer(thread)) {
      System.out.println("NOFREE: " + thread);
    }
    
    if (! _priorityQueue.isEmpty() || ! _taskQueue.isEmpty()) {
      wakeScheduler();
    }
    
    return false;
  }
  
  public void wakeScheduler()
  {
    _scheduleWorker.wake();
  }
  
  public final void scheduleUnpark(Thread thread)
  {
    LockSupport.unpark(thread);
    
    // _unparkQueue.put(thread);
    // _scheduleWorker.wake();
  }
  
  //
  // lifecycle methods
  //

  boolean isActive()
  {
    return _lifecycle.isActive();
    
  }
  /**
   * Resets the thread pool, letting old threads drain.
   */
  public void reset()
  {
    _resetCount.incrementAndGet();
  }

  /**
   * Resets the thread pool, letting old threads drain.
   */
  public void closeEnvironment(ClassLoader env)
  {
    // XXX: incorrect
    reset();
  }

  /**
   * interrupts all the idle threads.
   */
  public void clearIdleThreads()
  {
    ResinThread2 thread;
    
    while ((thread = _idleThreadRing.poll()) != null) {
      thread.close();
    }
  }

  public void close()
  {
    if (this == _globalThreadPool.get())
      throw new IllegalStateException(L.l("Cannot close global thread pool"));
    
    _lifecycle.toDestroy();
    _launcher.close();
    _scheduleWorker.close();
    
    clearIdleThreads();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }

  final class OverflowThread extends Thread {
    private Runnable _task;
    private ClassLoader _loader;

    OverflowThread(Runnable task)
    {
      super("resin-overflow-" + task.getClass().getSimpleName());
      setDaemon(true);

      _task = task;
      _loader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * The main thread execution method.
     */
    @Override
    public void run()
    {
      Thread thread = Thread.currentThread();
      thread.setContextClassLoader(_loader);

      try {
        _overflowCount.incrementAndGet();

        _task.run();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
  
  class ThreadScheduleWorker extends AbstractTaskWorker2 {
    ThreadScheduleWorker()
    {
      super(ThreadScheduleWorker.class.getClassLoader());
    }
    
    @Override
    public boolean isPermanent()
    {
      return true;
    }

    @Override
    public long runTask()
    {
      int loopCount = 2;
      int i;
      
      for (i = 0; i <= loopCount; i++) {
        while (invoke()) {
          i = -1;
        }
      }
      
      return 1000;
    }
    
    private boolean invoke()
    {
      boolean isInvoke = false;
      
      if (invokePriorityQueue()) {
        isInvoke = true;
      }
      else if (invokeIdleQueue()) {
        isInvoke = true;
      }
      
      if (invokeUnpark()) {
        isInvoke = true;
      }
      
      return isInvoke;
    }
    
    private boolean invokeUnpark()
    {
      Thread thread = _unparkQueue.poll();
      
      if (thread != null) {
        LockSupport.unpark(thread);
        return true;
      }
      
      return false;
    }
    
    private boolean invokePriorityQueue()
    {
      if (_priorityQueue.isEmpty()) {
        return false;
      }
       
      ResinThread2 thread = _idleThreadRing.poll();
        
      if (thread == null) {
        _launcher.wake();
        return true;
      }
         
      if (_priorityQueue.takeAndSchedule(thread)) {
        thread.unpark();
        return true;
      }

      _idleThreadRing.offer(thread);
       
      return false;
    }
    
    private boolean invokeIdleQueue()
    {
      if (_taskQueue.isEmpty()) {
        return false;
      }

      if (_launcher.getIdleCount() <= getPriorityIdleMin()) {
        _launcher.wake();
        return true;
      }
       
      ResinThread2 thread = _idleThreadRing.poll();
        
      if (thread == null) {
        _launcher.wake();
        return true;
      }
         
      if (_taskQueue.takeAndSchedule(thread)) {
        thread.unpark();
        return true;
      }

      _idleThreadRing.offer(thread);
       
      return false;
    }

    @Override
    protected void startWorkerThread()
    {
      boolean isValid = false;
      
      try {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("resin-thread-scheduler");
        thread.start();
        
        isValid = true;
      } finally {
        if (! isValid) {
          ShutdownSystem.shutdownActive(ExitCode.THREAD,
                                         "Cannot create ThreadPool thread.");
        }
      }
    }
    
    @Override
    protected void unpark(Thread thread)
    {
      LockSupport.unpark(thread);
    }
  }
}
