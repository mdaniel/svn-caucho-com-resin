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

import java.util.concurrent.TimeUnit;

import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * Configuration for a new Cache.
 */
public interface CacheConfiguration<K,V>
{
  public boolean isReadThrough();
  
  public boolean isWriteThrough();
  
  public boolean isStoreByValue();
  
  public boolean isStatisticsEnabled();
  
  public boolean isTransactionEnabled();
  
  public IsolationLevel getTransactionIsolationLevel();
  
  public Mode getTransactionMode();
  
  public CacheLoader<K, ? extends V> getCacheLoader();
  
  public CacheWriter<? super K, ? super V> getCacheWriter();
  
  public void setExpiry(ExpiryType type, Duration duration);
  
  public Duration getExpiry(ExpiryType type);
  
  public static class Duration {
    public static final Duration ETERNAL = new Duration(TimeUnit.SECONDS, 0);
    
    private final TimeUnit timeUnit;
    
    private final long timeToLive;
    
    public Duration(TimeUnit timeUnit, long timeToLive)
    {
      if (timeUnit == null)
        throw new NullPointerException();
      
      switch (timeUnit) {
      case NANOSECONDS:
      case MICROSECONDS:
        throw new InvalidConfigurationException();
      default:
        this.timeUnit = timeUnit;
        break;
      }
      
      if (timeToLive < 0)
        throw new IllegalArgumentException();
      
      this.timeToLive = timeToLive;
    }
    
    public TimeUnit getTimeUnit()
    {
      return this.timeUnit;
    }
    
    public long getTimeToLive()
    {
      return this.timeToLive;
    }
  }
  
  public enum ExpiryType {
    MODIFIED,
    ACCESSED;
  }
}
