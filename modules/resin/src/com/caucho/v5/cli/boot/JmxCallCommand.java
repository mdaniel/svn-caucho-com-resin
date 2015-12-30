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

import java.util.HashSet;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.admin.JmxCallQueryReply;
import com.caucho.v5.server.admin.ManagerClientApi;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;

public class JmxCallCommand extends JmxCommand
{
  private static final L10N L = new L10N(JmxCallCommand.class);
  private static final Set<String> options = new HashSet<String>();
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("pattern", "pattern", "pattern to match MBean");
    addValueOption("operation", "operation", "operation to invoke");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "calls a JMX operation on a server MBean";
  }
  
  @Override
  public String getUsageTailArgs()
  {
    return " value...";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi client)
  {
    String []trailingArgs = args.getDefaultArgs();

    String pattern = args.getArg("-pattern");
    if (pattern == null)
      throw new ConfigException(L.l("jmx-call must specify -pattern"));

    try {
      ObjectName.getInstance(pattern);
    } catch (MalformedObjectNameException e) {
      throw new ConfigException(L.l("incorrect pattern `{0}' :`{`}'",
                                    pattern,
                                    e.getMessage()));
    }

    String operation = args.getArg("-operation");
    if (operation == null)
      throw new ConfigException(L.l("jmx-call must specify -operation"));

    int operationIndex = -1;
    if (operation.contains(":")) {
      int i = operation.indexOf(':');
      String name = operation.substring(0, i);
      String index = operation.substring(i + 1, operation.length());
      operation = name;
      operationIndex  = Integer.parseInt(index);
    }

    JmxCallQueryReply reply = client.callJmx(pattern,
                                                    operation,
                                                    operationIndex,
                                                    trailingArgs);

    String message = L.l("method `{0}' called on `{1}' returned `{2}'.",
                         reply.getOperation(),
                         reply.getBean(),
                         reply.getReturnValue());

    System.out.println(message);

    return ExitCode.OK;
  }
}
