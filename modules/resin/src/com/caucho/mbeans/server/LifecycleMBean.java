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

import java.util.Date;

import com.caucho.jmx.Description;

/**
 * MBeans which have lifecycle transitions will normally implement
 * the lifecycle mbean.
 */
public interface LifecycleMBean {
  /**
   * Returns the lifecycle state.
   */
  @Description("The lifecycle state of the client")
  public String getState();

  //
  // Statistics attributes
  //

  /**
   * Returns the number of transitions to the failed state.
   */
  @Description("The total number of failures")
  public long getFailTotalCount();

  /**
   * Returns the time of the last failure.
   */
  @Description("The time of the last failure")
  public Date getLastFailTime();

  //
  // Operations on the mbean
  //

  /**
   * Enables the mbean
   */
  @Description("Enables the mbean")
  public void start();

  /**
   * Disables the mbean
   */
  @Description("Disables the mbean")
  public void stop();
}
