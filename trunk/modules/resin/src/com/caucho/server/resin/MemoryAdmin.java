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

package com.caucho.server.resin;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.MemoryMXBean;
import com.caucho.util.MemoryPoolAdapter;

public class MemoryAdmin extends AbstractManagedObject 
  implements MemoryMXBean
{
  private static final Logger log
    = Logger.getLogger(MemoryAdmin.class.getName());
  
  private MemoryPoolAdapter _memoryPoolAdapter;
  
  private MemoryAdmin()
  {
    _memoryPoolAdapter = new MemoryPoolAdapter();
    registerSelf();
  }

  static MemoryAdmin create()
  {
    return new MemoryAdmin();
  }

  @Override
  public String getName()
  {
    return null;
  }

  public long getCodeCacheCommitted()
  {
    try {
      return _memoryPoolAdapter.getCodeCacheCommitted();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheMax()
  {
    try {
      return _memoryPoolAdapter.getCodeCacheMax();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheUsed()
  {
    try {
      return _memoryPoolAdapter.getCodeCacheUsed();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheFree()
  {
    try {
      return _memoryPoolAdapter.getCodeCacheFree();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenCommitted()
  {
    try {
      return _memoryPoolAdapter.getEdenCommitted();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenMax()
  {
    try {
      return _memoryPoolAdapter.getEdenMax();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenUsed()
  {
    try {
      return _memoryPoolAdapter.getEdenUsed();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenFree()
  {
    try {
      return _memoryPoolAdapter.getEdenFree();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenCommitted()
  {
    try {
      return _memoryPoolAdapter.getPermGenCommitted();
    } catch (JMException e) {
      log.log(Level.FINEST, e.toString(), e);
      return -1;
    }
  }

  public long getPermGenMax()
  {
    try {
      return _memoryPoolAdapter.getPermGenMax();
    } catch (JMException e) {
      log.log(Level.FINEST, e.toString(), e);
      return -1;
    }
  }

  public long getPermGenUsed()
  {
    try {
      return _memoryPoolAdapter.getPermGenUsed();
    } catch (JMException e) {
      log.log(Level.FINEST, e.toString(), e);
      return -1;
    }
  }

  public long getPermGenFree()
  {
    try {
      return _memoryPoolAdapter.getPermGenFree();
    } catch (JMException e) {
      log.log(Level.FINEST, e.toString(), e);
      return -1;
    }
  }

  public long getSurvivorCommitted()
  {
    try {
      return _memoryPoolAdapter.getSurvivorCommitted();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorMax()
  {
    try {
      return _memoryPoolAdapter.getSurvivorMax();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorUsed()
  {
    try {
      return _memoryPoolAdapter.getSurvivorUsed();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorFree()
  {
    try {
      return _memoryPoolAdapter.getSurvivorFree();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredCommitted()
  {
    try {
      return _memoryPoolAdapter.getTenuredCommitted();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredMax()
  {
    try {
      return _memoryPoolAdapter.getTenuredMax();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredUsed()
  {
    try {
      return _memoryPoolAdapter.getTenuredUsed();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredFree()
  {
    try {
      return _memoryPoolAdapter.getTenuredFree();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getGarbageCollectionTime()
  {
    try {
      return _memoryPoolAdapter.getGarbageCollectionTime();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }

  public long getGarbageCollectionCount()
  {
    try {
      return _memoryPoolAdapter.getGarbageCollectionCount();
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
  }
}
