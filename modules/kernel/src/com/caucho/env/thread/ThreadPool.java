/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.env.thread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.ThreadDump;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
public final class ThreadPool {
  private static final L10N L = new L10N(ThreadPool.class);
  private static final Logger log
    = Logger.getLogger(ThreadPool.class.getName());

  private static final long MAX_EXPIRE = Long.MAX_VALUE / 2;
  
  private static final int DEFAULT_EXECUTOR_TASK_MAX = 16;

  private static final long PRIORITY_TIMEOUT = 10L;
  
  private static final int THREAD_IDLE_MIN = 16;
  private static final int THREAD_IDLE_MAX = 256;
  
  private static final NullRunnable NULL_RUNNABLE = new NullRunnable();

  private static final AtomicReference<ThreadPool> _globalThreadPool
    = new AtomicReference<ThreadPool>();
  
  private final String _name;
  
  // configuration items

  private int _executorTaskMax = DEFAULT_EXECUTOR_TASK_MAX;

  private final ThreadLauncher _launcher;
  private final Lifecycle _lifecycle = new Lifecycle(); 
  
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
  
  private final AtomicReference<ThreadNode> _idleHead
    = new AtomicReference<ThreadNode>();
  
  private final AtomicReference<ThreadNode> _priorityIdleHead
    = new AtomicReference<ThreadNode>();

  //
  // task/priority overflow queues
  //

  private final ConcurrentLinkedQueue<ThreadTask> _taskQueue
    = new ConcurrentLinkedQueue<ThreadTask>();
  
  private final ConcurrentLinkedQueue<ThreadTask> _priorityQueue
    = new ConcurrentLinkedQueue<ThreadTask>();

  private int _waitCount;
  
  //
  // executor
  //

  private final Object _executorLock = new Object();
  // number of executor tasks running
  private int _executorTaskCount;
  // queue for waiting executor tasks
  private ExecutorQueueItem _executorQueueHead;
  private ExecutorQueueItem _executorQueueTail;

  public ThreadPool()
  {
    this("system");
  }

  public ThreadPool(String name)
  {
    _name = name;
    
    _launcher = new ThreadLauncher(this);
    _launcher.setIdleMin(THREAD_IDLE_MIN);
    _launcher.setIdleMax(THREAD_IDLE_MAX);

    // initialize default values
    init();
  }

  public static ThreadPool getCurrent()
  {
    return getThreadPool();
  }

  public static ThreadPool getThreadPool()
  {
    ThreadPool pool = _globalThreadPool.get();
    
    if (pool == null) {
      pool = new ThreadPool();
      
      if (_globalThreadPool.compareAndSet(null, pool)) {
        pool.start();
      }
      else {
        pool = _globalThreadPool.get();
      }
    }
    
    return pool;
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
    _launcher.setIdleMin(min);
  }

  /**
   * Gets the minimum number of idle threads.
   */
  public int getIdleMin()
  {
    return _launcher.getIdleMin();
  }
  
  /**
   * Returns the thread idle max.
   */
  public int getIdleMax()
  {
    return _launcher.getIdleMax();
  }
  
