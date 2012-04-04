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

package com.caucho.server.admin;

import java.util.logging.Logger;

import com.caucho.bam.proxy.ReplyCallback;
import com.caucho.env.deploy.DeployControllerService;
import com.caucho.env.deploy.DeployTagItem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.util.L10N;

public class DeployActorProxyImpl
{
  private static final L10N L = new L10N(DeployActorProxyImpl.class);
  
  private static final Logger log
    = Logger.getLogger(DeployActorProxyImpl.class.getName());
  
  private static final String UID = "deploy";
  
  private DeployActor _deployActor;
  private DeployActorProxy _podDeployProxy;
  
  public DeployActorProxyImpl(DeployActor deployActor)
  {
    _deployActor = deployActor;
  }
  
  public void restartCluster(String tag, ReplyCallback<ControllerStateActionQueryReply> cb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public ControllerStateActionQueryReply controllerRestart(String tag)
  {
    LifecycleState state = restart(tag);

    ControllerStateActionQueryReply result
      = new ControllerStateActionQueryReply(tag, state);

    log.fine(this
             + " restart '"
             + tag
             + "' -> "
             + state.getStateName());

    return result;
  }

  private LifecycleState restart(String tag)
  {
    DeployControllerService service = DeployControllerService.getCurrent();

    final DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      throw new IllegalArgumentException(L.l("'{0}' is an unknown controller",
                                             tag));

    ThreadPool.getCurrent().schedule(new Runnable() {
      public void run() {
        controller.toRestart();
      }
    });

    return controller.getState();
  }
  
  public String getUid()
  {
    return UID;
  }
}
