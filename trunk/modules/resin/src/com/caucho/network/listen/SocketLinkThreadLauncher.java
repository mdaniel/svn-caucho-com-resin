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

package com.caucho.network.listen;

import java.util.concurrent.atomic.AtomicInteger;

import com.caucho.env.thread.AbstractThreadLauncher;
import com.caucho.env.thread.ThreadPool;
import com.caucho.inject.Module;
import com.caucho.util.RingValueQueue;

/**
 * Represents a protocol connection.
 */
@Module
class SocketLinkThreadLauncher extends AbstractThreadLauncher
{
  private final ThreadPool _threadPool = ThreadPool.getThreadPool();
  private TcpPort _listener;
  
  private final RingValueQueue<AcceptTask> _acceptTaskQueue
    = new RingValueQueue<AcceptTask>(1024);
  
  private final RingValueQueue<ConnectionTask> _resumeTaskQueue
    = new RingValueQueue<ConnectionTask>(16 * 1024);
  
  private String _threadName;
  
  private final AtomicInteger _resumeStartCount = new AtomicInteger();

  SocketLinkThreadLauncher(TcpPort listener)
  {
    _listener = listener;
  }

  @Override
  protected boolean isEnable()
  {
    if (_listener.isClosed())
      return false;
    else
      return super.isEnable();
  }

  public void init()
  {
    _threadName = generateThreadName() + "-launcher";
  }
  
  boolean offerResumeTask(ConnectionTask task)
  {
    if (! _resumeTaskQueue.offer(task)) {
      System.out.println("FAILED_SUBMIT:");
    }
    
    /*
    if (_resumeTaskQueue.getSize() > 512) {
      System.out.println("SIZE: " + _resumeTaskQueue.getSize());
    }
    */
    
    wakeResumeTask(1);
    
    return true;
  }
  
  boolean submitResumeTask(ConnectionTask task)
  {
    if (! _resumeTaskQueue.offer(task)) {
      System.out.println("FAILED_SUBMIT:");
    }
    
    wakeResumeTask(1);
    
    return true;
  }

  void wakeScheduler()
  {
    _threadPool.wakeScheduler();
  }
  
  @Override
  protected String getThreadName()
  {
    if (_threadName == null) {
      _threadName = generateThreadName() + "-launcher";
    }
    
    return _threadName;
  }

  String generateThreadName()
  {
    String address = _listener.getAddress();
    int port = _listener.getPort();

    if (address != null) {
      return ("resin-port-" + address + ":" + port);
    }
    else {
      return ("resin-port-" + port);
    }
  }
 

  /**
   * Cycles through task from a thread. 
   */
  void handleTasks(boolean isResume)
  {
    int retryMax = 8;
    int retryCount = retryMax;
    
    while (retryCount-- >= 0) {
      ConnectionTask task = _acceptTaskQueue.poll();
      
      if (task == null) {
        if (! isResume) {
          isResume = true;
          _resumeStartCount.incrementAndGet();
        }
        
        task = _resumeTaskQueue.poll();
      }
      
      if (! _resumeTaskQueue.isEmpty()) {
        wakeResumeTask(4);
      }
      
      if (isResume) {
        isResume = false;
        _resumeStartCount.decrementAndGet();
      }
      
      if (! _resumeTaskQueue.isEmpty()) {
        wakeResumeTask(1);
      }
      
      if (task != null) {
        retryCount = retryMax;
        task.run();
      }
    }
  }

  void wakeResumeTask(int min)
  {
    int startCount = 0;

    while (startCount < min) {
      int threadCount = getThreadCount();
      int startingCount = getStartingCount();
      int resumeCount = _resumeStartCount.get();
      
      if (getThreadMax() <= threadCount + startingCount + resumeCount) {
        return;
      }
    
      int size = _resumeTaskQueue.size();
      if (size < min) {
        min = size;
      }
      
      if (min <= resumeCount) {
        return;
      }
      
      if (_resumeStartCount.compareAndSet(resumeCount, resumeCount + 1)) {
        startCount++;

        _threadPool.schedule(new TcpSocketResumeThread(this));
      }
    }
  }
  
  void addResumeStart()
  {
    _resumeStartCount.incrementAndGet();
  }


  @Override
  protected void launchChildThread(int id)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    TcpSocketLink startConn = null;
    
    try {
      thread.setContextClassLoader(_listener.getClassLoader());
      
      startConn = _listener.allocateConnection();

      AcceptTask acceptTask = startConn.requestAccept();
      
      if (acceptTask != null && _acceptTaskQueue.offer(acceptTask)) {
        startConn = null;
        
        _threadPool.schedule(new TcpSocketAcceptThread(this));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (startConn != null)
        _listener.closeConnection(startConn);
      
      thread.setContextClassLoader(loader);
    }
  }

  @Override
  protected void startWorkerThread()
  {
    _threadPool.schedule(this);
  }
 
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listener + "]";
  }
}
