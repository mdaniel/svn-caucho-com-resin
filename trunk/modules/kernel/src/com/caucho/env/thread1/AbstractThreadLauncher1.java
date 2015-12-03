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

package com.caucho.env.thread1;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.thread1.AbstractTaskWorker1;
import com.caucho.inject.Module;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

@Module
abstract public class AbstractThreadLauncher1 extends AbstractTaskWorker1 {
  private static final L10N L = new L10N(AbstractThreadLauncher1.class);
  private static final Logger log
    = Logger.getLogger(AbstractThreadLauncher1.class.getName());
  
  private static final long LAUNCHER_TIMEOUT = 60000L;

  private static final int DEFAULT_THREAD_MAX = 8192;
  private static final int DEFAULT_IDLE_MIN = 2;
  private static final int DEFAULT_IDLE_MAX = Integer.MAX_VALUE / 2;

  private static final long DEFAULT_IDLE_TIMEOUT = 60000L;
  
  // configuration items

  private int _threadMax = DEFAULT_THREAD_MAX;
  private int _idleMin = DEFAULT_IDLE_MIN;
  private int _idleMax = DEFAULT_IDLE_MAX;
  private long _idleTimeout = DEFAULT_IDLE_TIMEOUT;
  
  private long _throttlePeriod = 1000L;
  private int _throttleLimit = 100;
  private long _throttleSleep = 0;
  
  //
  // thread max and thread lifetime counts
  //

  private final AtomicInteger _threadCount = new AtomicInteger();
  private final AtomicInteger _idleCount = new AtomicInteger();
  
  // number of threads which are in the process of starting
  private final AtomicInteger _startingCount = new AtomicInteger();
  
  private final AtomicLong _createCountTotal = new AtomicLong();
  // private final AtomicLong _overflowCount = new AtomicLong();

  // next time when an idle thread can expire
  private final AtomicLong _threadIdleExpireTime = new AtomicLong();
  
  // throttle/warning counters
  
  private long _throttleTimestamp;
  private long _throttleCount;
  private boolean _isThrottle;
  
  private final AtomicInteger _gId = new AtomicInteger();
  
  private final Lifecycle _lifecycle;

  protected AbstractThreadLauncher1()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  protected AbstractThreadLauncher1(ClassLoader loader)
  {
    super(loader);
    
    setWorkerIdleTimeout(LAUNCHER_TIMEOUT);
    
    _lifecycle = new Lifecycle();
  }
  
  //
  // abstract to be implemented by children
  //

  abstract protected void launchChildThread(int id);

  //
  // Configuration properties
  //

