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

package com.caucho.env.service;

/**
 * Interface for a service registered with the Resin Server.
 */
public interface ResinSubSystem
{
  // the start priority of an environment service with no network dependencies
  public static final int START_PRIORITY_ENV_SYSTEM = 30;
  
  public static final int START_PRIORITY_DATABASE_SYSTEM = 35;
  
  // the start priority of the cluster network service
  public static final int START_PRIORITY_NETWORK_CLUSTER = 40;
  
  // the lowest priority of a cache service
  public static final int START_PRIORITY_CACHE_SERVICE = 45;
  
  // the lowest priority of a cluster service
  public static final int START_PRIORITY_CLUSTER_SERVICE = 50;
  
  public static final int START_PRIORITY_CLASSLOADER = 70;
  public static final int STOP_PRIORITY_CLASSLOADER = 10;
  
  public static final int START_PRIORITY_DEFAULT = 80;

  /**
   * Returns the start priority of the service, used to determine which
   * services to start first.
   */
  public int getStartPriority();
  
  /**
   * Starts the service.
   * @throws Exception 
   */
  public void start()
    throws Exception;
  
  /**
   * Returns the stop priority of the service, used to determine which
   * services to stop first.
   */
  public int getStopPriority();

  /**
   * Stops the service.
   * @throws Exception 
   */
  public void stop() throws Exception;
  
  /**
   * Destroys the service.
   */
  public void destroy();
}
