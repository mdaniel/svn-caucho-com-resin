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

package com.caucho.boot;

import com.caucho.config.ConfigException;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class DeployCommand extends AbstractDeployCommand {
  private static final L10N L = new L10N(DeployCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("host", "host", "virtual host to make application available on");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "deploys an application";
  }
  
  @Override
  protected Path getDeployPath(WatchdogArgs args)
  {
    String war = args.getDefaultArg();
    
    if (war == null) {
      throw new ConfigException(L.l("Cannot find .war argument in command line"));
    }
    
    Path path = Vfs.lookup(war);
    
    if (! war.endsWith(".war") && ! path.isDirectory()) {
      throw new ConfigException(L.l("Deploy expects to be used with a *.war file or a directory at {0}",
                                    war));
    }
    
    return path;
  }
  
  @Override
  protected CommitBuilder createCommitBuilder(WatchdogArgs args,
                                              Path path)
  {
    return createWebAppCommit(args, path);
  }
  
  static CommitBuilder createWebAppCommit(WatchdogArgs args, Path path)
  {
    CommitBuilder commit = new CommitBuilder();
    commit.type("webapp");
    
    String stage = args.getArg("-stage");
    
    if (stage != null)
      commit.stage(stage);
    
    String host = args.getArg("-host");
    
    if (host == null)
      host = "default";
    
    String name = getWebAppName(args, path);
    
    commit.tagKey(host + "/" + name);
    
    return commit;
  }
  
  @Override
  public String getUsageArgs()
  {
    return " <war-file>";
  }
}
