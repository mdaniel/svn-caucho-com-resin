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

package com.caucho.v5.server.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.util.ThreadDump;




/**
 * interface to the public management
 */
public class AdminRestServiceImpl
{
  public String threadDump()
  {
    ThreadDump threadDump = ThreadDump.create();
    
    int depth = 16;
    boolean isOnlyActive = true;

    return threadDump.getThreadDump(depth, isOnlyActive);
  }

  public String []webAppList()
  {
    HttpContainerServlet httpContainer = HttpContainerServlet.current();
    
    if (httpContainer == null) {
      return null;
    }
    
    ArrayList<String> webAppList = new ArrayList<>();
    
    for (DeployHandle<Host> hostHandle : httpContainer.getHostHandles()) {
      Host host = hostHandle .getDeployInstance();
      
      if (host == null) {
        continue;
      }
      
      for (DeployHandle<WebApp> webAppCtrl : host.getWebAppContainer().getWebAppHandles()) {
        WebApp webApp = webAppCtrl.getDeployInstance();
        
        if (webApp == null) {
          continue;
        }

        webAppList.add(webApp.getId());
      }
    }
    
    String []webAppArray = new String[webAppList.size()];
    webAppList.toArray(webAppArray);
    
    return webAppArray;
  }

  public Object webAppStatus(String id)
  {
    HttpContainerServlet httpContainer = HttpContainerServlet.current();
    
    if (httpContainer == null) {
      return null;
    }
    
    DeployHandle<WebApp> webApp = httpContainer.findWebAppHandle(id);
    
    if (webApp == null) {
      return null;
    }
    
    Map<String,Object> props = new LinkedHashMap<>();
    
    props.put("id", webApp.getId());
    props.put("state", webApp.getState().getStateName());
    
    Throwable exn = webApp.getConfigException();
    
    if (exn != null) {
      props.put("exception", String.valueOf(exn));
    }
    
    return props;
  }
}
