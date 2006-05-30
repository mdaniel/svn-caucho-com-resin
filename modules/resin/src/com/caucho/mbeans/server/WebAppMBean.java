/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.mbeans.server;

/**
 * MBean API for the WebApp.
 */
public interface WebAppMBean extends DeployControllerMBean {
  /**
   * Returns the root directory.
   */
  public String getRootDirectory();

  /**
   * Returns the application's context path.
   */
  public String getContextPath();

  /**
   * Returns the session timeout
   */
  public long getSessionTimeout();

  /**
   * Returns the persistent store type.
   */
  public String getSessionStoreType();

  /**
   * Returns the current number of requests being serviced by the web-app.
   */
  public int getConnectionCount();

  /**
   * Returns the count of active sessions.
   */
  public int getActiveSessionCount();

  /**
   * Returns the count of active sessions.
   */
  public long getSessionActiveCount();

  /**
   * Returns the count of sessions created
   */
  public long getSessionCreateCount();

  /**
   * Returns the count of sessions invalidated
   */
  public long getSessionInvalidateCount();

  /**
   * Returns the count of sessions timeout
   */
  public long getSessionTimeoutCount();

  /**
   * Returns the total number of requests serviced by the web-app
   * since it started.
   */
  public long getLifetimeConnectionCount();

  /**
   * Returns the total duration in milliseconds that connections serviced by
   * this web-app have taken.
   */
  public long getLifetimeConnectionTime();

  /**
   * Returns the total number of bytes that connections serviced by
   * this web-app have read.
   */
  public long getLifetimeReadBytes();

  /**
   * Returns the total number of bytes that connections serviced by this
   * web-app have written.
   */
  public long getLifetimeWriteBytes();

  /**
   * Returns the number of connections that have ended with a
   * {@link com.caucho.vfs.ClientDisconnectException} for this web-app in it's lifetime.
   */
  public long getLifetimeClientDisconnectCount();
}
