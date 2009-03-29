/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;

/**
 * A jar artifact in the repository
 */
public class Artifact
{
  private static final L10N L = new L10N(Artifact.class);

  private final Path _path;
  
  private final String _org;
  private final String _module;
  private final String _name;
  private final String _version; // XXX: -> Version

  private final ArtifactDependency []_dependencies;

  public Artifact(Path path,
		  String org,
		  String module,
		  String name,
		  String version,
		  ArrayList<ArtifactDependency> dependencyList)
  {
    _path = path;
    
    _org = org;
    if (org == null)
      throw new NullPointerException(L.l("artifact org cannot be null"));
    
    _module = module;
    
    _name = name;
    if (name == null)
      throw new NullPointerException(L.l("artifact name cannot be null"));
    
    _version = version;

    _dependencies = new ArtifactDependency[dependencyList.size()];
    dependencyList.toArray(_dependencies);
  }

  /**
   * Returns the artifact's path
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Returns the artifact's owning organization (groupId)
   */
  public String getOrg()
  {
    return _org;
  }

  /**
   * Returns the artifact's name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the artifact's version
   */
  public String getVersion()
  {
    return _version;
  }

  /**
   * Returns the artifact's dependencies
   */
  public ArtifactDependency []getDependencies()
  {
    return _dependencies;
  }

  /**
   * Returns true if the artifact matches the dependency
   */
  public boolean isMatch(ArtifactDependency dependency)
  {
    if (! dependency.getOrg().equals(_org))
      return false;
    
    if (! dependency.getName().equals(_name))
      return false;

    if (dependency.getVersion() != null
	&& ! dependency.getVersion().equals(_version)) {
      return false;
    }

    return true;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[org=" + _org
	    + ",name=" + _name
	    + ",version=" + _version
	    + "]");
  }
}
