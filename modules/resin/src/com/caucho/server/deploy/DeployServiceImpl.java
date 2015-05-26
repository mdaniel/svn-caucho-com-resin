/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.server.deploy;

import com.caucho.baratine.Remote;
import io.baratine.core.Result;
import io.baratine.core.Service;
import com.caucho.baratine.ServiceApi;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.caucho.amp.thread.ThreadPool;
import com.caucho.deploy.DeploySystem;
import com.caucho.deploy.DeployTagItem;
import com.caucho.env.system.SystemManager;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.util.L10N;

@Service("public:///deploy")
@Remote
@ServiceApi(DeployService.class)
public class DeployServiceImpl
{
  private static final L10N L = new L10N(DeployServiceImpl.class);
  
  private static final Logger log
    = Logger.getLogger(DeployServiceImpl.class.getName());
  
  public static final String ADDRESS = "/deploy";
  
  private String _serverId;
  // private RepositorySpi _repository;
  
  private final AtomicBoolean _isInit = new AtomicBoolean();
  
  private DeployService _podDeployProxy;
  
  public DeployServiceImpl()
  {
    _serverId = SystemManager.getCurrentId();
    /*
    RampManager rampManager = AmpSystem.getCurrentManager();
    
    _podDeployProxy
      = cloudManager.createPodAllProxy(DeployService.class, 
                                       ADDRESS);
                                       */
  }

  public void init()
  {
    if (_isInit.getAndSet(true)) {
      return;
    }

    //_repository = RepositorySystem.getCurrentRepositorySpi();

    /*
    Broker broker = BamSystem.getCurrentBroker();
    BamManager bamManager = BamSystem.getCurrentManager();
    
    Mailbox mailbox = bamManager.createService(ADDRESS, this);
    
    String proxyAddress = UID + '@' + broker.getAddress();
    bamManager.addMailbox(proxyAddress, mailbox);
    */
  }

  public DeployTagResult[] queryTags(String string)
  {
    DeploySystem deploy = DeploySystem.getCurrent();

    Set<String> tags = deploy.getTagNames();

    DeployTagResult[] results = new DeployTagResult[tags.size()];

    int i = 0;

    for (String tag : tags) {
      results[i++] = new DeployTagResult(tag, "");
    }

    return results;
  }

  public DeployTagStateQuery getTagState(String tag)
  {
    // XXX: just ping the tag?
    // updateDeploy();
    
    DeploySystem deploy = DeploySystem.getCurrent();
    DeployTagItem item = null;
    
    if (deploy != null) {
      deploy.update(tag);
      item = deploy.getTagItem(tag);
    }
    
    if (item != null) {
      return new DeployTagStateQuery(tag, item.getStateName(),
                               item.getDeployException());
    }
    else {
      return null;
    }
  }

  //
  // start/restart
  //

  public DeployControllerState start(String tag)
  {
    LifecycleState state = startImpl(tag);

    DeployControllerState result
      = new DeployControllerState(tag, state);

    log.fine(this
             + " start '"
             + tag
             + "' -> "
             + state.getStateName());

    return result;
  }

  private LifecycleState startImpl(String tag)
  {
    DeploySystem service = DeploySystem.getCurrent();
    
    DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      throw new IllegalArgumentException(L.l("'{0}' is an unknown controller",
                                             tag));

    controller.toStart();

    return controller.getState();
  }

  /**
   * @deprecated
   */
  public DeployControllerState stop(String tag)
  {
    LifecycleState state = stopImpl(tag);

    log.fine(this + " stop '" + tag + "' -> " + state.getStateName());

    return new DeployControllerState(tag, state);
  }

  private LifecycleState stopImpl(String tag)
  {
    DeploySystem service = DeploySystem.getCurrent();

    DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      throw new IllegalArgumentException(L.l("'{0}' is an unknown controller",
                                             tag));
    controller.toStop();

    //windows WEB-INF/lib/*.jar release for eclipse-plugin's web-app-stop
    System.gc();

    return controller.getState();
  }

  public void controllerRestart(String tag,
                                Result<DeployControllerState> cb)
  {
    restart(tag, cb);
  }

  public void restart(String tag,
                      Result<DeployControllerState> cb)
  {
    restartImpl(tag, cb);
    /*
    LifecycleState state = restartImpl(tag);

    DeployControllerState result
      = new DeployControllerState(tag, state);

    log.fine(this
             + " restart '"
             + tag
             + "' -> "
             + state.getStateName());

    return result;
    */
  }

  public void restartCluster(String tag,
                             Result<DeployControllerState> cb)
  {
    _podDeployProxy.controllerRestart(tag, cb);
  }

  private void restartImpl(final String tag,
                           final Result<DeployControllerState> cb)
  {
    DeploySystem service = DeploySystem.getCurrent();

    final DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      throw new IllegalArgumentException(L.l("'{0}' is an unknown controller",
                                             tag));

    ThreadPool.getCurrent().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          controller.toRestart();
        } finally {
          cb.complete(new DeployControllerState(tag, controller.getState()));
        }
      }
    });

    // return controller.getState();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _serverId + "]";
  }
}
