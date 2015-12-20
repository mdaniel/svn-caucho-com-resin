/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.util.L10N;

/**
 * cache of the last 120m of data
 */
@SuppressWarnings("serial")
final class StatDataCache implements java.io.Serializable
{
  private static final Logger log
    = Logger.getLogger(StatDataCache.class.getName());

  private static final L10N L = new L10N(StatDataCache.class);

  private static final int ENTRIES = 7 * 60;
  private static final long TICK = 60000L;
  
  public static final long CACHE_PERIOD = (ENTRIES - ENTRIES / 8) * TICK;
  
  private final long _id;
  private final double []_data;
  
  private int _head;
  private long _timeStart;
  private long _timeEnd;
  
  StatDataCache(long id)
  {
    _id = id;
    
    _data = new double[ENTRIES];
    
    for (int i = 0; i < _data.length; i++) {
      _data[i] = Double.NaN;
    }
  }
  
  final long getId()
  {
    return _id;
  }
  
  final void clearToNow(long now)
  {
    int index = (int) ((now / TICK) % ENTRIES);
    
    if (_head != index) {
      do {
        _head = (_head + 1) % ENTRIES;
        
        _data[_head] = Double.NaN;
      } while (_head != index);
    }
    
    _timeEnd = now;
    _timeStart = now - CACHE_PERIOD;
  }
  
  final double get(long time)
  {
    if (time < _timeStart || (_timeEnd + 60000L) < time) {
      return Double.NaN;
    }
    
    int index = (int) ((time / TICK) % ENTRIES);
    
    return _data[index];
  }
  
  final void set(long sampleTime, double value)
  {
    if (sampleTime < _timeStart || _timeEnd < sampleTime) {
      return;
    }
    
    int index = (int) ((sampleTime / TICK) % ENTRIES);
    
    // if the index has a valid value don't overwrite it with zero
    if (_data[index] == value || 
        (value == 0.0D && 
         (_data[index] != 0.0D && ! Double.isNaN(_data[index])))) {
      return;
    }
    
    if (! Double.isNaN(_data[index])) {
      if (log.isLoggable(Level.FINER)) {
        log.finer(L.l("sample overwriting index {1} value '{2}' with '{3}' ({0})",
                     toString(),
                     index, _data[index], value));
      }
    }

    _data[index] = value;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + Long.toHexString(_id) + "]";
  }
}
