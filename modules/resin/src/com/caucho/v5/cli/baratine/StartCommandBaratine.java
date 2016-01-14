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

package com.caucho.v5.cli.baratine;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.StartCommand;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Command to start a Baratine application.
 * 
 * bin/baratine start
 */
public class StartCommandBaratine extends StartCommand
{
  private static final Logger log
    = Logger.getLogger(StartCommandBaratine.class.getName());

  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();
    
    addFlagOption("package", "loads build-in packages");
  }
  
  @Override
  public String getDescription()
  {
    return "starts baratine servers and deploys service jars";
  }
  
  @Override
  public String getUsageTailArgs()
  {
    return " [service1.jar service2.jar...]";
  }

  @Override
  protected ExitCode doDeploy(ConfigBoot boot, ArgsDaemon args)
  {
    ExitCode code = super.doDeploy(boot, args);
    
    if (code != ExitCode.OK) {
      return code;
    }
    
    if (! isPackageDeploy(args)) {
      return code;
    }
    
    for (String deployPath : getPackageDeployPaths()) {
      code = doDeployPath(boot, args, deployPath);
      
      if (code != ExitCode.OK) {
        return code;
      }
    }
    
    return code;
  }
  
  protected boolean isPackageDeploy(ArgsDaemon args)
  {
    return args.getArgBoolean("package");
  }
  
  private ArrayList<String> getPackageDeployPaths()
  {
    ArrayList<String> paths = new ArrayList<>();
    
    String resourceName = getClass().getName().replace('.', '/') + ".class";
    
    URL url = getClass().getClassLoader().getResource(resourceName);

    if (url == null) {
      return paths;
    }
    
    String urlName = url.toString();
    int p = urlName.indexOf('!');
    
    if (! urlName.startsWith("jar:") || p < 0) {
      return paths;
    }
    
    String fileName = urlName.substring(4, p);
    
    try (ReadStream is = VfsOld.lookup(fileName).openRead()) {
      try (ZipInputStream zIn = new ZipInputStream(is)) {
        ZipEntry entry;
        
        while ((entry = zIn.getNextEntry()) != null) {
          String entryName = entry.getName();
          
          if (entry.isDirectory()) {
            continue;
          }
          
          if (entryName.startsWith("com/caucho/package/")) {
            paths.add("classpath:" + entryName);
          }
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return paths;
    }
    
    return paths;
  }
}
