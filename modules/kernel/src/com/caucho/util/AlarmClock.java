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

package com.caucho.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Logger;

import com.caucho.env.thread.ThreadPool;

/**
 * The alarm class provides a lightweight event scheduler.  This allows
 * an objects to schedule a timeout without creating a new thread.
 *
 * <p>A separate thread periodically tests the queue for alarms ready.
 */
public class AlarmClock {
  private static final Logger log
    = Logger.getLogger(AlarmClock.class.getName());
  private static final int CLOCK_PERIOD = 60 * 1000;
  
  private Alarm []_clockArray = new Alarm[CLOCK_PERIOD];
  
  private AtomicLong _now = new AtomicLong();
  private AtomicLong _nextAlarmTime = new AtomicLong();
  
  private final ArrayList<Alarm> _currentAlarms = new ArrayList<Alarm>();
  
  private long _lastTime;
  
  private ArrayList<Alarm> _overflow
    = new ArrayList<Alarm>();
  
  private Object _lock = new Object();

  /**
   * Queue the alarm for wakeup.
   *
   * @param delta time in milliseconds to wake
   */
  boolean queueAt(Alarm alarm, long wakeTime)
  {
    boolean isEarliest = false;
    
    long prevNextAlarmTime;
    
    do {
      prevNextAlarmTime = _nextAlarmTime.get();
    } while (wakeTime > 0 && wakeTime < prevNextAlarmTime 
             && ! _nextAlarmTime.compareAndSet(prevNextAlarmTime, wakeTime));
    
    if (wakeTime < prevNextAlarmTime)
      isEarliest = true;
    
    long oldWakeTime = alarm.getAndSetWakeTime(wakeTime);
    
    if (oldWakeTime == wakeTime)
      return false;
    
    if (oldWakeTime > 0) {
      if (! dequeueImpl(alarm)) {
        /*
        System.out.println("FAIL: " + oldWakeTime + " " + wakeTime
                           + " " + alarm);
                           */
      }
    }
    
    if (wakeTime <= 0)
      return false;
    
    long now = _now.get();
    
    if (wakeTime <= now) {
      queueCurrent(alarm);
      return true;
    }
    
    synchronized (_lock) {
      if (alarm.getBucket() >= 0)
        return false;
      
      int bucket = (int) (wakeTime % CLOCK_PERIOD);
      alarm.setBucket(bucket);
    
      Alarm top = _clockArray[bucket];
      alarm.setNext(top);
      _clockArray[bucket] = alarm;
    }
    
    now = _now.get();
    
    long nextWakeTime = alarm.getWakeTime();
    
    if (nextWakeTime != wakeTime || wakeTime < now) {
      dequeueImpl(alarm);
      queueCurrent(alarm);
    }
    
    return isEarliest;
  }
  
  private void queueCurrent(Alarm alarm)
  {
    synchronized (_currentAlarms) {
      _currentAlarms.add(alarm);
    }
  }
  
  void dequeue(Alarm alarm)
  {
    long oldWakeTime = alarm.getAndSetWakeTime(0);
    
    if (alarm.getBucket() >= 0)
      dequeueImpl(alarm);
  }
  
  private boolean dequeueImpl(Alarm alarm)
  {
     synchronized (_lock) {
      int bucket = alarm.getBucket();
      Alarm next = alarm.getNext();
      
      alarm.setBucket(-1);
      alarm.setNext(null);
      
      if (bucket < 0)
        return false;
      
      Alarm head = _clockArray[bucket];
      
      if (head == null)
        return false;
      
      if (head == alarm) {
        _clockArray[bucket] = next;
        return true;
      }
      
      Alarm prev = head;
      Alarm ptr = prev.getNext();

      while (ptr != null) {
        if (ptr == alarm) {
          prev.setNext(next);
          return true;
        }
        
        prev = ptr;
        ptr = ptr.getNext();
      }
    }
    
    return false;
  }
  
