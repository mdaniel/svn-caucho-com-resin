/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jmx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;

/**
 * Static convenience methods.
 */
public class JmxUtilResin {
  private static final L10N L = new L10N(JmxUtilResin.class);
  private static final Logger log 
    = Logger.getLogger(JmxUtilResin.class.getName());
  public static final String DOMAIN = "caucho";

  /**
   * Gets the static mbean server.
   */
  public static EnvironmentMBeanServer getMBeanServer()
  {
    return EnvironmentMBeanServer.getGlobal();
  }
  
  /**
   * Returns a copy of the context properties.
   */
  public static LinkedHashMap<String,String> copyContextProperties()
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      return mbeanServer.createContext().copyProperties();
    else
      return new LinkedHashMap<String,String>();
  }
  
  /**
   * Returns a copy of the context properties.
   */
  public static LinkedHashMap<String,String>
    copyContextProperties(ClassLoader loader)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      return mbeanServer.createContext(loader).copyProperties();
    else
      return new LinkedHashMap<String,String>();
  }

  /**
   * Sets the context properties.
   */
  public static void setContextProperties(Map<String,String> properties)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      mbeanServer.createContext().setProperties(properties);
  }

  /**
   * Sets the context properties.
   */
  public static void setContextProperties(Map<String,String> properties,
                                          ClassLoader loader)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      mbeanServer.createContext(loader).setProperties(properties);
  }

  /**
   * Adds a new JMX context property.
   */
  public static void addContextProperty(String key, String value)
  {
    Objects.requireNonNull(value);
    
    Map<String,String> map = copyContextProperties();
    
    map.put(key, value);
    
    setContextProperties(map);
  }

  /**
   * Adds a new JMX context property.
   */
  public static void addContextProperty(String key, String value,
                                        ClassLoader loader)
  {
    Objects.requireNonNull(value);
    
    Map<String,String> map = copyContextProperties(loader);
    
    map.put(key, value);
    
    setContextProperties(map, loader);
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object, String name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    if (name.indexOf(':') < 0) {
      Map<String,String> props = parseProperties(name);

      if (props.get("type") == null) {
        String type = object.getClass().getName();
        int p = type.lastIndexOf('.');
        if (p > 0)
          type = type.substring(p + 1);

        props.put("type", type);
      }

      ObjectName objectName = getObjectName(DOMAIN, props);

      return register(object, objectName);
    }
    else
      return register(object, new ObjectName(name));
      
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance registerContext(Object object, String name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    if (name.indexOf(':') < 0) {
      Map<String,String> nameProps = parseProperties(name);
      
      Map<String,String> props = copyContextProperties();
      props.putAll(nameProps);

      if (props.get("type") == null) {
        String type = object.getClass().getName();
        int p = type.lastIndexOf('.');
        if (p > 0)
          type = type.substring(p + 1);

        props.put("type", type);
      }

      ObjectName objectName = getObjectName(DOMAIN, props);

      return register(object, objectName);
    }
    else
      return register(object, new ObjectName(name));
      
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object,
                                        Map<String,String> properties)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    Map<String,String> props = copyContextProperties();
    props.putAll(properties);

    return register(object, getObjectName(DOMAIN, props));
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object or null if the object
   * doesn't have an MBean interface.
   */
  public static ObjectInstance register(Object object, ObjectName name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    return getMBeanServer().registerMBean(object, name);
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object, 
                                        ObjectName name,
                                        ClassLoader loader)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      return getMBeanServer().registerMBean(object, name);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Unregisters an MBean with the server.
   *
   * @param name the name of the mbean.
   */
  public static void unregister(ObjectName name)
    throws MBeanRegistrationException,
           InstanceNotFoundException
  {
    getMBeanServer().unregisterMBean(name);
  }
  
  /**
   * Unregisters an MBean with the server.
   *
   * @param name the name of the mbean.
   */
  public static void unregister(ObjectName name, ClassLoader loader)
    throws MBeanRegistrationException,
           InstanceNotFoundException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);
      
      getMBeanServer().unregisterMBean(name);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static void unregister(String name)
    throws InstanceNotFoundException,
           MalformedObjectNameException,
           MBeanRegistrationException

  {
    ObjectName objectName = getObjectName(name);

    getMBeanServer().unregisterMBean(objectName);
    // return register(object, objectName);
  }

  public static <T> T find(String name, Class<T> api)
  {
    try {
      ObjectName objectName = getObjectName(name);
      MBeanServer server = getMBeanServer();
      
      if (server.isRegistered(objectName)) {
        return JMX.newMXBeanProxy(getMBeanServer(), objectName, api);
      }

      // webapp/1f00
      objectName = getContextObjectName(name);

      if (objectName != null && server.isRegistered(objectName)) {
        return JMX.newMXBeanProxy(getMBeanServer(), objectName, api);
      }
      else {
        return null;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Returns an ObjectName based on a short name.
   */
  public static ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    return getMBeanServer().createContext().getObjectName(name);
  }

  /**
   * Returns an ObjectName based on a short name.
   */
  public static ObjectName getContextObjectName(String name)
    throws MalformedObjectNameException
  {
    return getMBeanServer().findContextName(name);
  }

  /**
   * Returns an ObjectName based on a short name.
   */
  public static ObjectName getServerObjectName(String name)
    throws MalformedObjectNameException
  {
    return getMBeanServer().createContext().getServerObjectName(name);
  }

  /**
   * Returns an ObjectName based on a short name.
   */
  public static ObjectName findContextMatch(String name)
    throws MalformedObjectNameException
  {
    ObjectName objectName = getContextObjectName(name);
    
    if (objectName != null && getMBeanServer().isRegistered(objectName)) {
      return objectName;
    }
    
    objectName = getServerObjectName(name);
    
    if (objectName != null && getMBeanServer().isRegistered(objectName)) {
      return objectName;
    }
    else {
      return null;
    }
    // EnvironmentMBeanServer server = getMBeanServer();
    
    // return server.findContextName(name);
  }

  /**
   * Parses a name.
   */
  public static LinkedHashMap<String,String> parseProperties(String name)
  {
    LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
    
    parseProperties(map, name);

    return map;
  }

  /**
   * Parses a name.
   */
  public static void parseProperties(Map<String,String> properties,
                                     String name)
  {
    parseProperties(properties, name, 0);
  }

  /**
   * Parses a name.
   */
  private static void
    parseProperties(Map<String,String> properties, String name, int i)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    int len = name.length();
    
    while (i < len) {
      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }

      cb.clear();

      int ch;
      for (; i < len && (ch = name.charAt(i)) != '=' && ch != ',' &&
             ! Character.isWhitespace((char) ch); i++) {
        cb.append((char) ch);
      }

      String key = cb.toString();

      if (key.length() == 0) {
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));
      }

      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }

      if (len <= i || (ch = name.charAt(i)) == ',') {
        properties.put(key, "");
      }
      else if (ch == '=') {
        for (i++; i < len && Character.isWhitespace(name.charAt(i)); i++) {
        }

        if (len <= i || (ch = name.charAt(i)) == ',') {
          properties.put(key, "");
        }
        else if (ch == '"' || ch == '\'') {
          int end = ch;
          cb.clear();

          for (i++; i < len && (ch = name.charAt(i)) != end; i++) {
            if (ch == '\\') {
              ch = name.charAt(++i);
              cb.append((char) ch);
            }
            else
              cb.append((char) ch);
          }

          if (ch != end)
            throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                                   name));

          i++;

          String value = cb.toString();

          properties.put(key, value);
        }
        else {
          cb.clear();

          for (; i < len && (ch = name.charAt(i)) != ','; i++)
            cb.append((char) ch);

          properties.put(key, cb.toString());
        }
      }
      else {
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));
      }

      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }
      
      if (i < len && name.charAt(i) != ',')
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));

      i++;
    }
  }

  /**
   * Creates the clean name
   */
  public static ObjectName getObjectName(String domain,
                                         Map<String,String> properties)
    throws MalformedObjectNameException
  {
    StringBuilder cb = new StringBuilder();
    cb.append(domain);
    cb.append(':');

    boolean isFirst = true;

    Pattern escapePattern = Pattern.compile("[,=:\"*?]");

    // sort type first

    String type = properties.get("type");
    if (type != null) {
      cb.append("type=");
      if (escapePattern.matcher(type).find())
        type = ObjectName.quote(type);
      cb.append(type);

      isFirst = false;
    }

    for (String key : properties.keySet()) {
      if (key.equals("type")) {
        continue;
      }
      
      if (! isFirst)
        cb.append(',');
      isFirst = false;

      cb.append(key);
      cb.append('=');

      String value = properties.get(key);
      
      if (value == null) {
        throw new NullPointerException(String.valueOf(key));
      }

      if (value.length() == 0
          || (escapePattern.matcher(value).find()
              && ! (value.startsWith("\"") && value.endsWith("\"")))) {
        value = ObjectName.quote(value);
      }
      
      cb.append(value);
    }

    return new ObjectName(cb.toString());
  }

  /**
   * Returns the local view.
   */
  /*
  public static MBeanView getLocalView()
  {
    MBeanContext context = MBeanContext.getLocal();

    return context.getView();
  }
  */

  // static
  private JmxUtilResin() {}
}

