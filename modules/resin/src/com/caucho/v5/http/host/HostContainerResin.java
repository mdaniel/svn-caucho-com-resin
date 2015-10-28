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

package com.caucho.v5.http.host;

import java.util.HashMap;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;

import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.dispatch.Invocation;
import com.caucho.v5.http.host.HostConfig;
import com.caucho.v5.http.host.HostContainer;
import com.caucho.v5.http.host.HostController;
import com.caucho.v5.http.rewrite.RewriteDispatch;
import com.caucho.v5.http.webapp.AccessLogFilterChain;
import com.caucho.v5.http.webapp.FilterChainWebApp;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.vfs.Path;

/**
 * Resin's host container implementation.
 */
public class HostContainerResin extends HostContainer
{
  // dispatch mapping
  private RewriteDispatch _rewriteDispatch;

  // List of default ear configurations
  /*
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();
    */


  /**
   * Creates the webApp with its environment loader.
   */
  public HostContainerResin(HttpContainer server)
  {
    super(server);
  }

  /**
   * Adds an ear default
   */
  /*
  public void addEarDefault(EarConfig init)
  {
    _earDefaultList.add(init);
  }
  */

  /**
   * Returns the list of ear defaults
   */
  /*
  public ArrayList<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }
  */

  @Override
  protected HostController createController(String id, 
                                            Path rootDir,
                                            String hostName,
                                            HostConfig config,
                                            HashMap<String, Object> varMap)
  {
    return new HostControllerResin(id, rootDir, hostName, config, this, varMap);
  }

  /**
   * Creates the invocation.
   */
  @Override
  public Invocation buildInvocation(Invocation invocation)
  {
    invocation = super.buildInvocation(invocation);
    
    String hostName = invocation.getHostName(); 

    if (_rewriteDispatch != null) {
      String url;

      if (invocation.isSecure())
        url = "https://" + hostName + invocation.getURI();
      else
        url = "http://" + hostName + invocation.getURI();

      String queryString = invocation.getQueryString();

      FilterChain chain = invocation.getFilterChain();
      FilterChain rewriteChain = _rewriteDispatch.map(DispatcherType.REQUEST,
                                                      url,
                                                      queryString,
                                                      chain);

      if (rewriteChain != chain) {
        HttpContainer server = getServer();
        WebApp webApp = server.getDefaultWebApp();
        invocation.setWebApp(webApp);

        if (webApp != null) {
          rewriteChain = new FilterChainWebApp(rewriteChain, webApp);

          if (webApp.getAccessLog() != null)
            rewriteChain = new AccessLogFilterChain(rewriteChain, webApp);
        }

        invocation.setFilterChain(rewriteChain);
      }
    }

    return invocation;
  }
}