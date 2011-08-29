/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.cache;

import com.caucho.config.types.Bytes;
import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.Path;

import javax.servlet.FilterChain;

/**
 * Cached response.
 */
abstract public class AbstractCache {
  /**
   * Sets the path to the cache directory.
   */
  abstract public void setPath(Path path);
  
  /**
   * Returns the path from the cache directory.
   */
  abstract public Path getPath();

  /**
   * Sets the disk size of the cache
   */
  abstract public void setDiskSize(Bytes size);

  /**
   * Sets the max entry size of the cache
   */
  abstract public int getMaxEntrySize();

  /**
   * Set true if enabled.
   */
  abstract public void setEnable(boolean isEnabled);

  /**
   * Return true if enabled.
   */
  abstract public boolean isEnable();

  /**
   * Sets the max number of entries.
   */
  abstract public void setEntries(int entries);

  /**
   * Sets the path to the cache directory (backwards compatibility).
   */
  abstract public void setDir(Path path);

  /**
   * Sets the size of the the cache (backwards compatibility).
   */
  abstract public void setSize(Bytes size);
  
  /**
   * Creates the filter.
   */
  abstract public FilterChain createFilterChain(FilterChain next,
						WebApp app);

  /**
   * Clears the cache.
   */
  abstract public void clear();

  /**
   * Returns the hit count.
   */
  abstract public long getHitCount();

  /**
   * Returns the miss count.
   */
  abstract public long getMissCount();

  /**
   * Returns the memory block hit count.
   */
  abstract public long getMemoryBlockHitCount();

  /**
   * Returns the memory block miss count.
   */
  abstract public long getMemoryBlockMissCount();
}
