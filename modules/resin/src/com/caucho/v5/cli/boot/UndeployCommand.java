/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Alex Rojkov
 */

package com.caucho.v5.cli.boot;

import com.caucho.v5.bartender.files.FilesDeployService;
import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.watchdog.ArgsWatchdog;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

public class UndeployCommand extends DeployCommandRepositoryBase
{
  private static final L10N L = new L10N(UndeployCommand.class);
  
  protected void initBootOptions()
  {
    addValueOption("host", "host", "virtual host to make application available on");
    addValueOption("stage", "stage", "stage to deploy application to, defaults to production");
    addValueOption("version", "version", "version of application formatted as <major.minor.micro.qualifier>");
    addValueOption("m", "message", "commit message");
    
    super.initBootOptions();
  }
  
  @Override
  public int getTailArgsMinCount()
  {
    return 0;
  }

  @Override
  public String getDescription()
  {
    return "undeploys an application";
  }
  
  @Override
  protected void doCommand(ArgsCli args,
                           ServerConfigBoot server,
                           FilesDeployService filesService)
  {
    for (String name : args.getTailArgs()) {
      String dir = getUndeployDir(args, server, name);
      
      // filesService.removeFile(dir + "/" + name + ".war");
      
      filesService.removeFile(dir);
      
      System.out.println("  undeployed " + dir);
    }
  }
  
  protected PathImpl getDeployPath(ArgsWatchdog args)
  {
    String war = args.getDefaultArg();
    
    if (war == null) {
      throw new ConfigException(L.l("Cannot find {0} argument in command line",
                                    getExtension()));
    }
    
    PathImpl path = Vfs.lookup(war);
    
    if (! war.endsWith(getExtension()) && ! path.isDirectory()) {
      throw new ConfigException(L.l("Deploy expects to be used with a *{0} file or a directory at {1}",
                                    getExtension(), war));
    }
    
    return path;
  }
  
  @Override
  protected String getDeployType()
  {
    return "webapp";
  }
  
  @Override
  protected String getExtension()
  {
    return ".war";
  }

  @Override
  public String getUsageTailArgs()
  {
    return " [<name>]";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }
}
