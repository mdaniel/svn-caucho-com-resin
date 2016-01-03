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
 * @author Scott Ferguson
 */

package com.caucho.v5.server.watchdog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.ProgramInfoDaemon;
import com.caucho.v5.cli.server.ServerELContext;
import com.caucho.v5.cli.shell.EnvCli;
import com.caucho.v5.cli.spi.CommandManager;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

public class ArgsWatchdog extends ArgsDaemon
{
  private static final CommandManager<ArgsWatchdog> _commandManagerWatchdog;

  protected ArgsWatchdog()
  {
  }

  protected ArgsWatchdog(String[] argv, 
                         ProgramInfoDaemon programInfo)
  {
    super(new EnvCli(), argv, programInfo);
  }

  protected ArgsWatchdog(EnvCli env, 
                         String[] argv, 
                         ProgramInfoDaemon programInfo)
  {
    super(env, argv, programInfo);
  }
  
  @Override
  public boolean isWatchdog()
  {
    return true;
  }
  
  public boolean isSkipLog()
  {
    return true;
  }
  
  public CommandWatchdog getWatchdogCommand()
  {
    return (CommandWatchdog) getCommand();
  }

  @Override
  public ServerELContext<? extends ArgsWatchdog> getELContext()
  {
    return new ELContextConfig<ArgsWatchdog>(this);
  }

  public ArgsWatchdog createArgsChild(String[] argv)
  {
    return new ArgsWatchdog(envCli(), argv, getProgramInfo());
  }

  static String calculateClassPath(PathImpl homeDir,
                            String programName)
    throws IOException
  {
    ArrayList<String> classPath = new ArrayList<String>();

    return calculateClassPath(classPath, homeDir, programName);
  }

  static String calculateClassPath(ArrayList<String> classPath,
                                   PathImpl homeDir,
                                   String programName)
    throws IOException
  {
    String oldClassPath = System.getProperty("java.class.path");
    if (oldClassPath != null) {
      for (String item : oldClassPath.split("[" + File.pathSeparatorChar + "]")) {
        PathImpl path = Vfs.lookup(item);
        
        addClassPath(classPath, path.getNativePath());
      }
    }

    oldClassPath = System.getenv("CLASSPATH");
    if (oldClassPath != null) {
      for (String item : oldClassPath.split("[" + File.pathSeparatorChar + "]")) {
        addClassPath(classPath, item);
      }
    }

    //Path javaHome = Vfs.lookup(System.getProperty("java.home"));

    /*
    if (javaHome.lookup("lib/tools.jar").canRead())
      addClassPath(classPath, javaHome.lookup("lib/tools.jar").getNativePath());
    else if (javaHome.getTail().startsWith("jre")) {
      String tail = javaHome.getTail();
      tail = "jdk" + tail.substring(3);
      Path jdkHome = javaHome.getParent().lookup(tail);

      if (jdkHome.lookup("lib/tools.jar").canRead())
        addClassPath(classPath, jdkHome.lookup("lib/tools.jar").getNativePath());
    }

    if (javaHome.lookup("../lib/tools.jar").canRead())
      addClassPath(classPath, javaHome.lookup("../lib/tools.jar").getNativePath());
      */

    PathImpl libDir = homeDir.lookup("lib");

    if (libDir.isDirectory()) {
      String jarName = programName + ".jar";
    
      addClassPath(classPath, libDir.lookup(jarName).getNativePath());

      String []list = libDir.list();

      for (int i = 0; i < list.length; i++) {
        if (! list[i].endsWith(".jar")) {
          continue;
        }

        PathImpl item = libDir.lookup(list[i]);

        String pathName = item.getNativePath();

        if (! classPath.contains(pathName)) {
          addClassPath(classPath, pathName);
        }
      }
    }

    String cp = "";

    for (int i = 0; i < classPath.size(); i++) {
      if (! "".equals(cp))
        cp += File.pathSeparatorChar;

      cp += classPath.get(i);
    }

    return cp;
  }

  private static void addClassPath(ArrayList<String> cp, String item)
  {
    if (! cp.contains(item))
      cp.add(item);
  }
  
  @Override
  public CommandManager<? extends ArgsWatchdog> getCommandManager()
  {
    return _commandManagerWatchdog;
  }
  
  @Override
  protected void initCommands(CommandManager<?> commandManager)
  {
    super.initCommands(commandManager);
    
    CommandManager<? extends ArgsDaemon> manager = (CommandManager) commandManager;

    manager.addCommand("start", new StartCommandWatchdog());
    manager.addCommand("run", new StartCommandWatchdog());
    manager.addCommand("start-all", new StartCommandWatchdog());
    manager.addCommand("restart", new StartCommandWatchdog());
    //manager.addCommand("console", new StartCommandWatchdog());
  }

  public class ELContextConfig<T extends ArgsWatchdog> extends ServerELContext<T>
  {
    ELContextConfig(T args)
    {
      super(args);
    }

    public PathImpl getHomeDir()
    {
      return getArgs().getHomeDirectory();
    }
  }
  
  static {
    _commandManagerWatchdog = new CommandManager<>();
    
    new ArgsWatchdog().initCommands(_commandManagerWatchdog);
  }
}
