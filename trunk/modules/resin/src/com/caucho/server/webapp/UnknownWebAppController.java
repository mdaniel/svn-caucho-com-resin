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

package com.caucho.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import com.caucho.config.Config;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.deploy.DeployConfig;
import com.caucho.env.deploy.DeployControllerAdmin;
import com.caucho.env.deploy.EnvironmentDeployController;
import com.caucho.inject.Module;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.host.Host;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.vfs.Path;

/**
 * Manages the lifecycle of a web-app. The same WebAppController is used for
 * each web-app instantiation, for example on restarts. It's only created or
 * destroyed if the web-app-deploy indicates it should be created/destroyed.
 * 
 * Each WebAppController corresponds to a DeployNetworkService tag with the
 * name "WebApp/[host]/[context-path]"
 */
@Module
public class UnknownWebAppController extends WebAppController
{
  public UnknownWebAppController(String id, 
                          Path rootDirectory, 
                          WebAppContainer container)
  {
    super(id, rootDirectory, container);
  }

  /**
   * Instantiate the webApp.
   */
  @Override
  protected WebApp instantiateDeployInstance()
  {
    return new UnknownWebApp(this);
  }
}
