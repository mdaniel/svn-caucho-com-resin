/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.lifecycle;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.Alarm;

/**
 * Lifecycle class.
 */
public final class Lifecycle implements LifecycleState {
  private final Logger _log;
  private String _name;
  private Level _level = Level.FINE;
  
  private int _state;

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
   * Creates an lifecycle with logger and name.
   */
  public Lifecycle(Logger log, String name, Level level)
  {
    _log = log;
    _name = name;
    _level = level;
  }

  /**
   * Gets the lifecycle name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the lifecycle name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the lifecycle level.
   */
  public Level getLevel()
  {
    return _level;
  }

  /**
   * Sets the lifecycle level.
   */
  public void setLevel(Level level)
  {
    _level = level;
  }
  
  /**
   * Returns the current state.
   */
  public int getState()
  {
    return _state;
  }

  /**
   * Returns the current state name.
   */
  public String getStateName()
  {
    switch (_state) {
    case IS_NEW:
      return "new";
    case IS_INITIALIZING:
      return "initializing";
    case IS_INIT:
      return "init";
    case IS_STARTING:
      return "starting";
    case IS_FAILED:
      return "failed";
    case IS_ACTIVE:
      return "active";
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
   * Returns true for the failed state.
   */
  public boolean isFailed()
  {
    return _state == IS_FAILED;
  }

  /**
   * Returns true if the state is stopping.
   */
  public boolean isStopped()
  {
    return IS_STOPPING <= _state;
  }

  /**
   * Returns true if the state is stopping.
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
    
    _state = IS_INITIALIZING;

    if (_log != null && _log.isLoggable(Level.FINE))
      _log.fine(_name + " initializing");

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

     _state = IS_INIT;

     if (_log != null && _log.isLoggable(Level.FINE))
       _log.fine(_name + " initialized");

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
       _state = IS_INIT;
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
      _state = IS_STARTING;

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " starting");

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
    if (_state < IS_ACTIVE || _state == IS_STOPPED) {
      _state = IS_ACTIVE;

      if (_log != null && _log.isLoggable(Level.FINE))
	_log.fine(_name + " active");

      notifyAll();

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
  public synchronized boolean toFailed()
  {
    if (_state != IS_DESTROYED && _state != IS_FAILED) {
      _state = IS_FAILED;

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " failed");

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
      _state = IS_STOPPING;

      if (_log != null && _log.isLoggable(_level))
	_log.log(_level, _name + " stopping");

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
      else if (_log.isLoggable(Level.FINE))
	_log.fine(_name + " stopped");
      
      _state = IS_STOPPED;

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
      _state = IS_DESTROYING;

      if (_log != null && _log.isLoggable(Level.FINE))
	_log.fine(_name + " destroying");

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
      _state = IS_DESTROYED;

      if (_log != null && _log.isLoggable(Level.FINE))
	_log.fine(_name + " destroyed");

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
