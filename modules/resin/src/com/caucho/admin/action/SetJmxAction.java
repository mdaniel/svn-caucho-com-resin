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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.env.system.SystemManager;
import com.caucho.health.action.JmxSetQueryReply;
import com.caucho.util.L10N;

public class SetJmxAction extends AbstractJmxAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(SetJmxAction.class.getName());

  private static final L10N L = new L10N(SetJmxAction.class);

  public JmxSetQueryReply execute(String pattern,
                                  String attributeName,
                                  String value)
    throws ConfigException, JMException, ClassNotFoundException
  {
    final List<MBeanServer> servers = new LinkedList<MBeanServer>();
  
    servers.addAll(MBeanServerFactory.findMBeanServer(null));

    ObjectName nameQuery = ObjectName.getInstance(pattern);
    
    ObjectName serverNameQuery = null;
    
    try {
      String serverId = SystemManager.getCurrentId();
      
      if (pattern.indexOf("*") < 0) {
        serverNameQuery = ObjectName.getInstance(pattern + ",Server=" + serverId);
      }
    } catch (Exception e) {
    }
    
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
    
    if (subjectBean == null) {
      for (final MBeanServer server : servers) {
        for (final ObjectName mbean : server.queryNames(serverNameQuery, null)) {
          if (subjectBean != null) {
            throw new ConfigException(L.l("multiple beans match `{0}'", pattern));
          }

          subjectBean = mbean;
          subjectBeanServer = server;
        }
      }
    }

    MBeanAttributeInfo attributeInfo = null;
    if (subjectBean != null) {
      for (MBeanAttributeInfo info : subjectBeanServer.getMBeanInfo(
        subjectBean).getAttributes()) {
        if (info.getName().equals(attributeName)) {
          attributeInfo = info;
          break;
        }
      }
    }

    if (subjectBean == null) {
      throw new ConfigException(L.l("no beans match `{0}'", pattern));
    }
    else if (attributeInfo == null) {
      throw new ConfigException(L.l("bean at `{0}' does not appear to have attribute `{1}'",
                                    pattern,
                                    attributeName));
    }
    else {
      Object oldValue = subjectBeanServer.getAttribute(subjectBean,
                                                       attributeInfo.getName());

      final Object attribValue = toValue(attributeInfo.getType(), value);
      final Attribute attribute = new Attribute(attributeName, attribValue);

      subjectBeanServer.setAttribute(subjectBean, attribute);

      JmxSetQueryReply reply
        = new JmxSetQueryReply(subjectBean.getCanonicalName(),
                               attributeName,
                               String.valueOf(oldValue),
                               String.valueOf(attribValue));
      
      return reply;
    }
  }
}
