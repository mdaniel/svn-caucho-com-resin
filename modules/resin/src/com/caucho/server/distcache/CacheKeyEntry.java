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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.distcache;

import com.caucho.cluster.CacheEntry;
import com.caucho.server.cluster.ClusterTriad;
import com.caucho.util.Alarm;
import com.caucho.util.Hex;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An entry in the cache map
 */
public class CacheKeyEntry {
  private final HashKey _keyHash;

  private final ClusterTriad.Owner _owner;

  private Object _key;

  private final AtomicBoolean _isReadUpdate
    = new AtomicBoolean();
  
  private final AtomicReference<CacheMapEntry> _entryRef
    = new AtomicReference<CacheMapEntry>();

  public CacheKeyEntry(Object key,
		       HashKey keyHash,
		       ClusterTriad.Owner owner)
  {
    _key = key;
    _keyHash = keyHash;
    _owner = owner;
  }

  /**
   * Returns the key
   */
  public final Object getKey()
  {
    return _key;
  }

  /**
   * Returns the keyHash
   */
  public final HashKey getKeyHash()
  {
    return _keyHash;
  }

  /**
   * Returns the owner
   */
  public final ClusterTriad.Owner getOwner()
  {
    return _owner;
  }

  /**
   * Returns the current value.
   */
  public final CacheMapEntry getEntry()
  {
    return _entryRef.get();
  }

  /**
   * Peeks the current value without checking the backing store.
   */
  public Object peek()
  {
    return null;
  }

  /**
   * Returns the object, checking the backing store if necessary.
   */
  public Object get(CacheConfig config)
  {
    return null;
  }

  /**
   * Fills the value with a stream
   */
  public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException
  {
    return false;
  }

  /**
   * Returns the current value.
   */
  public CacheMapEntry getEntry(CacheConfig config)
  {
    return null;
  }

  /**
   * Sets the value by an input stream
   */
  public Object put(Object value, CacheConfig config)
  {
    return null;
  }

  /**
   * Sets the value by an input stream
   */
  public CacheEntry put(InputStream is, CacheConfig config, long idleTimeout)
    throws IOException
  {
    return null;
  }

  /**
   * Remove the value
   */
  public boolean remove(CacheConfig config)
  {
    return false;
  }

  /**
   * Conditionally starts an update of a cache item, allowing only a
   * single thread to update the data.
   *
   * @return true if the thread is allowed to update
   */
  public final boolean startReadUpdate()
  {
    return _isReadUpdate.compareAndSet(false, true);
  }

  /**
   * Completes an update of a cache item.
   */
  public final void finishReadUpdate()
  {
    _isReadUpdate.set(false);
  }

  /**
   * Sets the current value.
   */
  public final boolean compareAndSet(CacheMapEntry oldEntry,
				     CacheMapEntry entry)
  {
    return _entryRef.compareAndSet(oldEntry, entry);
  }
  
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[key=" + _key
	    + ",keyHash=" + Hex.toHex(_keyHash.getHash(), 0, 4)
	    + ",owner=" + _owner
	    + "]");
  }
}
