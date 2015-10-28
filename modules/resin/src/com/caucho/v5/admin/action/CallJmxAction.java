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
 */

package com.caucho.v5.admin.action;

import java.util.*;
import java.util.logging.*;

import javax.management.*;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.server.admin.JmxCallQueryReply;
import com.caucho.v5.util.L10N;

public class CallJmxAction extends AbstractJmxAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(CallJmxAction.class.getName());

  private static final L10N L = new L10N(CallJmxAction.class);

  public JmxCallQueryReply execute(String pattern,
                                   String operationName,
                                   int operationIndex,
                                   String []params)
  throws ConfigException, JMException, ClassNotFoundException
  {
    final List<MBeanServer> servers = new LinkedList<MBeanServer>();

    servers.addAll(MBeanServerFactory.findMBeanServer(null));

    ObjectName nameQuery = ObjectName.getInstance(pattern);

    ObjectName subjectBean = null;
    MBeanServer subjectBeanServer = null;

    for (final MBeanServer server : servers) {
      for (final ObjectName mbean : server.queryNames(nameQuery, null)) {
        if (subjectBean != null) {
          throw new ConfigException(L.l("multiple beans match `{0}'", pattern));
        }

        subjectBean = mbean;
        subjectBeanServer = server;
      }
    }

    List<MBeanOperationInfo> operations = new ArrayList<MBeanOperationInfo>();
    if (subjectBean != null) {
      for (final MBeanOperationInfo operation : subjectBeanServer.getMBeanInfo(
        subjectBean).getOperations()) {
        if (operation.getName().equals(operationName)
            && operation.getSignature().length == params.length) {
          operations.add(operation);
        }
      }
    }

    if (subjectBean == null) {
      throw new ConfigException(L.l("no beans match `{0}'", pattern));
    }
    else if (operations.isEmpty()) {
      throw new ConfigException(L.l("bean at `{0}' does not appear to have operation `{1}' accepting `{2}' arguments",
                                    pattern,
                                    operationName,
                                    params.length));
    } else if (operations.size() > 1 && operationIndex == -1) {
      sort(operations);
      StringBuilder builder = new StringBuilder(L.l(
        "Multiple operations match `{0}', please specify operation name with index e.g `{0}:0`\n",
        operationName));

      for (int i = 0; i < operations.size(); i++) {
        MBeanOperationInfo operation = operations.get(i);
        builder.append(operation.getName()).append(':').append(i).append('(');
        MBeanParameterInfo[] paramInfos = operation.getSignature();
        for (int j = 0; j < paramInfos.length; j++) {
          MBeanParameterInfo paramInfo = paramInfos[j];
          builder.append(paramInfo.getType());
          if (j + 1 < paramInfos.length)
            builder.append(", ");
        }
        builder.append(')');

        if (i + 1 < operations.size())
          builder.append('\n');
      }

      throw new ConfigException(builder.toString());
    }
    else {
      MBeanOperationInfo operationInfo;
      sort(operations);
      if (operationIndex > -1)
        operationInfo = operations.get(operationIndex);
      else
        operationInfo = operations.get(0);

      MBeanParameterInfo[] parameters = operationInfo.getSignature();
      String[] signature = new String[parameters.length];
      for (int i = 0; i < parameters.length; i++) {
        signature[i] = parameters[i].getType();
      }

      Object[] paramValues = new Object[parameters.length];
      for (int i = 0; i < params.length; i++) {
        String param = params[i];
        if ("__NULL__".equals(param))
          continue;

        String type = signature[i];
        Object value = toValue(type, param);
        paramValues[i] = value;
      }

      Object obj = subjectBeanServer.invoke(subjectBean,
                                            operationName,
                                            paramValues,
                                            signature);
      
      JmxCallQueryReply reply 
        = new JmxCallQueryReply(subjectBean.getCanonicalName(), getSignature(operationInfo), String.valueOf(obj));

      return reply;

    }
  }
}
