/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.util;

import com.caucho.config.ConfigException;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public final class ThreadPool {
  private static final L10N L = new L10N(ThreadPool.class);
  private static final Logger log
    = Logger.getLogger(ThreadPool.class.getName());
  
  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;

  private static final int DEFAULT_THREAD_MAX = 8192;
  private static final int DEFAULT_THREAD_IDLE_MIN = 10;
  private static final int DEFAULT_THREAD_IDLE_GAP = 5;

  private static final long OVERFLOW_TIMEOUT = 30000L;

  private static final long PRIORITY_TIMEOUT = 10L;

  private static ThreadPool _globalThreadPool;

  private final AtomicInteger _gId = new AtomicInteger();
    
  private int _threadMax = DEFAULT_THREAD_MAX;
  
  private int _threadIdleMax = -1;
  private boolean _isThreadIdleMaxSet;
  
  private int _threadIdleMin = -1;
  private boolean _isThreadIdleMinSet;
  
  // the minimum number of free threads reserved for priority tasks
  private int _threadPriority = -1;
  private boolean _isThreadPrioritySet = false;

  private int _executorTaskMax = -1;

  private final AtomicLong _resetCount = new AtomicLong();

  private final ArrayList<TaskItem> _taskQueue = new ArrayList<TaskItem>();
  private final ArrayList<TaskItem> _priorityQueue = new ArrayList<TaskItem>();

  private final Object _idleLock = new Object();

  private final ThreadLauncher _launcher = new ThreadLauncher();

  // the idle stack
  private PoolThread _idleHead;
  // number of threads in the idle stack
  private int _idleCount;
  // number of threads in the wait queue
  private int _waitCount;

  private final AtomicInteger _activeCount = new AtomicInteger();
  // number of threads which are in the process of starting
  private final AtomicInteger _startingCount = new AtomicInteger();

  // throttling time before a thread can expire itself to reduce churn
  private long _threadIdleOverflowExpire;

  private final Object _executorLock = new Object();
  // number of executor tasks running
  private int _executorTaskCount;
  // queue for waiting executor tasks
  private ExecutorQueueItem _executorQueueHead;
  private ExecutorQueueItem _executorQueueTail;
  
  private final AtomicLong _createCount = new AtomicLong();
  private final AtomicLong _overflowCount = new AtomicLong();

  private boolean _isInit;

  private ThreadPool()
  {
     // initialize default values
    init();
  }

  public static ThreadPool getCurrent()
  {
    return getThreadPool();
  }
  
  public static ThreadPool getThreadPool()
  {
    synchronized (ThreadPool.class) {
      if (_globalThreadPool == null)
	_globalThreadPool = new ThreadPool();
      
      return _globalThreadPool;
    }
  }

  //
  // Configuration properties
  //

  /**
   * Sets the maximum number of threads.
   */
  public void setThreadMax(int max)
  {
    synchronized (this) {
      int threadIdleMax = _threadIdleMax;
      
      if (max < threadIdleMax && _isThreadIdleMaxSet)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})", threadIdleMax, max));
	
      _threadMax = max;
    }

    init();
  }

  /**
   * Gets the maximum number of threads.
   */
  public int getThreadMax()
  {
    return _threadMax;
  }

  /**
   * Sets the minimum number of idle threads.
   */
  public void setThreadIdleMin(int min)
  {
    synchronized (this) {
      int threadIdleMax = _threadIdleMax;
      
      if (threadIdleMax < min && _isThreadIdleMaxSet)
	throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})", min, threadIdleMax));
    
      _threadIdleMin = min;
      _isThreadIdleMinSet = true;
    }
    
    init();
  }

  /**
   * Gets the minimum number of idle threads.
   */
  public int getThreadIdleMin()
  {
    return _threadIdleMin;
  }

  /**
   * Sets the maximum number of idle threads.
   */
  public void setThreadIdleMax(int max)
  {
    synchronized (this) {
      int threadIdleMin = _threadIdleMin;
      if (max < threadIdleMin && _isThreadIdleMinSet)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be greater than <thread-idle-min> ({1})",
				      max, threadIdleMin));

      int threadMax = _threadMax;
      if (threadMax < max)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
				      max, threadMax));
    
      _threadIdleMax = max;
      _isThreadIdleMaxSet = true;
    }
    
    init();
  }

  /**
   * Sets the minimum number of free threads reserved for priority tasks.
   */
  public void setThreadPriority(int priority)
  {
    _threadPriority = priority;
    _isThreadPrioritySet = true;
    
    init();
  }

  /**
   * Gets the maximum number of idle threads.
   */
  public int getThreadIdleMax()
  {
    return _threadIdleMax;
  }

  /**
   * Sets the maximum number of executor threads.
   */
  public void setExecutorTaskMax(int max)
  {
    synchronized (this) {
      if (_threadMax < max)
	throw new ConfigException(L.l("<thread-executor-max> ({0}) must be less than <thread-max> ({1})",
				      max, _threadMax));
    
      if (max == 0)
	throw new ConfigException(L.l("<thread-executor-max> must not be zero."));
    
      _executorTaskMax = max;
    }
  }

  /**
   * Gets the maximum number of executor threads.
   */
  public int getExecutorTaskMax()
  {
    return _executorTaskMax;
  }

  public int getThreadPriority()
  {
    return _threadPriority;
  }

  private int getDefaultPriority()
  {
    return _threadPriority;
  }

  //
  // statistics
  //

  /**
   * Returns the total thread count.
   */
  public int getThreadCount()
  {
    return _activeCount.get();
  }

  /**
   * Returns the active thread count.
   */
  public int getThreadActiveCount()
  {
    return _activeCount.get() - _idleCount;
  }

  /**
   * Returns the starting thread count.
   */
  public int getThreadStartingCount()
  {
    return _startingCount.get();
  }

  /**
   * Returns the idle thread count.
   */
  public int getThreadIdleCount()
  {
    return _idleCount;
  }

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
    return _threadMax - _activeCount.get() - _startingCount.get();
  }

  /**
   * Returns the total created thread count.
   */
  public long getThreadCreateCountTotal()
  {
    return _createCount.get();
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
    return _priorityQueue.size();
  }

  /**
   * Returns task queue size
   */
  public int getThreadTaskQueueSize()
  {
    return _taskQueue.size();
  }

  //
  // Resin methods
  //

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
   * Schedules a new task.
   */
  public boolean schedule(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;
    
    return schedule(task, loader, MAX_EXPIRE, isPriority, isQueue);
  }

  /**
   * Adds a new task.
   */
  public boolean schedule(Runnable task, long timeout)
  {
    long expire;
    
    if (timeout < 0 || timeout > MAX_EXPIRE)
      expire = MAX_EXPIRE;
    else
      expire = Alarm.getCurrentTime() + timeout;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;
    
    return schedule(task, loader, expire, isPriority, isQueue);
  }

  /**
   * Adds a new task.
   */
  public void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = Alarm.getCurrentTime() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;
    
    if (! schedule(task, loader, expire, isPriority, isQueue)) {
      log.warning(this + " unable to schedule priority thread " + task
		  + " pri=" + _threadPriority
		  + " active=" + _activeCount.get()
		  + " idle=" + (_idleCount + _startingCount.get())
		  + " max=" + _threadMax);

      OverflowThread item = new OverflowThread(task);
      item.start();
    }
  }

  /**
   * Schedules an executor task.
   */
  public boolean scheduleExecutorTask(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    synchronized (_executorLock) {
      _executorTaskCount++;

      if (_executorTaskCount <= _executorTaskMax || _executorTaskMax < 0) {
	boolean isPriority = false;
	boolean isQueue = true;
    
	return schedule(task, loader, MAX_EXPIRE, isPriority, isQueue);
      }
      else {
	ExecutorQueueItem item = new ExecutorQueueItem(task, loader);

	if (_executorQueueTail != null)
	  _executorQueueTail._next = item;
	else
	  _executorQueueHead = item;

	_executorQueueTail = item;

	return false;
      }
    }
  }

  /**
   * Called when an executor task completes
   */
  public void completeExecutorTask()
  {
    synchronized (_executorLock) {
      _executorTaskCount--;

      assert(_executorTaskCount >= 0);

      if (_executorQueueHead != null) {
	ExecutorQueueItem item = _executorQueueHead;

	_executorQueueHead = item._next;

	if (_executorQueueHead == null)
	  _executorQueueTail = null;

	Runnable task = item.getRunnable();
	ClassLoader loader = item.getLoader();

	boolean isPriority = false;
	boolean isQueue = true;
	
	schedule(task, loader, MAX_EXPIRE, isPriority, isQueue);
      }
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
    
    return schedule(task, loader, MAX_EXPIRE, isPriority, isQueue);
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
      expire = Alarm.getCurrentTime() + timeout;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = false;
    
    return schedule(task, loader, expire, isPriority, isQueue);
  }

  /**
   * Adds a new task.
   */
  public void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    long expire = Alarm.getCurrentTime() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;
    
    if (! schedule(task, loader, expire, isPriority, isQueue)) {
      log.warning(this + " unable to start priority thread " + task
		  + " pri=" + _threadPriority
		  + " active=" + _activeCount.get()
		  + " idle=" + _idleCount
		  + " starting=" + _startingCount.get()
		  + " max=" + _threadMax);

      ThreadDump.dumpThreads();
      
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
      expire = Alarm.getCurrentTime() + timeout;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = true;
    boolean isQueue = false;
    
    return schedule(task, loader, expire, isPriority, isQueue);
  }

  /**
   * interrupts all the idle threads.
   */
  public void interrupt()
  {
    synchronized (_idleLock) {
      PoolThread item = _idleHead;
      _idleHead = null;

      while (item != null) {
	PoolThread next = item._next;

	item._next = null;
        if (item._isIdle) {
          item._isIdle = false;
          _idleCount--;
        }

        try {
          item.unpark();
        } catch (Exception e) {
        }

	item = next;
      }
    }
  }

  /**
   * Adds a new task.
   */
  private boolean schedule(Runnable task,
			   ClassLoader loader,
			   long expireTime,
			   boolean isPriority,
			   boolean isQueueIfFull)
  {
    PoolThread poolThread = null;
    boolean isWakeLauncher = false;

    int freeThreadsRequired = isPriority ? 0 : _threadPriority;
    
    TaskItem item = new TaskItem(task, loader);

    do {
      try {
	synchronized (_idleLock) {
	  int idleCount = _idleCount;
	  int threadCount = _activeCount.get() + _startingCount.get();
	  int freeCount = idleCount + _threadMax - threadCount;
	  boolean startNew = false;

	  boolean isFreeThreadAvailable = freeThreadsRequired <= freeCount;

	  if (isFreeThreadAvailable) {
            // grab next idle thread, skipping any closed threads
            while ((poolThread = _idleHead) != null
                   && ! poolThread.scheduleTask(item)) {
              poolThread = null;
            }
	  }
          
          if (idleCount < _threadIdleMin)
            isWakeLauncher = true;
          
	  if (poolThread == null) {
	    // queue if schedule() or if there's space to run but no
	    // active thread yet
	    if (isQueueIfFull || isFreeThreadAvailable) {
	      isWakeLauncher = true;
	      isQueueIfFull = true;
	    
	      if (isPriority) {
		_priorityQueue.add(item);
	      }
	      else {
		_taskQueue.add(item);
	      }
	    }
	    else if (Alarm.getCurrentTime() < expireTime) {
	      _launcher.wake();

              // clear interrupted flag
              Thread.interrupted();

              _waitCount++;
              try {
                if (! Alarm.isTest()) {
                  long delta = expireTime - Alarm.getCurrentTime();

                  if (delta > 0)
                    _idleLock.wait(delta);
                }
                else
                  _idleLock.wait(1000);
              } finally {
                _waitCount--;
              }
	    }
	  }
	}
      
	if (isWakeLauncher)
	  _launcher.wake();
      } catch (OutOfMemoryError e) {
	try {
	  System.err.println("ThreadPool.schedule exiting due to OutOfMemoryError");
	} finally {
	  System.exit(11);
	}
      } catch (Throwable e) {
	e.printStackTrace();
      }
    } while (poolThread == null
	     && ! isQueueIfFull
	     && Alarm.getCurrentTime() <= expireTime);

    if (poolThread != null) {
      poolThread.unpark();

      return true;
    }
    else
      return isQueueIfFull;
  }

  private void init()
  {
    if (_threadMax < 0)
      _threadMax = DEFAULT_THREAD_MAX;

    if (! _isThreadIdleMaxSet) {
      _threadIdleMax = _threadMax / 16;

      if (_threadIdleMax > 64)
	_threadIdleMax = 64;
      if (_threadIdleMax < 16)
	_threadIdleMax = 16;
      
      if (_isThreadIdleMinSet && _threadIdleMax <= _threadIdleMin)
	_threadIdleMax = _threadIdleMin + 8;
      
      if (_threadMax < _threadIdleMax)
	_threadIdleMax = _threadMax;
    }

    if (! _isThreadIdleMinSet) {
      _threadIdleMin = _threadIdleMax / 2;

      if (_threadIdleMin <= 0)
	_threadIdleMin = 1;
    }
    
    if (! _isThreadPrioritySet) {
      _threadPriority = _threadIdleMin / 2;

      if (_threadPriority <= 0)
	_threadPriority = _threadIdleMin;
    }
  }

  /**
   * Checks if the launcher should start another thread.
   */
  private boolean doStart()
  {
    synchronized (_idleLock) {
      int idleCount = _idleCount;
      int threadCount = _activeCount.get() + _startingCount.get();

      if (_threadMax < threadCount)
        return false;
      else if (_threadIdleMin < idleCount + _startingCount.get())
        return false;
      else {
        _startingCount.incrementAndGet();

        return true;
      }
    }
  }


  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  final class PoolThread extends Thread {
    final int _id;
    final String _name;

    long _threadResetCount;

    private PoolThread _next;
    private boolean _isIdle;

    private final AtomicReference<TaskItem> _itemRef
      = new AtomicReference<TaskItem>();

    PoolThread()
    {
      _id = _gId.incrementAndGet();
      _name = "resin-" + _id;

      setName(_name);
      setDaemon(true);
    }

    /**
     * Returns the name.
     */
    public String getDebugName()
    {
      return _name;
    }

    /**
     * Returns the thread id.
     */
    public long getThreadId()
    {
      return getId();
    }

    /**
     * Sets the thread's task.  Must be called inside _idleLock
     */
    final boolean scheduleTask(TaskItem item)
    {
      if (this != _idleHead) {
        log.severe("ThreadPool wake() head mismatch.  Forcing quit.");
        System.exit(10);
      }

      _idleHead = _next;
      _next = null;

      if (! _isIdle) {
        return false;
      }

      _idleCount--;
      _isIdle = false;

      _itemRef.set(item);

      return true;
    }

    /**
     * Wake the thread.  Called outside of _idleLock
     */
    final void unpark()
    {
      LockSupport.unpark(this);
    }

    /**
     * The main thread execution method.
     */
    public void run()
    {
      int startCount = _startingCount.decrementAndGet();

      if (startCount < 0) {
        _startingCount.set(0);
        new IllegalStateException().printStackTrace();
      }
      
      _activeCount.incrementAndGet();
      
      try {
        _launcher.wake();
        _createCount.incrementAndGet();
      
	runTasks();
      } finally {
        _activeCount.decrementAndGet();

	_launcher.wake();
      }
    }

    private void runTasks()
    {
      _threadResetCount = _resetCount.get();
    
      Thread thread = this;
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

      while (true) {
        thread.setName(_name);
	  
        Runnable task = null;
        ClassLoader classLoader = null;

        TaskItem item = nextTask();

        if (item == null)
          return;
          
        task = item.getRunnable();
        classLoader = item.getLoader();

        try {
          // if the task is available, run it in the proper context
          thread.setContextClassLoader(classLoader);
	    
          thread.setName(_name + "-" + task.getClass().getSimpleName());
	      
          task.run();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
      }
    }

    /**
     * Returns the next available task, returning null if the thread
     * should exit.
     */
    private TaskItem nextTask()
    {
      synchronized (_idleLock) {
        // process any priority queue item
        if (_priorityQueue.size() > 0) {
          return _priorityQueue.remove(0);
        }

        // if we have spare threads, process any task queue item
        if (_taskQueue.size() > 0 && _threadPriority <= _idleCount) {
          return _taskQueue.remove(0);
        }

        long now = Alarm.getCurrentTime();
        
        // if idle queue overflows, return and exit
        if (_threadIdleMax < _idleCount
            && _threadIdleOverflowExpire < now) {
          _threadIdleOverflowExpire = now + OVERFLOW_TIMEOUT;
          return null;
        }

        _next = _idleHead;
        _idleHead = this;
                
        _isIdle = true;
        _idleCount++;

        _idleLock.notifyAll();
      }

      try {
        while (true) {
          TaskItem item = _itemRef.getAndSet(null);
          if (item != null || ! _isIdle)
            return item;
          
          Thread.interrupted();
          LockSupport.park();
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);

        return null;
      } finally {
        synchronized (_idleLock) {
          if (_isIdle) {
            _isIdle = false;
            _idleCount--;
          }
        }
      }
    }
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

  final class ThreadLauncher extends Thread {
    private ThreadLauncher()
    {
      super("resin-thread-launcher");
      setDaemon(true);

      start();
    }

    void wake()
    {
      LockSupport.unpark(this);
    }

    /**
     * Starts a new connection
     */
    private boolean startConnection(boolean isWait)
      throws InterruptedException
    {
      if (doStart()) {
	try {
	  long now = Alarm.getCurrentTime();
	  _threadIdleOverflowExpire = now + OVERFLOW_TIMEOUT;
	  
	  PoolThread poolThread = new PoolThread();
	  poolThread.start();

          return true;
	} catch (Throwable e) {
          e.printStackTrace();
          System.err.println("Resin exiting because of failed thread");

          System.exit(1);
	}
      }
      else {
	Thread.interrupted();

	if (isWait)
	  LockSupport.park();
      }
	
      return false;
    }
    
    public void run()
    {
      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      
      Thread.currentThread().setContextClassLoader(systemLoader);

      try {
	for (int i = 0; i < _threadIdleMin; i++)
	  startConnection(false);
      } catch (Throwable e) {
	e.printStackTrace();
      }
      
      while (true) {
	try {
	  startConnection(true);
	} catch (Throwable e) {
	  e.printStackTrace();
	  System.exit(10);
	}
      }
    }
  }

  static class TaskItem {
    private Runnable _runnable;
    private ClassLoader _loader;

    TaskItem(Runnable runnable, ClassLoader loader)
    {
      _runnable = runnable;
      _loader = loader;
    }

    public final Runnable getRunnable()
    {
      return _runnable;
    }

    public final ClassLoader getLoader()
    {
      return _loader;
    }
  }

  static class ExecutorQueueItem {
    Runnable _runnable;
    ClassLoader _loader;

    ExecutorQueueItem _next;

    ExecutorQueueItem(Runnable runnable, ClassLoader loader)
    {
      _runnable = runnable;
      _loader = loader;
    }

    Runnable getRunnable()
    {
      return _runnable;
    }

    ClassLoader getLoader()
    {
      return _loader;
    }
  }
}
