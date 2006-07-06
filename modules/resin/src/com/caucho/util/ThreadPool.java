/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.vfs.EnvironmentStream;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public class ThreadPool implements Runnable {
  private static final Logger log
    = Logger.getLogger(ThreadPool.class.getName());
  
  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;

  private static final int SPARE_GAP = 5;
  
  private static int _maxThreads = 128;
  
  private static int _minSpareThreads = 5;

  private static long _resetCount;

  private final static ArrayList<ThreadPool> _threads
    = new ArrayList<ThreadPool>();

  private final static ArrayList<Runnable> _taskQueue
    = new ArrayList<Runnable>();

  private final static ArrayList<ClassLoader> _loaderQueue
    = new ArrayList<ClassLoader>();

  private final static ThreadLauncher _launcher = ThreadLauncher.create();
  private final static ScheduleThread _scheduler = ScheduleThread.create();
  
  private static boolean _isQueuePriority;

  private static final Object _idleLock = new Object();
  
  private static ThreadPool _idleHead;

  private static int _threadCount;
  // number of threads in the idle stack
  private static int _idleCount;
  // number of threads which are in the process of starting
  private static int _startCount;

  private static int _scheduleWaitCount;

  private static int _g_id;

  private final int _id;
  private final String _name;

  private Thread _thread;
  private Thread _queueThread;

  private ThreadPool _prev;
  private ThreadPool _next;
  private boolean _isIdle;

  private long _threadResetCount;
  
  private Runnable _task;
  private ClassLoader _classLoader;

  private ThreadPool()
  {
    synchronized (ThreadPool.class) {
      _id = _g_id++;
      _name = "resin-" + _id;
    }
  }

  /**
   * Sets the maximum number of threads.
   */
  public static void setThreadMax(int max)
  {
    _maxThreads = max;
  }

  /**
   * Gets the maximum number of threads.
   */
  public static int getThreadMax()
  {
    return _maxThreads;
  }

  /**
   * Sets the minimum number of idle threads.
   */
  public static void setSpareThreadMin(int min)
  {
    _minSpareThreads = min;
  }

  /**
   * Gets the minimum number of idle threads.
   */
  public static int getSpareThreadMin()
  {
    return _minSpareThreads;
  }

  /**
   * Returns the total thread count.
   */
  public static int getThreadCount()
  {
    return _threadCount;
  }

  /**
   * Returns the idle thread count.
   */
  public static int getIdleThreadCount()
  {
    return _idleCount;
  }

  /**
   * Returns the active thread count.
   */
  public static int getActiveThreadCount()
  {
    return getThreadCount() - getIdleThreadCount();
  }

  /**
   * Returns the free thread count.
   */
  public static int getFreeThreadCount()
  {
    return _maxThreads - _threadCount;
  }

  /**
   * Resets the thread pool, letting old threads drain.
   */
  public static void reset()
  {
    // XXX: not reliable
    _resetCount++;
  }

  /**
   * Schedules a new task.
   */
  public static boolean schedule(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, _minSpareThreads, MAX_EXPIRE, true);
  }

  /**
   * Adds a new task.
   */
  public static boolean schedule(Runnable task, long timeout)
  {
    long expire;
    
    if (timeout < 0 || timeout > MAX_EXPIRE)
      expire = MAX_EXPIRE;
    else
      expire = Alarm.getCurrentTime() + timeout;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, _minSpareThreads, expire, true);
  }

  /**
   * Adds a new task.
   */
  public static void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    schedule(task, loader, 0, MAX_EXPIRE, true);
  }

  /**
   * Adds a new task.
   */
  public static boolean start(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, _minSpareThreads, MAX_EXPIRE, false);
  }

  /**
   * Adds a new task.
   */
  public static boolean start(Runnable task, long timeout)
  {
    long expire;
    
    if (timeout < 0 || timeout > MAX_EXPIRE)
      expire = MAX_EXPIRE;
    else
      expire = Alarm.getCurrentTime() + timeout;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, _minSpareThreads, expire, false);
  }

  /**
   * Adds a new task.
   */
  public static void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    schedule(task, loader, 0, MAX_EXPIRE, false);
  }

  /**
   * Adds a new task.
   */
  public static boolean startPriority(Runnable task, long timeout)
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
   * interrupts all the threads.
   */
  public static void interrupt()
  {
    synchronized (_idleLock) {
      for (ThreadPool item = _idleHead;
	   item != null;
	   item = item._next) {
	Thread thread = item.getThread();

	if (thread != null) {
	  try {
	    thread.interrupt();
	  } catch (Throwable e) {
	  }
	}
      }
    }
  }

  /**
   * Adds a new task.
   */
  private static boolean schedule(Runnable task,
				  ClassLoader loader,
				  int freeThreads,
				  long expireTime,
				  boolean queueIfFull)
  {
    ThreadPool poolItem = null;

    while (poolItem == null) {
      try {
	synchronized (_idleLock) {
	  int idleCount = _idleCount;
	  int freeCount = idleCount + _maxThreads - _threadCount;
	  boolean startNew = false;

	  if (idleCount > 0 && freeThreads < freeCount) {
	    poolItem = _idleHead;
	    _idleHead = poolItem._next;

	    poolItem._next = null;
	    poolItem._prev = null;
	    poolItem._isIdle = false;
	    if (_idleHead != null)
	      _idleHead._prev = null;

	    _idleCount--;

	    if (idleCount < _minSpareThreads)
	      startNew = true;
	  }
	  else
	    startNew = true;

	  if (startNew) {
	    synchronized (_launcher) {
	      _launcher.notify();
	    }
	    
	    if (poolItem == null) {
	      if (queueIfFull) {
		synchronized (_taskQueue) {
		  _taskQueue.add(task);
		  _loaderQueue.add(loader);
		  _taskQueue.notify();
		}
		
		return false;
	      }

	      if (expireTime < Alarm.getCurrentTime())
		return false;
		  
	      _scheduleWaitCount++;
		
	      try {
		// clear interrupted flag
		Thread.interrupted();
		
		_idleLock.wait(5000);
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
    }
    
    poolItem.start(task, loader);

    return true;
  }

  /**
   * Returns the id.
   */
  int getId()
  {
    return _id;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the thread id.
   */
  public long getThreadId()
  {
    return _thread.getId();
  }

  /**
   * Returns the thread.
   */
  Thread getThread()
  {
    return _thread;
  }

  /**
   * Starts the thread.
   */
  private boolean start(Runnable task, ClassLoader loader)
  {
    synchronized (this) {
      _task = task;
      _classLoader = loader;

      notify();
    }

    return true;
  }

  /**
   * The main thread execution method.
   */
  public void run()
  {
    _thread = Thread.currentThread();

    synchronized (_idleLock) {
      _threadCount++;
      _startCount--;
      _threads.add(this);

      if (_startCount < 0) {
	System.out.println("ThreadPool start count is negative: " + _startCount);
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

      if (_threadCount < _minSpareThreads) {
	synchronized (_launcher) {
	  _launcher.notify();
	}
      }
    }
  }

  private void runTasks()
  {
    _threadResetCount = _resetCount;
    
    Thread thread = Thread.currentThread();
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    boolean isIdle = false;

    while (true) {
      try {
	// put the thread into the idle ring
	if (! isIdle) {
	  _isQueuePriority = true;
	  
	  isIdle = true;
	  
	  synchronized (_idleLock) {
	    _next = _idleHead;
	    _prev = null;
	    _isIdle = true;

	    if (_idleHead != null)
	      _idleHead._prev = this;

	    _idleHead = this;
	    _idleCount++;

	    if (_scheduleWaitCount > 0)
	      _idleLock.notify();
	  }
	}

	Runnable task = null;
	ClassLoader classLoader = null;

	// clear interrupted flag
	Thread.interrupted();
	
	// wait for the next available task
	synchronized (this) {
	  if (_task == null) {
	    thread.setContextClassLoader(systemClassLoader);
	    wait(60000L);
	  }

	  task = _task;
	  _task = null;

	  classLoader = _classLoader;
	  _classLoader = null;
	}

	// if the task is available, run it in the proper context
	if (task != null) {
	  isIdle = false;
	  
	  thread.setContextClassLoader(classLoader);
	  try {
	    task.run();
	  } catch (Throwable e) {
	    log.log(Level.WARNING, e.toString(), e);
	  } finally {
	    thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
	  }
	}
	else {
	  boolean isDead = false;
	  boolean isReset = false;

	  // check to see if we're over the spare thread limit
	  synchronized (_idleLock) {
	    if (_isIdle &&
		(_minSpareThreads + SPARE_GAP < _idleCount ||
		 _resetCount != _threadResetCount)) {
	      isDead = true;

	      isReset = _resetCount != _threadResetCount;
	      
	      ThreadPool next = _next;
	      ThreadPool prev = _prev;

	      _next = null;
	      _prev = null;
	      _isIdle = false;

	      if (next != null)
		next._prev = prev;

	      if (prev != null)
		prev._next = next;
	      else
		_idleHead = next;

	      _idleCount--;
	    }
	  }

	  if (isReset) {
	    synchronized (_launcher) {
	      _launcher.notify();
	    }
	  }
	  
	  if (isDead)
	    return;
	}
      } catch (Throwable e) {
      }
    }
  }

  static class ThreadLauncher implements Runnable {
    private static ThreadLauncher _launcher;
    
    private ThreadLauncher()
    {
      Thread thread = new Thread(this);
      thread.setName("resin-thread-launcher");
      thread.setDaemon(true);

      thread.start();
    }

    static ThreadLauncher create()
    {
      if (_launcher == null)
	_launcher = new ThreadLauncher();

      return _launcher;
    }

    /**
     * Starts a new connection
     */
    private boolean startConnection(long waitTime)
      throws InterruptedException
    {
      boolean doStart = true;
      
      synchronized (_idleLock) {
	int idleCount = _idleCount;

	if (_maxThreads < _threadCount + _startCount)
	  doStart = false;
	else if (_minSpareThreads < idleCount + _startCount)
	  doStart = false;

	if (doStart)
	  _startCount++;
      }

      if (doStart) {
	try {
	  ThreadPool poolItem = new ThreadPool();
    
	  Thread thread = new Thread(poolItem, poolItem.getName());
	  thread.setDaemon(true);

	  thread.start();
	} catch (Throwable e) {
	  _startCount--;

	  e.printStackTrace(EnvironmentStream.getOriginalSystemErr().getPrintWriter());
	  if (_startCount < 0) {
	    Thread.dumpStack();
	    _startCount = 0;
	  }
	}

	// Thread.yield();
      }
      else {
	Thread.interrupted();
	synchronized (this) {
	  wait(waitTime);
	  return false;
	}
      }

      return true;
    }
    
    public void run()
    {
      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      
      Thread.currentThread().setContextClassLoader(systemLoader);

      try {
	for (int i = 0; i < _minSpareThreads; i++)
	  startConnection(0);
      } catch (Throwable e) {
	e.printStackTrace();
      }
      
      while (true) {
	try {
	  startConnection(10000);

	  //Thread.currentThread().sleep(5);
	  Thread.currentThread().yield();
	} catch (OutOfMemoryError e) {
	  System.exit(10);
	} catch (Throwable e) {
	  e.printStackTrace();
	}
      }
    }
  }

  static class ScheduleThread implements Runnable {
    private static ScheduleThread _scheduler;
    
    private ScheduleThread()
    {
      Thread thread = new Thread(this);
      thread.setName("resin-thread-scheduler");
      thread.setDaemon(true);

      thread.start();
    }

    static ScheduleThread create()
    {
      if (_scheduler == null)
	_scheduler = new ScheduleThread();

      return _scheduler;
    }
    
    public void run()
    {
      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

      Thread thread = Thread.currentThread();
      thread.setContextClassLoader(systemLoader);
      
      while (true) {
	try {
	  Runnable task = null;
	  ClassLoader loader = null;

	  Thread.interrupted();
	  
	  synchronized (_taskQueue) {
	    if (_taskQueue.size() > 0) {
	      task = _taskQueue.remove(0);
	      loader = _loaderQueue.remove(0);
	    }
	    else {
	      try {
		_taskQueue.wait(60000);
	      } catch (Throwable e) {
		thread.interrupted();
		log.finer(e.toString());
	      }
	    }
	  }

	  if (task != null) {
	    schedule(task, loader, _minSpareThreads, MAX_EXPIRE, false);
	  }
	} catch (OutOfMemoryError e) {
	  System.exit(10);
	} catch (Throwable e) {
	  e.printStackTrace();
	}
      }
    }
  }
}
