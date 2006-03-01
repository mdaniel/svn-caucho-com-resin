/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.server.e_app;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import com.caucho.util.L10N;
import com.caucho.util.CauchoSystem;

import com.caucho.vfs.Path;

import com.caucho.config.ConfigException;

import com.caucho.make.Dependency;

import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.deploy.DeployContainer;

import com.caucho.server.webapp.ApplicationContainer;
import com.caucho.server.webapp.WebAppController;

/**
 * The generator for the ear-deploy
 */
public class EarSingleDeployGenerator extends DeployGenerator<EarDeployController> {
  private String _urlPrefix = "";

  private ApplicationContainer _parentContainer;
  
  private EarDeployController _entry;

  public EarSingleDeployGenerator(DeployContainer<EarDeployController> deployContainer,
			 ApplicationContainer parentContainer,
			 EarConfig config)
  {
    super(deployContainer);
    
    _parentContainer = parentContainer;

    _entry = new EarDeployController("", parentContainer, config);
  }

  /**
   * Returns the parent container;
   */
  ApplicationContainer getContainer()
  {
    return _parentContainer;
  }

  /**
   * Returns any matching web-app entry.
   */
  public WebAppController findWebAppEntry(String name)
  {
    WebAppController entry = _entry.findWebAppController(name);

    return entry;
  }
  
  /**
   * Returns the current array of application entries.
   */
  public EarDeployController createEntry(String name)
    throws Exception
  {
    return null;
  }
}
