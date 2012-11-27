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

public class DeployCopyCommand extends AbstractRepositoryCommand {
  private static final L10N L = new L10N(DeployCopyCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("source", "source", "source context");
    addValueOption("source-host", "source-host", "source host");
    addValueOption("source-stage", "source-stage", "source stage");
    addValueOption("source-version", "source-version", "source version");
    
    addSpacerOption();
    
    addValueOption("target", "target", "target context");
    addValueOption("target-host", "target-host", "target host");
    addValueOption("target-stage", "target-stage", "target stage");
    addValueOption("target-version", "target-version", "target version");
    
    addSpacerOption();

    addValueOption("m", "message", "commit message");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "copies a deployment to a new tag name";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    String sourceContext = args.getArg("-source");

    if (sourceContext != null) {
    }
    else if (args.getTailArgs().size() == 2) {
      sourceContext = args.getTailArgs().get(0);
    }
    else {
      throw new ConfigException("must specify -source attribute");
    }

    String sourceHost = args.getArg("-source-host");

    if (sourceHost == null)
      sourceHost = "default";

    CommitBuilder source = new CommitBuilder();
    source.type("webapp");

    String sourceStage = args.getArg("-source-stage");

    if (sourceStage != null)
      source.stage(sourceStage);

    source.tagKey(sourceHost + "/" + sourceContext);

    String version = args.getArg("-source-version");
    if (version != null)
      DeployClient.fillInVersion(source, version);

    String targetContext = args.getArg("-target");

    if (targetContext != null) {
    }
    else if (args.getTailArgs().size() == 2) {
      targetContext = args.getTailArgs().get(1);
    }
    else {
      throw new ConfigException("must specify -target attribute");
    }

    String targetHost = args.getArg("-target-host");

    if (targetHost == null)
      targetHost = sourceHost;

    CommitBuilder target = new CommitBuilder();
    target.type("webapp");

    String targetStage = args.getArg("-target-stage");

    if (targetStage != null)
      target.stage(targetStage);

    target.tagKey(targetHost + "/" + targetContext);

    String message = args.getArg("-m");

    if (message == null)
      message = args.getArg("-message");

    if (message == null)
      message = L.l("copy '{0}' to '{1}'", source.getTagKey(), target.getTagKey());

    target.message(message);

    target.attribute("user", System.getProperty("user.name"));

    String targetVersion = args.getArg("-target-version");
    if (targetVersion != null)
      DeployClient.fillInVersion(target, targetVersion);

    deployClient.copyTag(target, source);

    System.out.println(L.l("copied {0} to {1}",
                           source.getId(), target.getId()));

    return 0;
  }

  /*
  @Override
  public void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-conf <file>] [-server <id>] deploy-copy -user <user> -password <password> [options]"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   copies a deployed application according to the given options"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -conf <file>                     : resin configuration file"));
    System.err.println(L.l("   -server <id>                     : id of a server"));
    System.err.println(L.l("   -address <address>               : ip or host name of the server"));
    System.err.println(L.l("   -port <port>                     : server http port"));
    System.err.println(L.l("   -user <user>                     : user name used for authentication to the server"));
    System.err.println(L.l("   -password <password>             : password used for authentication to the server"));
    System.err.println(L.l("   -source <source>                 : source context"));
    System.err.println(L.l("   -source-host <source-host>       : source host"));
    System.err.println(L.l("   -source-stage <source-stage>     : source stage"));
    System.err.println(L.l("   -source-version <source-version> : source version"));
    System.err.println(L.l("   -target <target>                 : target context"));
    System.err.println(L.l("   -target-host <target-host>       : target host"));
    System.err.println(L.l("   -target-stage <target-stage>     : target stage"));
    System.err.println(L.l("   -target-version <target-version> : target version"));
    System.err.println(L.l("   -m <message>                     : commit message"));
  }
  */
}
