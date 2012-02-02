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
import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class DeployCommand extends AbstractRepositoryCommand {
  private static final L10N L = new L10N(ConfigDeployCommand.class);

  public DeployCommand()
  {
    addValueOption("host", "host", "virtual host to make application available on");
    addValueOption("name", "name", "name of the context to deploy to, defaults to war-file name");
    addValueOption("stage", "stage", "stage to deploy application to, defaults to production");
    addValueOption("version", "version", "version of application formatted as <major.minor.micro.qualifier>");
    addValueOption("m", "message", "commit message");
  }

  @Override
  public String getDescription()
  {
    return "deploys an application";
  }
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    String war = args.getDefaultArg();
    
    if (war == null) {
      throw new ConfigException(L.l("Cannot find .war argument in command line"));
    }
    
    if (! war.endsWith(".war")) {
      throw new ConfigException(L.l("Deploy expects to be used with a *.war file at {0}",
                                    war));
    }

    String name = args.getArg("-name");
    
    String webapp = args.getArg("-web-app");
    
    if (webapp != null)
      name = webapp;
    
    String host = args.getArg("-host");
    
    if (host == null)
      host = "default";
    
    CommitBuilder commit = new CommitBuilder();
    commit.type("webapp");
    
    String stage = args.getArg("-stage");
    
    if (stage != null)
      commit.stage(stage);
    
    Path path = Vfs.lookup(war);
    
    if (name == null) {
      String tail = path.getTail();
      
      int p = tail.lastIndexOf('.');

      name = tail.substring(0, p);
    }
    
    commit.tagKey(host + "/" + name);

    /*
    String tag = args.getArg("-tag");
    if (tag != null)
      commit.tagKey(tag);
      */
    
    if (! path.isFile()) {
      throw new ConfigException(L.l("'{0}' is not a readable file.",
                                    path.getFullPath()));
    }
    
    String message = args.getArg("-m");
    
    if (message == null)
      message = args.getArg("-message");
    
    if (message == null)
      message = "deploy " + war + " from command line";
    
    commit.message(message);
    
    commit.attribute("user", System.getProperty("user.name"));

    String version = args.getArg("-version");
    if (version != null)
      DeployClient.fillInVersion(commit, version);

    deployClient.commitArchive(commit, path);

    System.out.println("Deployed " + commit.getId() + " from " + war + " to "
                       + deployClient.getUrl());

    return 0;
  }
  
  @Override
  public String getUsageArgs()
  {
    return " <war-file>";
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }
}
