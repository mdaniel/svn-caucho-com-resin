/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.v5.baratine.Remote;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.L10N;

/**
 * Remote administration service for JMX
 */
@Remote
public class JmxActor implements JmxActorApi
{
  private static final Logger log
    = Logger.getLogger(JmxActor.class.getName());

  private static final L10N L = new L10N(JmxActor.class);

  private MBeanServer _mbeanServer;

  JmxActor(MBeanServer mbeanServer)
  {
    _mbeanServer = mbeanServer;
  }

  @Override
  public MBeanInfo getMBeanInfo(String name)
  {
    try {
      ObjectName objName = getObjectName(name);

      return _mbeanServer.getMBeanInfo(objName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  @Override
  public HashMap<String,Object> lookup(String name)
  {
    try {
      /*
      if (name.startsWith("resin:") && name.indexOf("Server=") < 0) {
        // cloud/1107
        name = name + ",Server=" + ResinSystem.getCurrentId();
      }
      */
      ObjectName objName = getObjectName(name);

      MBeanInfo info = _mbeanServer.getMBeanInfo(objName);

      HashMap<String,Object> attrs = new HashMap<String,Object>();

      attrs.put("__caucho_info_class", info.getClassName());
                
      for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
        if (! attrInfo.isReadable())
          continue;

        String attrName = attrInfo.getName();

        Object value = _mbeanServer.getAttribute(objName, attrName);

        attrs.put(attrName, value);
      }

      return attrs;
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  @Override
  public String []query(String name)
  {
    try {
      ObjectName pattern = new ObjectName(name);

      Set names;

      names = _mbeanServer.queryNames(pattern, null);

      if (names == null)
        return null;

      String []nameArray = new String[names.size()];
      Iterator iter = names.iterator();
      int i = 0;
      while (iter.hasNext()) {
        ObjectName match = (ObjectName) iter.next();

        nameArray[i++] = match.toString();
      }

      return nameArray;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  @Override
  public Object invoke(String name, String opName,
                       Object []args, String []sig)
  {
    try {
      ObjectName objName = new ObjectName(name);

      return _mbeanServer.invoke(objName, opName, args, sig);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }
  
  private ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    ObjectName objName = new ObjectName(name);
  
    if (_mbeanServer.isRegistered(objName)) {
      return objName;
    }
      // cloud/1107, cloud/1131
    else if (name.startsWith("resin:") && name.indexOf("Server=") < 0) {
      // cloud/1107
      name = name + ",Server=" + SystemManager.getCurrentId();
      
      return new ObjectName(name);
    }
    else {
      return objName;
    }
  }
}
