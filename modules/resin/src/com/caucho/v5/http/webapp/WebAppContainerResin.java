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

package com.caucho.v5.http.webapp;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;

import com.caucho.http.rewrite.RewriteDispatch;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.dispatch.Invocation;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.rewrite.DispatchRule;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.http.webapp.WebAppContainer;
import com.caucho.v5.http.webapp.WebAppController;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.vfs.Path;


/**
 * Resin's virtual host implementation.
 */
public class WebAppContainerResin extends WebAppContainer
{
  private RewriteDispatch _rewriteDispatch;
  
  //private DeployContainerImpl<EnterpriseApplication, EarDeployController> _earDeploy;
  //private DeployGeneratorWebAppEar _webAppGeneratorEar;
  
  /*
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<>();
    */

  /**
   * Creates the webApp with its environment loader.
   */
  public WebAppContainerResin(HttpContainer server,
                         Host host,
                         Path rootDirectory,
                         EnvironmentClassLoader loader,
                         Lifecycle lifecycle)
  {
    super(server, host, rootDirectory, loader, lifecycle);
  }
  
  protected void initConstructor()
  {
    super.initConstructor();
    
    // These need to be in the proper class loader so they can
    // register themselves with the environment
    // _earDeploy = new DeployContainerImpl<>(EarDeployController.class);
  }

  @Override
  protected FilterChain buildFilterChain(Invocation invocation,
                                         FilterChain chain)
  {
    if (_rewriteDispatch != null) {
      String uri = invocation.getURI();
      String queryString = invocation.getQueryString();

      FilterChain rewriteChain = _rewriteDispatch.map(DispatcherType.REQUEST,
                                                      uri,
                                                      queryString,
                                                      chain);

      if (rewriteChain != chain) {
        // server/13sf, server/1kq1, server/1krd
        // WebApp rootWebApp = findWebAppByURI("/");
        WebApp rootWebApp = findWebAppByURI(uri);
        
        if (rootWebApp == null) {
          // server/1u12
          rootWebApp = getErrorWebApp();
        }

        invocation.setWebApp(rootWebApp);
        
        // server/1k21 vs server/1kk7
        // if (rootWebApp != webApp)
        rewriteChain = rootWebApp.getDispatcher().createWebAppFilterChain(rewriteChain, 
                                                          invocation,
                                                          true);

        invocation.setFilterChain(rewriteChain);
        // isAlwaysModified = false;
        
        // http/1kw1
        return rewriteChain;
      }
    }
    
    return chain;
  }

  @Override
  public WebAppController createWebAppController(String id,
                                                 Path rootDirectory,
                                                 String urlPrefix)
  {
    // DeployHandle<WebApp> handle = createHandle(id);
    
    return new WebAppControllerResin(id, rootDirectory, this, urlPrefix);
  }

  public void add(DispatchRule dispatchRule)
  {
    if (_rewriteDispatch == null) {
      _rewriteDispatch = new RewriteDispatch(getHttpContainer());
    }
    
    _rewriteDispatch.addRule(dispatchRule);
  }

  /*
  public Collection<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }

  public DeployContainer<EnterpriseApplication, ?> getEarDeployContainer()
  {
    return _earDeploy;
  }
  */

  /**
   * @return
   */
  /*
  public EarDeployGenerator createEarDeploy()
  {
    return new EarDeployGenerator(getId(),
                                  _earDeploy,
                                  this);
  }
  */

  /**
   * ear-deploy: configures ear deployment directory
   */
  /*
  public void addEarDeploy(EarDeployGenerator earDeploy)
  {
    _earDeploy.add(earDeploy);
    
    if (_webAppGeneratorEar == null) {
      DeployContainer<WebApp, WebAppController> webAppDeploy = getWebAppDeployContainer();
      
      _webAppGeneratorEar = new DeployGeneratorWebAppEar(webAppDeploy,
                                                         this,
                                                         _earDeploy);
      
      webAppDeploy.add(_webAppGeneratorEar);
      
      // _webAppGeneratorEar.start();
    }
  }
  */
}
