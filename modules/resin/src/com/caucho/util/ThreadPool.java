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

import java.util.*;

import java.util.concurrent.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public class ThreadPool {
  private static final L10N L = new L10N(ThreadPool.class);
  private static final Logger log
    = Logger.getLogger(ThreadPool.class.getName());
  
  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;

  private static final ThreadPool _globalThreadPool = new ThreadPool();

  private static int _g_id;
    
  private int _threadMax = Integer.MAX_VALUE / 2;
  
  private int _threadIdleMin = 5;
  private int _threadIdleMax = 10;

  private long _resetCount;

  private final ArrayList<Item> _threads
    = new ArrayList<Item>();

  private final ArrayList<Runnable> _taskQueue
    = new ArrayList<Runnable>();

  private final ArrayList<ClassLoader> _loaderQueue
    = new ArrayList<ClassLoader>();

  private final ThreadLauncher _launcher = new ThreadLauncher();
  private final ScheduleThread _scheduler = new ScheduleThread();
  
  private boolean _isQueuePriority;

  private final Object _idleLock = new Object();
  
  private Item _idleHead;

  private static int _threadCount;
  // number of threads in the idle stack
  private static int _idleCount;
  // number of threads which are in the process of starting
  private static int _startCount;

  private static int _scheduleWaitCount;

  private ThreadPool()
  {
  }

  public static ThreadPool getThreadPool()
  {
    return _globalThreadPool;
  }

  //
  // Configuration properties
  //

  /**
   * Sets the maximum number of threads.
   */
  public void setThreadMax(int max)
  {
    if (max < _threadIdleMax)
      throw new ConfigException(L.l("lt;thread-max> ({0}) must be less than &lt;thread-idle-max> ({1})", max, _threadIdleMax));
	
    _threadMax = max;

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
    if (_threadIdleMax < min)
      throw new ConfigException(L.l("lt;thread-idle-min> ({0}) must be less than &lt;thread-idle-max> ({1})", min, _threadIdleMax));
    
    _threadIdleMin = min;
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
    if (max < _threadIdleMin)
      throw new ConfigException(L.l("lt;thread-idle-max> ({0}) must be greater than &lt;thread-idle-min> ({1})",
				    max, _threadIdleMin));
    
    if (_threadMax < max)
      throw new ConfigException(L.l("lt;thread-idle-max> ({0}) must be less than &lt;thread-max> ({1})",
				    max, _threadMax));
    
    _threadIdleMax = max;
  }

  /**
   * Gets the maximum number of idle threads.
   */
  public int getThreadIdleMax()
  {
    return _threadIdleMax;
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
    
    return schedule(task, loader, _threadIdleMin, MAX_EXPIRE, true);
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
    
    return schedule(task, loader, _threadIdleMin, expire, true);
  }

  /**
   * Adds a new task.
   */
  public void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    schedule(task, loader, 0, MAX_EXPIRE, true);
  }

  /**
   * Adds a new task.
   */
  public boolean start(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return schedule(task, loader, _threadIdleMin, MAX_EXPIRE, false);
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
    
    return schedule(task, loader, _threadIdleMin, expire, false);
  }

  /**
   * Adds a new task.
   */
  public void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    schedule(task, loader, 0, MAX_EXPIRE, false);
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
   * interrupts all the threads.
   */
  public void interrupt()
  {
    synchronized (_idleLock) {
      for (Item item = _idleHead;
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
  private boolean schedule(Runnable task,
			   ClassLoader loader,
			   int freeThreads,
			   long expireTime,
			   boolean queueIfFull)
  {
    Item poolItem = null;

    while (poolItem == null) {
      try {
	synchronized (_idleLock) {
	  int idleCount = _idleCount;
	  int freeCount = idleCount + _threadMax - _threadCount;
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

	    if (idleCount < _threadIdleMin)
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

  class Item implements Runnable {
    private final int _id;
    private final String _name;

    private Thread _thread;
    private Thread _queueThread;

    private Item _prev;
    private Item _next;
    private boolean _isIdle;

    private long _threadResetCount;
  
    private Runnable _task;
    private ClassLoader _classLoader;

    private Item()
    {
      synchronized (Item.class) {
	_id = _g_id++;
	_name = "resin-" + _id;
      }
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

	if (_threadCount < _threadIdleMin) {
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
	      if (_threadIdleMax < _idleCount) {
		return;
	      }
	      
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

	    // check to see if we're over the idle thread limit
	    synchronized (_idleLock) {
	      if (_isIdle &&
		  (_threadIdleMax < _idleCount ||
		   _resetCount != _threadResetCount)) {
		isDead = true;

		isReset = _resetCount != _threadResetCount;
	      
		Item next = _next;
		Item prev = _prev;

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
  }

  class ThreadLauncher implements Runnable {
    private ThreadLauncher()
    {
      Thread thread = new Thread(this);
      thread.setName("resin-thread-launcher");
      thread.setDaemon(true);

      thread.start();
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

	if (_threadMax < _threadCount + _startCount)
	  doStart = false;
	else if (_threadIdleMin < idleCount + _startCount)
	  doStart = false;

	if (doStart)
	  _startCount++;
      }

      if (doStart) {
	try {
	  Item poolItem = new Item();
    
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
	for (int i = 0; i < _threadIdleMin; i++)
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

  class ScheduleThread implements Runnable {
    private ScheduleThread()
    {
      Thread thread = new Thread(this);
      thread.setName("resin-thread-scheduler");
      thread.setDaemon(true);

      thread.start();
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
	    schedule(task, loader, _threadIdleMin, MAX_EXPIRE, false);
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
