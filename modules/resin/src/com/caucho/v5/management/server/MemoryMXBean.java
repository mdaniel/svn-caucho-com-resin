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

import com.caucho.v5.jmx.Counter;
import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.Gauge;
import com.caucho.v5.jmx.Units;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Management interface for the memory
 *
 * <pre>
 * resin:type=Memory
 * </pre>
 */
@Description("Memory returns the JDK's memory statistics")
public interface MemoryMXBean extends ManagedObjectMXBean {
  @Description("Current allocated CodeCache memory")
  public long getCodeCacheCommitted();
  
  @Description("Max allocated CodeCache memory")
  @Units("bytes")
  @Gauge
  public long getCodeCacheMax();
  
  @Description("Current used CodeCache memory")
  @Units("bytes")
  @Gauge
  public long getCodeCacheUsed();
  
  @Description("Current free CodeCache memory")
  @Units("bytes")
  @Gauge
  public long getCodeCacheFree();

  @Description("Current allocated Eden memory")
  @Units("bytes")
  @Gauge
  public long getEdenCommitted();
  
  @Description("Max allocated Eden memory")
  @Units("bytes")
  @Gauge
  public long getEdenMax();
  
  @Description("Current used Eden memory")
  @Units("bytes")
  @Gauge
  public long getEdenUsed();
  
  @Description("Current free Eden memory")
  @Units("bytes")
  @Gauge
  public long getEdenFree();
  
  @Description("Current allocated Survivor memory")
  @Units("bytes")
  @Gauge
  public long getSurvivorCommitted();
  
  @Description("Max allocated Survivor memory")
  @Units("bytes")
  @Gauge
  public long getSurvivorMax();
  
  @Description("Current used Survivor memory")
  @Units("bytes")
  @Gauge
  public long getSurvivorUsed();
  
  @Description("Current free Survivor memory")
  @Units("bytes")
  @Gauge
  public long getSurvivorFree();
  
  @Description("Current allocated Tenured memory")
  @Units("bytes")
  @Gauge
  public long getTenuredCommitted();
  
  @Description("Max allocated Tenured memory")
  @Units("bytes")
  @Gauge
  public long getTenuredMax();
  
  @Description("Current used Tenured memory")
  @Units("bytes")
  @Gauge
  public long getTenuredUsed();
  
  @Description("Current free Tenured memory")
  @Units("bytes")
  @Gauge
  public long getTenuredFree();
  
  @Description("Current total GC time")
  @Units("milliseconds")
  @Counter
  public long getGarbageCollectionTime();
  
  @Description("Current total GC count")
  @Units("milliseconds")
  @Counter
  public long getGarbageCollectionCount();
}
