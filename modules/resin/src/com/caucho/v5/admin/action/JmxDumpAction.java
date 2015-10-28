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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.jmx.JmxUtil;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.QDate;

public class JmxDumpAction extends AbstractJmxAction implements AdminAction
{
  public static void main(String args[]) throws Exception
  {
    System.out.println(new JmxDumpAction().execute());
  }
  
  public String execute()
    throws ConfigException, JMException, ClassNotFoundException
  {
    MBeanServer server = JmxUtil.getMBeanServer();
    if (server == null)
      server = ManagementFactory.getPlatformMBeanServer();
    
    if (server == null)
      return null;

    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");
    sb.append("  \"jmx\" : {\n");
    
    fillServer(sb, server);

    sb.append("\n  }");
    sb.append("\n}");
    
    return sb.toString();
  }
  
  private void fillServer(StringBuilder sb, MBeanServer server)
  {
    Set<ObjectName> beans = new HashSet<ObjectName>();

    //Set<ObjectName> objectNames = server.queryNames(new ObjectName("java.lang:type=Runtime"), null);
    ArrayList<ObjectName> objectNames = new ArrayList<ObjectName>();
    
    objectNames.addAll(server.queryNames(ObjectName.WILDCARD, null));
    
    Collections.sort(objectNames);
    
    boolean isFirst = true;
    
    for (ObjectName objectName : objectNames) {
      if (beans.contains(objectName))
        continue;

      beans.add(objectName);
      
      if (! isFirst)
        sb.append(",\n");
      
      isFirst = false;
      
      sb.append("\"");
      escapeString(sb, String.valueOf(objectName));
      sb.append("\" : {\n");

      dumpMBean(sb, server, objectName);
      
      sb.append("\n}");
    }
  }
  
  private void dumpMBean(StringBuilder sb, 
                         MBeanServer server,
                         ObjectName objectName)
  {
    MBeanAttributeInfo []attributes = null;
    
    try {
      synchronized (server) {
        attributes = server.getMBeanInfo(objectName).getAttributes();
      }
    } catch (Exception e) {
      sb.append("\"mbean_exception\": \"" + e + "\"\n");
      return;
    }
    
    boolean isFirst = true;
    
    for (MBeanAttributeInfo attribute : attributes) {
      if (! isFirst)
        sb.append(",\n");
      isFirst = false;
      
      Object value = null;
      
      try {
        value = server.getAttribute(objectName, attribute.getName());
      } catch (Throwable e) {
        value = e;
      }
      
      dumpNameValue(sb, attribute.getName(), value, "  ");
    }
  }
  
  private void dumpNameValue(StringBuilder sb,
                             String name,
                             Object value, String padding)
  {
    sb.append(padding);
    sb.append("\"");
    escapeString(sb, name);
    sb.append("\"");
    sb.append(": ");
    
    dumpValue(sb, value, padding);
  }
  
  private void dumpValue(StringBuilder sb, Object value, String padding)
  {
    if (value == null) {
      sb.append("null");
    } else if (value instanceof Object[]) {
      Object[] values = (Object[]) value;
      sb.append("[");
      
      boolean isFirst = true;
      for (Object v : values) {
        if (! isFirst)
          sb.append(",");
        isFirst = false;
        
        sb.append("\n" + padding + "  ");
        dumpValue(sb, v, padding + "  ");
      }
      
      sb.append("\n" + padding + "]");
    } else if (value instanceof CompositeData) {
      CompositeData data  = (CompositeData) value;
      CompositeType type = data.getCompositeType();

      sb.append(" {\n");
      sb.append(padding);
      sb.append("  \"java_class\": \"" + type.getTypeName() + "\"");
      
      for (String key : type.keySet()) {
        sb.append(",\n");
        dumpNameValue(sb, key, data.get(key), padding + "  ");
      }
      
      sb.append("\n" + padding + "}");
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
      sb.append("{\n");
      sb.append(padding);
      sb.append("  \"java_class\":\"" + value.getClass().getName() + "\"");
      
      for(Map.Entry<Object, Object> entry : data.entrySet()) {
        sb.append(",\n");
        dumpNameValue(sb, entry.getKey().toString(), entry.getValue(), 
                      padding + "  ");
      }
      sb.append("\n" + padding);
      sb.append("}");
    } else if (value instanceof List) {
      List<Object> values = (List<Object>) value;
      sb.append("[\n");
      
      boolean isFirst = true;
      
      for (Object v : values) {
        if (! isFirst) {
          sb.append(",\n");
        }
        isFirst = false;
        
        sb.append(padding + "  ");
        dumpValue(sb, v, padding + "  ");
      }
      sb.append(padding);
      sb.append("]");
    } else if (value instanceof Throwable) {
      Throwable e = (Throwable) value;
      if (e instanceof UnsupportedOperationException) {
        sb.append("\"Not supported\"");
      } else {
        Throwable cause = e.getCause();
        if (cause != null) {
          dumpValue(sb, cause, padding);
        } else {
          sb.append("\"" + e + "\"");
        }
      }
    } else if (value instanceof Number) {
      sb.append(value);
    } else if (value instanceof Boolean) {
      sb.append(value);
    } else if (value instanceof Date) {
      sb.append("\"" + QDate.formatISO8601(((Date) value).getTime()) + "\"");
    } else {
      sb.append("\"");
      escapeString(sb, String.valueOf(value));
      sb.append("\"");
    }
  }
  
  private void escapeString(StringBuilder sb, String value)
  {
    int len = value.length();
    
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);
      
      switch (ch) {
      case '"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
        break;
      }
    }
  }
  
}
