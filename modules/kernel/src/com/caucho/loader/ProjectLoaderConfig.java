/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.loader;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.loader.maven.MavenProject;
import com.caucho.loader.module.Artifact;
import com.caucho.loader.module.ArtifactDependency;
import com.caucho.util.L10N;
import com.caucho.repository.ModuleRepository;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import java.util.logging.*;
import javax.annotation.PostConstruct;

/**
 * Adds a new project (pom.xml)
 */
public class ProjectLoaderConfig
{
  private static final L10N L = new L10N(ProjectLoaderConfig.class);
  private static final Logger log
    = Logger.getLogger(ProjectLoaderConfig.class.getName());

  private Path _path;

  /**
   * Sets a specific path to a jar file
   */
  public void setPath(Path path)
  {
    if (! path.getTail().equals("pom.xml"))
      throw new ConfigException(L.l("project-loader path='{0}' must be a pom.xml file.",
				    path));

    _path = path;
  }

  @PostConstruct
  public void init()
  {
    if (_path == null)
      throw new ConfigException(L.l("project-loader requires a 'path' attribute"));

    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    loader.addDependency(new Depend(_path));

    if (_path.canRead()) {
      try {
	MavenProject project = new MavenProject();

	new Config().configure(project, _path);

	Artifact artifact = project.toArtifact(_path);

	for (ArtifactDependency dependency : artifact.getDependencies())  {
	  loader.createArtifactManager().addDependency(dependency);
	}
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }

    // loader.addJar(path);
  }
}
