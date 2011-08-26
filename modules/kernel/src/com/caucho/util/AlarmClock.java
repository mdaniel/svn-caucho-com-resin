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
  
  private AtomicReferenceArray<Alarm> _clockArray
    = new AtomicReferenceArray<Alarm>(CLOCK_PERIOD);
  
  private AtomicLong _now = new AtomicLong();
  private AtomicLong _nextAlarmTime = new AtomicLong();
  
  private AtomicReference<Alarm> _currentAlarms = new AtomicReference<Alarm>();
  
  private long _lastTime;
  
  private ArrayList<Alarm> _overflow
    = new ArrayList<Alarm>();

  /**
   * Queue the alarm for wakeup.
   *
   * @param delta time in milliseconds to wake
   */
  boolean queueAt(Alarm alarm, long wakeTime)
  {
    if (wakeTime <= 0)
      return false;
    
    boolean isEarliest = false;
    
    long prevNextAlarmTime;
    
    do {
      prevNextAlarmTime = _nextAlarmTime.get();
    } while (wakeTime < prevNextAlarmTime 
             && ! _nextAlarmTime.compareAndSet(prevNextAlarmTime, wakeTime));
    
    if (wakeTime < prevNextAlarmTime)
      isEarliest = true;
    
    long oldWakeTime = alarm.getAndSetWakeTime(wakeTime);
    
    dequeueImpl(alarm, oldWakeTime);
    
    long now = _now.get();
    
    if (wakeTime <= now) {
      queueCurrent(alarm);
    }
    
    int bucket = (int) (wakeTime % CLOCK_PERIOD);
    
    Alarm top;
    do {
      top = _clockArray.get(bucket);
      alarm.setNext(top);
    } while (! _clockArray.compareAndSet(bucket, top, alarm));
    
    now = _now.get();
    wakeTime = alarm.getWakeTime();
    
    if (wakeTime > 0 && wakeTime < now) {
      dequeueImpl(alarm, wakeTime);
      queueCurrent(alarm);
    }
    
    return isEarliest;
  }
  
  private void queueCurrent(Alarm alarm)
  {
    Alarm top;
    
    do {
      top = _currentAlarms.get();
      alarm.setNext(top);
    } while (! _currentAlarms.compareAndSet(top, alarm));
  }
  
  void dequeue(Alarm alarm)
  {
    long oldWakeTime = alarm.getAndSetWakeTime(0);
    
    dequeueImpl(alarm, oldWakeTime);
  }
  
  private boolean dequeueImpl(Alarm alarm, long wakeTime)
  {
    if (wakeTime == 0)
      return false;
    
    int bucket = (int) (wakeTime % CLOCK_PERIOD);
    
    Alarm head;
    
    head_loop:
    while ((head = _clockArray.get(bucket)) != null) {
      Alarm next = head.getNext();
      
      if (head == alarm) {
        if (_clockArray.compareAndSet(bucket, alarm, next)) {
          head.setNext(next, null);
        
          return true;
        }
        else {
          continue head_loop;
        }
      }
      
      Alarm ptr = head;
      Alarm prev;
      while (next != null) {
        prev = ptr;
        ptr = next;
        
        next = ptr.getNext();
        
        if (ptr == alarm) {
          if (prev.setNext(ptr, next)) {
            ptr.setNext(next, null);
            
            return true;
          }
          else {
            continue head_loop;
          }
        }
      }
      
      return false;
    }
    
    return false;
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
    
    for (int i = 0; i <= delta; i++) {
      long time = lastTime + i;
      int bucket = (int) (time % CLOCK_PERIOD);

      Alarm alarm;
      
      while ((alarm = _clockArray.get(bucket)) != null) {
        Alarm next = alarm.getNext();
        
        boolean isReady = false;
        
        for (Alarm ptr = alarm; ptr != null; ptr = ptr.getNext()) {
          if (ptr.getWakeTime() <= now) {
            isReady = true;
            break;
          }
        }
        
        if (! isReady)
          break;
        
        if (_clockArray.compareAndSet(bucket, alarm, next)) {
          if (next != null)
            alarm.setNext(next, null);
          
          if (alarm.getWakeTime() <= now)
            dispatch(alarm, now, isTest);
          else {
            synchronized (_overflow) {
              _overflow.add(alarm);
            }
          }
        }
      }
      
      if (_overflow.size() > 0) {
        synchronized (_overflow) {
          for (int j = _overflow.size() - 1; j >= 0; j--) {
            Alarm overflowAlarm = _overflow.get(j);
        
            queueAt(overflowAlarm, overflowAlarm.getWakeTime());
          }
          
          _overflow.clear();
        }
      }
    }
    
    extractCurrentAlarms(now, isTest);
    
    updateNextAlarmTime(now);
    
    _lastTime = now;
  }
  
  private void extractCurrentAlarms(long now, boolean isTest)
  {
    Alarm alarm;
    while ((alarm = _currentAlarms.get()) != null) {
      Alarm next = alarm.getNext();
      
      if (_currentAlarms.compareAndSet(alarm, next)) {
        alarm.setNext(next, null);
        
        dispatch(alarm, now, isTest);
      }
    }
  }
  
  private void updateNextAlarmTime(long now)
  {
    long nextTime = _nextAlarmTime.get();
    
    long delta = nextTime - now;
    
    for (int i = 0; i < delta; i++) {
      long time = nextTime + i;
      
      int bucket = (int) (time % CLOCK_PERIOD);
      
      Alarm head;
      if ((head = _clockArray.get(bucket)) != null) {
        for (; head != null; head = head.getNext()) {
          long wakeTime = head.getWakeTime();
          
          if (wakeTime > 0 && wakeTime < nextTime) {
            _nextAlarmTime.compareAndSet(nextTime, wakeTime);
            return;
          }
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
    
    long wakeTime = alarm.getWakeTime();
    
    if (wakeTime <= 0 
        || now < wakeTime
        || ! alarm.setWakeTime(wakeTime, 0)) {
      return;
    }

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
  
  private void dispatchHead(Alarm alarm, long now)
  {
    Alarm next = null;
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
    _currentAlarms.set(null);
    
    for (int i = CLOCK_PERIOD - 1; i >= 0; i--) {
      _clockArray.set(i, null);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
