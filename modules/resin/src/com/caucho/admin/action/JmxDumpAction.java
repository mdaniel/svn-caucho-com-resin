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
import javax.management.openmbean.*;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.util.L10N;

public class JmxDumpAction extends AbstractJmxAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(JmxDumpAction.class.getName());

  private static final L10N L = new L10N(JmxDumpAction.class);
  
  public static void main(String args[]) throws Exception
  {
    System.out.println(new JmxDumpAction().execute());
  }
  
  public String execute()
    throws ConfigException, JMException, ClassNotFoundException
  {
    MBeanServer server = Jmx.getMBeanServer();
    if (server == null)
      server = ManagementFactory.getPlatformMBeanServer();

    StringBuilder dump = new StringBuilder();
    
    dump.append("JMX Dump:\n");
    
    if (server == null)
      return dump.toString();

    Set<ObjectName> beans = new HashSet<ObjectName>();

    //Set<ObjectName> objectNames = server.queryNames(new ObjectName("java.lang:type=Runtime"), null);
    Set<ObjectName> objectNames = server.queryNames(ObjectName.WILDCARD, null);
    
    for (ObjectName objectName : objectNames) {
      if (beans.contains(objectName))
        continue;

      beans.add(objectName);
      
      dump.append(objectName);
      dump.append(" {\n");

      dumpMBean(server, objectName, dump);
      
      dump.append("}\n");
    }

    return dump.toString();
  }
  
  
  private void dumpMBean(MBeanServer server, ObjectName objectName, StringBuilder dump)
  {
    MBeanAttributeInfo []attributes = null;
    
    try {
      attributes = server.getMBeanInfo(objectName).getAttributes();
    } catch (Exception e) {
      dump.append(e.getMessage());
      dump.append('\n');
      return;
    }
    
    for (MBeanAttributeInfo attribute : attributes) {
      Object value = null;
      
      try {
        value = server.getAttribute(objectName, attribute.getName());
      } catch (Exception e) {
        value = e;
      }
      
      dumpNameValue(attribute.getName(), value, dump, "  ");
    }
  }
  
  private void dumpNameValue(String name, Object value, StringBuilder dump, String padding)
  {
    dump.append(padding);
    dump.append(name);
    dump.append("=");
    
    dumpValue(value, dump, padding);
  }
  
  private void dumpValue(Object value, StringBuilder dump, String padding)
  {
    if (value == null) {
      dump.append("null");
      dump.append('\n');
    } else if (value instanceof Object[]) {
      Object[] values = (Object[]) value;
      dump.append("[\n");
      for (Object v : values) {
        dump.append(padding + "  ");
        dumpValue(v, dump, padding + "  ");
      }
      dump.append(padding);
      dump.append("]\n");
    } else if (value instanceof CompositeData) {
      CompositeData data  = (CompositeData) value;
      CompositeType type = data.getCompositeType();
      dump.append(type.getTypeName());
      dump.append(" {\n");
      for(String key : type.keySet())
        dumpNameValue(key, data.get(key), dump, padding + "  ");
      dump.append(padding);
      dump.append("}\n");
//    } else if (value instanceof TabularData) {
//      TabularData data = (TabularData) value;
//      TabularType type = data.getTabularType();
//      List<String> names = type.getIndexNames();
//      dump.append(type.getTypeName());
//      dump.append(" {\n");
//      for(String name : names) {
//        dump.append(name);
//        //dumpNameValue(name, data.get(name), dump, padding + "  ");
//      }
//      dump.append(padding);
//      dump.append("}\n");
    } else if (value instanceof Map) {
      Map<Object, Object> data = (Map<Object, Object>) value;
      dump.append(value.getClass().getName());
      dump.append(" {\n");
      for(Map.Entry<Object, Object> entry : data.entrySet())
        dumpNameValue(entry.getKey().toString(), entry.getValue(), dump, padding + "  ");
      dump.append(padding);
      dump.append("}\n");
    } else if (value instanceof List) {
      List<Object> values = (List<Object>) value;
      dump.append(value.getClass().getName());
      dump.append(" {\n");
      for (Object v : values) {
        dump.append(padding + "  ");
        dumpValue(v, dump, padding + "  ");
      }
      dump.append(padding);
      dump.append("}\n");
    } else if (value instanceof Throwable) {
      Throwable e = (Throwable) value;
      if (e instanceof UnsupportedOperationException) {
        dump.append("Not supported");
        dump.append('\n');
      } else {
        Throwable cause = e.getCause();
        if (cause != null) {
          dumpValue(cause, dump, padding);
        } else {
          dump.append(e.getMessage());
          dump.append('\n');
        }
      }
    } else {
      dump.append(value);
      dump.append('\n');
    }
  }
  
}
