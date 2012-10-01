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

package com.caucho.distcache;

/**
 * Implements the distributed cache
 */

public interface ResinCacheBuilder
{

  /**
   * Defines the scope options for a cache.
   */
  public enum Scope {

    /** Not distributed, no persistence.*/
    TRANSIENT,

    /** Not distributed, single or no persistence */
    LOCAL,

    /** Distributed across a cluster, persistence required.*/
    CLUSTER;
  }

  /**
   * Defines the persistence options for a cache.
   */
  public enum Persistence {

    /**
     * No persistence.
     */
    NONE,

    /**
     * A single copy of the cache is persisted on one server in the cluster.
     */
    SINGLE,

    /**
     * Three copies of the cache and its entrys are saved on three servers.
     */
    TRIPLE
  }
}
