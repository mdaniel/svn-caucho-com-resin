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
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.deploy.DeployClient;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

abstract public class AbstractDeployCommand extends AbstractRepositoryCommand {
  private static final L10N L = new L10N(AbstractDeployCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("stage", "stage", "stage to deploy application to, defaults to production");
    addValueOption("version", "version", "version of application formatted as <major.minor.micro.qualifier>");
    addValueOption("m", "message", "commit message");
    addValueOption("timeout", "timeout", "timeout for long deploys");
    
    super.initBootOptions();
  }
  
  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }
  
  @Override
  public String getDescription()
  {
    return "deploys a configuration directory or jar file";
  }
  
  @Override
  public String getUsageArgs()
  {
    return " <filename>";
  }
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    Path path = getDeployPath(args);

    CommitBuilder commit = createCommitBuilder(args, path);
    
    long timeout = args.getArgInt("timeout", 120) * 1000;
    
    if (timeout <= 0) {
      timeout = 120000;
    }
    
    deploy(args, deployClient, path, commit, timeout);
    
    return 0;
  }
  
  protected Path getDeployPath(WatchdogArgs args)
  {
    String jar = args.getDefaultArg();
  
    if (jar == null) {
      throw new ConfigException(L.l("Cannot find archive or directory argument in command line"));
    }
  
    Path jarPath = Vfs.lookup(jar);
  
    if (! jar.endsWith(".jar") && ! jarPath.isDirectory()) {
      throw new ConfigException(L.l("Deploy expects to be used with a *.jar file or directory at {0}",
                                  jar));
    }
    
    return jarPath;
  }
  
  abstract protected CommitBuilder createCommitBuilder(WatchdogArgs args, 
                                                       Path path);
  
  protected void deploy(WatchdogArgs args,
                        WebAppDeployClient deployClient,
                        Path path,
                        CommitBuilder commit,
                        long timeout)
  {
    if (! path.isFile() && ! path.isDirectory()) {
      throw new ConfigException(L.l("'{0}' is not a readable file.",
                                    path.getFullPath()));
    }
    
    String message = args.getArg("-m");
    
    if (message == null)
      message = args.getArg("-message");
    
    if (message == null)
      message = "deploy " + path.getNativePath() + " from command line";
    
    commit.message(message);
    
    commit.attribute("user", System.getProperty("user.name"));

    String version = args.getArg("-version");
    if (version != null)
      DeployClient.fillInVersion(commit, version);

    if (path.isDirectory())
      deployClient.commitPath(commit, path, timeout);
    else
      deployClient.commitArchive(commit, path, timeout);

    System.out.println("Deployed " + commit.getId() + " from " + path + " to "
                       + deployClient.getUrl());
  }

}
