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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.config.types.Period;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.util.L10N;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class LogLevelCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(LogLevelCommand.class);

  private static final Map<String,Level> _options
    = new LinkedHashMap<String,Level>();

  @Override
  protected void initBootOptions()
  {
    addFlagOption("all", "all logs (rare to use)");
    addFlagOption("finest", "finest debugging log");
    addFlagOption("finer", "finer debugging logs (developer-level)");
    addFlagOption("fine", "finer debugging logs (admin/user-level)");
    addFlagOption("config", "configuration debugging logs");
    addFlagOption("info", "default logging level");
    addFlagOption("warning", "non-fatal warnings");
    addFlagOption("severe", "severe, typically fatal warnings");
    addFlagOption("off", "disable logging");
    
    addSpacerOption();

    addValueOption("active-time",
                   "time",
                   "specifies temporary level active time (default permanent). e.g. 5s");
    
    super.initBootOptions();
  }

  @Override
  public String getUsageArgs()
  {
    return " <logger> <logger> ...";
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }

  @Override
  public String getDescription()
  {
    return "sets the java.util.logging level for debugging";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    Level logLevel = null;

    for (Map.Entry<String,Level> entry : _options.entrySet()) {
      if (args.hasOption(entry.getKey())) {
        logLevel = entry.getValue();
        break;
      }
    }

    if (logLevel == null) {
      usage(false);

      return 3;
    }

    long period = 0;

    String time = args.getArg("-active-time");

    if (time != null)
      period = Period.toPeriod(time);

    String[] loggers = args.getDefaultArgs();
    if (loggers == null || loggers.length == 0) {
      loggers = new String[2];
      loggers[0] = "";
      loggers[1] = "com.caucho";
    }

    StringQueryReply result = managerClient.setLogLevel(loggers,
                                                         logLevel,
                                                         period);

    System.out.println(result.getValue());

    return 0;
  }

  public static Level getLevel(String level)
  {
    return _options.get(level);
  }

  static {
    _options.put("-all", Level.ALL);
    _options.put("-finest", Level.FINEST);
    _options.put("-finer", Level.FINER);
    _options.put("-fine", Level.FINE);
    _options.put("-config", Level.CONFIG);
    _options.put("-info", Level.INFO);
    _options.put("-warning", Level.WARNING);
    _options.put("-severe", Level.SEVERE);
    _options.put("-off", Level.OFF);
  }
}
