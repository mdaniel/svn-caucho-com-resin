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

package com.caucho.server.resin;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.MemoryMXBean;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import java.util.logging.Logger;
import java.lang.management.MemoryUsage;
import javax.management.*;
import javax.management.openmbean.*;

/**
 * Facade for the JVM's memory statistics
 */
public class MemoryAdmin extends AbstractManagedObject
  implements MemoryMXBean
{
  private static final L10N L = new L10N(MemoryAdmin.class);
  private static final Logger log
    = Logger.getLogger(MemoryAdmin.class.getName());

  private final MBeanServer _mbeanServer;

  private final ObjectName _codeCacheName;
  private final ObjectName _edenName;
  private final ObjectName _permGenName;
  private final ObjectName _survivorName;
  private final ObjectName _tenuredName;

  private MemoryAdmin()
  {
    _mbeanServer = Jmx.getGlobalMBeanServer();

    try {
      _codeCacheName = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
      _edenName = new ObjectName("java.lang:type=MemoryPool,name=Eden Space");
      _permGenName = new ObjectName("java.lang:type=MemoryPool,name=Perm Gen");
    
      _survivorName = new ObjectName("java.lang:type=MemoryPool,name=Survivor Space");
    
      _tenuredName = new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen");
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    registerSelf();
  }

  static MemoryAdmin create()
  {
    return new MemoryAdmin();
  }

  @Override
  public String getName()
  {
    return null;
  }

  public long getCodeCacheCommitted()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheMax()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheUsed()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheFree()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenCommitted()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenMax()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenUsed()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenFree()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenCommitted()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenMax()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenUsed()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenFree()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorCommitted()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorMax()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorUsed()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorFree()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredCommitted()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredMax()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredUsed()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredFree()
  {
    try {
      CompositeData data
	= (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
