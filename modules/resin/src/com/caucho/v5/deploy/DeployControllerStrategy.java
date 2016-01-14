/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

import com.caucho.v5.amp.spi.ShutdownModeAmp;


/**
 * DeployController controls the lifecycle of the DeployInstance.
 *
 * <h3>States</h3>
 *
 * <ul>
 * <li>active - responds to requests normally
 * <li>modified - active with a dependency change like a web.xml
 * <li>active-idle - active idle state, which can be stopped on timeout
 * <li>active-error - configuration error (before timeout)
 * <li>error - configuration error (after timeout)
 * <li>stop - admin stop, refuses requests with a 503
 * <li>stop-lazy - lazy stop
 * </ul>
 *
 * error-wait is equivalent to the active state.  idle and inactive only
 * applies to startup=lazy.
 *
 * <h3>events</h3>
 *
 * <ul>
 * <li>startOnInit - called at startup time for automatic start
 * <li>start - admin start
 * <li>stop - admin stop
 * <li>update - admin update/restart, ends up in initial state
 * <li>request - top-level request
 * <li>subrequest - include/forward
 * <li>alarm - timeout
 * <li>
 */
public interface DeployControllerStrategy
{
  /**
   * Called at initialization time for automatic start.
   *
   * @param controller the owning controller
   */
  <I extends DeployInstance>
  void startOnInit(DeployControllerServiceImpl<I> controller);

  /**
   * Starts the instance from an admin command.
   *
   * @param controller the owning controller
   */
  <I extends DeployInstance>
  void start(DeployControllerServiceImpl<I> controller);

  /**
   * Stops the instance from an admin command.
   *
   * @param controller the owning controller
   */
  <I extends DeployInstance>
  void stop(DeployControllerServiceImpl<I> controller,
            ShutdownModeAmp mode);

  /**
   * Checks for updates from an admin command.  The target state will be the
   * initial state, i.e. update will not start a lazy instance.
   *
   * @param controller the owning controller
   */
  <I extends DeployInstance>
  void update(DeployControllerServiceImpl<I> controller);

  /**
   * On a top-level request, returns the deploy instance, starting if necessary.
   *
   * @param controller the owning controller
   * @return the active deploy instance or null if none are active
   */
  /* XXX: should request always return an instance? */
  <I extends DeployInstance>
  I request(DeployControllerServiceImpl<I> controller);

  /**
   * On a sub-request, returns the deploy instance, starting if necessary.
   *
   * @param controller the owning controller
   * @return the active deploy instance or null if none are active
   */
  /* XXX: should request always return an instance? */
  <I extends DeployInstance>
  I subrequest(DeployControllerServiceImpl<I> controller);


  /**
   * On a timeout, update or restart as necessary.
   *
   * @param controller the owning controller
   */
  <I extends DeployInstance>
  void alarm(DeployControllerServiceImpl<I> controller);

}
