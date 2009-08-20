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

import java.util.HashMap;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.TagQuery;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to list versions of Resin applications deployed via the 
 * ResinDeployWar task to production.
 */
public class ResinListVersions extends ResinQueryTags {
  /**
   * For ant.
   **/
  public ResinListVersions()
  {
  }

  @Override
  protected String getTaskName()
  {
    return "resin-list-versions";
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
    TagQuery []tags = client.queryTags(getStaging(), getType(), 
                                       getVirtualHost(), getContextRoot());

    HashMap<String, TagQuery> reverseTagMap = new HashMap<String, TagQuery>();
    TagQuery headLink = null;

    for (TagQuery tag : tags) {
      if (tag.getTag().endsWith('/' + getContextRoot()))
        headLink = tag;
      else
        reverseTagMap.put(tag.getRoot(), tag);
    }

    TagQuery head = null;

    if (headLink != null)
      head = reverseTagMap.get(headLink.getRoot());

    for (TagQuery tag : tags) {
      if (tag == head)
        System.out.println(tag.getTag() + " (HEAD)");
      else if (tag == headLink && head != null)
        System.out.println(tag.getTag() + " -> " + head.getTag());
      else
        System.out.println(tag.getTag());
    }
  }
}
