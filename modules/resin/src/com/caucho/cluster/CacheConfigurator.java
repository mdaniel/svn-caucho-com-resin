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

import com.caucho.config.Configurable;

import javax.cache.CacheListener;

public interface CacheConfigurator extends CacheConfiguration {

  public interface Authorization extends CacheConfiguration.Authorization {

    /**
     * Adds an authorizer to the cache.
     *
     * @note: All authorizers must approve, and are called in the order they were entered.
     */
    @Configurable
    public void addAuthorizer(Authorization authorizer);

    /**
     * Removes an authorizer from the list.
     */
    @Configurable
    public void removeAuthorizer(Authorization authorizer);
  }


  public interface Name extends CacheConfiguration.Name {

    /**
     * Defines the required name of this cache.
     */
    @Configurable
    public void setName(String name);

  }


  public interface Provider extends CacheConfiguration.Provider {

    /**
     * Sets the CacheLoader that the Cache can then use to populate cache misses for a reference store (database)
     */
    @Configurable
    public void setCacheLoader(CacheLoader cacheLoader);

    /**
     * Sets the {@link CacheConfiguration.Scope} of this cache.
     */
    @Configurable
    public void setScope(Scope scope);

    /**
     * Set the {@link CacheConfiguration.Scope} of this cache by name.
     *
     * @param scope
     */
    @Configurable
    public void setScopeName(String scope);

    /**
     * Sets the {@link CacheConfiguration.Persistence} of this cache.
     */
    @Configurable
    public void setPersistence(Persistence persistence);

    @Configurable
    public void setNoPersistence(boolean noPersistence);

    /**
     * Used to set the peristence of this cache to SINGLE.
     */
    @Configurable
    public void setSinglePersistence(boolean singlePersistence);

    /**
     * Used to set the peristence of this cache to TRIPLE.
     */
    @Configurable
    public void setTriplePersistence(boolean triplePersistence);

    /**
     * Sets the globally-unique id for the cache
     */
    public void setGuid(String guid);

  }


  /**
   * Defines the time values that determine how an entry is managed by the Cache.
   *
   * @note These values are key to get the best performance from the Cache.
   * <p/>
   * TODO(fred): Develop default time configurations.
   */
  public interface Timer extends CacheConfiguration.Time {


    /**
     * The maximum valid time for an item.  Items stored in the cache
     * for longer than the expire time are no longer valid and will
     * return null from a get.
     * <p/>
     * The deefault is infinite.
     */
    @Configurable
    public void setExpireTimeout(long expireTimeout);

    /**
     * Provides the opportunity to control the expire check window,
     * i.e. the precision of the expirecheck.
     * <p/>
     * Since an expired item can cause a massive cascade of
     * attempted loads from the backup, the actual expiration is randomized.
     */
    @Configurable
    public void setExpireTimeoutWindow(long expireTimeout);

    /**
     * The maximum time that an item can remain in cache without being referenced.
     * For example, session data could be configured to be removed if idle for more than 30 minutes.
     * <p/>
     * Cached data would typically use an infinite idle time because
     * it doesn't depend on how often it's accessed.
     */
    @Configurable
    public void setIdleTimeout(long timeout);

    /**
     * Provides the option to set the idle check window,  the amount of time
     * in which the idle time limit can be spread out to smooth performance.
     * <p/>
     * If this optional value is not set, the system  uses a fraction of the
     * idle time.
     */
    @Configurable
    public void setIdleTimeoutWindow(long timoutWindow);


    /**
     * The lease timeout is the time a server can use the local version
     * if it owns it, before a timeout.
     */
    @Configurable
    public void setLeaseTimeout(long leaseTimeout);


    /**
     * The local read timeout is the time a local copy of the
     * cache is considered valid.
     */
    @Configurable
    public void setLocalReadTimeout(long timeout);


  }


}
