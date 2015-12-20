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

package com.caucho.v5.bartender.admin;

import io.baratine.core.Result;
import io.baratine.files.BfsFileSync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.caucho.v5.baratine.ServiceApi;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ClusterBartender;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.vfs.WriteStream;

/**
 * /proc/services
 */
@ServiceApi(BfsFileSync.class)
public class ProcWebApps extends ProcFileBase
{
  private final BartenderSystem _bartender;
  
  public ProcWebApps(BartenderSystem bartender)
  {
    super("/webapps");
    
    _bartender = bartender;
  }

  @Override
  public void list(Result<String[]> result)
  {
    ArrayList<String> clusterNames = new ArrayList<>();
    
    for (ClusterBartender cluster :_bartender.getRoot().getClusters()) {
      clusterNames.add(cluster.getId());
    }
    
    Collections.sort(clusterNames);
    
    String []clusterNameArray = new String[clusterNames.size()];
    clusterNames.toArray(clusterNameArray);
    
    result.ok(clusterNameArray);
  }

  @Override
  protected boolean fillRead(WriteStream out)
    throws IOException
  {
    HttpContainerServlet httpContainer = HttpContainerServlet.current();
    
    if (httpContainer == null) {
      out.println("[]");
      return true;
    }
    
    out.println("[");
    
    boolean isFirstHost= true;
    for (DeployHandle<Host> hostController : httpContainer.getHostHandles()) {
      Host host = hostController.getDeployInstance();
      
      if (host == null) {
        continue;
      }
      
      if (! isFirstHost) {
        out.println(",");
      }
      isFirstHost = false;
      
      out.println("{ \"host\" : \"" + host.getIdTail() + "\",");
      
      out.println("  \"web-apps\" : [");
      
      boolean isFirstWebApp = true;
      
      for (DeployHandle<WebApp> webAppCtrl : host.getWebAppContainer().getWebAppHandles()) {
        if (! isFirstWebApp) {
          out.println(",");
        }
        isFirstWebApp = false;
        
        out.println("{ \"web-app\" : \"" + webAppCtrl.getId() + "\",");
        out.println("  \"state\" : \"" + webAppCtrl.getState() + "\",");
        
        if (webAppCtrl.getConfigException() != null) {
          out.println("  \"exception\" : \"" + webAppCtrl.getConfigException() + "\",");
        }
        
        WebApp webApp = webAppCtrl.getDeployInstance();
        
        if (webApp == null) {
          out.println("}");
          continue;
        }
        
        out.println("}");
      }
      out.println("  ]");
      
      out.println("}");
    }
    
    out.println("]");
    
    return true;
  }
}
