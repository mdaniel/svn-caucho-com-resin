/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.server.deploy.ExpandDeployGenerator;
import com.caucho.server.deploy.DeployContainer;

import com.caucho.server.webapp.ApplicationContainer;
import com.caucho.server.webapp.WebAppController;

/**
 * The generator for the ear-deploy
 */
public class EarExpandDeployGenerator extends ExpandDeployGenerator<EarDeployController> {
  private String _urlPrefix = "";

  private ApplicationContainer _parentContainer;
  
  private EarConfig _earDefault;

  public EarExpandDeployGenerator(DeployContainer<EarDeployController> deployContainer,
			 ApplicationContainer parentContainer)
  {
    super(deployContainer);

    try {
      setExpandPrefix("_ear_");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    _parentContainer = parentContainer;
  }

  /**
   * Returns the parent container;
   */
  ApplicationContainer getContainer()
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
  public EarConfig createEarDefault()
  {
    _earDefault = new EarConfig();

    return _earDefault;
  }

  /**
   * Converts the archive name to the entry name, returns null if
   * the path name is not a valid entry name.
   */
  protected String archiveNameToEntryName(String archiveName)
  {
    if (! archiveName.endsWith(".ear"))
      return null;
    else
      return archiveName.substring(0, archiveName.length() - 4);
  }

  /**
   * Returns any matching web-app entry.
   */
  public WebAppController findWebAppEntry(String name)
  {
    ArrayList<EarDeployController> entries = getEntries();

    for (int i = 0; i < entries.size(); i++) {
      EarDeployController ear = entries.get(i);
      
      WebAppController entry = ear.findWebAppController(name);

      if (entry != null)
	return entry;
    }

    return null;
  }
  
  /**
   * Returns the current array of application entries.
   */
  public EarDeployController createEntry(String name)
    throws Exception
  {
    Path rootDirectory = getExpandDirectory().lookup(getExpandPrefix() + name);

    EarDeployController entry = new EarDeployController(_parentContainer, null);

    entry.setName(name);
    entry.setRootDirectory(rootDirectory);

    Path jarPath = getArchiveDirectory().lookup(name + ".ear");
    entry.setArchivePath(jarPath);
    entry.addEarDefault(_earDefault);

    return entry;
  }
}
