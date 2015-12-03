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

package com.caucho.env.deploy;

import com.caucho.lifecycle.LifecycleState;

/**
 * The start-mode="manual", redeploy-model="manual" controller strategy.
 *
 * initial state = stop
 *
 * <table>
 * <tr><th>input  <th>stopped  <th>active  <th>modified   <th>error
 * <tr><td>start  <td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>update <td>startImpl<td>-       <td>restartImpl<td>restartImpl
 * <tr><td>stop   <td>-        <td>stopImpl<td>stopImpl   <td>stopImpl
 * <tr><td>request<td>-        <td>-       <td>-          <td>-
 * <tr><td>include<td>-        <td>-       <td>-          <td>-
 * <tr><td>alarm  <td>-        <td>-       <td>-          <td>-
 * </table>
 */
public class StartManualRedeployManualStrategy
  extends AbstractDeployControllerStrategy {
  public final static StartManualRedeployManualStrategy STRATEGY
    = new StartManualRedeployManualStrategy();

  protected StartManualRedeployManualStrategy()
  {
  }

  /**
   * Returns the start="lazy" redeploy="automatic" strategy
   *
   * @return the singleton strategy
   */
  public static DeployControllerStrategy create()
  {
    return STRATEGY;
  }
  
  /**
   * Called at initialization time for automatic start.
   *
   * @param controller the owning controller
   */
  @Override
  public<I extends DeployInstance>
  void startOnInit(DeployController<I> controller)
  {
    controller.stopImpl();
  }

  /**
   * Checks for updates from an admin command.  The target state will be the
   * initial state, i.e. update will not start a lazy instance.
   *
   * @param controller the owning controller
   */
  @Override
  public<I extends DeployInstance>
  void update(DeployController<I> controller)
  {
    LifecycleState state = controller.getState();
    
    if (state.isStopped()) {
      controller.startImpl();
    }
    else if (state.isError()) {
      controller.restartImpl();
    }
    else if (controller.isModifiedNow()) {
      controller.restartImpl();
    }
    else { /* active */
    }
  }


  /**
   * Returns the current instance.  This strategy does not lazily restart
   * the instance.
   *
   * @param controller the owning controller
   * @return the current deploy instance
   */
  /* XXX: should request always return an instance? */
  @Override
  public <I extends DeployInstance>
  I request(DeployController<I> controller)
  {
    return controller.getDeployInstance();
  }

  /**
   * Returns the current instance.  This strategy does not lazily restart
   * the instance.
   *
   * @param controller the owning controller
   * @return the current deploy instance
   */
  /* XXX: should request always return an instance? */
  @Override
  public <I extends DeployInstance>
  I subrequest(DeployController<I> controller)
  {
    return controller.getDeployInstance();
  }

  /**
   * Returns the current instance.  This strategy does not lazily restart
   * the instance.
   *
   * @param controller the owning controller
   */
  /* XXX: should request always return an instance? */
  @Override
  public <I extends DeployInstance>
  void alarm(DeployController<I> controller)
  {
  }
}
