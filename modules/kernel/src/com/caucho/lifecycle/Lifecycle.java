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

package com.caucho.lifecycle;

import com.caucho.util.Alarm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lifecycle class.
 */
public final class Lifecycle implements LifecycleState {
  private final Logger _log;
  private String _name;
  
  private Level _level = Level.FINE;
  private Level _lowLevel = Level.FINER;
  
  private int _state;

  private long _activeCount;
  private long _failCount;

  private long _lastFailTime;
  private long _lastChangeTime;

  private ArrayList<WeakReference<LifecycleListener>> _listeners;

  /**
   * Creates an anonymous lifecycle.
   */
  public Lifecycle()
  {
    _log = null;
  }

  /**
   * Creates an lifecycle with logger and name.
   */
  public Lifecycle(Logger log)
  {
    _log = log;
  }

  /**
   * Creates an lifecycle with logger and name.
   */
  public Lifecycle(Logger log, String name)
  {
    _log = log;
    _name = name;
  }

  /**
   * Creates an lifecycle with logger,  a name, and a level.
   */
  public Lifecycle(Logger log, String name, Level level)
  {
    _log = log;
    _name = name;
    
    setLevel(level);
  }

  /**
   * Gets the lifecycle name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the lifecycle name, and the level to Level.INFO.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the lifecycle logging level.
   */
  public Level getLevel()
  {
    return _level;
  }

  /**
   * Sets the lifecycle logging level.
   */
  public void setLevel(Level level)
  {
    _level = level;

    if (level.intValue() < _lowLevel.intValue())
      _lowLevel = level;
  }

  /**
   * Adds a listener to detect lifecycle changes.
   */
  public void addListener(LifecycleListener listener)
  {
    synchronized (this) {
      if (isDestroyed()) {
        IllegalStateException e = new IllegalStateException("attempted to add listener to a destroyed lifecyle " + this);

        if (_log != null)
          _log.log(Level.WARNING, e.toString(), e);
        else
          Logger.getLogger(Lifecycle.class.getName()).log(Level.WARNING, e.toString(), e);

        return;
      }

      if (_listeners == null)
        _listeners = new ArrayList<WeakReference<LifecycleListener>>();

      for (int i = _listeners.size() - 1; i >= 0; i--) {
        LifecycleListener oldListener = _listeners.get(i).get();

        if (listener == oldListener)
          return;
        else if (oldListener == null)
          _listeners.remove(i);
      }

      _listeners.add(new WeakReference<LifecycleListener>(listener));
    }
  }

  /**
   * Removes a listener.
   */
  public void removeListener(LifecycleListener listener)
  {
    synchronized (this) {

      if (_listeners == null)
        return;

      for (int i = _listeners.size() - 1; i >= 0; i--) {
        LifecycleListener oldListener = _listeners.get(i).get();

        if (listener == oldListener) {
          _listeners.remove(i);

          return;
        }
        else if (oldListener == null)
          _listeners.remove(i);
      }
    }
  }

  /**
   * Returns the listeners.
   */
  private void notifyListeners(int oldState, int newState)
  {
    synchronized (this) {
      if (_listeners == null) {
        return;
      }
      else {
        for (int i = 0; i < _listeners.size(); i++) {
          LifecycleListener listener = _listeners.get(i).get();

          if (listener != null) {
            listener.lifecycleEvent(oldState, newState);
          }
          else {
            _listeners.remove(i);
            i--;
          }
        }
      }
    }
  }

  /**
   * Returns the current state.
   */
  public int getState()
  {
    return _state;
  }

  /**
   * Returns the state name for the passed state.
   */
  public static String getStateName(int state)
  {
    switch (state) {
      case IS_NEW:
        return "new";
      case IS_INITIALIZING:
        return "initializing";
      case IS_INIT:
        return "init";
      case IS_STARTING:
        return "starting";
      case IS_ACTIVE:
        return "active";
      case IS_FAILED:
        return "failed";
      case IS_STOPPING:
        return "stopping";
      case IS_STOPPED:
        return "stopped";
      case IS_DESTROYING:
        return "destroying";
      case IS_DESTROYED:
        return "destroyed";
      default:
        return "unknown";
    }
  }

