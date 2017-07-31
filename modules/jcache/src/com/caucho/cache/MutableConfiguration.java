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

import java.util.ArrayList;

import com.caucho.cache.event.CacheEntryEventFilter;
import com.caucho.cache.event.CacheEntryListener;
import com.caucho.cache.event.CacheEntryListenerRegistration;
import com.caucho.cache.transaction.IsolationLevel;
import com.caucho.cache.transaction.Mode;

/**
 * Configuration for a new Cache.
 */
public class MutableConfiguration<K,V> implements Configuration<K,V>
{
  protected ArrayList<CacheEntryListenerRegistration<? super K, ? super V>>
  cacheEntryListenerRegistrations;
  
  protected CacheLoader<K, ? extends V> cacheLoader;
  
  protected CacheWriter<? super K, ? super V> cacheWriter;
  
  protected ExpiryPolicy<? super K, ? super V> expiryPolicy;
  
  protected boolean isReadThrough;
  
  protected boolean isWriteThrough;
  
  protected boolean isStatisticsEnabled;
  
  protected boolean isStoreByValue;
  
  protected boolean isTransactionsEnabled;
  
  protected IsolationLevel txnIsolationLevel;
  
  protected Mode txnMode;
  
  public MutableConfiguration()
  {
    this.cacheEntryListenerRegistrations
    = new ArrayList<CacheEntryListenerRegistration<? super K, ? super V>>();
    
    this.expiryPolicy = new ExpiryPolicy.Default<K,V>();
    this.isStoreByValue = true;
    this.txnIsolationLevel = IsolationLevel.NONE;
    this.txnMode = Mode.NONE;
  }
  
  public MutableConfiguration(
    Iterable<CacheEntryListenerRegistration<? super K, ? super V>> regs,
    CacheLoader<K, ? extends V> cacheLoader,
    CacheWriter<? super K, ? super V> cacheWriter,
    ExpiryPolicy<? super K, ? super V> expiryPolicy,
    boolean isReadThrough,
    boolean isWriteThrough,
    boolean isStatisticsEnabled,
    boolean isStoreByValue,
    boolean isTransactionsEnabled,
    IsolationLevel txnIsolationLevel,
    Mode txnMode)
  {
    this.cacheEntryListenerRegistrations
      = new ArrayList<CacheEntryListenerRegistration<? super K, ? super V>>();
    
    for (CacheEntryListenerRegistration<? super K, ? super V> reg : regs) {
      registerCacheEntryListener(reg.getCacheEntryListener(),
                                 reg.isOldValueRequired(),
                                 reg.getCacheEntryFilter(),
                                 reg.isSynchronous());
    }
    
    this.cacheLoader = cacheLoader;
    this.cacheWriter = cacheWriter;
    this.expiryPolicy = expiryPolicy;
    this.isReadThrough = isReadThrough;
    this.isWriteThrough = isWriteThrough;
    this.isStatisticsEnabled = isStatisticsEnabled;
    this.isStoreByValue = isStoreByValue;
    this.isTransactionsEnabled = isTransactionsEnabled;
    this.txnIsolationLevel = txnIsolationLevel;
    this.txnMode = txnMode;
  }
  
  public MutableConfiguration(Configuration<K,V> cfg)
  {
    this(cfg.getCacheEntryListenerRegistrations(),
         cfg.getCacheLoader(),
         cfg.getCacheWriter(),
         cfg.getExpiryPolicy(),
         cfg.isReadThrough(),
         cfg.isWriteThrough(),
         cfg.isStatisticsEnabled(),
         cfg.isStoreByValue(),
         cfg.isTransactionsEnabled(),
         cfg.getTransactionIsolationLevel(),
         cfg.getTransactionMode());
  }
  
  @Override
  public boolean isReadThrough()
  {
    return this.isReadThrough;
  }
  
  public MutableConfiguration<K,V> setReadThrough(boolean isReadThrough)
  {
    this.isReadThrough = isReadThrough;
    
    return this;
  }
  
  @Override
  public boolean isWriteThrough()
  {
    return this.isWriteThrough;
  }
  
  public MutableConfiguration<K,V> setWriteThrough(boolean isWriteThrough)
  {
    this.isWriteThrough = isWriteThrough;
    
    return this;
  }
  
