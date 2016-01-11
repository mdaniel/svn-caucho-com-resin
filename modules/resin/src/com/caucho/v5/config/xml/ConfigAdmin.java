/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.xml;

import java.util.*;

import com.caucho.v5.jmx.server.ConfigMXBean;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.vfs.PathImpl;

public class ConfigAdmin extends ManagedObjectBase implements ConfigMXBean
{
  public static EnvironmentLocal<Map<PathImpl, ConfigMXBean>> _environmentConfigs = 
    new EnvironmentLocal<Map<PathImpl, ConfigMXBean>>();
  
  private PathImpl _path;
  private String _url;
  private long _length;
  private long _lastModified;
  private long _crc64 = -1;
  
  ConfigAdmin(PathImpl path)
  {
    _path = path;
    
    // caching for efficiency... can Path attributes change?
    _url = _path.getURL();
    _length = _path.length();
    _lastModified = _path.getLastModified();
    _crc64 = _path.getCrc64();
  }

  public static void registerPath(PathImpl path)
  {
    if (path.getURL().toLowerCase().endsWith(".license"))
      return;
    
    Map<PathImpl, ConfigMXBean> map = _environmentConfigs.getLevel();
    
    if (map == null) {
      map = new HashMap<PathImpl, ConfigMXBean>();
      _environmentConfigs.set(map);
    }
    
    if (! map.containsKey(path)) {
      ConfigAdmin admin = new ConfigAdmin(path);
      admin.register();
      
      map.put(path, admin);
    }
  }
  
  public static Collection<ConfigMXBean> getMBeans()
  {
    Map<PathImpl, ConfigMXBean> map = _environmentConfigs.get();
    if (map == null)
      return Collections.emptyList();
    return Collections.unmodifiableCollection(map.values());
  }
  
  public static Collection<ConfigMXBean> getMBeans(ClassLoader classLoader)
  {
    Map<PathImpl, ConfigMXBean> map = _environmentConfigs.get(classLoader);
    if (map == null)
      return Collections.emptyList();
    return Collections.unmodifiableCollection(map.values());
  }

  @Override
  public String getPath()
  {
    return _path.getFullPath();
  }

  public long getLastModified()
  {
    return _lastModified;
  }

  public long getLength()
  {
    return _length;
  }

  public long getCrc64()
  {
    return _crc64;
  }

  @Override
  public String getName()
  {
    return _url;
  }

  void register()
  {
    registerSelf();
  }

  void unregister()
  {
    unregisterSelf();
  }
}
