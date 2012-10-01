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

package com.caucho.env.shutdown;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.*;

import com.caucho.env.service.*;
import com.caucho.env.warning.WarningService;
import com.caucho.lifecycle.*;
import com.caucho.util.*;
import com.caucho.vfs.TempBuffer;

/**
 * The ShutdownSystem manages the Resin shutdown and includes a timeout
 * thread. If the timeout takes longer than shutdown-wait-max, the ShutdownSystem
 * will force a JVM exit.
 */
public class ShutdownSystem extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = 1;

  private static final Logger log = 
    Logger.getLogger(ShutdownSystem.class.getName());
  private static final L10N L = new L10N(ShutdownSystem.class);

  private static final AtomicReference<ShutdownSystem> _activeService
    = new AtomicReference<ShutdownSystem>();
  
  private long _shutdownWaitMax = 120000L;
  
  private boolean _isShutdownOnOutOfMemory = true;

  private WeakReference<ResinSystem> _resinSystemRef;
  private WarningService _warningService;
  
  private Lifecycle _lifecycle = new Lifecycle();
  
  private FailSafeHaltThread _failSafeHaltThread;
  private FailSafeMemoryFreeThread _failSafeMemoryFreeThread;
  private ShutdownThread _shutdownThread;
  
  private boolean _isEmbedded;
  
  private AtomicReference<ExitCode> _exitCode = 
    new AtomicReference<ExitCode>();
  
  private ArrayList<Runnable> _memoryFreeTasks = new ArrayList<Runnable>();
  
  private ShutdownSystem(boolean isEmbedded)
  {
    _isEmbedded = isEmbedded;
    
    _resinSystemRef = new WeakReference<ResinSystem>(ResinSystem.getCurrent());
    
    _warningService = ResinSystem.getCurrentService(WarningService.class);
    
    if (_warningService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                           ShutdownSystem.class.getSimpleName(),
                                           WarningService.class.getSimpleName()));
    }
  }
  
  public static ShutdownSystem createAndAddService()
  {
    return createAndAddService(CurrentTime.isTest());
  }

  public static ShutdownSystem createAndAddService(boolean isEmbedded)
  {
    ResinSystem system = preCreate(ShutdownSystem.class);
      
    ShutdownSystem service = new ShutdownSystem(isEmbedded);
    system.addService(ShutdownSystem.class, service);
    
    return service;
  }

  public static ShutdownSystem getCurrent()
  {
    return ResinSystem.getCurrentService(ShutdownSystem.class);
  }
  
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }
  
  public void setShutdownWaitTime(long shutdownTime)
  {
    _shutdownWaitMax = shutdownTime;
  }

  public void setShutdownOnOutOfMemory(boolean isShutdown)
  {
    _isShutdownOnOutOfMemory = isShutdown;
  }

  public boolean isShutdownOnOutOfMemory()
  {
    return _isShutdownOnOutOfMemory;
  }
  
  /**
   * Returns the current lifecycle state.
   */
  public LifecycleState getLifecycleState()
  {
    return _lifecycle.getState();
  }
  
  public ExitCode getExitCode()
  {
    return _exitCode.get();
  }
  
  public void addMemoryFreeTask(Runnable task)
  {
    _memoryFreeTasks.add(task);
  }
  
  /**
   * Start the server shutdown
   */
  public static void shutdownOutOfMemory(String msg)
  {
    freeMemoryBuffers();
    
    ShutdownSystem shutdown = _activeService.get();
    
    if (shutdown != null && ! shutdown.isShutdownOnOutOfMemory()) {
      System.err.println(msg);
      return;
    }
    else {
      shutdownActive(ExitCode.MEMORY, msg);
    }
  }
  
  /**
   * Attempt to free as much memory as possible for OOM handling.
   * These calls must not allocate memory.
   */
  private static void freeMemoryBuffers()
  {
    TempBuffer.clearFreeLists();
  }
  
  public static void startFailsafe(String msg)
  {
    ShutdownSystem shutdown = _activeService.get();
    
    if (shutdown != null) {
      shutdown.startFailSafeShutdown(msg);
      return;
    }
    
    shutdown = getCurrent();
    
    if (shutdown != null) {
      shutdown.startFailSafeShutdown(msg);
      return;
    }
    
    log.warning("ShutdownService is not active: failsafe: " + msg);
    System.out.println("ShutdownService is not active: failsafe: " + msg);
  }
    
  /**
   * Start the server shutdown
   */
  public static void shutdownActive(ExitCode exitCode, String msg)
  {
    ShutdownSystem shutdown = _activeService.get();
    
    if (shutdown != null) {
      shutdown.shutdown(exitCode, msg);
      return;
    }
    
    shutdown = getCurrent();
    
    if (shutdown != null) {
      shutdown.shutdown(exitCode, msg);
      return;
    }
    
    log.warning("ShutdownService is not active");
    System.out.println("ShutdownService is not active");
  }

  /**
   * Start the server shutdown
   */
  public void shutdown(ExitCode exitCode, String msg)
  {
    startFailSafeShutdown(msg);

    ShutdownThread shutdownThread = _shutdownThread;
    
    if (shutdownThread != null) {
      shutdownThread.startShutdown(exitCode);

      if (! _isEmbedded) {
        waitForShutdown();
      
        System.out.println("Shutdown timeout");
        System.exit(exitCode.ordinal());
      }
    }
    else {
      shutdownImpl(exitCode);
    }
  }

  public void startFailSafeShutdown(String msg)
  {
    startFailSafeShutdown(msg, _shutdownWaitMax);
  }

  public void startFailSafeShutdown(String msg, long period)
  {
    // start the fail-safe thread in case the shutdown fails
    FailSafeHaltThread haltThread = _failSafeHaltThread;

    if (haltThread != null) {
      haltThread.startShutdown(period);
    }
    
    FailSafeMemoryFreeThread memoryFreeThread = _failSafeMemoryFreeThread;
    
    if (memoryFreeThread != null) {
      memoryFreeThread.startShutdown();
    }

    try {
      _warningService.sendWarning(this, "Shutdown: " + msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Closes the server.
   */
  private void shutdownImpl(ExitCode exitCode)
  {
    // start the fail-safe thread in case the shutdown fails
    FailSafeHaltThread haltThread = _failSafeHaltThread;
    
    if (haltThread != null)
      haltThread.startShutdown();

    if (exitCode == null)
      exitCode = ExitCode.FAIL_SAFE_HALT;
    
    _exitCode.compareAndSet(null, exitCode);

    try {
      try {
        ResinSystem resinSystem = _resinSystemRef.get();
        
        if (resinSystem != null)
          resinSystem.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        _resinSystemRef = null;
      }
    } finally {
      _lifecycle.toDestroy();

      System.err.println("\nShutdown Resin reason: " + exitCode + "\n");

      log.warning("Shutdown Resin reason: " + exitCode);

      if (! _isEmbedded) {
        System.exit(exitCode.ordinal());
      }
    }
  }
  
  private ResinSystem getResinSystem()
  {
    WeakReference<ResinSystem> resinSystemRef = _resinSystemRef;
    
    if (resinSystemRef != null)
      return resinSystemRef.get();
    else
      return null;
  }

  /**
   * Dump threads for debugging
   */
  public void dumpThreads()
  {
  }
  
  //
  // Service API
  //
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  /**
   * Starts the server.
   */
  @Override
  public void start()
  {
    _lifecycle.toActive();
    
    _exitCode.set(null);
    
    if (! _isEmbedded) {
      // _activeService.compareAndSet(null, this);
      _activeService.set(this);
    }
    
    if (! CurrentTime.isTest() && ! _isEmbedded) {
      _failSafeHaltThread = new FailSafeHaltThread();
      _failSafeHaltThread.start();
      
      _failSafeMemoryFreeThread = new FailSafeMemoryFreeThread();
      _failSafeMemoryFreeThread.start();
    }

    if (! _isEmbedded) {
      _shutdownThread = new ShutdownThread();
      _shutdownThread.setDaemon(true);
      _shutdownThread.start();
    }
  }
  
  /**
   * Starts the server.
   */
  @Override
  public void stop()
  {
    _lifecycle.toDestroy();
    
    _activeService.set(null);
    
    FailSafeHaltThread failSafeThread = _failSafeHaltThread;
    
    if (failSafeThread != null)
      failSafeThread.wake();
    
    FailSafeMemoryFreeThread memoryFreeThread = _failSafeMemoryFreeThread;
    
    if (memoryFreeThread != null)
      memoryFreeThread.startShutdown();
    
    ShutdownThread shutdownThread = _shutdownThread;
    
    if (shutdownThread != null)
      shutdownThread.wake();
  }

  @Override
  public void destroy()
  {
    _lifecycle.toDestroy();
  }
  
  private void waitForShutdown()
  {
    waitForShutdown(-1);
  }
  
  private void waitForShutdown(long period)
  {
    if (period <= 0)
      period = _shutdownWaitMax;
    
    long expire = System.currentTimeMillis() + period;
    long now;

    while ((now = System.currentTimeMillis()) < expire) {
      try {
        Thread.interrupted();
        Thread.sleep(expire - now);
      } catch (Exception e) {
      }
    }
  }

  @Override
  public String toString()
  {
    ResinSystem resinSystem = getResinSystem();
    
    if (resinSystem != null)
      return getClass().getSimpleName() + "[id=" + resinSystem.getId() + "]";
    else
      return getClass().getSimpleName() + "[closed]";
  }

  class ShutdownThread extends Thread {
    private AtomicReference<ExitCode> _shutdownExitCode
      = new AtomicReference<ExitCode>();

    ShutdownThread()
    {
      setName("resin-shutdown");
      setDaemon(true);
    }

    /**
     * Starts the destroy sequence
     */
    void startShutdown(ExitCode exitCode)
    {
      _shutdownExitCode.compareAndSet(null, exitCode);
      
      wake();
    }
    
    void wake()
    {
      LockSupport.unpark(this);
    }

    @Override
    public void run()
    {
      while (_shutdownExitCode.get() == null 
             && _lifecycle.isActive()
             && _activeService.get() == ShutdownSystem.this) {
        try {
          Thread.interrupted();
          LockSupport.park();
        } catch (Exception e) {
        }
      }

      ExitCode exitCode = _shutdownExitCode.get();
      
      if (exitCode != null) {
        shutdownImpl(exitCode);
      }
    }
  }

  class FailSafeMemoryFreeThread extends Thread {
    private volatile boolean _isShutdown;

    FailSafeMemoryFreeThread()
    {
      setName("resin-fail-safe-memory-free");
      setDaemon(true);
    }

    /**
     * Starts the shutdown sequence
     */
    void startShutdown()
    {
      _isShutdown = true;

      LockSupport.unpark(this);
    }

    @Override
    public void run()
    {
      while (! _isShutdown && _lifecycle.isActive()) {
        try {
          Thread.interrupted();
          LockSupport.park();
        } catch (Exception e) {
        }
      }
      
      for (int i = 0; i < _memoryFreeTasks.size(); i++) {
        Runnable task = _memoryFreeTasks.get(i);
        
        try {
          task.run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      
      if (! _lifecycle.isActive())
        return;
    }
  }

  class FailSafeHaltThread extends Thread {
    private volatile boolean _isShutdown;
    private volatile long _period = -1;

    FailSafeHaltThread()
    {
      setName("resin-fail-safe-halt");
      setDaemon(true);
    }

    /**
     * Starts the shutdown sequence
     */
    void startShutdown()
    {
      startShutdown(-1);
    }
    
    /**
     * Starts the shutdown sequence
     */
    void startShutdown(long period)
    {
      _isShutdown = true;
      _period = period;

      wake();
    }
    
    void wake()
    {
      LockSupport.unpark(this);
    }

    @Override
    public void run()
    {
      while (! _isShutdown && _lifecycle.isActive()) {
        try {
          Thread.interrupted();
          LockSupport.park();
        } catch (Exception e) {
        }
      }
      
      if (! _lifecycle.isActive())
        return;
      
      waitForShutdown(_period);

      if (_lifecycle.isActive()) {
        Runtime.getRuntime().halt(ExitCode.FAIL_SAFE_HALT.ordinal());
      }
    }
  }
}
