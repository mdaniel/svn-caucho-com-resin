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

package com.caucho.make;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.loader.Environment;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.Dependency;

/**
 * A cached dependency only checks the dependency at an interval.
 */
abstract public class CachedDependency implements Dependency {
  private long _checkInterval;
  private AtomicLong _lastCheckTime = new AtomicLong();
  private boolean _isModified;

  public CachedDependency()
  {
    setCheckInterval(Environment.getDependencyCheckInterval());
  }
  
  /**
   * Gets the check interval.
   */
  public long getCheckInterval()
  {
    return _checkInterval;
  }

  /**
   * Gets the check interval.
   */
  public void setCheckInterval(long checkInterval)
  {
    _checkInterval = Math.max(0, checkInterval);
  }
  
  /**
   * Returns true if the underlying resource has changed.
   */
  @Override
  public final boolean isModified()
  {
    long now = CurrentTime.getCurrentTime();
    long lastCheckTime = _lastCheckTime.get();
    
    if (now <= lastCheckTime + _checkInterval) {
      return _isModified;
    }

    if (! _lastCheckTime.compareAndSet(lastCheckTime, now)) {
      return _isModified;
    }

    if (isModifiedImpl()) {
      _isModified = true;
      //_lastCheckTime.set(0);
    }
    else {
      _isModified = false;
    }

    return _isModified;
  }
  
  /**
   * Returns true if the underlying resource has changed.
   */
  abstract public boolean isModifiedImpl();
}
