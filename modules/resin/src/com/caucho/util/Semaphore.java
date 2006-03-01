/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.util.logging.Logger;

/**
 * The Semaphore handles timed locks.
 */
public class Semaphore {
  private static final Logger log =
    Logger.getLogger(Semaphore.class.getName());
  
  private volatile int _permits;

  public Semaphore(int permits, boolean fair)
  {
    _permits = permits;
  }
  
  /**
   * Allocates the semaphore, returns true on success.
   */
  public boolean tryAcquire(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    long ms = unit.toMillis(timeout);
    long now = System.currentTimeMillis();
    long expire = ms + now;
    
    synchronized (this) {
      do {
	if (_permits > 0) {
	  _permits--;
	  return true;
	}

	now = System.currentTimeMillis();
	long delta = expire - now;
	if (delta > 0) {
	  wait(delta);

	  if (_permits > 0) {
	    _permits--;
	    return true;
	  }
	}
      } while (System.currentTimeMillis() < expire);
    }

    return false;
  }
  
  /**
   * Releases the permit.
   */
  public void release()
  {
    synchronized (this) {
      _permits++;

      notifyAll();
    }
  }
}
