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

package com.caucho.server.distcache;

import java.util.concurrent.locks.LockSupport;

/**
 * callback listener for a load complete
 */
class DistCacheEntryLoadCallback implements CacheLoaderCallback {
  private volatile Thread _thread;
  private volatile boolean _isDone;
  private volatile boolean _isValue;
  
  DistCacheEntryLoadCallback()
  {
  }

  @Override
  public void onLoad(DistCacheEntry entry, Object value)
  {
    // cloud/6900 vs server/0180
    
    if (value != null) {
      entry.putInternal(value);
    }
    
    _isValue = ! entry.getMnodeEntry().isValueNull();
    _isDone = true;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }

  @Override
  public void onLoadFail(DistCacheEntry entry)
  {
    _isDone = true;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }
  
  boolean get()
  {
    _thread = Thread.currentThread();
    
    try {
      long expire = System.currentTimeMillis() + 60 * 1000L;
      
      while (! _isDone && System.currentTimeMillis() < expire) {
        LockSupport.parkUntil(expire);
      }
      
      return _isValue;
    } finally {
      _thread = null;
    }
  }
}
