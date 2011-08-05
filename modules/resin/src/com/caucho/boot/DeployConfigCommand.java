/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class DeployConfigCommand extends AbstractRepositoryCommand {
  private static final L10N L = new L10N(DeployConfigCommand.class);
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    String jar = args.getDefaultArg();
    
    if (jar == null) {
      throw new ConfigException(L.l("Cannot find .jar argument in command line"));
    }
    
    Path jarPath = Vfs.lookup(jar);
    
    
    if (! jar.endsWith(".jar") && ! jarPath.isDirectory()) {
      throw new ConfigException(L.l("Deploy expects to be used with a *.jar file at {0}",
                                    jar));
    }

    String name = args.getArg("-name");
    
    CommitBuilder commit = new CommitBuilder();
    commit.type("config");
    
    String stage = args.getArg("-stage");
    
    if (stage != null)
      commit.stage(stage);
    
    if (name == null) {
      name = "resin";
    }
    
    commit.tagKey(name);
    
    String message = args.getArg("-m");
    
    if (message == null)
      message = args.getArg("-message");
    
    if (message == null)
      message = "deploy " + jar + " from command line";
    
    commit.message(message);
    
    commit.attribute("user", System.getProperty("user.name"));

    String version = args.getArg("-version");
    if (version != null)
      fillInVersion(commit, version);

    if (jarPath.isFile())
      deployClient.commitArchive(commit, jarPath);
    else
      deployClient.commitPath(commit, jarPath);

    System.out.println("Deployed " + commit.getId() + " from " + jar + " to "
                       + deployClient.getUrl());

    return 0;
  }

  @Override
  public void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-conf <file>] deploy -user <user> -password <password> [options] <war-file>"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   deploys application specified in a <war-file> to resin server"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -address <address>    : ip or host name of the server"));
    System.err.println(L.l("   -port <port>          : server http port"));
    System.err.println(L.l("   -user <user>          : user name used for authentication to the server"));
    System.err.println(L.l("   -password <password>  : password used for authentication to the server"));
    System.err.println(L.l("   -host <host>          : virtual host to make application available on"));
    System.err.println(L.l("   -name <name>          : name of the context to deploy to, defaults to war-file name"));
    System.err.println(L.l("   -stage <stage>        : stage to deploy application to, defaults to production"));
    System.err.println(L.l("   -version <version>    : version of application formatted as <major.minor.micro.qualifier>"));
    System.err.println(L.l("   -m <message>          : commit message"));
  }
}
