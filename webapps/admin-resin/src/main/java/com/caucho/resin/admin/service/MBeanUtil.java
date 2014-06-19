package com.caucho.resin.admin.service;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.jmx.Jmx;
import com.caucho.server.admin.RemoteMBeanConnectionFactory;

public class MBeanUtil
{
  public static <T> T find(String name, Class<T> api)
  {
    return find(name, api, null);
  }

  public static <T> T find(String name, Class<T> api, String serverId)
  {
    try {
      if (true) {
        return Jmx.find(name, api);
      }

      ObjectName objectName = getObjectName(name);
      MBeanServerConnection server = getServer(serverId);

      if (server.isRegistered(objectName)) {
        return JMX.newMXBeanProxy(server, objectName, api);
      }

      objectName = getContextObjectName(name);

      if (objectName != null && server.isRegistered(objectName)) {
        return JMX.newMXBeanProxy(server, objectName, api);
      }
      else {
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  private static ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    ObjectName objectName = Jmx.getObjectName(name);

    return objectName;
  }

  private static ObjectName getContextObjectName(String name)
    throws MalformedObjectNameException
  {
    return Jmx.getContextObjectName(name);
  }

  public static MBeanServerConnection getServer()
  {
    return getServer(null);
  }

  public static MBeanServerConnection getServer(String serverId)
  {
    return lookupServer(serverId);
  }

  private static MBeanServerConnection lookupServer(String serverId)
  {
    if (serverId == null
        || "".equals(serverId)) {
      return Jmx.getMBeanServer();
    }
    else {
      return RemoteMBeanConnectionFactory.create(serverId);
    }
  }
}
