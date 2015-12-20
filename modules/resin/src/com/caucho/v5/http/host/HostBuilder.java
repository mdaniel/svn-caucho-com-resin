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

package com.caucho.v5.http.host;

import java.util.Objects;

import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.http.webapp.WebAppContainer;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.L10N;

/**
 * Builder for the webapp to encapsulate the configuration process.
 */
public class HostBuilder implements DeployInstanceBuilder<Host>
{
  private static final L10N L = new L10N(HostBuilder.class);
  
  private final HostController _controller;
  private EnvironmentClassLoader _classLoader;
  
  private Throwable _configException;

  // XXX: transitional copy of the web-app during building 
  private Host _host;

  private HostContainer _hostContainer;

  private String _hostName;
  
  /**
   * Builder Creates the webApp with its environment loader.
   */
  public HostBuilder(HostContainer parent, 
                     HostController controller, 
                     String hostName)
  {
    Objects.requireNonNull(parent);
    Objects.requireNonNull(controller);
    Objects.requireNonNull(hostName);
    
    _hostContainer = parent;
    
    _controller = controller;
    
    _hostName = hostName;

    _classLoader
      = EnvironmentClassLoader.create(controller.getParentClassLoader(),
                                      "host:" + getId());
  }
  
  public Host getInstance()
  {
    return getHost();
  }
  
  String getId()
  {
    return _controller.getId();
  }

  HostContainer getHostContainer()
  {
    return _hostContainer;
  }

  HostController getController()
  {
    return _controller;
  }
  
  String getHostName()
  {
    return _hostName;
  }
  
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  @Override
  public void setConfigException(Throwable exn)
  {
    if (exn != null) { 
      exn.printStackTrace();
    
      getHost().setConfigException(exn);
    }
  }
  
  @Override
  public Throwable getConfigException()
  {
    return _configException;
  }
  
  @Override
  public void preConfigInit()
  {
    getHost().preConfigInit();
  }

  protected WebAppContainer getWebAppContainer()
  {
    return getHost().getWebAppContainer();
  }
  
  public void addProgram(ConfigProgram program)
  {
    Host host = getHost();
    
    Objects.requireNonNull(host);
    
    program.configure(host);
  }
  
  @Override
  public Host build()
  {
    return getHost();
  }
  
  protected Host getHost()
  {
    Host host = _host;
    
    if (host == null) {
      _host = host = createHost();
    }
    
    return host;
  }
  
  protected Host createHost()
  {
    return new Host(this);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
