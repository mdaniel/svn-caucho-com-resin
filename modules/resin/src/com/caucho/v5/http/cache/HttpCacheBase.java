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

package com.caucho.v5.http.cache;

import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.vfs.PathImpl;

import javax.servlet.FilterChain;

/**
 * Cached response.
 */
public class HttpCacheBase
{
  private int _entries = -1;

  /**
   * Sets the path to the cache directory.
   */
  public void setPath(PathImpl path)
  {
  }
  
  /**
   * Returns the path from the cache directory.
   */
  public PathImpl getPath()
  {
    return null;
  }

  /**
   * Sets the disk size of the cache
   */
  public void setDiskSize(Bytes size)
  {
  }

  /**
   * Sets the max entry size of the cache
   */
  public int getMaxEntrySize()
  {
    return 0;
  }

  /**
   * Set true if enabled.
   */
  public void setEnable(boolean isEnabled)
  {
  }

  /**
   * Return true if enabled.
   */
  public boolean isEnable()
  {
    return false;
  }

  /**
   * Sets the max number of entries.
   */
  public final void setEntries(int entries)
  {
    _entries = entries;
  }
  
  public final int getEntries()
  {
    return _entries;
  }
  
  public void setEnableMmap(boolean isEnable)
  {
  }

  /**
   * Sets the path to the cache directory (backwards compatibility).
   */
  public void setDir(PathImpl path)
  {
  }

  /**
   * Sets the size of the the cache (backwards compatibility).
   */
  public void setSize(Bytes size)
  {
  }
  
  public void setMemorySize(Bytes bytes)
  {
  }
  
  public void setRewriteVaryAsPrivate(boolean isEnable)
  {
    
  }
  
  /**
   * Creates the filter.
   */
  public FilterChain createFilterChain(FilterChain next,
                                       WebAppResinBase app)
  {
    return next;
  }

  /**
   * Clears the cache.
   */
  public void clear()
  {
  }

  /**
   * Returns the hit count.
   */
  public long getHitCount()
  {
    return 0;
  }

  /**
   * Returns the miss count.
   */
  public long getMissCount()
  {
    return 0;
  }

  /**
   * Returns the memory block hit count.
   */
  public long getMemoryBlockHitCount()
  {
    return 0;
  }

  /**
   * Returns the memory block miss count.
   */
  public long getMemoryBlockMissCount()
  {
    return 0;
  }

  public void start()
  {

  }

  public void close()
  {

  }
}
