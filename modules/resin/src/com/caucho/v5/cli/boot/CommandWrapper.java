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

import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.BootCommand;
import com.caucho.v5.cli.spi.ArgsBase;
import com.caucho.v5.cli.spi.CommandArgumentException;
import com.caucho.v5.cli.spi.OptionCommandLine;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.watchdog.ArgsWatchdog;

/**
 * Command to start Resin server
 * bin/resin.sh start-all
 */
abstract public class CommandWrapper<A extends ArgsWatchdog> implements BootCommand<A>
{
  abstract protected BootCommand<A> getDelegate();

  @Override
  public String name()
  {
    return getDelegate().name();
  }

  @Override
  public String getDescription()
  {
    return getDelegate().getDescription();
  }

  @Override
  public ExitCode doCommand(A args)
      throws CommandArgumentException
  {
    return getDelegate().doCommand(args);
  }

  /*
  @Override
  public boolean isValueOption(String key)
  {
    return getDelegate().isValueOption(key);
  }

  @Override
  public boolean isIntValueOption(String key)
  {
    return getDelegate().isIntValueOption(key);
  }
  */

  /*
  @Override
  public boolean isFlag(String key)
  {
    return getDelegate().isFlag(key);
  }
  */
  
  @Override
  public OptionCommandLine<? super A> getOption(String arg)
  {
    return getDelegate().getOption(arg);
  }

  @Override
  public int getTailArgsMinCount()
  {
    return getDelegate().getTailArgsMinCount();
  }

  @Override
  public String usage(ArgsBase args)
  {
    return getDelegate().usage(args);
  }

  @Override
  public ExitCode doCommandImpl(A args)
      throws BootArgumentException
  {
    return getDelegate().doCommandImpl(args);
  }

  /*
  @Override
  public boolean isRetry()
  {
    return getDelegate().isRetry();
  }

  @Override
  public boolean isStart()
  {
    return getDelegate().isStart();
  }

  @Override
  public boolean isStartAll()
  {
    return getDelegate().isStartAll();
  }

  @Override
  public boolean isShutdown()
  {
    return getDelegate().isShutdown();
  }

  @Override
  public boolean isConsole()
  {
    return getDelegate().isConsole();
  }
  */

  /*
  @Override
  public boolean isRemote(WatchdogArgs args)
  {
    return getDelegate().isRemote(args);
  }
  */
}
