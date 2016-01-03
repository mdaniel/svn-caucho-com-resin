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
 * @author Sam
 */

package com.caucho.v5.deploy;

import com.caucho.v5.management.server.ArchiveDeployMXBean;
import com.caucho.v5.vfs.PathImpl;

abstract public class DeployGeneratorExpandAdmin<C extends DeployGeneratorExpand>
  extends DeployGeneratorAdmin<C>
  implements ArchiveDeployMXBean
{
  public DeployGeneratorExpandAdmin(C expandDeployGenerator)
  {
    super(expandDeployGenerator);
  }

  public String getName()
  {
    PathImpl containerRootDirectory = getDeployGenerator().getContainerRootDirectory();

    PathImpl archiveDirectory = getDeployGenerator().getArchiveDirectory();

    if (containerRootDirectory == null)
      return archiveDirectory.getNativePath();
    else
      return containerRootDirectory.lookupRelativeNativePath(archiveDirectory);
  }

  public long getDependencyCheckInterval()
  {
    return getDeployGenerator().getDependencyCheckInterval();
  }

  public String getArchiveDirectory()
  {
    return getDeployGenerator().getArchiveDirectory().getNativePath();
  }

  public String getArchivePath(String name)
  {
    return getDeployGenerator().getArchivePath(name).getNativePath();
  }

  public String getExpandDirectory()
  {
    return getDeployGenerator().getExpandDirectory().getNativePath();
  }

  public String getExpandPrefix()
  {
    return getDeployGenerator().getExpandPrefix();
  }

  public String getExpandPath(String name)
  {
    PathImpl path =  getDeployGenerator().getExpandPath(name);

    return path == null ? null : path.getNativePath();
  }

  public String getExpandSuffix()
  {
    return getDeployGenerator().getExpandSuffix();
  }

  public String getExtension()
  {
    return getDeployGenerator().getExtension();
  }

  public String[] getNames()
  {
    return getDeployGenerator().getNames();
  }

  public void deploy(String name)
  {
    getDeployGenerator().deploy(name);
  }

  public void start(String name)
  {
    getDeployGenerator().start(name);
  }

  public void stop(String name)
  {
    getDeployGenerator().stop(name);
  }

  public void undeploy(String name)
  {
    getDeployGenerator().undeploy(name);
  }

  public Throwable getConfigException(String name)
  {
    return getDeployGenerator().getConfigException(name);
  }
}
