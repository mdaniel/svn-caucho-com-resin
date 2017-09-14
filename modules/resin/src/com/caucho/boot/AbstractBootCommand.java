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

import java.util.*;

import com.caucho.Version;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

public abstract class AbstractBootCommand implements BootCommand {
  private static final L10N L = new L10N(AbstractBootCommand.class);
  
  private final Map<String,BootOption> _optionMap
    = new LinkedHashMap<String,BootOption>();
  
  protected AbstractBootCommand()
  {
    initBootOptions();
  }
  
  protected void initBootOptions()
  {
    addSubsectionHeaderOption("general options:");

    addValueOption("conf", "file", "alternate resin.xml file");
    addValueOption("user-properties", "file", "select an alternate $HOME/.resin file");
    addValueOption("mode", "string", "select .resin properties mode");
    
    addSpacerOption();

    addValueOption("resin-home", "dir", "alternate resin home");
    addValueOption("root-directory", "dir", "alternate root directory");
    addValueOption("resin-root", "dir", "alternate root directory", true);
    addValueOption("server-root", "dir", "alternate root directory", true);
    addValueOption("log-directory", "dir", "alternate log directory");
    addValueOption("license-directory", "dir", "alternate license directory");
    addValueOption("data-directory", "dir", "alternate resin-data directory");
    
    addSpacerOption();
    
    addFlagOption("verbose", "produce verbose output");
  }

  @Override
  public String getName()
  {
    String name = getClass().getSimpleName();
    
    int p = name.indexOf("Command");
    if (p >= 0)
      name = name.substring(0, p);
    
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      
      if (Character.isUpperCase(ch)) {
        if (i > 0)
          sb.append('-');
        
        sb.append(Character.toLowerCase(ch));
      }
      else {
        sb.append(ch);
      }
    }
    