  @Override
  public boolean isStoreByValue()
  {
    return this.isStoreByValue;
  }
  
  public MutableConfiguration<K,V> setStoreByValue(boolean isStoreByValue)
  {
    this.isStoreByValue = isStoreByValue;
    
    return this;
  }
  
  @Override
  public boolean isStatisticsEnabled()
  {
    return this.isStatisticsEnabled;
  }
  
  public MutableConfiguration<K,V> setStatisticsEnabled(boolean isEnabled)
  {
    this.isStatisticsEnabled = isEnabled;
    
    return this;
  }
  
  @Override
  public boolean isTransactionsEnabled()
  {
    return this.isTransactionsEnabled;
  }
  
  public MutableConfiguration<K,V> setTransactionsEnabled(boolean isEnabled)
  {
    this.isTransactionsEnabled = isEnabled;
    
    return this;
  }
  
  @Override
  public IsolationLevel getTransactionIsolationLevel()
  {
    return this.txnIsolationLevel;
  }
  
  public MutableConfiguration<K,V> setTransactions(IsolationLevel level,
                                                   Mode mode)
  {
    this.txnIsolationLevel = level;
    this.txnMode = mode;
    
    return this;
  }
  
  @Override
  public Mode getTransactionMode()
  {
    return this.txnMode;
  }
  
  @Override
  public Iterable<CacheEntryListenerRegistration<? super K, ? super V>>
  getCacheEntryListenerRegistrations()
  {
    return this.cacheEntryListenerRegistrations;
  }
  
  public MutableConfiguration<K,V> 
  registerCacheEntryListener(CacheEntryListener<? super K, ? super V> listener,
                             boolean requireOldValue,
                             CacheEntryEventFilter<? super K, ? super V> filter,
                             boolean synchronous)
  {
    SimpleCacheEntryListenerRegistration<K,V> reg
      = new SimpleCacheEntryListenerRegistration<K,V>(listener,
                                                   filter,
                                                   requireOldValue,
                                                   synchronous);

    cacheEntryListenerRegistrations.add(reg);

    return this;
  }
  
  @Override
  public CacheLoader<K, ? extends V> getCacheLoader()
  {
    return this.cacheLoader;
  }
  
  
  public MutableConfiguration<K,V> setCacheLoader(CacheLoader<K, ? extends V> loader)
  {
    this.cacheLoader = loader;
    
    return this;
  }
  
  @Override
  public CacheWriter<? super K, ? super V> getCacheWriter()
  {
    return this.cacheWriter;
  }
  
  public MutableConfiguration<K,V> 
  setCacheWriter(CacheWriter<? super K, ? super V> writer)
  {
    this.cacheWriter = writer;
    
    return this;
  }
  
  @Override
  public ExpiryPolicy<? super K, ? super V> getExpiryPolicy()
  {
    return this.expiryPolicy;
  }
  
  public MutableConfiguration<K,V> 
  setExpiryPolicy(ExpiryPolicy<? super K, ? super V> policy)
  {
    this.expiryPolicy = policy;
    
    return this;
  }
  
  static class SimpleCacheEntryListenerRegistration<K,V>
    implements CacheEntryListenerRegistration<K,V>
  {
    private final CacheEntryListener<? super K, ? super V> _listener;
    private final CacheEntryEventFilter<? super K, ? super V> _filter;
    private final boolean _isOldValueRequired;
    private final boolean _isSynchronous;

    SimpleCacheEntryListenerRegistration(
      CacheEntryListener<? super K, ? super V> listener,
      CacheEntryEventFilter<? super K, ? super V> filter,
      boolean isOldValueRequired,
      boolean isSynchronous)
    {
      if (listener == null) {
        throw new NullPointerException();
      }
      
       _listener = listener;
       _filter = filter;
       _isOldValueRequired = isOldValueRequired;
       _isSynchronous = isSynchronous;
    }

    @Override
    public CacheEntryListener<? super K, ? super V> getCacheEntryListener()
    {
      return _listener;
    }

    @Override
    public boolean isOldValueRequired()
    {
      return _isOldValueRequired;
    }

    @Override
    public CacheEntryEventFilter<? super K, ? super V> getCacheEntryFilter()
    {
      return _filter;
    }

    @Override
    public boolean isSynchronous()
    {
      return _isSynchronous;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _listener + "]";
    }
  }
}