  /**
   * Sets the maximum number of threads.
   */
  public void setThreadMax(int max)
  {
    if (max < _idleMin)
      throw new ConfigException(L.l("IdleMin ({0}) must be less than ThreadMax ({1})", _idleMin, max));
    if (max < 1)
      throw new ConfigException(L.l("ThreadMax ({0}) must be greater than zero", 
                                    max));

    _threadMax = max;

    update();
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
  public void setIdleMin(int min)
  {
    if (_threadMax < min)
      throw new ConfigException(L.l("IdleMin ({0}) must be less than ThreadMax ({1})", min, _threadMax));
    if (min <= 0)
      throw new ConfigException(L.l("IdleMin ({0}) must be greater than 0.", min));

    _idleMin = min;
    
    update();
  }

  /**
   * Gets the minimum number of idle threads.
   */
  public int getIdleMin()
  {
    return _idleMin;
  }

  /**
   * Sets the maximum number of idle threads.
   */
  public void setIdleMax(int max)
  {
    if (_threadMax < max)
      throw new ConfigException(L.l("IdleMax ({0}) must be less than ThreadMax ({1})", max, _threadMax));
    if (max <= 0)
      throw new ConfigException(L.l("IdleMax ({0}) must be greater than 0.", max));

    _idleMax = max;

    update();
  }

  /**
   * Gets the maximum number of idle threads.
   */
  public int getIdleMax()
  {
    return _idleMax;
  }
  
  /**
   * Sets the idle timeout
   */
  public void setIdleTimeout(long timeout)
  {
    _idleTimeout = timeout;
  }
  
  /**
   * Returns the idle timeout.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }
  
  protected boolean isEnable()
  {
    return _lifecycle.isActive();
  }
  
  //
  // Throttle configuration
  //
  
  /**
   * Sets the throttle period.
   */
  public void setThrottlePeriod(long period)
  {
    _throttlePeriod = period;
  }
  
  /**
   * Sets the throttle limit.
   */
  public void setThrottleLimit(int limit)
  {
    _throttleLimit = limit;
  }
  
  /**
   * Sets the throttle sleep time.
   */
  public void setThrottleSleepTime(long period)
  {
    _throttleSleep = period;
  }
  
  //
  // lifecycle method
  //
  
  public void start()
  {
    _lifecycle.toActive();
    
    wake();
  }
  
  @Override
  public void destroy()
  {
    super.destroy();
    
    _lifecycle.toDestroy();
  }
  
  //
  // child thread callbacks
  //

  public boolean isThreadMax()
  {
    return _threadMax <= _threadCount.get() && _startingCount.get() == 0;
  }
  
  /**
   * Thread activity management
   */
  public void onChildThreadBegin()
  {
    _threadCount.incrementAndGet();
    
    int startCount = _startingCount.decrementAndGet();

    if (startCount < 0) {
      _startingCount.set(0);
      
      new IllegalStateException().printStackTrace();
    }

    _createCountTotal.incrementAndGet();
  }
  
  /**
   * Resume a child, i.e. start an active thread from an external source.
   */
  public void onChildThreadResume()
  {
    _threadCount.incrementAndGet();
  }
  
  /**
   * Thread activity management
   */
  public void onChildThreadEnd()
  {
    if (_threadMax <= _threadCount.getAndDecrement()) {
      wake();
    }

    if (_idleCount.get() <= _idleMin) {
      wake();
    }
  }
  
  //
  // idle management
  //
  
  /**
   * Returns true if the thread should expire instead of going to the idle state.
   */
  public boolean isIdleExpire()
  {
    if (! _lifecycle.isActive())
      return true;
    
    long now = getCurrentTimeActual();
    
    long idleExpire = _threadIdleExpireTime.get();
    
    int idleCount = _idleCount.get();

    // if idle queue is full and the expire is set, return and exit
    if (_idleMin < idleCount) {
      long nextIdleExpire = now + _idleTimeout;

      if (_idleMax < idleCount 
          && _idleMin < _idleMax) {
        /*
                && nextIdleExpire - idleExpire > 100
          && _threadIdleExpireTime.compareAndSet(idleExpire, nextIdleExpire)) {
          */
        _threadIdleExpireTime.compareAndSet(idleExpire, nextIdleExpire);
        
        return true;
      }
      else if (idleExpire < now
               && _threadIdleExpireTime.compareAndSet(idleExpire,
                                                      nextIdleExpire)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Start the idle if the child idle is less than idle max.
   */
  
  public boolean childIdleBegin()
  {
    int idleCount;
    
    do {
      idleCount = _idleCount.get();
      
      if (isIdleExpire())
        return false;
    } while (! _idleCount.compareAndSet(idleCount, idleCount + 1));
    
    return true;
  }
  
  /**
   * Called by the thread before going into the idle state.
   */
  
  public void onChildIdleBegin()
  {
    _idleCount.incrementAndGet();
  }
  
  /**
   * Called by the thread after exiting the idle state.
   */
  public void onChildIdleEnd()
  {
    int idleCount = _idleCount.decrementAndGet();

    if (idleCount <= _idleMin) {
      updateIdleExpireTime(getCurrentTimeActual());

      wake();
    }
  }

  /**
   * updates the thread idle expire time.
   */
  protected void updateIdleExpireTime(long now)
  {
    _threadIdleExpireTime.set(now + _idleTimeout);
  }
   
  //
  // implementation methods
  //

  /**
   * Checks if the launcher should start another thread.
   */
  protected boolean doStart()
  {
    if (! _lifecycle.isActive())
      return false;
    
    if (! isEnable())
      return false;
    
    int startingCount = _startingCount.getAndIncrement();

    int threadCount = _threadCount.get() + startingCount;

    if (_threadMax < threadCount) {
      _startingCount.decrementAndGet();
      
      onThreadMax();
      
      return false;
    }
    else if (isIdleTooLow(startingCount)) {
      return true;
    }
    else {
      _startingCount.decrementAndGet();
      
      return false;
    }
  }
  
  private void onStartFail()
  {
    _startingCount.getAndDecrement();
  }
  
  protected boolean isIdleTooLow(int startingCount)
  {
    return (_idleCount.get() + startingCount <= _idleMin);
  }
  
  @Override
  protected boolean isPermanent()
  {
    return true;
  }

  protected void update()
  {
    long now = getCurrentTimeActual();
    
    _threadIdleExpireTime.set(now + _idleTimeout);
    wake();
  }

  /**
   * Starts a new connection
   */
  private void startConnection()
  {
    while (doStart()) {
      boolean isValid = false;

      try {
        long now = getCurrentTimeActual();

        updateIdleExpireTime(now);

        int id = _gId.incrementAndGet();
        
        updateThrottle();
        
        launchChildThread(id);
        
        isValid = true;
      } finally {
        if (! isValid)
          onStartFail();
      }
    }
  }
  
  /**
   * Throttle the thread creation, so only 100 threads/sec (default) can
   * be created.
   */
  protected void updateThrottle()
  {
    long now = CurrentTime.getCurrentTimeActual();
    
    if (_throttleTimestamp + _throttlePeriod < now) {
      _throttleTimestamp = now;
      _throttleCount = 1;
      _isThrottle = false;
      
      return;
    }
    
    _throttleCount++;
    
    if (_throttleCount < _throttleLimit) {
      return;
    }
    System.out.println("THROTTLE: " + _throttleCount + " " + this);
    if (! _isThrottle) {
      _isThrottle = true;
      
      String msg = (this + " " + _throttleCount
                    + " threads created in " + _throttlePeriod + "ms"
                    + " sleep=" + _throttleSleep + "ms");
      
      onThrottle(msg);
    }
    
    if (_throttleSleep > 0) {
      try {
        Thread.sleep(_throttleSleep);
      } catch (Exception e) {
      }
    }
  }
  
  protected void onThreadMax()
  {
  }
  
  protected void onThrottle(String msg)
  {
    log.warning(msg);
  }
  
  //
  // statistics
  //

  public int getThreadCount()
  {
    return _threadCount.get();
  }

  public int getIdleCount()
  {
    return _idleCount.get();
  }

  public int getStartingCount()
  {
    return _startingCount.get();
  }

  public long getCreateCountTotal()
  {
    return _createCountTotal.get();
  }
  
  @Override
  public long runTask()
  {
    startConnection();
    
    return -1;
  }
}
