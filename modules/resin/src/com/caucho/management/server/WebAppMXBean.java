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

package com.caucho.management.server;

import com.caucho.jmx.*;

/**
 * MBean API for the WebApp.
 *
 * <pre>
 * resin:type=WebAppMBean,name=/wiki,Host=foo.com
 * </pre>
 */
@Description("The web-app management interface")
public interface WebAppMXBean extends DeployControllerMXBean {
  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning host
   */
  @Description("The web-app's host")
  public HostMXBean getHost();

  /**
   * Returns the session manager
   */
  @Description("The web-app's session manager")
  public SessionManagerMXBean getSessionManager();

  //
  // Configuration attributes
  //
  
  /**
   * Returns the root directory.
   */
  public String getRootDirectory();

  /**
   * Returns the application's context path.
   */
  public String getContextPath();

  //
  // Statistics attributes
  //
  
  /**
   * Returns the current number of requests being serviced by the web-app.
   */
  @Description("Current number of requests served by the web-app")
  public int getRequestCount();

  /**
   * Returns the total number of requests serviced by the web-app
   * since it started.
   */
  @Description("Total number of requests served by the web-app since starting")
  public long getRequestCountTotal();

  /**
   * Returns the total duration in milliseconds that connections serviced by
   * this web-app have taken.
   */
  @Description("Total time taken by requests served by the web-app")
  @Units("milliseconds")
  public long getRequestTimeTotal();

  /**
   * Returns the total number of bytes that requests serviced by
   * this web-app have read.
   */
  @Description("Total bytes requests served by the web-app have read")
  @Units("bytes")
  public long getRequestReadBytesTotal();

  /**
   * Returns the total number of bytes that connections serviced by this
   * web-app have written.
   */
  @Description("Total bytes requests served by the web-app have written")
  @Units("bytes")
  public long getRequestWriteBytesTotal();

  /**
   * Returns the number of connections that have ended with a
   * {@link com.caucho.vfs.ClientDisconnectException} for this web-app in it's lifetime.
   */
  public long getClientDisconnectCountTotal();
}
