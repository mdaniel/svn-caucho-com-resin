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

package com.caucho.admin.action;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.env.system.SystemManager;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.util.L10N;

public class ListJmxAction extends AbstractJmxAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ListJmxAction.class.getName());

  private static final L10N L = new L10N(ListJmxAction.class);

  public ListJmxQueryReply execute(String pattern,
                                    boolean isPrintAttributes,
                                    boolean isPrintValues,
                                    boolean isPrintOperations,
                                    boolean isAllBeans,
                                    boolean isUsePlatform)
    throws ConfigException, JMException, ClassNotFoundException
  {
    final List<MBeanServer> servers = new LinkedList<MBeanServer>();

    if (isUsePlatform) {
      servers.add(ManagementFactory.getPlatformMBeanServer());
    }
    else {
      servers.addAll(MBeanServerFactory.findMBeanServer(null));
    }

    Set<ObjectName> beans = new HashSet<ObjectName>();

    ObjectName nameQuery = null;
    
    if (pattern != null) {
      nameQuery = ObjectName.getInstance(pattern);
    }
    else if (isAllBeans) {
      nameQuery = ObjectName.WILDCARD;
    }
    else if (nameQuery == null && isUsePlatform) {
      nameQuery = ObjectName.getInstance("java.lang:*");
    }
    else {
      nameQuery = ObjectName.getInstance("resin:*");
    }

    ListJmxQueryReply listJmxQueryResult
      = new ListJmxQueryReply();

    for (final MBeanServer server : servers) {
      Set<ObjectName> mbeanSet = server.queryNames(nameQuery, null);
      /*
      if (mbeanSet == null || mbeanSet.size() == 0 && serverNameQuery != null) {
        mbeanSet = server.queryNames(serverNameQuery, null);
      }
*/
      ArrayList<ObjectName> mbeans = new ArrayList<ObjectName>(mbeanSet);
      Collections.sort(mbeans);

      for (final ObjectName mbean : mbeans) {
        if (beans.contains(mbean))
          continue;

        beans.add(mbean);

        ListJmxQueryReply.Bean bean = new ListJmxQueryReply.Bean();
        bean.setName(mbean.toString());

        if (isPrintAttributes || isPrintValues) {
          MBeanAttributeInfo []attributes = server.getMBeanInfo(mbean)
                                                  .getAttributes();
          for (MBeanAttributeInfo attribute : attributes) {
            ListJmxQueryReply.Attribute attr
              = new ListJmxQueryReply.Attribute();

            attr.setName(attribute.getName());
            attr.setInfo(attribute.toString());

            if (isPrintValues) {
              try {
                Object value = server.getAttribute(mbean, attribute.getName());

                attr.value(value);
              } catch (Throwable e) {
                e.printStackTrace();
                log.log(Level.FINER, e.getMessage(), e);

                attr.value(e);
              }
            }

            bean.add(attr);
          }
        }

          if (isPrintOperations) {
            for (MBeanOperationInfo operation : server.getMBeanInfo(mbean)
                                                      .getOperations()) {
              ListJmxQueryReply.Operation op
                = new ListJmxQueryReply.Operation();

              op.setName(operation.getName());

              MBeanParameterInfo []params = operation.getSignature();
              if (params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                  MBeanParameterInfo param = params[i];

                  ListJmxQueryReply.Param par
                    = new ListJmxQueryReply.Param();

                  par.setName(param.getName());
                  par.setType(param.getType());
                  par.setDescription(param.getDescription());

                  op.add(par);
                }
              }

              op.setDescription(operation.getDescription());

              op.setType(operation.getReturnType());

              bean.add(op);
            }
          }
        listJmxQueryResult.add(bean);
      }
    }

    return listJmxQueryResult;
  }
}
