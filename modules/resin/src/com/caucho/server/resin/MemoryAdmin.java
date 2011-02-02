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

package com.caucho.server.resin;

import com.caucho.management.server.*;
import com.caucho.util.MemoryPoolAdapter;

public class MemoryAdmin extends AbstractManagedObject 
  implements MemoryMXBean
{
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
    return _memoryPoolAdapter.getCodeCacheCommitted();
  }

  public long getCodeCacheMax()
  {
    return _memoryPoolAdapter.getCodeCacheMax();
  }

  public long getCodeCacheUsed()
  {
    return _memoryPoolAdapter.getCodeCacheUsed();
  }

  public long getCodeCacheFree()
  {
    return _memoryPoolAdapter.getCodeCacheFree();
  }

  public long getEdenCommitted()
  {
    return _memoryPoolAdapter.getEdenCommitted();
  }

  public long getEdenMax()
  {
    return _memoryPoolAdapter.getEdenMax();
  }

  public long getEdenUsed()
  {
    return _memoryPoolAdapter.getEdenUsed();
  }

  public long getEdenFree()
  {
    return _memoryPoolAdapter.getEdenFree();
  }

  public long getPermGenCommitted()
  {
    return _memoryPoolAdapter.getPermGenCommitted();
  }

  public long getPermGenMax()
  {
    return _memoryPoolAdapter.getPermGenMax();
  }

  public long getPermGenUsed()
  {
    return _memoryPoolAdapter.getPermGenUsed();
  }

  public long getPermGenFree()
  {
    return _memoryPoolAdapter.getPermGenFree();
  }

  public long getSurvivorCommitted()
  {
    return _memoryPoolAdapter.getSurvivorCommitted();
  }

  public long getSurvivorMax()
  {
    return _memoryPoolAdapter.getSurvivorMax();
  }

  public long getSurvivorUsed()
  {
    return _memoryPoolAdapter.getSurvivorUsed();
  }

  public long getSurvivorFree()
  {
    return _memoryPoolAdapter.getSurvivorFree();
  }

  public long getTenuredCommitted()
  {
    return _memoryPoolAdapter.getTenuredCommitted();
  }

  public long getTenuredMax()
  {
    return _memoryPoolAdapter.getTenuredMax();
  }

  public long getTenuredUsed()
  {
    return _memoryPoolAdapter.getTenuredUsed();
  }

  public long getTenuredFree()
  {
    return _memoryPoolAdapter.getTenuredFree();
  }
}
