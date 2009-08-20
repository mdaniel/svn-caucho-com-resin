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
import com.caucho.server.admin.TagQuery;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to query tags of Resin applications deployed via the 
 * ResinDeployWar task to production.
 */
public class ResinQueryTags extends ResinDeployClientTask {
  private String _staging = "default";
  private String _type = "wars";

  /**
   * For ant.
   **/
  public ResinQueryTags()
  {
  }

  public void setStaging(String staging)
  {
    if ("*".equals(staging))
      _staging = null;
    else
      _staging = staging;
  }

  public String getStaging()
  {
    return _staging;
  }

  public void setType(String type)
  {
    if ("*".equals(type))
      _type = null;
    else
      _type = type;
  }

  public String getType()
  {
    return _type;
  }
  
  @Override
  public void setVirtualHost(String virtualHost)
  {
    if ("*".equals(virtualHost))
      super.setVirtualHost(null);
    else
      super.setVirtualHost(virtualHost);
  }

  @Override
  public void setContextRoot(String contextRoot)
  {
    if ("*".equals(contextRoot))
      super.setContextRoot(null);
    else
      super.setContextRoot(contextRoot);
  }

  @Override
  protected String getTaskName()
  {
    return "resin-query-tags";
  }

  @Override
  protected void doTask(DeployClient client)
    throws BuildException
  {
    TagQuery []tags = 
      client.queryTags(_staging, _type, getVirtualHost(), getContextRoot());

    for (TagQuery tag : tags) {
      System.out.println(tag.getTag());
    }
  }
}
