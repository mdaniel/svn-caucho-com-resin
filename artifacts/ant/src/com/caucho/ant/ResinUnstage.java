/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.ant;

import java.io.File;
import java.io.IOException;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.DeployClient;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to move staged Resin applications deployed via the 
 * ResinDeployWar task to production.
 */
public class ResinUnstage extends ResinDeployClientTask {
  private String _staging = "staging";
  private String _production = "default";

  /**
   * For ant.
   **/
  public ResinUnstage()
  {
  }

  public void setStaging(String staging)
  {
    _staging = staging;
  }

  public void setProduction(String production)
  {
    _production = production;
  }

  @Override
  protected String getTaskName()
  {
    return "resin-unstage";
  }

  @Override
  protected void validate()
    throws BuildException
  {
    if (getContextRoot() == null)
      throw new BuildException("contextRoot is required by " + getTaskName());
  }

  @Override
  protected void doTask(DeployClient client)
    throws BuildException
  {
    String oldTag = getVersionedWarTag(_staging);
    String newTag = getVersionedWarTag(_production);

    client.copyTag(newTag, oldTag, getUser(), getCommitMessage(), getVersion());
  }
}
