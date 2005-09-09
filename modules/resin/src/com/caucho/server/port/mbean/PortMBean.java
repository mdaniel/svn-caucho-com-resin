/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.port.mbean;

/**
 * Represents a protocol connection.
 */
public interface PortMBean {
  /**
   * Returns the port's protocol name.
   */
  public String getProtocolName();
  
  /**
   * Returns the port's host.
   */
  public String getHost();
  
  /**
   * Returns the port's port.
   */
  public int getPort();
  
  /**
   * Returns the maximum connections.
   */
  public int getConnectionMax();

  /**
   * Returns the maximum keepalive connections.
   */
  public int getKeepaliveMax();

  /**
   * Returns true for an active port.
   */
  public boolean isActive();

  /**
   * Returns the thread count.
   */
  public int getThreadCount();

  /**
   * Returns the active thread count.
   */
  public int getActiveThreadCount();

  /**
   * Returns the idle thread count.
   */
  public int getIdleThreadCount();

  /**
   * Returns the total connections.
   */
  public int getTotalConnectionCount();

  /**
   * Returns the active connections.
   */
  public int getActiveConnectionCount();

  /**
   * Returns the keepalive connections.
   */
  public int getKeepaliveConnectionCount();

  /**
   * Returns the select connections.
   */
  public int getSelectConnectionCount();

  /**
   * Returns the keepalive connection count.
   */
  public int getKeepaliveCount();

}
