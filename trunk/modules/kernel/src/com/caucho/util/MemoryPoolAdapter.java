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
 */

package com.caucho.util;

import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.*;
import javax.management.openmbean.CompositeData;

import com.caucho.jmx.Jmx;

public class MemoryPoolAdapter
{
  private static final Logger log
    = Logger.getLogger(MemoryPoolAdapter.class.getName());
  
  private final MBeanServer _mbeanServer;
  private final ObjectName []_gcObjectNames;
  
  private Map<PoolType, ObjectName> _poolNamesMap = 
    new HashMap<MemoryPoolAdapter.PoolType, ObjectName>();
  
  public MemoryPoolAdapter()
  {
    _mbeanServer = Jmx.getGlobalMBeanServer();

    try {
      ObjectName query = new ObjectName("java.lang:type=MemoryPool,*");
      Set<ObjectName> objectNames = _mbeanServer.queryNames(query, null);
      
      for (PoolType poolType : PoolType.values()) {
        ObjectName objectName = poolType.find(objectNames);
        _poolNamesMap.put(poolType, objectName);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    try {
      ObjectName query = new ObjectName("java.lang:type=GarbageCollector,*");
      Set<ObjectName> objectNames = _mbeanServer.queryNames(query, null);
      
      _gcObjectNames = new ObjectName[objectNames.size()];
      
      int i = 0;
      
      for (ObjectName name : objectNames) {
        _gcObjectNames[i++] = name;
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  public ObjectName getCodeCacheName()
  {
    return _poolNamesMap.get(PoolType.CODE_CACHE);
  }

  public void setCodeCacheName(ObjectName codeCacheName)
  {
    _poolNamesMap.put(PoolType.CODE_CACHE, codeCacheName);
  }

  public ObjectName getEdenName()
  {
    return _poolNamesMap.get(PoolType.EDEN);
  }

  public void setEdenName(ObjectName edenName)
  {
    _poolNamesMap.put(PoolType.EDEN, edenName);
  }

  /*
  public ObjectName getPermGenName()
  {
    return _poolNamesMap.get(PoolType.PERM_GEN);
  }

  public void setPermGenName(ObjectName permGenName)
  {
    _poolNamesMap.put(PoolType.PERM_GEN, permGenName);
  }
  */

  public ObjectName getSurvivorName()
  {
    return _poolNamesMap.get(PoolType.SURVIVOR);
  }

  public void setSurvivorName(ObjectName survivorName)
  {
    _poolNamesMap.put(PoolType.SURVIVOR, survivorName);
  }

  public ObjectName getTenuredName()
  {
    return _poolNamesMap.get(PoolType.TENURED);
  }

  public void setTenuredName(ObjectName tenuredName)
  {
    _poolNamesMap.put(PoolType.TENURED, tenuredName);
  }

  public long getCodeCacheCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getCodeCacheName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getCodeCacheMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getCodeCacheName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getCodeCacheUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getCodeCacheName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getCodeCacheFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getCodeCacheName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  public long getEdenCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getEdenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getEdenMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getEdenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getEdenUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getEdenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getEdenFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getEdenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  /*
  public long getPermGenCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getPermGenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getPermGenMax()
    throws JMException
  {
    CompositeData data
    = (CompositeData) _mbeanServer.getAttribute(getPermGenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getPermGenUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getPermGenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getPermGenFree()
  throws JMException
  {
      CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getPermGenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }
  
  public MemUsage getPermGenMemUsage()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getPermGenName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return new MemUsage(usage.getMax(), usage.getCommitted(), usage.getUsed());
  }
  */
  public long getPermGenCommitted()
    throws JMException
  {
    return 0;
  }

  public long getPermGenMax()
    throws JMException
  {
    return 0;
  }

  public long getPermGenUsed()
    throws JMException
  {
    return 0;
  }

  public long getPermGenFree()
    throws JMException
  {
    return 0;
  }

  public long getSurvivorCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getSurvivorName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getSurvivorMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getSurvivorName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getSurvivorUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getSurvivorName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getSurvivorFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getSurvivorName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  public long getTenuredCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getTenuredName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getTenuredMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getTenuredName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getTenuredUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getTenuredName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getTenuredFree()
    throws JMException
  {
      CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getTenuredName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }
  
  public MemUsage getTenuredMemUsage()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(getTenuredName(), "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return new MemUsage(usage.getMax(), 
                        usage.getCommitted(),
                        usage.getUsed());
  }

  public long getGarbageCollectionTime()
    throws JMException
  {
    long gcTime = 0;
    
    for (ObjectName name : _gcObjectNames) {
      try {
        Object value = _mbeanServer.getAttribute(name, "CollectionTime");
        
        if (value instanceof Number)
          gcTime += ((Number) value).longValue();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    return gcTime;
  }

  public long getGarbageCollectionCount()
    throws JMException
  {
    long gcCount = 0;
    
    for (ObjectName name : _gcObjectNames) {
      try {
        Object value = _mbeanServer.getAttribute(name, "CollectionCount");
        
        if (value instanceof Number)
          gcCount += ((Number) value).longValue();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    return gcCount;
  }

  public static class MemUsage
  {
    private long _max;
    private long _used;
    
    protected MemUsage(long max,
                       long committed,
                       long used)
    {
      if (max < committed)
        _max = committed;
      else
        _max = max;
      
      _used = used;
    }

    public long getMax()
    {
      return _max;
    }

    public long getUsed()
    {
      return _used;
    }
    
    public long getFree()
    {
      return getMax() - getUsed();
    }
  }
  
  public static enum PoolType 
  {
    CODE_CACHE ("java.lang:type=MemoryPool,name=Code Cache", 
                "class storage","class memory","code cache","class","code"),
    EDEN ("java.lang:type=MemoryPool,name=Eden Space", 
          "eden"),
          /*
    PERM_GEN("java.lang:type=MemoryPool,name=Perm Gen", 
             "perm gen","perm"),
             */
    SURVIVOR ("java.lang:type=MemoryPool,name=Survivor Space", 
              "survivor"),
    TENURED ("java.lang:type=MemoryPool,name=Tenured Gen", 
             "tenured","old gen","java heap","old space","old","heap");

    private final String _defaultName;
    private final String[] _keywords;
    
    PoolType(String defaultName, String... keywords) 
    {
      _defaultName = defaultName;
      _keywords = keywords;
    }
  
    public String defaultName()
    {
      return _defaultName;
    }
  
    public String[] keywords()
    {
      return _keywords;
    }

    // this does a breadth first search for the 1st objectName that matches a keyword
    // keywords are ordered  most specific to least specific
    public ObjectName find(Set<ObjectName> objectNames) 
      throws MalformedObjectNameException
    {
      for (String keyword : _keywords) {
        for (ObjectName objectName : objectNames) {
          String name = objectName.getKeyProperty("name").toLowerCase();
          if (name.contains(keyword)) {
            return objectName;
          }
        }
      }
      
      return new ObjectName(_defaultName);
    }
  }
}