  /**
   * Returns the current state name.
   */
  public String getStateName()
  {
    return getStateName(_state);
  }

  /**
   * Returns the last lifecycle change time.
   */
  public long getLastChangeTime()
  {
    return _lastChangeTime;
  }

  /**
   * Returns the last failure time.
   */
  public long getLastFailTime()
  {
    return _lastFailTime;
  }

  /**
   * Returns the number of times the lifecycle has switched to active.
   */
  public long getActiveCount()
  {
    return _activeCount;
  }

  /**
   * Returns the number of times the lifecycle has switched to failing.
   */
  public long getFailCount()
  {
    return _failCount;
  }

  /**
   * Returns true for the initializing state.
   */
  public boolean isInitializing()
  {
    return _state == IS_INITIALIZING;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isInit()
  {
    return _state == IS_INIT;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isBeforeInit()
  {
    return _state < IS_INIT;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isAfterInit()
  {
    return _state >= IS_INIT;
  }

  /**
   * Returns true if the service is starting.
   */
  public boolean isStarting()
  {
    return _state == IS_STARTING;
  }

  /**
   * Returns true if the service is starting.
   */
  public boolean isAfterStarting()
  {
    return _state >= IS_STARTING;
  }

  /**
   * Returns true for the warmup state.
   */
  public boolean isWarmup()
  {
    return _state == IS_WARMUP;
  }

  /**
   * Returns true for the initializing state.
   */
  public boolean isBeforeActive()
  {
    return _state < IS_ACTIVE;
  }

  /**
   * Returns true for the closing states
   */
  public boolean isAfterActive()
  {
    return IS_ACTIVE < _state;
  }

  /**
   * Wait for a period of time until the service starts.
   */
  public boolean waitForActive(long timeout)
  {
    if (_state == IS_ACTIVE)
      return true;
    
    long waitEnd = Alarm.getCurrentTime() + timeout;
    
    synchronized (this) {
      while (Alarm.getCurrentTime() < waitEnd) {
	if (_state == IS_ACTIVE)
	  return true;
	else if (IS_ACTIVE < _state)
	  return false;
	else if (Alarm.isTest())
	  return false;

	try {
	  wait(waitEnd - Alarm.getCurrentTime());
	} catch (InterruptedException e) {
	}
      }
    }
    
    return _state == IS_ACTIVE;
  }

  /**
   * Returns true for the active state.
   */
  public boolean isActive()
  {
    return _state == IS_ACTIVE;
  }

  /**
   * Returns true for the a runnable state, including warmup
   */
  public boolean isRunnable()
  {
    return IS_WARMUP <= _state && _state <= IS_ACTIVE;
  }

  /**
   * Returns true for the failed state.
   */
  public boolean isError()
  {
    return isFailed();
  }

  /**
   * Returns true for the failed state.
   */
  public boolean isFailed()
  {
    return _state == IS_FAILED;
  }

  /**
   * Returns true if the state is stopping.
   */
  public boolean isStopping()
  {
    return IS_STOPPING <= _state;
  }

  /**
   * Returns true if the state is stopping.
   */
  public boolean isStopped()
  {
    return IS_STOPPING <= _state;
  }

  /**
   * Returns true if the state is closed
   */
  public boolean isDestroying()
  {
    return IS_DESTROYING <= _state;
  }

  /**
   * Returns true if the state is closed
   */
  public boolean isDestroyed()
  {
    return IS_DESTROYED <= _state;
  }
  
  /**
   * Changes to the initializing state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toInitializing()
  {
    if (IS_INITIALIZING <= _state)
      return false;

    int oldState = _state;

    _state = IS_INITIALIZING;
    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_lowLevel))
      _log.log(_lowLevel, _name + " initializing");

    notifyListeners(oldState, _state);

    return true;
  }

  /**
    * Changes to the init state.
    *
    * @return true if the transition is allowed
    */
   public synchronized boolean toInit()
   {
     if (IS_INIT <= _state)
       return false;

     int oldState = _state;

     _state = IS_INIT;
     _lastChangeTime = Alarm.getCurrentTime();

     if (_log != null && _log.isLoggable(_lowLevel))
       _log.log(_lowLevel, _name + " initialized");

     notifyListeners(oldState, _state);

     return true;
   }
  /**
    * Changes to the init from the stopped state.
    *
    * @return true if the transition is allowed
    */
   public synchronized boolean toPostInit()
   {
     if (IS_STOPPED == _state) {
       int oldState = _state;

       _state = IS_INIT;
       _lastChangeTime = Alarm.getCurrentTime();

       notifyListeners(oldState, _state);

       return true;
     }
     else if (IS_INIT == _state) {
       return true;
     }

     return false;
   }

  /**
   * Changes to the starting state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toStarting()
  {
    if (_state < IS_STARTING || _state == IS_STOPPED) {
      int oldState = _state;

      _state = IS_STARTING;
       _lastChangeTime = Alarm.getCurrentTime();
       
      if (_log != null && _log.isLoggable(Level.FINE)
	  && _log.isLoggable(_level))
	_log.fine(_name + " starting");

      notifyListeners(oldState, _state);

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the active state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toActive()
  {
    if (_state < IS_ACTIVE || IS_FAILED <= _state && _state <= IS_STOPPED) {
      int oldState = _state;

      _state = IS_ACTIVE;
      _lastChangeTime = Alarm.getCurrentTime();
      _activeCount++;

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " active");

      notifyListeners(oldState, _state);

      notifyAll();

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the error state.
   *
   * @return true if the transition is allowed
   */
  public boolean toError()
  {
    return toFail();
  }
  
  /**
   * Changes to the failed state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toFail()
  {
    if (_state < IS_DESTROYING && _state != IS_FAILED) {
      int oldState = _state;

      _state = IS_FAILED;
      _lastChangeTime = Alarm.getCurrentTime();
      _failCount++;

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " error");

      notifyListeners(oldState, _state);

      notifyAll();

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the stopping state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toStopping()
  {
    if (_state < IS_STOPPING && _state != IS_STARTING) {
      int oldState = _state;

      _state = IS_STOPPING;
      _lastChangeTime = Alarm.getCurrentTime();

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " stopping");

      notifyListeners(oldState, _state);

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the stopped state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toStop()
  {
    if (_state < IS_STOPPED) {
      if (_log == null) {
      }
      else if (_state < IS_STOPPING && _log.isLoggable(_level))
	_log.log(_level, _name + " stopped");
      else if (_log.isLoggable(_lowLevel))
	_log.log(_lowLevel, _name + " stopped");
      
      int oldState = _state;

      _state = IS_STOPPED;
      _lastChangeTime = Alarm.getCurrentTime();

      notifyListeners(oldState, _state);

      notifyAll();

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the destroying state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toDestroying()
  {
    if (_state < IS_DESTROYING) {
      int oldState = _state;

      _state = IS_DESTROYING;
      _lastChangeTime = Alarm.getCurrentTime();

      if (_log != null && _log.isLoggable(_lowLevel))
	_log.log(_lowLevel, _name + " destroying");

      notifyListeners(oldState, _state);

      return true;
    }
    else
      return false;
  }
  
  /**
   * Changes to the closed state.
   *
   * @return true if the transition is allowed
   */
  public synchronized boolean toDestroy()
  {
    if (_state < IS_DESTROYED) {
      int oldState = _state;

      _state = IS_DESTROYED;
      _lastChangeTime = Alarm.getCurrentTime();

      if (_log != null && _log.isLoggable(_lowLevel))
	_log.log(_lowLevel, _name + " destroyed");

      notifyListeners(oldState, _state);

      notifyAll();

      return true;
    }
    else
      return false;
  }
  
  /**
   * Copies from a target state.
   *
   * @return true if the transition is allowed
   */
  public void copyState(Lifecycle source)
  {
    _state = source._state;
  }

  /**
   * Debug string value.
   */
  public String toString()
  {
    if (_name != null)
      return "Lifecycle[" + _name + ", " + getStateName() + "]";
    else
      return "Lifecycle[" + getStateName() + "]";
  }
}
