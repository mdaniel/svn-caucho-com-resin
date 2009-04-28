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

  private static final long OVERFLOW_TIMEOUT = 5000L;

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

  private final ArrayList<PoolThread> _threads
    = new ArrayList<PoolThread>();

  private final ArrayList<TaskItem> _taskQueue = new ArrayList<TaskItem>();
  private final ArrayList<TaskItem> _priorityQueue = new ArrayList<TaskItem>();

  private final Object _idleLock = new Object();

  private final ThreadLauncher _launcher = new ThreadLauncher();

  // the idle stack
  private PoolThread _idleHead;
  // number of threads in the idle stack
  private int _idleCount;

  private int _activeCount;
  // number of threads which are in the process of starting
  private int _startCount;

  // count of start requests waiting for a thread
  private int _scheduleWaitCount;

  // throttling time before a thread can expire itself to reduce churn
  private long _threadIdleOverflowExpire;

  private final Object _executorLock = new Object();
  // number of executor tasks running
  private int _executorTaskCount;
  // queue for waiting executor tasks
  private ExecutorQueueItem _executorQueueHead;
  private ExecutorQueueItem _executorQueueTail;

  private boolean _isInit;

  private ThreadPool()
  {
     // initialize default values
    init();
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

  /**
   * Returns the total thread count.
   */
  public int getThreadCount()
  {
    return _activeCount;
  }

  /**
   * Returns the idle thread count.
   */
  public int getThreadIdleCount()
  {
    return _idleCount;
  }

  /**
   * Returns the active thread count.
   */
  public int getThreadActiveCount()
  {
    return getThreadCount() - getThreadIdleCount();
  }

  /**
   * Returns the free thread count.
   */
  public int getFreeThreadCount()
  {
    return _threadMax - _activeCount - _startCount;
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
		  + " active=" + _activeCount
		  + " idle=" + (_idleCount + _startCount)
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
    boolean isQueue = false;
    
    if (! schedule(task, loader, expire, isPriority, isQueue)) {
      log.warning(this + " unable to start priority thread " + task
		  + " pri=" + _threadPriority
		  + " active=" + _activeCount
		  + " idle=" + (_idleCount + _startCount)
		  + " max=" + _threadMax);

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

      _idleCount = 0;
      
      while (item != null) {
	PoolThread next = item._next;

	item._next = null;
	item._isIdle = false;

	try {
	  item.interrupt();
	} catch (Throwable e) {
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
			   boolean queueIfFull)
  {
    PoolThread poolThread = null;
    boolean isWakeLauncher = false;

    int freeThreadsRequired = isPriority ? 0 : _threadPriority;

    do {
      try {
	synchronized (_idleLock) {
	  int idleCount = _idleCount;
	  int threadCount = _activeCount + _startCount;
	  int freeCount = idleCount + _threadMax - threadCount;
	  boolean startNew = false;

	  poolThread = _idleHead;

	  boolean isFreeThreadAvailable = freeThreadsRequired <= freeCount;

	  if (poolThread != null && isFreeThreadAvailable) {
	    if (_idleCount <= 0) {
	      String msg = this + " critical internal error in ThreadPool, requiring Resin restart";
	      System.err.println(msg);
	      System.exit(10);
	    }
	    
	    poolThread = _idleHead;
	    _idleHead = poolThread._next;
	    poolThread._next = null;
	    poolThread._isIdle = false;
	    
	    poolThread.setTask(task);
	    poolThread.setLoader(loader);

	    _idleCount--;
	    
	    if (idleCount < _threadIdleMin)
	      isWakeLauncher = true;
	  }
	  else {
	    poolThread = null;

	    // queue if schedule() or if there's space to run but no
	    // active thread yet
	    if (queueIfFull || isFreeThreadAvailable) {
	      isWakeLauncher = true;
	    
	      TaskItem item = new TaskItem(task, loader);
	      
	      if (isPriority) {
		_priorityQueue.add(item);
	      }
	      else {
		_taskQueue.add(item);
	      }
	    }
	    else if (Alarm.getCurrentTime() < expireTime) {
	      _launcher.wake();
	      
	      _scheduleWaitCount++;
		
	      try {
		// clear interrupted flag
		Thread.interrupted();

		if (! Alarm.isTest()) {
		  long delta = expireTime - Alarm.getCurrentTime();
		
		  _idleLock.wait(delta);
		}
		else
		  _idleLock.wait(1000);
	      } finally {
		_scheduleWaitCount--;
	      }
	    }
	  }
	}
      
	if (isWakeLauncher)
	  _launcher.wake();
      } catch (OutOfMemoryError e) {
	try {
	  System.err.println("Exiting due to OutOfMemoryError");
	} finally {
	  System.exit(11);
	}
      } catch (Throwable e) {
	e.printStackTrace();
      }
    } while (poolThread == null
	     && ! queueIfFull
	     && Alarm.getCurrentTime() <= expireTime);

    if (poolThread != null) {
      LockSupport.unpark(poolThread);

      return true;
    }
    else
      return queueIfFull;
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

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  final class PoolThread extends Thread {
    final int _id;
    final String _name;

    Thread _thread;
    Thread _queueThread;

    PoolThread _next;
    boolean _isIdle;

    long _threadResetCount;
  
    Runnable _task;
    ClassLoader _classLoader;

    PoolThread()
    {
      _id = _gId.incrementAndGet();
      _name = "resin-" + _id;

      setName(_name);
      setDaemon(true);
    }

    /**
     * Returns the id.
     */
    /*
    int getId()
    {
      return _id;
    }
    */

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
     * Returns the thread.
     */
    Thread getThread()
    {
      return _thread;
    }

    /**
     * Sets the thread's task
     */
    final void setTask(Runnable task)
    {
      _task = task;
    }

    /**
     * Sets the thread's loader
     */
    final void setLoader(ClassLoader loader)
    {
      _classLoader = loader;
    }

    /**
     * The main thread execution method.
     */
    public void run()
    {
      synchronized (_idleLock) {
	_startCount--;
	_activeCount++;
	_threads.add(this);

	if (_startCount < 0) {
	  _startCount = 0;
	  new IllegalStateException().printStackTrace();
	}
      }

      _launcher.wake();
      
      try {
	runTasks();
      } finally {
	synchronized (_idleLock) {
	  _activeCount--;

	  _threads.remove(this);
	}

	_launcher.wake();
      }
    }

    private void runTasks()
    {
      _threadResetCount = _resetCount.get();
    
      Thread thread = this;
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

      while (true) {
	try {
	  thread.setName(_name);
	  
	  Runnable task = null;
	  ClassLoader classLoader = null;
	  
	  // put the thread into the idle ring
	  synchronized (_idleLock) {
	    if (_isIdle) {
	    }
	    else if (_task != null) {
	      task = _task;
	      _task = null;
	      classLoader = _classLoader;
	      _classLoader = null;
	    }
	    else {
	      TaskItem item = null;
	      
	      if (_priorityQueue.size() > 0) {
		item = _priorityQueue.remove(0);
	      }

	      if (item == null && _threadPriority <= _idleCount) {
		if (_taskQueue.size() > 0) {
		  item = _taskQueue.remove(0);
		}
	      }

	      if (item != null) {
		task = item.getRunnable();
		classLoader = item.getLoader();
	      }
	      else {
		long now = Alarm.getCurrentTime();
		if (_threadIdleMax < _idleCount
		    && _threadIdleOverflowExpire < now) {
		  _threadIdleOverflowExpire = now + OVERFLOW_TIMEOUT;
		  return;
		}
	      
		_next = _idleHead;
		_isIdle = true;

		_idleHead = this;
		_idleCount++;

		if (_scheduleWaitCount > 0)
		  _idleLock.notifyAll();
	      }
	    }
	  }
	  
	  // clear interrupted flag
	  Thread.interrupted();
	  // wait for next request
	  if (task == null) {
	    LockSupport.park();
	  }

	  // if the task is available, run it in the proper context
	  if (task != null) {
	    thread.setContextClassLoader(classLoader);
	    
	    try {
	      thread.setName(_name + "-" + task.getClass().getSimpleName());
	      
	      task.run();
	    } catch (Throwable e) {
	      log.log(Level.WARNING, e.toString(), e);
	    } finally {
	      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
	    }
	  }
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
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
      boolean doStart = true;
      
      synchronized (_idleLock) {
	int idleCount = _idleCount;
	int threadCount = _activeCount + _startCount;

	if (_threadMax < threadCount)
	  doStart = false;
	else if (_threadIdleMin < idleCount + _startCount)
	  doStart = false;

	if (doStart) {
	  _startCount++;
	}
      }

      if (doStart) {
	try {
	  long now = Alarm.getCurrentTime();
	  _threadIdleOverflowExpire = now + OVERFLOW_TIMEOUT;
	  
	  PoolThread poolThread = new PoolThread();
	  poolThread.start();
	} catch (Throwable e) {
	  // XXX: should we exit in this case?
	  
	  synchronized (_idleLock) {
	    _startCount--;
	  }

	  e.printStackTrace();
	  if (_startCount < 0) {
	    Thread.dumpStack();
	    _startCount = 0;
	  }
	}
      }
      else {
	Thread.interrupted();

	if (isWait)
	  LockSupport.park();
	
	return false;
      }

      return true;
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
	} catch (OutOfMemoryError e) {
	  System.exit(10);
	} catch (Throwable e) {
	  e.printStackTrace();
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
