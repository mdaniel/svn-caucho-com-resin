/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import javax.management.*;
import javax.management.openmbean.CompositeData;

import com.caucho.jmx.Jmx;

public class MemoryPoolAdapter
{
  private final MBeanServer _mbeanServer;

  private ObjectName _codeCacheName;
  private ObjectName _edenName;
  private ObjectName _permGenName;
  private ObjectName _survivorName;
  private ObjectName _tenuredName;
  
  public MemoryPoolAdapter()
  {
    _mbeanServer = Jmx.getGlobalMBeanServer();

    try {
      ObjectName query = new ObjectName("java.lang:type=MemoryPool,*");

      ObjectName codeCacheName
        = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
      ObjectName edenName
        = new ObjectName("java.lang:type=MemoryPool,name=Eden Space");
      ObjectName permGenName
        = new ObjectName("java.lang:type=MemoryPool,name=Perm Gen");
      ObjectName survivorName
        = new ObjectName("java.lang:type=MemoryPool,name=Survivor Space");
      ObjectName tenuredName
        = new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen");
      
      for (ObjectName objName : _mbeanServer.queryNames(query, null)) {
        String name = objName.getKeyProperty("name");

        if (name.toLowerCase().contains("code"))
          codeCacheName = objName;
        else if (name.toLowerCase().contains("eden"))
          edenName = objName;
        else if (name.toLowerCase().contains("perm"))
          permGenName = objName;
        else if (name.toLowerCase().contains("surv"))
          survivorName = objName;
        else if (name.toLowerCase().contains("tenured"))
          tenuredName = objName;
        else if (name.toLowerCase().contains("old"))
          tenuredName = objName;
      }
      
      _codeCacheName = codeCacheName;
      _edenName = edenName;
      _permGenName = permGenName;
      _survivorName = survivorName;
      _tenuredName = tenuredName;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  public ObjectName getCodeCacheName()
  {
    return _codeCacheName;
  }

  public void setCodeCacheName(ObjectName codeCacheName)
  {
    _codeCacheName = codeCacheName;
  }

  public ObjectName getEdenName()
  {
    return _edenName;
  }

  public void setEdenName(ObjectName edenName)
  {
    _edenName = edenName;
  }

  public ObjectName getPermGenName()
  {
    return _permGenName;
  }

  public void setPermGenName(ObjectName permGenName)
  {
    _permGenName = permGenName;
  }

  public ObjectName getSurvivorName()
  {
    return _survivorName;
  }

  public void setSurvivorName(ObjectName survivorName)
  {
    _survivorName = survivorName;
  }

  public ObjectName getTenuredName()
  {
    return _tenuredName;
  }

  public void setTenuredName(ObjectName tenuredName)
  {
    _tenuredName = tenuredName;
  }

  public long getCodeCacheCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getCodeCacheMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getCodeCacheUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getCodeCacheFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  public long getEdenCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getEdenMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getEdenUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getEdenFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  public long getPermGenCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getPermGenMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getPermGenUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getPermGenFree()
  throws JMException
  {
      CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }
  
  public MemUsage getPermGenMemUsage()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return new MemUsage(usage.getMax(), usage.getUsed());
  }

  public long getSurvivorCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getSurvivorMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getSurvivorUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getSurvivorFree()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }

  public long getTenuredCommitted()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getCommitted();
  }

  public long getTenuredMax()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax();
  }

  public long getTenuredUsed()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getUsed();
  }

  public long getTenuredFree()
    throws JMException
  {
      CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return usage.getMax() - usage.getUsed();
  }
  
  public MemUsage getTenuredMemUsage()
    throws JMException
  {
    CompositeData data
      = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

    MemoryUsage usage = MemoryUsage.from(data);

    return new MemUsage(usage.getMax(), usage.getUsed());
  }

  public static class MemUsage
  {
    private long _max;
    private long _used;
    
    protected MemUsage(long max, long used)
    {
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
}
