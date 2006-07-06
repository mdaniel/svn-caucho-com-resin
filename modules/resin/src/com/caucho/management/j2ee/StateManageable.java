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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.management.j2ee;

/**
 * Base class interface for managed objects that have lifecycle state management.
 *
 * {@link #getState()} returns an integer representing the current state.
 *
 * <table>
 * <tr><th>name<th>value<th>description<th>transition operations allowed
 * <tr><td>STARTING<td>0<td>the managed object is starting<td>
 * <tr><td>RUNNING <td>1<td>the managed object is running<td>{@link #stop()}
 * <tr><td>STOPPING<td>2<td>the managed object is stopping<td>
 * <tr><td>STOPPEDG<td>3<td>the managed object is stoped<td>{@link #start()}
 * <tr><td>FAILED  <td>4<td>an attempt to start or stop the managed object failed<td>{@link #start()}, {@link #stop()}
 * </table>
 */
public interface StateManageable {
  public static final int STARTING = 0;
  public static final int RUNNING = 1;
  public static final int STOPPING = 2;
  public static final int STOPPED = 3;
  public static final int FAILED = 4;

  /**
   * The time that the managed object was started and entered the {@link RUNNING}
   * state.  The value is the number of milliseconds since 00:00 January 1, 1970.
   */
  public long getStartTime();

  /**
   * Returns the current state of the managed object.
   *
   */
  public int getState();

  /**
   * Start the managed object when it is in the {@link STOPPED} or {@link FAILED}
   * state.
   *
   * If there are children of this managed object they are <i>not</i>
   * started, see {@link #startRecursive()}.
   *
   * An {@link EventProvider} will issue a
   * {@link NotificationTypes.J2EE_STATE_STARTING} notification, and then
   * depending on the success of the start a
   * {@link NotificationTypes.J2EE_STATE_RUNNING} or a
   * {@link NotificationTypes.J2EE_STATE_FAILED} notification.
   */
  public void start();

  /**
   * Starts the managed object (See {@link #start()}, and then recursively
   * starts any children.
   */
  public void startRecursive();

  /**
   * Stop the managed object when it is in the {@link RUNNING} or {@link FAILED}
   * state.
   *
   * An {@link EventProvider} will issue a
   * {@link NotificationTypes.J2EE_STATE_STARTING} notification, and then
   * depending on the success of the start a
   * {@link NotificationTypes.J2EE_STATE_RUNNING} or a
   * {@link NotificationTypes.J2EE_STATE_FAILED} notification.
   */
  public void stop();

}
