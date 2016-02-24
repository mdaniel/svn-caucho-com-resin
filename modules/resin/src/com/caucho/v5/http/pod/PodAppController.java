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

package com.caucho.v5.http.pod;

import java.util.logging.Logger;

import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.web.webapp.HttpBaratine;

/**
 * A configuration entry for an pod-application, the services deployed to
 * a pod's node.
 */
public class PodAppController
  //extends PodControllerBase<PodApp>
{
  private static final Logger log
    = Logger.getLogger(PodAppController.class.getName());
  
  PodAppController(String id,
                   PathImpl rootDirectory,
                   PodBartender pod,
                   PodContainer podContainer)
  {
    /*
    super(id, 
          rootDirectory,
          pod,
          podContainer);
          */
    
    // setService(podContainer.getPodAppService(id));
  }

  /**
   * Instantiate the webApp.
   */
  //@Override
  protected DeployInstanceBuilder<PodApp> createInstanceBuilder()
  {
    //return new PodAppBuilder(this);
    return null;
  }
  
  //@Override
  protected DeployHandle<?> getHandle()
  {
    //return getContainer().getPodAppHandle(getId()).getHandle();
    return null;
  }

  //@Override
  protected void onStartComplete()
  {
    //super.onStartComplete();
    
    /*
    for (PodAppWeb web : getWebList()) {
      String contextPath = web.getContextPath();
      String dataPath = web.getPath();
      
      PathImpl path = getRootDirectory().lookup(dataPath);
      
      if (! path.isDirectory()) {
        continue;
      }
      
      HttpBaratine http = HttpBaratine.current();
      
      if (http == null) {
        continue;
      }
      
    }
    */
  }
}
