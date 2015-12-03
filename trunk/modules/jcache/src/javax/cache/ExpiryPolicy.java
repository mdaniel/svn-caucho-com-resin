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

package javax.cache;

import javax.cache.Cache.Entry;
import javax.cache.Configuration.Duration;


/**
 * Configuration for the expiration policy.
 */
public interface ExpiryPolicy<K,V>
{
  Duration getTTLForCreatedEntry(Entry<? extends K, ? extends V> entry);
  
  Duration getTTLForAccessedEntry(Entry<? extends K, ? extends V> entry,
                                  Duration duration);
  
  Duration getTTLForModifiedEntry(Entry<? extends K, ? extends V> entry,
                                  Duration duration);
  
  public static final class Accessed<K,V> implements ExpiryPolicy<K,V>
  {
    private Duration expiryDuration;
    
    public Accessed(Duration expiryDuration)
    {
      this.expiryDuration = expiryDuration;
    }

    @Override
    public Duration getTTLForCreatedEntry(Entry<? extends K, ? extends V> entry)
    {
      return this.expiryDuration;
    }

    @Override
    public Duration getTTLForAccessedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return this.expiryDuration;
    }

    @Override
    public Duration getTTLForModifiedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return duration;
    }
    
    @Override
    public String toString()
    {
      return getClass().getName() + "[]";
    }
  }
  
  public static final class Modified<K,V> implements ExpiryPolicy<K,V>
  {
    private final Duration _expiryDuration;
    
    public Modified(Duration expiryDuration)
    {
      _expiryDuration = expiryDuration;
    }

    @Override
    public Duration getTTLForCreatedEntry(Entry<? extends K, ? extends V> entry)
    {
      return _expiryDuration;
    }

    @Override
    public Duration getTTLForAccessedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return _expiryDuration;
    }

    @Override
    public Duration getTTLForModifiedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return duration;
    }
    
    @Override
    public String toString()
    {
      return getClass().getName() + "[" + _expiryDuration + "]";
    }
  }

  /**
   * Default expiry is to not expire.
   */
  public static final class Default<K,V> implements ExpiryPolicy<K,V>
  {
    @Override
    public Duration getTTLForCreatedEntry(Entry<? extends K, ? extends V> entry)
    {
      return Duration.ETERNAL;
    }

    @Override
    public Duration getTTLForAccessedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return duration;
    }

    @Override
    public Duration getTTLForModifiedEntry(Entry<? extends K, ? extends V> entry,
                                           Duration duration)
    {
      return duration;
    }
    
    @Override
    public int hashCode()
    {
      return Default.class.hashCode();
    }
    
    @Override
    public boolean equals(Object value)
    {
      return value instanceof Default;
    }
    
    @Override
    public String toString()
    {
      return getClass().getName() + "[]";
    }
  }
}
