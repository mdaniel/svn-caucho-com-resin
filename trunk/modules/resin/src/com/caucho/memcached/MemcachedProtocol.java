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

package com.caucho.memcached;

import javax.annotation.PostConstruct;

import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.ResinCacheBuilder;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;

/**
 * Custom serialization for the cache
 */
public class MemcachedProtocol implements Protocol
{
  private ClusterCache _cache;
  
  public MemcachedProtocol()
  {
    _cache = new ClusterCache();
    _cache.setName("memcache");
    _cache.setLocalExpireTimeoutMillis(1000);
    _cache.setLeaseExpireTimeoutMillis(60 * 60 * 1000);
  }
  
  public void setMode(ResinCacheBuilder.Scope scope)
  {
    _cache.setScopeMode(scope);
  }
  
  @PostConstruct
  public void init()
  {
    _cache.init();
  }
  
  ClusterCache getCache()
  {
    return _cache;
  }
  
  @Override
  public ProtocolConnection createConnection(SocketLink link)
  {
    return new MemcachedConnection(this, link);
  }

  @Override
  public String getProtocolName()
  {
    return "memcache";
  }
}
