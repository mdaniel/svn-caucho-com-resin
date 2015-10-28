/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.hessian.io.ExtSerializerFactory;
import com.caucho.v5.hessian.io.StringValueDeserializer;
import com.caucho.v5.hessian.io.StringValueSerializer;
import com.caucho.v5.jmx.JmxUtil;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.server.admin.JmxInfoQuery;
import com.caucho.v5.server.admin.JmxInfoResult;
import com.caucho.v5.server.admin.JmxInvokeQuery;
import com.caucho.v5.server.admin.JmxLookupQuery;
import com.caucho.v5.server.admin.JmxQueryQuery;
import com.caucho.v5.util.L10N;

/**
 * Remote administration service for JMX
 */
public class JmxService
{
  private static final Logger log
    = Logger.getLogger(JmxService.class.getName());

  private static final L10N L = new L10N(JmxService.class);

  private MBeanServer _mbeanServer;
  private ExtSerializerFactory _extFactory;

  private Lifecycle _lifecycle = new Lifecycle();

  public JmxService()
  {
  }
  
  public void setEnable(boolean isEnable)
  {
  }
  
  public void setPassword(String password)
  {
    throw new ConfigException(L.l("'password' is no longer supported for jmx-service.  Add an adminitration <user> to the <management> configuration instead."));
  }

  /**
   * Start the JMXService
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;

    // ServletService server = ServletService.getCurrent();

    // setAddress("jmx@" + server.getBamAdminName());

    // getBroker().createAgent(getActor());

    _mbeanServer = JmxUtil.getMBeanServer();

    _extFactory = new ExtSerializerFactory();
    _extFactory.addSerializer(ObjectName.class,
                              new StringValueSerializer());
    _extFactory.addDeserializer(ObjectName.class,
                                new StringValueDeserializer(ObjectName.class));

    // log.info(L.l("JMX management service '{0}' started", SERVICE_NAME));
  }
  
  public Map<?,?> queryLookup(JmxLookupQuery lookup)
  {
    String pattern = lookup.getPattern();

    HashMap<?,?> value = lookup(pattern);

    return value;
  }
  
  public JmxInfoResult queryInfo(JmxInfoQuery lookup)
  {
    String name = lookup.getName();

    JmxInfoResult result = new JmxInfoResult(getMBeanInfo(name));

    return result;
  }
  
  public Object query(Serializable query)
  {
    if (query instanceof JmxQueryQuery) {
      JmxQueryQuery queryQuery = (JmxQueryQuery) query;

      String pattern = queryQuery.getPattern();

      String []value = query(pattern);

      return value;
    }
    else if (query instanceof JmxInvokeQuery) {
      JmxInvokeQuery invoke = (JmxInvokeQuery) query;

      String name = invoke.getName();
      String opName = invoke.getOp();
      Object []args = invoke.getArgs();
      String []sig = invoke.getSig();

      Serializable value = (Serializable) invoke(name, opName, args, sig);

      return value;
    }
    else {
      // super.query(id, to, from, query);
      return null;
    }
  }

  private MBeanInfo getMBeanInfo(String name)
  {
    try {
      ObjectName objName = new ObjectName(name);

      return _mbeanServer.getMBeanInfo(objName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  private HashMap<String,Object> lookup(String name)
  {
    try {
      ObjectName objName = new ObjectName(name);

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
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

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
}
