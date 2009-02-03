/**
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.cluster;

import javax.cache.CacheLoader;
import javax.cache.Cache;
import java.security.Principal;
import java.security.Permission;

/**
 * Defines the configuration for a cache, as required for various concerns.
 */
public interface CacheConfiguration extends Cache {

  /**
   * Provides the means to control access to a cache.
   */
  public interface Authorization {

    /**
     * Returns true if the principal has the permission for the requested operation.
     *
     * @param principal  the entity (person or system) requesting permission
     * @param permission that describes the action to be performed
     * @return true if the permission is granted.
     *         <p/>
     *         TODO(fred): Define permissions and test.
     */
    public boolean checkPermission(Principal principal, Permission permission);

  }

  /**
   * Provides access to the full name of the Cache.
   * <p/>
   * TODO(fred): finalize name definition by 4.0 release, needed for global cache.
   */
  public interface Name {

    /**
     * Returns the name of the cache.
     */
    public String getName();
  }

  /**
   * Provides access to details of the configuration of the cache.
   */
  public interface Provider {
    /**
     * Returns an optional provider of the CacheLoader interface.
     */
    public CacheLoader getCacheLoader();

    /**
     * Returns the key serializer
     */
    public CacheSerializer getKeySerializer();

    /**
     * Returns the value serializer
     */
    public CacheSerializer getValueSerializer();

    /**
     * Returns true if the level of persistence is at least single.
     */
    public boolean hasSinglePersistence();

    /**
     * Returns true is the level of persistence is triple (3 copies)
     */
    public boolean hasTriplePersistence();

    /**
     * Returns if if there is no persistence of the objects in the cache.
     */
    public boolean hasNoPersistence();

    /**
     * Returns the persistence of the cache
     */
    public Persistence getPersistence();

    /**
     * Returns the name of the {@link Scope} of this cache.
     */
    public String getScopeName();

    /**
     * Returns the {@link Scope} of this cache.
     *
     * @return
     */
    public Scope getScope();


    /**
     * Returns the globally unique id of this cache.
     */
    public String getGuid();
  }

  /**
   * Defines the persistence options for a Cache.
   */
  public enum Persistence {
    /**
     * No persistence is provided
     */
    NONE,

    /**
     * A single copy of cached items is maintained on persistent storage
     */
    SINGLE,

    /**
     * Three copies of the cached items are maintained by the server
     */
    TRIPLE;
  }

  /**
   * Defines the scope (of access) for the cache.
   */
  public enum Scope {


    /**
     * Not sharable
     */
    LOCAL,

    /**
     * The cache may be shared across the server
     */
    SERVER,

    /**
     * The cache is shared across an entire Resin Pod
     */
    POD,

    /**
     * The cache is shared across an entire Resin Cluster
     */
    ClUSTER,

    /**
     * The cache may be accessed from anywhere
     */
    GLOBAL,

    /**
     * The cache is a listener to another cache.
     */
    LISTENER;
  }

  /**
   * Defines the time values that determine how an entry is managed by the Cache.
   *
   * @note These values are key to get the best performance from the Cache.
   *
   * TODO(fred): Develop default time configurations.
   */
  public interface Time {

    /**
     * The maximum valid time for an item.  Items stored in the cache
     * for longer than the expire time are no longer valid and will
     * return null from a get.
     * <p/>
     * Default is infinite.
     */
    public long getExpireTimeout();

    /**
     * Returns the expire check window, i.e. the precision of the expire
     * check.  Since an expired item can cause a massive cascade of
     * attempted loads from the backup, the actual expiration is randomized.
     */
    public long getExpireTimeoutWindow();

    /**
     * The maximum time that an item can remain in cache without being referenced.
     * For example, session data could be configured to be removed if idle for more than 30 minutes.
     * <p/>
     * Cached data would typically have infinite idle time because
     * it doesn't depend on how often it's accessed.
     * <p/>
     * Default is infinite.
     */
    public long getIdleTimeout();

    /**
     * Returns the idle check window,  the amount of time
     * in which the idle time limit can be spread out to smooth performance.
     * <p/>
     * If this optional value is not set, the system  uses a fraction of the
     * idle time.
     */
    public long getIdleTimeoutWindow();

    /**
     * Returns the lease timeout, which is the time a server can use the local version
     * if it owns it, before a timeout.
     */
    public long getLeaseTimeout();

    /**
     * The local read timeout is the time a local copy of the
     * cache is considered valid.
     */
    public long getLocalReadTimeout();

  }


}
