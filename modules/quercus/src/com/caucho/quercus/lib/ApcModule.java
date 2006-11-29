/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.Alarm;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.annotation.Optional;

/**
 * APC object oriented API facade
 */
public class ApcModule extends AbstractQuercusModule {
  private static final Logger log = Logger.getLogger(ApcModule.class.getName());
  private static final L10N L = new L10N(ApcModule.class);

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  private LruCache<String,Entry> _cache = new LruCache<String,Entry>(4096);

  private HashMap<String,Value> _constMap = new HashMap<String,Value>();

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "apc" };
  }

  /**
   * Returns cache information.
   */
  public Value apc_cache_info(@Optional String type)
  {
    ArrayValue value = new ArrayValueImpl();

    value.put("num_slots", 1000);
    value.put("ttl", 0);
    value.put("num_hits", 0);
    value.put("num_misses", 0);
    value.put("start_time", 0);
    value.put(new StringValueImpl("cache_list"), new ArrayValueImpl());

    return value;
  }

  /**
   * Clears the cache
   */
  public boolean apc_clear_cache(Env env, @Optional String type)
  {
    _cache.clear();

    return true;
  }

  /**
   * Defines constants
   */
  public boolean apc_define_constants(Env env,
                                      String key,
                                      ArrayValue values,
                                      @Optional("true") boolean caseSensitive)
  {
    _constMap.put(key, values.copy(env));

    return true;
  }

  /**
   * Deletes a value.
   */
  public boolean apc_delete(Env env, String key)
  {
    return _cache.remove(key) != null;
  }

  /**
   * Returns a value.
   */
  public Value apc_fetch(Env env, String key)
  {
    Entry entry = _cache.get(key);

    if (entry == null)
      return BooleanValue.FALSE;

    Value value = entry.getValue();

    if (entry.isValid() && value != null)
      return value.copy(env);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Defines constants
   */
  public boolean apc_load_constants(Env env,
                                    String key,
                                    @Optional("true") boolean caseSensitive)
  {
    ArrayValue array = (ArrayValue) _constMap.get(key);

    if (array == null)
      return false;

    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      env.addConstant(entry.getKey().toString(),
                      entry.getValue().copy(env),
                      ! caseSensitive);
    }

    return true;
  }

  /**
   * Returns cache information.
   */
  public Value apc_sma_info(@Optional String type)
  {
    ArrayValue value = new ArrayValueImpl();

    value.put("num_seg", 1);
    value.put("seg_size", 1024 * 1024);
    value.put("avail_mem", 1024 * 1024);
    value.put(new StringValueImpl("block_lists"), new ArrayValueImpl());

    return value;
  }

  /**
   * Returns a value.
   */
  public Value apc_store(Env env, String key, Value value,
                         @Optional("0") int ttl)
  {
    _cache.put(key, new Entry(value.copy(env), ttl));

    return BooleanValue.TRUE;
  }

  static class Entry {
    private Value _value;
    private long _expire;

    Entry(Value value, int ttl)
    {
      _value = value;

      if (ttl <= 0)
        _expire = Long.MAX_VALUE;
      else
        _expire = Alarm.getCurrentTime() + ttl * 1000L;
    }

    public boolean isValid()
    {
      if (Alarm.getCurrentTime() <= _expire)
        return true;
      else {
        _value = null;
        return false;
      }
    }

    public Value getValue()
    {
      return _value;
    }
  }

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  static {
    addIni(_iniMap, "apc.enabled", "1", PHP_INI_ALL);
    addIni(_iniMap, "apc.shm_segments", "1", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.shm_size", "30", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.optimization", "0", PHP_INI_ALL);
    addIni(_iniMap, "apc.num_files_hint", "1000", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.ttl", "0", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.gc_ttl", "3600", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.cache_by_default", "1", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.filters", "", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.mmap_file_mask", "", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.slam_defense", "0", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.file_update_protection", "2", PHP_INI_SYSTEM);
    addIni(_iniMap, "apc.enable_cli", "0", PHP_INI_SYSTEM);
  }
}