    return sb.toString();
  }
  
  @Override
  public String getDescription()
  {
    return "";
  }

  @Override
  public boolean isProOnly()
  {
    return false;
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return false;
  }

  @Override
  public int doCommand(ResinBoot boot, WatchdogArgs args)
  {
    WatchdogClient client = findClient(boot, args);
    
    return doCommand(args, client);
  }

  @Override
  public void doWatchdogStart(WatchdogManager manager)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected WatchdogClient findClient(ResinBoot boot, WatchdogArgs args)
  {
    WatchdogClient client = findNamedClient(boot, args, args.getServerId());

    if (client != null) {
      return client;
    }
    
    client = findLocalClient(boot, args);
    
    if (client == null) {
      client = findWatchdogClient(boot, args);
    }
    
    if (client == null) {
      throw new ConfigException(L.l("No <server> can be found listening to a local IP address"));
    }
    
    return client;
  }
  
  protected WatchdogClient findNamedClient(ResinBoot boot, 
                                           WatchdogArgs args,
                                           String serverId)
  {
    return findNamedClientImpl(boot, args, args.getServerId());
  }
  
  protected WatchdogClient findNamedClientImpl(ResinBoot boot,
                                               WatchdogArgs args,
                                               String serverId)
  {
    return boot.findClient(serverId, args);
  }
  
  protected WatchdogClient findLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    return findLocalClientImpl(boot, args);
  }
    
  protected WatchdogClient findLocalClientImpl(ResinBoot boot, WatchdogArgs args)
  {
    ArrayList<WatchdogClient> clients = boot.findLocalClients();
    
    if (clients == null) {
      return null;
    }
    
    for (WatchdogClient client : clients) {
      if (! client.getConfig().isRequireExplicitId()) {
        return client;
      }
    }
    
    return null;
  }
  
  protected WatchdogClient findUniqueLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    ArrayList<WatchdogClient> clients = boot.findLocalClients();
    
    if (clients == null) {
      return null;
    }
    
    WatchdogClient foundClient = null;
    
    for (WatchdogClient client : clients) {
      if (client.getConfig().isRequireExplicitId()) {
        continue;
      }
        
      if (foundClient == null) {
        foundClient = client;
      }
      else {
        throw new ConfigException(L.l("Resin/{0}: server '{1}' does not match a unique <server> or <server-multi>\nwith a unique local IP in {2}.\n  server ids: {3}",
                                      Version.VERSION,
                                      "",
                                      args.getResinConf().getNativePath(),
                                      boot.findLocalClientIds()));
      }
    }
    
    return foundClient;
  }
  
  protected WatchdogClient findWatchdogClient(ResinBoot boot, WatchdogArgs args)
  {
    return findWatchdogClientImpl(boot, args);
  }
  
  protected WatchdogClient findWatchdogClientImpl(ResinBoot boot, 
                                                  WatchdogArgs args)
  {
    return boot.findWatchdogClient(args);
  }
  
  protected int doCommand(WatchdogArgs args, WatchdogClient client)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  
  protected void addOption(BootOption option)
  {
    addOption(option, 0);
  }

  protected void addOption(BootOption option, int orderOffset)
  {
    _optionMap.put(option.getName(), option);
  }
  
  protected void addFlagOption(String name, String description)
  {
    addOption(new FlagBootOption(name, description));
  }

  protected void addValueOption(String name,
                                String value,
                                String description)
  {
    addValueOption(name, value, description, false);
  }

  protected void addValueOption(String name,
                                String value,
                                String description, 
                                boolean deprecated)
  {
    addOption(new ValueBootOption(name, value, description, deprecated));
  }
  
  protected void addIntValueOption(String name, 
                                   String value,
                                   String description)
  {
    addOption(new ValueIntBootOption(name, value, description));
  }
  
  protected void addSpacerOption()
  {
    addOption(new SpacerBootOption());
  }
  
  protected void addSubsectionHeaderOption(String header)
  {
    addOption(new SubsectionBootOption(header));
  }
  
  public String getOptionUsage(boolean verbose)
  {
    StringBuilder sb = new StringBuilder();
    
    BootOption lastOption = null;
    for (BootOption option : _optionMap.values()) {
      if (option instanceof SpacerBootOption && 
        lastOption instanceof SpacerBootOption) {
        continue;
      }
      
      if (! verbose && option.isDeprecated()) {
        continue;
      }
      
      sb.append(option.getUsage() + "\n");
      lastOption = option;
    }
    
    return sb.toString();
  }

  @Override
  public boolean isValueOption(String key)
  {
    BootOption option = getBootOption(key);

    if (option != null && option.isValue())
      return true;

    return false;
  }

  @Override
  public boolean isIntValueOption(String key)
  {
    BootOption option = getBootOption(key);

    if (option != null && option.isIntValue())
      return true;

    return false;
  }

  @Override
  public boolean isFlag(String key)
  {
    BootOption option = getBootOption(key);

    if (option != null && option.isFlag())
      return true;

    return false;
  }

  private BootOption getBootOption(String key)
  {
    if (key.length() == 0 || key.charAt(0) != '-')
      return null;

    String cleanKey;

    if (key.length() > 1 && key.charAt(0) == '-' && key.charAt(1) == '-')
      cleanKey = key.substring(2);
    else
      cleanKey = key.substring(1);

    BootOption option = _optionMap.get(cleanKey);

    return option;
  }

  @Override
  public boolean isRetry()
  {
    return false;
  }
  
  @Override
  public int retryCount()
  {
    return isRetry() ? 5 : 0;
  }

  @Override
  public final void usage(boolean verbose)
  {
    System.err.println("usage: resinctl " + getName() + " [--options]"
                       + getUsageArgs());
    System.err.println();
    System.err.println("  " + getDescription()
                       + (isProOnly() ? " (Resin Pro)" : ""));
    System.err.println();
    System.err.println("where command options include:");
    System.err.println();
    System.err.print(getOptionUsage(verbose));
  }
  
  public String getUsageArgs()
  {
    return "";
  }
  
  @Override
  public boolean isStart()
  {
    return false;
  }
  
  @Override
  public boolean isStartAll()
  {
    return false;
  }
  
  @Override
  public boolean isConsole()
  {
    return false;
  }
  
  @Override
  public boolean isShutdown()
  {
    return false;
  }
  
  @Override
  public boolean isRemote(WatchdogArgs args)
  {
    return args.getArg("-address") != null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
