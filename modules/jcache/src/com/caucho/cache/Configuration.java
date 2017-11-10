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

package com.caucho.cache;

import java.util.concurrent.TimeUnit;

import com.caucho.cache.event.CacheEntryListenerRegistration;
import com.caucho.cache.transaction.IsolationLevel;
import com.caucho.cache.transaction.Mode;

/**
 * Configuration for a new Cache.
 */
public interface Configuration<K,V>
{
  boolean isReadThrough();
  
  boolean isWriteThrough();
  
  boolean isStoreByValue();
  
  boolean isStatisticsEnabled();
  
  boolean isTransactionsEnabled();
  
  IsolationLevel getTransactionIsolationLevel();
  
  Mode getTransactionMode();
  
  Iterable<CacheEntryListenerRegistration<? super K, ? super V>>
  getCacheEntryListenerRegistrations();
  
  CacheLoader<K, ? extends V> getCacheLoader();
  
  CacheWriter<? super K, ? super V> getCacheWriter();
  
  ExpiryPolicy<? super K, ? super V> getExpiryPolicy();
  
  public static class Duration {
    public static final Duration ETERNAL = new Duration();
    public static final Duration ZERO = new Duration(TimeUnit.SECONDS, 0);
    
    private final TimeUnit timeUnit;
    private final long durationAmount;
    
    private Duration()
    {
      this.timeUnit = null;
      this.durationAmount = 0;
    }
    
    public Duration(TimeUnit timeUnit, long durationAmount)
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
      
      if (durationAmount < 0)
        throw new IllegalArgumentException();
      
      this.durationAmount = durationAmount;
    }
    
    public TimeUnit getTimeUnit()
    {
      return this.timeUnit;
    }
    
    public long getDurationAmount()
    {
      return this.durationAmount;
    }
    
    public boolean isEternal()
    {
      return this.timeUnit == null && this.durationAmount == 0;
    }
    
    public long getAdjustedTime(long time)
    {
      if (isEternal()) {
        return Long.MAX_VALUE;
      }
      else {
        return time + this.timeUnit.toMillis(this.durationAmount);
      }
    }
    
    @Override
    public String toString()
    {
      return (getClass().getSimpleName()
              + "[" + getDurationAmount() + "," + getTimeUnit() + "]");
    }
  }
  
  public enum ExpiryType {
    MODIFIED,
    ACCESSED;
  }
}
