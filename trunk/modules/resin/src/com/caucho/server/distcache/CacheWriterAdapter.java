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

import java.util.Collection;

import javax.cache.Cache.Entry;
import javax.cache.CacheWriter;

/**
 * Extended cache loader
 */
public class CacheWriterAdapter<K,V> implements CacheWriterExt<K,V>
{
  private final CacheWriter<K,V> _writer;
  
  public CacheWriterAdapter(CacheWriter<K,V> writer)
  {
    _writer = writer;
  }

  @Override
  public void write(Entry<K, V> entry)
  {
    _writer.write(entry);
  }

  @Override
  public void writeAll(Collection<Entry<? extends K, ? extends V>> entries)
  {
    _writer.writeAll(entries);
  }

  @Override
  public void delete(Object key)
  {
    _writer.delete(key);
  }

  @Override
  public void deleteAll(Collection<?> entries)
  {
    _writer.deleteAll(entries);
  }

  @Override
  public void write(DistCacheEntry entry)
  {
    write(new ExtCacheEntryFacade(entry));
  }

  @Override
  public void delete(DistCacheEntry entry)
  {
    delete(entry.getKey());
  }

  @Override
  public void updateTime(DistCacheEntry distCacheEntry)
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _writer + "]";
  }
}
