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

package com.caucho.server.e_app;

import java.util.ArrayList;

import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.ExpandDeployGenerator;
import com.caucho.env.deploy.ExpandVersion;
import com.caucho.inject.Module;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.vfs.Path;

/**
 * The generator for the ear-deploy
 */
@Module
public class EarDeployGenerator
  extends ExpandDeployGenerator<EarDeployController>
{
  private final EarDeployGeneratorAdmin _admin
    = new EarDeployGeneratorAdmin(this);

  private String _urlPrefix = "";

  private WebAppContainer _parentContainer;
  
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();

  public EarDeployGenerator(String id,
                            DeployContainer<EarDeployController> deployContainer,
                            WebAppContainer parentContainer)
  {
    super(id, deployContainer, parentContainer.getRootDirectory());

    try {
      setExpandPrefix("_ear_");
      setExtension(".ear");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    _parentContainer = parentContainer;

    _earDefaultList.addAll(parentContainer.getEarDefaultList());
  }

  /**
   * Returns the parent container;
   */
  WebAppContainer getContainer()
  {
    return _parentContainer;
  }

  /**
   * Gets the URL prefix.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  /**
   * Sets the URL prefix.
   */
  public void setURLPrefix(String prefix)
  {
    _urlPrefix = prefix;
  }

  /**
   * Sets the ear default.
   */
  public void addEarDefault(EarConfig config)
  {
    _earDefaultList.add(config);
  }

  @Override
  protected void initImpl()
    throws ConfigException
  {
    super.initImpl();
  }

  @Override
  protected void startImpl()
    throws ConfigException
  {
    super.startImpl();

    _admin.register();
  }
 
  /**
   * Returns the current array of application entries.
   */
  @Override
  public EarDeployController createController(ExpandVersion version)
  {
    String key = version.getKey();
    
    String tag = getId() + "/" + key;
    
    Path rootDirectory = getExpandPath(key);
    Path archivePath = getArchivePath(key);

    EarDeployController controller
      = new EarDeployController(tag, rootDirectory, key, 
                                _parentContainer, null);

    controller.setArchivePath(archivePath);
    controller.setRedeployMode(getRedeployMode());

    for (EarConfig config : _earDefaultList) {
      controller.addConfigDefault(config);
    }

    return controller;
  }

  @Override
  protected void destroyImpl()
  {
    _admin.unregister();

    super.destroyImpl();
  }
}
