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

package com.caucho.env.deploy;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.util.ConcurrentArrayList;

/**
 * Interface for a service registered with the Resin Server.
 */
public class DeployTagItem {
  private final String _tag;

  private final Lifecycle _lifecycle = new Lifecycle();

  private final ConcurrentArrayList<DeployNotificationListener> _listeners
    = new ConcurrentArrayList<DeployNotificationListener>(
    DeployNotificationListener.class);

  private final ConcurrentArrayList<DeployActionHandler> _actionHandlers
    = new ConcurrentArrayList<DeployActionHandler>(DeployActionHandler.class);

  private Throwable _deployException;

  public DeployTagItem(String tag)
  {
    _tag = tag;
  }
  
  /**
   * Returns the tag name.
   */
  public String getTag()
  {
    return _tag;
  }
  
  /**
   * Change the state to an active state.
   */
  public void toStart()
  {
    if (! _lifecycle.toActive())
      return;

    _deployException = null;

    for (DeployActionHandler handler : _actionHandlers.toArray()) {
      handler.toStart();
    }
  }

  /**
   * Change the state to the stopped state.
   */
  public void toStop()
  {
    if (! _lifecycle.toStop())
      return;

    for (DeployActionHandler handler : _actionHandlers.toArray()) {
      handler.toStop();
    }
  }

  public void toRestart()
  {
    _lifecycle.toStop();

    if (!_lifecycle.toActive())
      return;

    for (DeployActionHandler handler : _actionHandlers) {
      handler.toRestart();
    }
  }

  public void onStart()
  {
    _lifecycle.toActive();

    _deployException = null;
    
    for (DeployNotificationListener listener : _listeners.toArray()) {
      listener.onStart();
    }
  }

  public void onStop()
  {
    _lifecycle.toStop();

    _deployException = null;
    
    for (DeployNotificationListener listener : _listeners.toArray()) {
      listener.onStop();
    }
  }

  public LifecycleState getState()
  {
    return _lifecycle.getState();
  }

  /**
   * Returns the lifecycle state of the item.
   */
  public String getStateName()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Change the state to an error state.
   */
  public void toError(Throwable exn)
  {
    _lifecycle.toError();
    
    _deployException = exn;
  }
  
  /**
   * Returns the deployment exception
   */
  public Throwable getDeployException()
  {
    return _deployException;
  }
  
  public void addNotificationListener(DeployNotificationListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    _listeners.add(listener);
  }
  
  public void removeNotificationListener(DeployNotificationListener listener)
  {
    _listeners.remove(listener);
  }

  public void addActionHandler(DeployActionHandler handler)
  {
    _actionHandlers.add(handler);
  }

  public void removeActionHandler(DeployActionHandler handler)
  {
    _actionHandlers.remove(handler);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _tag + "]";
  }

}