  private Alarm extractNextAlarm(int bucket, long time, boolean isTest)
  {
    if (_clockArray[bucket] == null)
      return null;
    
    synchronized (_lock) {
      Alarm ptr = _clockArray[bucket];
      Alarm prev = null;
      
      while (ptr != null) {
        Alarm next = ptr.getNext();
        
        long wakeTime = ptr.getWakeTime();
        
        if (wakeTime <= time) {
          ptr.setNext(null);
          ptr.setBucket(-1);
          
          if (prev != null) {
            prev.setNext(next);
          }
          else {
            _clockArray[bucket] = next;
          }
          
          return ptr;
        }
        
        prev = ptr;
        ptr = next;
      }
    }
    
    return null;
  }

  /**
   * Returns the next alarm ready to run
   */
  public void extractAlarm(long now, boolean isTest)
  {
    long lastTime = _now.getAndSet(now);
    
    _nextAlarmTime.set(now + CLOCK_PERIOD);
    
    int delta;
    
    if (CLOCK_PERIOD <= now - lastTime)
      delta = CLOCK_PERIOD;
    else
      delta = (int) (now - lastTime);

    Alarm alarm;
    
    for (int i = 0; i <= delta; i++) {
      long time = lastTime + i;
      int bucket = (int) (time % CLOCK_PERIOD);

      while ((alarm = extractNextAlarm(bucket, now, isTest)) != null) {
        dispatch(alarm, now, isTest);
      }
    }
    
    while ((alarm = extractNextCurrentAlarm()) != null) {
      dispatch(alarm, now, isTest);
    }
    
    updateNextAlarmTime(now);
    
    _lastTime = now;
  }
  
  private Alarm extractNextCurrentAlarm()
  {
    if (_currentAlarms.size() == 0)
      return null;
    
    synchronized (_currentAlarms) {
      if (_currentAlarms.size() > 0) {
        Alarm alarm = _currentAlarms.remove(_currentAlarms.size() - 1);
        return alarm;
      }
      
      return null;
    }
  }
  
  private void updateNextAlarmTime(long now)
  {
    long nextTime = _nextAlarmTime.get();
    
    long delta = nextTime - now;
    
    for (int i = 0; i < delta; i++) {
      long time = now + i;
      
      int bucket = (int) (time % CLOCK_PERIOD);
      
      if (_clockArray[bucket] != null) {
        long wakeTime = time;
          
        while (wakeTime < nextTime) {
          if (_nextAlarmTime.compareAndSet(nextTime, wakeTime))
            return;
          
          nextTime = _nextAlarmTime.get();
        }
      }
    }

  }
  
  private void dispatch(Alarm alarm, long now, boolean isTest)
  {
    boolean isStressTest = false;
    
    if (isStressTest)
      now = Alarm.getExactTime();
    else
      now = Alarm.getCurrentTime();
    
    long wakeTime = alarm.getAndSetWakeTime(0);

    long delta = now - wakeTime;

    if (delta > 10000) {
      log.warning(this + " slow alarm " + alarm + " " + delta + "ms"
                  + " coordinator-delta " + (now - _lastTime) + "ms");
    }
    else if (isStressTest && delta > 100) {
      System.out.println(this + " slow alarm " + alarm + " " + delta
                         + " coordinator-delta " + (now - _lastTime) + "ms");
    }

    if (isTest) {
      try {
        alarm.run();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    else if (alarm.isPriority())
      ThreadPool.getThreadPool().schedulePriority(alarm);
    else
      ThreadPool.getThreadPool().schedule(alarm);

  }

  /**
   * Returns the next alarm ready to run
   */
  long getNextAlarmTime()
  {
    return _nextAlarmTime.get();
  }

  // test

  void testClear()
  {
    _now.set(0);
    _nextAlarmTime.set(0);
    
    synchronized (_lock) {
      _currentAlarms.clear();
    
      for (int i = CLOCK_PERIOD - 1; i >= 0; i--) {
        Alarm alarm = _clockArray[i];
        _clockArray[i] = null;
      
        while (alarm != null) {
          Alarm next = alarm.getNext();
          alarm.setNext(null);
          alarm.setBucket(-1);
          alarm.setWakeTime(alarm.getWakeTime(), 0);
          alarm = next;
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
