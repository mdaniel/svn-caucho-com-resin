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
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public class ThreadPool {
  private static final L10N L = new L10N(ThreadPool.class);
  private static final Logger log
    = Logger.getLogger(ThreadPool.class.getName());
  
  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;

  private static final int DEFAULT_THREAD_MAX = 8192;
  private static final int DEFAULT_THREAD_IDLE_MIN = 10;
  private static final int DEFAULT_THREAD_IDLE_GAP = 5;

  private static final long OVERFLOW_TIMEOUT = 5000L;

  private static final long PRIORITY_TIMEOUT = 1000L;

  private static ThreadPool _globalThreadPool;

  private final AtomicInteger _gId = new AtomicInteger();
    
  private int _threadMax = -1;
  
  private int _threadIdleMax = -1;
  private int _threadIdleMin = -1;

  private int _executorTaskMax = -1;
  
  private int _threadPriority = 5;
  private boolean _threadPrioritySet = false;

  private long _resetCount;

  private final ArrayList<Item> _threads
    = new ArrayList<Item>();

  private final ArrayList<Runnable> _taskQueue
    = new ArrayList<Runnable>();

  private final ArrayList<ClassLoader> _loaderQueue
    = new ArrayList<ClassLoader>();

  private final Object _idleLock;

  private final ThreadLauncher _launcher;

  // the idle stack
  private Item _idleHead;
  // number of threads in the idle stack
  private int _idleCount;

  private int _threadCount;
  // number of threads which are in the process of starting
  private int _startCount;

  // time before a thread can expire itself
  private long _threadIdleOverflowExpire;

  private final Object _executorLock = new Object();
  // number of executor tasks running
  private int _executorTaskCount;
  // queue for waiting executor tasks
  private ExecutorQueueItem _executorQueueHead;
  private ExecutorQueueItem _executorQueueTail;

  private int _scheduleWaitCount;
  private boolean _isInit;

  private ThreadPool()
  {
    _idleLock = new Object();

    _launcher = new ThreadLauncher();
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
      if (max < threadIdleMax && threadIdleMax >= 0)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})", threadIdleMax, max));
	
      _threadMax = max;
    }
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
      if (threadIdleMax < min && threadIdleMax >= 0)
	throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})", min, threadIdleMax));
    
      _threadIdleMin = min;

      calculateThreadPriority();
    }
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
      if (max < threadIdleMin && threadIdleMin >= 0)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be greater than <thread-idle-min> ({1})",
				      max, threadIdleMin));

      int threadMax = _threadMax;
      if (threadMax < max && threadMax >= 0)
	throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
				      max, threadMax));
    
      _threadIdleMax = max;
    }
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
      if (_threadMax < max && _threadMax >= 0)
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

  public void setThreadPriority(int priority)
  {
    _threadPriority = priority;
    _threadPrioritySet = true;
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
    return _threadCount;
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
    return _threadMax - _threadCount;
  }

  //
  // Resin methods
  //

  /**
   * Resets the thread pool, letting old threads drain.
   */
  public void reset()
  {
    // XXX: not reliable
    _resetCount++;
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
    
    return schedule(task, loader, getDefaultPriority(), MAX_EXPIRE, true);
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
    
    return schedule(task, loader, getDefaultPriority(), expire, true);
  }

  /**
   * Adds a new task.
   */
  public void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = Alarm.getCurrentTime() + PRIORITY_TIMEOUT;
    if (! schedule(task, loader, 0, expire, true)) {
      log.warning(this + " unable to schedule priority thread " + task
		  + " pri=" + _threadPriority
		  + " active=" + _threadCount
		  + " max=" + _threadMax);

      OverflowItem item = new OverflowItem(task);
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
      if (! _isInit)
	init();
      
      _executorTaskCount++;

      if (_executorTaskCount <= _executorTaskMax || _executorTaskMax < 0)
	return schedule(task, loader, getDefaultPriority(), MAX_EXPIRE, true);
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
	
	schedule(task, loader, getDefaultPriority(), MAX_EXPIRE, true);
      }
    }
  }

  /**
   * Adds a new task.
   */
  public boolean start(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, getDefaultPriority(), MAX_EXPIRE, false);
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
    
    return schedule(task, loader, getDefaultPriority(), expire, false);
  }

  /**
   * Adds a new task.
   */
  public void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    long expire = Alarm.getCurrentTime() + PRIORITY_TIMEOUT;
    if (! schedule(task, loader, 0, expire, false)) {
      log.warning(this + " unable to start priority thread " + task
		  + " pri=" + _threadPriority
		  + " active=" + _threadCount
		  + " max=" + _threadMax);

      OverflowItem item = new OverflowItem(task);
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
    
    return schedule(task, loader, 0, expire, true);
  }

  /**
   * interrupts all the idle threads.
   */
  public void interrupt()
  {
    synchronized (_idleLock) {
      Item item = _idleHead;
      _idleHead = null;

      _idleCount = 0;
      
      while (item != null) {
	Item next = item._next;

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
			   int freeThreads,
			   long expireTime,
			   boolean queueIfFull)
  {
    Item poolItem = null;

    do {
      try {
	synchronized (_idleLock) {
	  if (! _isInit)
	    init();
      
	  int idleCount = _idleCount;
	  int freeCount = idleCount + _threadMax - _threadCount;
	  boolean startNew = false;

	  poolItem = _idleHead;

	  if (poolItem != null && freeThreads < freeCount) {
	    if (_idleCount <= 0) {
	      String msg = this + " critical internal error in ThreadPool, requiring Resin restart";
	      System.err.println(msg);
	      System.exit(10);
	    }
	    
	    _idleCount--;
	    
	    poolItem = _idleHead;
	    _idleHead = poolItem._next;
	    poolItem._next = null;
	    poolItem._isIdle = false;

	    if (idleCount < _threadIdleMin)
	      _launcher.wake();
	  }
	  else {
	    _launcher.wake();
	    
	    if (queueIfFull) {
	      synchronized (_taskQueue) {
		_taskQueue.add(task);
		_loaderQueue.add(loader);
	      }
	    }
	    else if (Alarm.getCurrentTime() <= expireTime) {
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
      } catch (OutOfMemoryError e) {
	try {
	  System.err.println("Exiting due to OutOfMemoryError");
	} finally {
	  System.exit(11);
	}
      } catch (Throwable e) {
	e.printStackTrace();
      }
    } while (poolItem == null
	     && ! queueIfFull
	     && Alarm.getCurrentTime() <= expireTime);

    if (poolItem != null) {
      poolItem.start(task, loader);

      return true;
    }
    else
      return queueIfFull;
  }

  private void init()
  {
    if (_threadMax < 0)
      _threadMax = DEFAULT_THREAD_MAX;

    if (_threadIdleMin < 0 && _threadIdleMax < 0) {
      _threadIdleMin = DEFAULT_THREAD_IDLE_MIN;
      _threadIdleMax = _threadIdleMin + DEFAULT_THREAD_IDLE_GAP;
    }
    else if (_threadIdleMax < 0) {
      _threadIdleMax = _threadIdleMin + DEFAULT_THREAD_IDLE_GAP;
    }
    else if (_threadIdleMin < 0) {
      _threadIdleMin = DEFAULT_THREAD_IDLE_MIN;

      if (_threadIdleMax < _threadIdleMin)
	_threadIdleMin = 1;
    }

    calculateThreadPriority();
  }

  private void calculateThreadPriority()
  {
    if (_threadPrioritySet) {
    }
    else if (_threadIdleMin <= 0)
      _threadPriority = 0;
    else if (_threadIdleMin <= 2)
      _threadPriority = _threadIdleMin;
    else
      _threadPriority = (_threadIdleMin + 1) / 2;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  class Item extends Thread {
    final int _id;
    final String _name;

    Thread _thread;
    Thread _queueThread;

    Item _next;
    boolean _isIdle;

    long _threadResetCount;
  
    Runnable _task;
    ClassLoader _classLoader;

    Item()
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
     * Starts this;
     */
    boolean start(Runnable task, ClassLoader loader)
    {
      if (task == null)
	throw new NullPointerException();
      if (_isIdle)
	throw new IllegalStateException();
      
      synchronized (this) {
	_task = task;
	_classLoader = loader;
      }

      LockSupport.unpark(this);

      return true;
    }

    /**
     * The main thread execution method.
     */
    public void run()
    {
      synchronized (_idleLock) {
	_threadCount++;
	_startCount--;
	_threads.add(this);

	if (_startCount < 0) {
	  _startCount = 0;
	}
      }
      
      try {
	runTasks();
      } finally {
	synchronized (_idleLock) {
	  _threadCount--;

	  _threads.remove(this);
	}

	if (_threadCount < _threadIdleMin) {
	  _launcher.wake();
	}
      }
    }

    private void runTasks()
    {
      _threadResetCount = _resetCount;
    
      Thread thread = this;
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

      while (true) {
	try {
	  thread.setName(_name);
	  
	  // put the thread into the idle ring
	  synchronized (_idleLock) {
	    if (_isIdle) {
	    }
	    else {
	      synchronized (_taskQueue) {
		if (_taskQueue.size() > 0) {
		  _task = _taskQueue.remove(0);
		  _classLoader = _loaderQueue.remove(0);
		}
	      }

	      if (_task == null) {
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
	  if (_task == null) {
	    LockSupport.park();
	  }

	  Runnable task = null;
	  ClassLoader classLoader = null;
	  
	  synchronized (this) {
	    task = _task;
	    _task = null;

	    classLoader = _classLoader;
	    _classLoader = null;
	  }

	  // if the task is available, run it in the proper context
	  if (task != null) {
	    thread.setContextClassLoader(classLoader);
	    
	    try {
	      task.run();
	    } catch (Throwable e) {
	      log.log(Level.WARNING, e.toString(), e);
	    } finally {
	      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
	    }
	  }
	} catch (Throwable e) {
	}
      }
    }
  }

  class OverflowItem implements Runnable {
    Runnable _task;
    ClassLoader _loader;

    OverflowItem(Runnable task)
    {
      _task = task;
      _loader = Thread.currentThread().getContextClassLoader();
    }

    void start()
    {
      Thread thread = new Thread(this, _task.getClass().getSimpleName() + "-Overflow");
      thread.setDaemon(true);
      thread.start();
    }

    /**
     * The main thread execution method.
     */
    public void run()
    {
      Thread thread = Thread.currentThread();
      thread.setContextClassLoader(_loader);

      _task.run();
    }
  }

  class ThreadLauncher implements Runnable {
    private final Thread _thread;
    
    private ThreadLauncher()
    {
      _thread = new Thread(this);
      _thread.setName("resin-thread-launcher");
      _thread.setDaemon(true);

      _thread.start();
    }

    void wake()
    {
      LockSupport.unpark(_thread);
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

	if (_threadMax < _threadCount + _startCount)
	  doStart = false;
	else if (_threadIdleMin < idleCount + _startCount)
	  doStart = false;

	if (doStart)
	  _startCount++;
      }

      if (doStart) {
	try {
	  long now = Alarm.getCurrentTime();
	  _threadIdleOverflowExpire = now + OVERFLOW_TIMEOUT;
	  
	  Item poolItem = new Item();
	  poolItem.start();
	} catch (Throwable e) {
	  _startCount--;

	  e.printStackTrace();
	  if (_startCount < 0) {
	    Thread.dumpStack();
	    _startCount = 0;
	  }
	}

	// Thread.yield();
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
