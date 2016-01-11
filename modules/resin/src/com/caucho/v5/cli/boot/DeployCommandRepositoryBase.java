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
 * @author Alex Rojkov
 */

package com.caucho.v5.cli.boot;

import com.caucho.v5.baratine.client.ServiceManagerClient;
import com.caucho.v5.bartender.files.FilesDeployService;
import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.RemoteCommandBase;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.server.config.ClusterConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.watchdog.ArgsWatchdog;
import com.caucho.v5.vfs.PathImpl;

public abstract class DeployCommandRepositoryBase extends RemoteCommandBase 
{
  @Override
  protected void initBootOptions()
  {
    // addValueOption("name", "name", "name of the deployment context");
    
    addValueOption("deploy-type", "value", "deploy type").deprecated();
    addValueOption("virtual-host", "value", "deploy virtual host").tiny("h").hide();
    addValueOption("url-prefix", "name", "deploy name").tiny("n").hide();
    addValueOption("version", "n.n.n.qual", "deploy version as <major.minor.micro.qualifier>").deprecated();
     
    super.initBootOptions();
  }

  @Override
  public final ExitCode doCommand(ArgsCli args,
                                  ServerConfigBoot server,
                                  ServiceManagerClient client)
    throws BootArgumentException
  {
    FilesDeployService filesService
      = client.service("remote:///bartender-files")
              .as(FilesDeployService.class);
    
    doCommand(args, server, filesService);
    
    return ExitCode.OK;
  }
  
  protected String getDeployType()
  {
    return "webapp";
  }
  
  protected String getExtension()
  {
    return ".war";
  }
  
  protected void doCommand(ArgsCli args,
                             ServerConfigBoot server,
                             FilesDeployService filesService)
  {
  }
  
  protected String getDeployDir(ArgsCli args,
                                ServerConfigBoot server,
                                PathImpl path)
  {
    return getDeployDirWebApp(args, server, path);
  }
  
  protected String getUndeployDir(ArgsCli args,
                                  ServerConfigBoot server,
                                  String name)
  {
    return getUndeployDirWebApp(args, server, name);
  }

  protected String getDeployDirWebApp(ArgsCli args,
                                      ServerConfigBoot server,
                                      PathImpl path)
  { 
    StringBuilder sb = new StringBuilder();
    
    sb.append("bfs:///system/webapp/deploy");
    
    String cluster = server.getCluster(args);
    
    
    
    if (cluster == null) {
      cluster = ClusterConfigBoot.DEFAULT_NAME;
    }
    
    sb.append("/").append(cluster);
    
    String host = args.getArg("virtual-host");
    
    if (host == null) {
      host = Host.DEFAULT_NAME;
    }
    
    sb.append("/").append(host);
    
    String name = args.getArg("name");
    
    if (name == null) {
      String tail = path.getTail();
      
      if (tail.endsWith(".war")) {
        name = tail.substring(0, tail.length() - ".war".length());
        
        if (name.equals("ROOT")) {
          name = "root";
        }
      }
      else {
        name = "root";
      }
    }
    
    sb.append("/").append(name);
    
    return sb.toString();
  }

  protected String getUndeployDirWebApp(ArgsCli args,
                                        ServerConfigBoot server,
                                        String name)
  { 
    StringBuilder sb = new StringBuilder();
    
    sb.append("bfs:///system/webapps");

    /*
    String cluster = server.getCluster(args);
    
    if (cluster == null) {
      cluster = ClusterConfigBoot.DEFAULT_NAME;
    }
    
    sb.append("/").append(cluster);
    */
    
    String host = args.getArg("virtual-host");
    
    if (host == null) {
      host = Host.DEFAULT_NAME;
    }
    
    sb.append("/").append(host);
    
    sb.append("/").append(name);
    sb.append(".war");
    
    return sb.toString();
  }
  
  protected String getName(ArgsWatchdog args, PathImpl path)
  {
    return getWebAppName(args, path);
  }
  
  String getWebAppName(ArgsWatchdog args, PathImpl path)
  {
    String name = args.getArg("-name");
    
    String webapp = args.getArg("-web-app");
    
    if (webapp != null) {
      name = webapp;
    }
    
    if (name == null && path != null) {
      String tail = path.getTail();
    
      int p = tail.lastIndexOf('.');

      if (p > 0) {
        name = getNameFromJar(tail.substring(0, p));
      }
      else {
        name = getNameFromJar(tail);
      }
    }

    /*
    if (name == null && args.getDefaultArg() != null) {
      name = args.getDefaultArg();
    }
    */
    
    if (name == null || name.equals("/")) {
      name = "ROOT";
    }
    else if (name.startsWith("/")) {
      name = name.substring(1);
    }
    
    return name;
  }
  
  protected String getNameFromJar(String jarName)
  {
    return jarName;
  }
}
