/**
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.server.distcache;

import com.caucho.cluster.ExtCacheEntry;
import com.caucho.server.cluster.ClusterTriad;

import java.io.InputStream;
import java.io.IOException;

/**
 * Defines methods for the Key section of a CacheEntry
 */
public interface CacheEntryKey {

  public CacheEntryValue getEntryValue(CacheConfig config);

  public Object put(Object value, CacheConfig config);

  /**
   * Sets the value by an input stream
   */
  public ExtCacheEntry put(InputStream is,
    CacheConfig config,
    long idleTimeout)
    throws IOException;

  /**
   * Remove the value
   */
  public boolean remove(CacheConfig config);

  public boolean isValid();

  public Object getKey();

  public HashKey getKeyHash();


  public ClusterTriad.Owner getOwner();

  public CacheEntryValue getEntryValue();

  /* methods surfaced in CacheEntryValue */

  public byte[] getValueHashArray();

  public HashKey getValueHash();

}
