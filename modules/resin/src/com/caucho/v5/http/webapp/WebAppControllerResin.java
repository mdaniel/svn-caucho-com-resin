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

package com.caucho.v5.http.webapp;

import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.webapp.WebAppResinBuilder;
import com.caucho.v5.http.webapp.WebAppContainer;
import com.caucho.v5.http.webapp.WebAppController;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.PathImpl;

/**
 * Manages the lifecycle of a web-app. The same WebAppController is used for
 * each web-app instantiation, for example on restarts. It's only created or
 * destroyed if the web-app-deploy indicates it should be created/destroyed.
 * 
 * Each WebAppController corresponds to a DeployNetworkService tag with the
 * name "WebApp/[host]/[context-path]"
 */
@ModulePrivate
public class WebAppControllerResin extends WebAppController
{
  public WebAppControllerResin(String id,
                               PathImpl rootDirectory, 
                               WebAppContainer container,
                               String urlPrefix)
  {
    super(id, rootDirectory, container, urlPrefix);
  }

  /**
   * Instantiate the webApp.
   */
  @Override
  protected WebAppResinBuilder createInstanceBuilder()
  {
    return new WebAppBuilderResin(this);
  }
}
