/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.management.server;

import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Management interface for the block manager used by the proxy cache
 * and persistent sessions.
 *
 * <pre>
 * resin:type=BlockManager
 * </pre>
 */
@Description("Resin's backing store block manager")
public interface BlockManagerMXBean extends ManagedObjectMXBean {
  
  /**
   * Returns the number of blocks in the block manager
   */
  @Description("The number of blocks in the block manager")
  public long getBlockCapacity();
  
  /**
   * Returns the number of bytes in the block manager
   */
  @Description("The number of bytes in the block manager")
  public long getMemorySize();
  
  //
  // Statistics
  //
  
  /**
   * Returns the block read count.
   */
  @Description("The total blocks read from the backing")
  public long getBlockReadCountTotal();
  
  /**
   * Returns the block write count.
   */
  @Description("The total blocks written to the backing")
  public long getBlockWriteCountTotal();
  
  /**
   * Returns the block LRU cache hit count.
   */
  @Description("The hit count is the number of block accesses found in"
               + " the cache.")
  public long getHitCountTotal();

  /**
   * Returns the block cache miss count.
   */
  @Description("The miss count is the number of block accesses missing in"
               + " the cache.")
  public long getMissCountTotal();

  /**
   * Returns the block cache miss rate.
   */
  @Description("The miss rate is the number of block accesses missing in"
               + " the cache.")
  public double getMissRate();
}