  /**
   * Returns the thread idle max.
   */
  public void setIdleMax(int idleMax)
  {
    _launcher.setIdleMax(idleMax);
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

  /**
   * Sets the minimum number of free threads reserved for priority tasks.
   */
  public void setPriorityIdleMin(int priority)
  {
    _launcher.setPriorityIdleMin(priority);
  }

  public int getPriorityIdleMin()
  {
    return _launcher.getPriorityIdleMin();
  }

  /**
   * Sets the maximum number of executor threads.
   */
  public void setExecutorTaskMax(int max)
  {
    if (getThreadMax() < max)
      throw new ConfigException(L.l("<thread-executor-max> ({0}) must be less than <thread-max> ({1})",
                                    max, getThreadMax()));

    if (max == 0)
      throw new ConfigException(L.l("<thread-executor-max> must not be zero."));

    _executorTaskMax = max;
  }

  /**
   * Gets the maximum number of executor threads.
   */
  public int getExecutorTaskMax()
  {
    return _executorTaskMax;
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
            - getThreadIdleCount()
            - getPriorityIdleCount());
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
  public int getPriorityIdleCount()
  {
    return _launcher.getPriorityIdleCount();
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

  /**
   * Schedules a new task.
   */
  public boolean schedule(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue);
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
      expire = Alarm.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = true;

    return scheduleImpl(task, loader, expire, isPriority, isQueue);
  }

  /**
   * Adds a new task.
   */
  public void schedulePriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = Alarm.getCurrentTimeActual() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;

    if (! scheduleImpl(task, loader, expire, isPriority, isQueue)) {
      log.warning(this + " unable to schedule priority thread " + task
                  + " pri-min=" + getPriorityIdleMin()
                  + " thread=" + getThreadCount()
                  + " idle=" + getThreadIdleCount()
                  + " pri-idle=" + getPriorityIdleCount()
                  + " starting=" + getThreadStartingCount()
                  + " max=" + getThreadMax());

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

        return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue);
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
    ExecutorQueueItem item = null;

    synchronized (_executorLock) {
      _executorTaskCount--;

      assert(_executorTaskCount >= 0);

      if (_executorQueueHead != null) {
        item = _executorQueueHead;

        _executorQueueHead = item._next;

        if (_executorQueueHead == null)
          _executorQueueTail = null;
      }
    }

    if (item != null) {
      Runnable task = item.getRunnable();
      ClassLoader loader = item.getLoader();

      boolean isPriority = false;
      boolean isQueue = true;

      scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue);
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

    return scheduleImpl(task, loader, MAX_EXPIRE, isPriority, isQueue);
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
      expire = Alarm.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = false;
    boolean isQueue = false;

    return scheduleImpl(task, loader, expire, isPriority, isQueue);
  }

  /**
   * Adds a new task.
   */
  public void startPriority(Runnable task)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    long expire = Alarm.getCurrentTimeActual() + PRIORITY_TIMEOUT;

    boolean isPriority = true;
    boolean isQueue = true;

    if (! scheduleImpl(task, loader, expire, isPriority, isQueue)) {
      log.warning(this + " unable to start priority thread " + task
                  + " pri-min=" + getPriorityIdleMin()
                  + " thread=" + getThreadCount()
                  + " idle=" + getThreadIdleCount()
                  + " pri-idle=" + getPriorityIdleCount()
                  + " starting=" + getThreadStartingCount()
                  + " max=" + getThreadMax());

      ThreadDump.create().dumpThreadsNoCache();

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
      expire = Alarm.getCurrentTimeActual() + timeout;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean isPriority = true;
    boolean isQueue = false;

    return scheduleImpl(task, loader, expire, isPriority, isQueue);
  }

  /**
   * main scheduling implementation class.
   */
  private boolean scheduleImpl(Runnable task,
                               ClassLoader loader,
                               long expireTime,
                               boolean isPriority,
                               boolean isQueueIfFull)
  {
    if (scheduleIdle(task, loader, isPriority)) {
      return true;
    }

    if (! isQueueIfFull && _launcher.isThreadMax()) {
      throw new IllegalStateException(L.l("Thread pool full"));
    }
    
    Thread requestThread = null;

    if (! isQueueIfFull) {
      requestThread = Thread.currentThread();
    }

    ThreadTask taskItem = new ThreadTask(task, loader, requestThread);

    if (isPriority) {
      _priorityQueue.offer(taskItem);
    }
    else {
      _taskQueue.offer(taskItem);
    }
    
    // wake any threads idled after our offer
    ResinThread thread = popIdleThread();

    if (thread != null) {
      thread.scheduleTask(NULL_RUNNABLE, null);
    }
    else if (isPriority && (thread = popPriorityThread()) != null) {
      thread.scheduleTask(NULL_RUNNABLE, null);
    }
    
    _launcher.wake();
    
    if (! isQueueIfFull) {
      taskItem.park(expireTime);
    }

    return true;
  }
  
  private boolean scheduleIdle(Runnable task,
                               ClassLoader loader,
                               boolean isPriority)
  {
    if (! _priorityQueue.isEmpty()) {
      return false;
    }
    
    if (_taskQueue.isEmpty() || isPriority) {
      ResinThread thread = popIdleThread();

      if (thread != null) {
        thread.scheduleTask(task, loader);
        return true;
      }
    }
    
    if (! isPriority) {
      return false;
    }

    ResinThread priorityThread = popPriorityThread();
    
    if (priorityThread != null) {
      priorityThread.scheduleTask(task, loader);
      return true;
    }
    else {
      return false;
    }
  }

  void startIdleThread()
  {
    _launcher.wake();
  }
  
