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

import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractBootCommand implements BootCommand {
  private static final L10N L = new L10N(AbstractBootCommand.class);
  
  private HashSet<String> _valueKeySet = new HashSet<String>();
  private HashSet<String> _optionSet = new HashSet<String>();
  
  private HashMap<String,BootOption> _optionMap
    = new HashMap<String,BootOption>();
  
  protected AbstractBootCommand()
  {
    addValueOption("conf", "file", "alternate resin.xml file");
    addValueOption("resin-home", "dir", "alternate resin home");
    addValueOption("server", "id", "select Resin server from config");
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
  public int doCommand(ResinBoot boot, WatchdogArgs args)
  {
    WatchdogClient client = findClient(boot, args);
    
    return doCommand(args, client);
  }
  
  protected WatchdogClient findClient(ResinBoot boot, WatchdogArgs args)
  {
    WatchdogClient client = boot.findClient(args.getServerId(), args);

    return client;
  }
  
  protected int doCommand(WatchdogArgs args, WatchdogClient client)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  
  protected void addOption(BootOption option)
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
    addOption(new ValueBootOption(name, value, description));
  }
  
  protected void addIntValueOption(String name, 
                                   String value,
                                   String description)
  {
    addOption(new ValueIntBootOption(name, value, description));
  }
  
  public String getOptionUsage()
  {
    StringBuilder sb = new StringBuilder();
    
    ArrayList<BootOption> optionList = new ArrayList<BootOption>();
    optionList.addAll(_optionMap.values());
    
    Collections.sort(optionList, new BootOptionComparator());
    
    for (BootOption option : optionList) {
      sb.append("  " + option.getUsage() + "\n");
    }
    
    return sb.toString();
  }

  public void validateArgs(String[] args) throws BootArgumentException
  {
    Set<String> intValueKeys = getIntValueKeys();

    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];

      if (getName().equals(arg)) {
        continue;
      }

      if (arg.startsWith("-J")
          || arg.startsWith("-D")
          || arg.startsWith("-X")) {
        continue;
      }

      if (arg.equals("-d64") || arg.equals("-d32")) {
        continue;
      }

      if (isOptionArg(arg))
        continue;

      if (! isValueArg(arg))
        throw new BootArgumentException(L.l("unknown argument '{0}'", arg));

      if (i + 1 == args.length)
        throw new BootArgumentException(L.l("option '{0}' requires a value",
                                              arg));
      String value = args[++i];

      if (isValueArg(value) || isOptionArg(value))
        throw new BootArgumentException(L.l("option '{0}' requires a value",
                                            arg));

      if (intValueKeys.contains(arg)) {
        try {
          Long.parseLong(value);
        } catch (NumberFormatException e) {
          throw new BootArgumentException(L.l("'{0}' argument must be a number: `{1}'", arg, value));
        }
      }
    }
  }
  
  protected boolean isOptionArg(String arg)
  {
    return getOptions().contains(arg);
  }
  
  protected boolean isValueArg(String arg)
  {
    return getValueKeys().contains(arg);
  }

  @Override
  public Set<String> getOptions()
  {
    return _optionSet;
  }
  
  protected void addValueKey(String key)
  {
    _valueKeySet.add(key);
  }

  @Override
  public Set<String> getValueKeys()
  {
    return _valueKeySet;
  }

  @Override
  public Set<String> getIntValueKeys()
  {
    return new HashSet<String>();
  }

  @Override
  public boolean isRetry()
  {
    return false;
  }

  @Override
  public void usage()
  {
    System.err.println("usage: resinctl [--options] " + getName()
                       + getUsageArgs());
    System.err.println();
    System.err.println("  " + getDescription());
    System.err.println();
    System.err.println("where options include:");
    System.err.print(getOptionUsage());
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
  public boolean isConsole()
  {
    return false;
  }
  
  @Override
  public boolean isShutdown()
  {
    return false;
  }
  
  static class BootOptionComparator implements Comparator<BootOption> {
    public int compare(BootOption a, BootOption b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}
