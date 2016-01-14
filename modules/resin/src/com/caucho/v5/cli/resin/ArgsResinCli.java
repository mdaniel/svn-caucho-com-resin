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
 * @author Scott Ferguson
 */

package com.caucho.v5.cli.resin;

import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.cli.baratine.BenchHttpCommand;
import com.caucho.v5.cli.baratine.BenchJampCommand;
import com.caucho.v5.cli.baratine.HeapDumpCommandBaratine;
import com.caucho.v5.cli.baratine.ProfileCommandBaratine;
import com.caucho.v5.cli.baratine.SleepCommand;
import com.caucho.v5.cli.boot.DisableCommand;
import com.caucho.v5.cli.boot.EnableCommand;
import com.caucho.v5.cli.boot.JmxCallCommand;
import com.caucho.v5.cli.boot.JmxDumpCommand;
import com.caucho.v5.cli.boot.JmxListCommand;
import com.caucho.v5.cli.boot.JmxSetCommand;
import com.caucho.v5.cli.boot.JspcCommand;
import com.caucho.v5.cli.boot.KillCommand;
import com.caucho.v5.cli.boot.ListRestartsCommand;
import com.caucho.v5.cli.boot.LogLevelCommand;
import com.caucho.v5.cli.boot.PasswordEncryptCommand;
import com.caucho.v5.cli.boot.PasswordGenerateCommand;
import com.caucho.v5.cli.boot.PdfReportCommand;
import com.caucho.v5.cli.boot.RestartCommand;
import com.caucho.v5.cli.boot.ScoreboardCommand;
import com.caucho.v5.cli.boot.StartWithForegroundCommand;
import com.caucho.v5.cli.boot.StatusCommand;
import com.caucho.v5.cli.boot.StoreLoadCommand;
import com.caucho.v5.cli.boot.ThreadDumpCommand;
import com.caucho.v5.cli.boot.UndeployCommand;
import com.caucho.v5.cli.server.BfsCatCommand;
import com.caucho.v5.cli.server.BfsGetCommand;
import com.caucho.v5.cli.server.BfsLsCommand;
import com.caucho.v5.cli.server.BfsPutCommand;
import com.caucho.v5.cli.server.BfsRmCommand;
import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.cli.server.ProgramInfoDaemon;
import com.caucho.v5.cli.server.ShutdownCommand;
import com.caucho.v5.cli.server.StopCommand;
import com.caucho.v5.cli.server.StoreSaveCommand;
import com.caucho.v5.cli.shell.EnvCli;
import com.caucho.v5.cli.spi.CommandManager;


public class ArgsResinCli extends ArgsCli
{
  private static final CommandManager<ArgsResinCli> _commandManagerResin;
  
  //private boolean _is64bit;
  
  protected ArgsResinCli()
  {
  }

  public ArgsResinCli(String[] argv)
  {
    this(new EnvCli(), argv, new ProgramInfoResin());
  }

  public ArgsResinCli(EnvCli env, String[] argv)
  {
    super(argv, new ProgramInfoResin());
  }

  public ArgsResinCli(EnvCli env, String[] argv, ProgramInfoDaemon info)
  {
    super(env, argv, new ProgramInfoResin());
  }
  
  @Override
  public ArgsResinCli createChild(String []argv)
  {
    return new ArgsResinCli(envCli(), argv, getProgramInfo());
  }
  

  //@Override
  protected BootConfigParser createParser()
  {
    return new BootConfigParserResin();
  }

  @Override
  public CommandManager<? extends ArgsResinCli> getCommandManager()
  {
    return _commandManagerResin;
  }
  
  @Override
  protected void initCommands(CommandManager<?> commandManager)
  {
    CommandManager<? extends ArgsResinCli> manager = (CommandManager) commandManager;
    
    super.initCommands(manager);
    
    
    // addOption(new WatchdogPort());
    // addOption(new ServerOption());
    manager.addCommand(new BenchHttpCommand().hide());
    manager.addCommand(new BenchJampCommand().hide());

    manager.addCommand(new BfsCatCommand());
    manager.addCommand(new BfsGetCommand());
    manager.addCommand(new BfsLsCommand());
    manager.addCommand(new BfsPutCommand());
    manager.addCommand(new BfsRmCommand());

    //manager.addCommand(new ServerCommandConsole());
    //manager.addCommand(new ConsoleCommand());
    
    //manager.addCommand(new DeployCommandResin());
    
    manager.addCommand(new DisableCommand().hide());
    manager.addCommand(new EnableCommand().hide());

    //manager.addCommand(new StartCommandGui().hide());

    // manager.addCommand(new HeapDumpCommand().hide());
    manager.addCommand(new HeapDumpCommandBaratine().hide());
    
    manager.addCommand(new JmxCallCommand().hide());
    manager.addCommand(new JmxDumpCommand().hide());
    manager.addCommand(new JmxListCommand().hide());
    manager.addCommand(new JmxSetCommand().hide());
    manager.addCommand(new JspcCommand().hide());
    manager.addCommand(new KillCommand().hide());
    // manager.addCommand(new LicenseAddCommand());
    manager.addCommand(new ListRestartsCommand().hide());
    manager.addCommand(new LogLevelCommand().hide());

    manager.addCommand(new PasswordEncryptCommand().hide());
    manager.addCommand(new PasswordGenerateCommand().hide());
    
    manager.addCommand(new PdfReportCommand().hide());
    
    manager.addCommand(new ProfileCommandBaratine().hide());

    manager.addCommand(new RestartCommand());
    // addCommand(new RunCommand());

    manager.addCommand(new ScoreboardCommand().hide());
    
    //manager.addCommand("deploy-pod", new DeployCommandService().hide());
    
    manager.addCommand(new ShutdownCommand());
    manager.addCommand(new SleepCommand().hide());
    // addCommand(new StartCloudCommand());
    manager.addCommand(new StartCommandResin());
    manager.addCommand("start-all", new StartCommandResin().hide());
    manager.addCommand(new StartWithForegroundCommand().hide());
    manager.addCommand(new StatusCommand());
    manager.addCommand(new StopCommand());
    manager.addCommand(new StoreSaveCommand().hide());
    manager.addCommand(new StoreLoadCommand().hide());

    manager.addCommand(new ThreadDumpCommand().hide());

    manager.addCommand(new UndeployCommand().hide());
    //addCommand(new UserAddCommand());
    //addCommand(new UserListCommand());
    //addCommand(new UserRemoveCommand());

    //manager.addCommand(new WebAppDeployCommand().hide());
    //manager.addCommand(new WebAppRestartCommand().hide());
    //manager.addCommand(new WebAppRestartClusterCommand().hide());
    //manager.addCommand(new WebAppStartCommand().hide());
    //manager.addCommand(new WebAppStopCommand().hide());
    //manager.addCommand(new WebAppUndeployCommand().hide());

    /*
    manager.addCommand("deploy-start", new WebAppStartCommand());
    manager.addCommand("deploy-stop", new WebAppStopCommand());
    manager.addCommand("deploy-restart", new WebAppRestartCommand());

    manager.addCommand("generate-password", new PasswordGenerateCommand());

    manager.addCommand("start-webapp", new WebAppStartCommand());
    manager.addCommand("stop-webapp", new WebAppStopCommand());
    manager.addCommand("restart-webapp", new WebAppRestartCommand());
    */
  }
  
  static {
    _commandManagerResin = new CommandManager<>();
    
    new ArgsResinCli().initCommands(_commandManagerResin);
  }
}
