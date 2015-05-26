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

package com.caucho.cli.boot;

import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.util.L10N;

public class JmxListCommand extends JmxCommand
{
  private static final L10N L = new L10N(JmxListCommand.class);

  @Override
  protected void initBootOptions()
  {
    addFlagOption("attributes", "prints MBean's attributes");
    addFlagOption("values", "prints attribute values");
    addFlagOption("operations", "prints operations");
    addFlagOption("all",
                  "when <pattern> not specified sets the wildcard pattern (*:*)");
    addFlagOption("platform",
                  "when <pattern> not specified sets the pattern to (java.lang:*)");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "lists the JMX MBeans in a Resin server";
  }

  public String getUsageTailArgs()
  {
    return " [<pattern>]";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    String pattern = args.getDefaultArg();

    if (pattern != null) {
      try {
        ObjectName.getInstance(pattern);
      } catch (MalformedObjectNameException e) {
        throw new ConfigException(L.l("invalid pattern `{0}': `{1}'",
                                      pattern,
                                      e.getMessage()));
      }
    }

    boolean isPrintAttributes = args.hasOption("-attributes");
    boolean isPrintOperations = args.hasOption("-operations");
    boolean isPrintValues = args.hasOption("-values");
    if (isPrintValues)
      isPrintAttributes = true;
    boolean isAll = args.hasOption("-all");
    boolean isPlatform = args.hasOption("-platform");

    ListJmxQueryReply result = managerClient.listJmx(pattern,
                                                      isPrintAttributes,
                                                      isPrintValues,
                                                      isPrintOperations,
                                                      isAll,
                                                      isPlatform);
    StringBuilder message = new StringBuilder();

    for (ListJmxQueryReply.Bean bean : result.getBeans()) {
      message.append(bean.getName()).append('\n');
      if (isPrintAttributes || isPrintValues) {
        message.append("  attributes:\n");

        for (ListJmxQueryReply.Attribute attribute : bean.getAttributes()) {
          message.append("    ").append(attribute.getInfo());

          if (isPrintValues) {
            message.append('=');
            Object value = attribute.value();
            message.append('=').append(value);
          }
          message.append('\n');
        }
      }

      if (isPrintOperations) {
        message.append("  operations:\n");
        for (ListJmxQueryReply.Operation operation : bean.getOperations()) {
          message.append("    ")
                 .append(operation.getName());

          List<ListJmxQueryReply.Param> params = operation.getParams();
          if (params != null && params.size() > 0) {
            message.append("\n      (\n");
            for (int i = 0; i < params.size(); i++) {
              ListJmxQueryReply.Param param = params.get(i);
              message.append("        ").append(i).append(":");
              message.append(param.getType())
                     .append(' ')
                     .append(param.getName());
              if (param.getDescription() != null
                  && !param.getDescription().isEmpty())
                message.append(" /*")
                       .append(param.getDescription())
                       .append("*/");

              message.append('\n');
            }
            message.append("      )");
          }
          else {
            message.append("()");
          }

          if (operation.getDescription() != null
              && !operation.getDescription().isEmpty()) {
            message.append("/*")
                   .append(operation.getDescription())
                   .append("*/");
          }

          message.append('\n');
        }
      }
    }

    System.out.print(message.toString());

    return ExitCode.OK;
  }
}