  void beginIdle(ResinThread thread)
  {
    if (_launcher.beginPriorityIdle()) {
      pushIdleThread(_priorityIdleHead, thread);
    }
    else {
      _launcher.onChildIdleBegin();

      pushIdleThread(_idleHead, thread);
    }
    
    ThreadTask item = nextQueueTask();
    
    if (item == null) {
      return;
    }
    
    ResinThread itemThread = popIdleThread();
      
    if (itemThread == null)
      itemThread = popPriorityThread();
    
    if (itemThread != null) {
      itemThread.scheduleTask(item.getRunnable(), item.getLoader());
      item.wake();
    }
    else {
      _priorityQueue.offer(item);
      _launcher.wake();
    }
  }
  
  /**
   * Returns the next available task from the queue.
   * should exit.
   */
  private ThreadTask nextQueueTask()
  {
    ThreadTask item = _priorityQueue.poll();
    
    if (item != null)
      return item;
    
    if (_taskQueue.size() == 0)
      return null;

    int priorityIdleMin = _launcher.getPriorityIdleMin();
    int priorityIdleCount = _launcher.getPriorityIdleCount();
    int idleCount = _launcher.getIdleCount();

    // if we have spare threads, process any task queue item
    if (priorityIdleMin <= priorityIdleCount + idleCount) {
      return _taskQueue.poll();
    }

    return null;
  }
  
  private ResinThread popIdleThread()
  {
    ResinThread thread = popIdleThread(_idleHead);
    
    if (thread != null) {
      _launcher.onChildIdleEnd();
    }
    
    return thread;
  }
  
  private ResinThread popPriorityThread()
  {
    ResinThread thread = popIdleThread(_priorityIdleHead);
    
    if (thread != null) {
      _launcher.onPriorityIdleEnd();
    }
    
    return thread;
  }
 
  private void pushIdleThread(AtomicReference<ThreadNode> idleHeadRef,
                              ResinThread thread)
  {
    ThreadNode head = new ThreadNode(thread);
    
    ThreadNode next;
    
    do {
      next = idleHeadRef.get();
      
      head.setNext(next);
    } while (! idleHeadRef.compareAndSet(next, head));
  }
  
  private ResinThread popIdleThread(AtomicReference<ThreadNode> idleHeadRef)
  {
    ThreadNode head;
    ThreadNode next;
    
    do {
      head = idleHeadRef.get();
      
      if (head == null)
        return null;
      
      next = head.getNext();
    } while (! idleHeadRef.compareAndSet(head, next));
    
    return head.getThread();
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
  public void interrupt()
  {
    ResinThread thread;
    
    while ((thread = popIdleThread()) != null) {
      thread.close();
    }
  }

  /*
  long onThreadStart()
  {
    _activeCount.incrementAndGet();
    
    int startCount = _startingCount.decrementAndGet();

    if (startCount < 0) {
      _startingCount.set(0);
      new IllegalStateException().printStackTrace();
    }

    _createCount.incrementAndGet();
    
    return _resetCount.get();
  }
  
  void onThreadFirstTask()
  {
    _launcher.wake();
  }
  
  void onThreadEnd()
  {
    _activeCount.decrementAndGet();

    _launcher.wake();
  }
  */

  /**
   * Checks if the launcher should start another thread.
   */
  /*
  boolean doStart()
  {
    if (! isActive())
      return false;
    
    int idleCount = _idleCount.get();
    int priorityIdleCount = _priorityIdleCount.get();
    int startingCount = _startingCount.getAndIncrement();

    int threadCount = _activeCount.get() + startingCount;
    
    if (_threadMax < threadCount) {
      _startingCount.decrementAndGet();
      
      return false;
    }
    else if (idleCount + startingCount < _idleMin
             || priorityIdleCount + startingCount < _priorityIdleMin) {
      return true;
    }
    else {
      _startingCount.decrementAndGet();
      
      return false;
    }
  }
  */

  public void close()
  {
    if (this == _globalThreadPool.get())
      throw new IllegalStateException(L.l("Cannot close global thread pool"));
    
    _lifecycle.toDestroy();
    _launcher.destroy();
    
    interrupt();
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
  
  static class NullRunnable implements Runnable {
    @Override
    public void run()
    {
    }
  }
  
  static final class ThreadNode {
    private final ResinThread _thread;
    private ThreadNode _next;
    
    ThreadNode(ResinThread thread)
    {
      _thread = thread;
    }
    
    ResinThread getThread()
    {
      return _thread;
    }

    void setNext(ThreadNode next)
    {
      _next = next;
    }
    
    ThreadNode getNext()
    {
      return _next;
    }
  }
}
