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
 */

package com.caucho.admin.action;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.logging.*;

import javax.management.*;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

public class ListJmxAction extends AbstractJmxAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ListJmxAction.class.getName());

  private static final L10N L = new L10N(ListJmxAction.class);

  public String execute(String pattern,
                        boolean printAttributes,
                        boolean printValues,
                        boolean printOperations,
                        boolean allBeans,
                        boolean usePlatform)
    throws ConfigException, JMException, ClassNotFoundException
  {
    final List<MBeanServer> servers = new LinkedList<MBeanServer>();

    if (usePlatform) {
      servers.add(ManagementFactory.getPlatformMBeanServer());
    }
    else {
      servers.addAll(MBeanServerFactory.findMBeanServer(null));
    }

    StringBuilder resultBuilder = new StringBuilder();

    Set<ObjectName> beans = new HashSet<ObjectName>();

    ObjectName nameQuery = null;
    if (pattern != null)
      nameQuery = ObjectName.getInstance(pattern);
    else if (allBeans)
      nameQuery = ObjectName.WILDCARD;
    else if (nameQuery == null && usePlatform)
      nameQuery = ObjectName.getInstance("java.lang:*");
    else
      nameQuery = ObjectName.getInstance("resin:*");

    for (final MBeanServer server : servers) {
      Set<ObjectName> mbeans = server.queryNames(nameQuery, null);

      for (final ObjectName mbean : mbeans) {
        if (beans.contains(mbean))
          continue;

        beans.add(mbean);

        resultBuilder.append(mbean).append('\n');
        if (printAttributes) {
          resultBuilder.append("  attributes:\n");
          MBeanAttributeInfo []attributes = server.getMBeanInfo(mbean)
                                                  .getAttributes();
          for (MBeanAttributeInfo attribute : attributes) {
            resultBuilder.append("    ").append(attribute);

            if (printValues) {
              resultBuilder.append('=');
              Object value = server.getAttribute(mbean, attribute.getName());
              resultBuilder.append('=').append(value);
            }
            resultBuilder.append('\n');
          }
        }

        if (printOperations) {
          resultBuilder.append("  operations:\n");
          for (MBeanOperationInfo operation : server.getMBeanInfo(mbean)
                                                    .getOperations()) {
            resultBuilder.append("    ")
                         .append(operation.getName());

            MBeanParameterInfo []params = operation.getSignature();
            if (params.length > 0) {
              resultBuilder.append("\n      (\n");
              for (int i = 0; i < params.length; i++) {
                MBeanParameterInfo param = params[i];

                resultBuilder.append("        ").append(i).append(":");
                resultBuilder.append(param.getType())
                             .append(' ')
                             .append(param.getName());
                if (param.getDescription() != null
                    && ! param.getDescription().isEmpty())
                  resultBuilder.append(" /*")
                               .append(param.getDescription())
                               .append("*/");

                resultBuilder.append('\n');
              }
              resultBuilder.append("      )");
            } else {
              resultBuilder.append("()");
            }

            if (operation.getDescription() != null
                && ! operation.getDescription().isEmpty()) {
              resultBuilder.append("/*")
                           .append(operation.getDescription())
                           .append("*/");
            }

            resultBuilder.append('\n');
          }
        }
      }
    }

    return resultBuilder.toString();
  }
}
