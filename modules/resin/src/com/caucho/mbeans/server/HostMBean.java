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
 * Management interface for the host.
 */

public interface HostMBean extends DeployControllerMBean {
  /**
   * Returns the host name.
   */
  public String getHostName();

  /**
   * Returns the URL
   */
  public String getURL();

  /**
   * Returns the root directory.
   */
  public String getRootDirectory();

  /**
   * Returns the document directory.
   */
  public String getDocumentDirectory();

  /**
   * Returns the primary war directory.
   */
  public String getWarDirectory();

  /**
   * Returns the primary war expand directory.
   */
  public String getWarExpandDirectory();

  /**
   * Returns an array of the webapp names.
   */
  public String []getWebAppObjectNames();

  /**
   * Returns an array of the webapp names
   * (obsolete, use {@link #getWebAppObjectNames()}.
   */
  public String []getWebAppNames();


  /**
   * Updates a web-app entry from the deployment directories.
   */
  public void updateWebAppDeploy(String name)
    throws Throwable;

  /**
   * Updates an ear entry from the deployment directories.
   */
  public void updateEarDeploy(String name)
    throws Throwable;

  /**
   * Expand an ear entry from the deployment directories.
   */
  public void expandEarDeploy(String name);

  /**
   * Start an ear entry from the deployment directories.
   */
  public void startEarDeploy(String name);
}
