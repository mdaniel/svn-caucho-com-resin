/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.beans.*;

import javax.servlet.jsp.el.VariableResolver;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.log.Log;
import com.caucho.el.*;

/**
 * Bean utilities.
 */
public class BeanUtil {
  static final Logger log = Log.open(BeanUtil.class);
  static L10N L = new L10N(BeanUtil.class);
  static HashMap<String,Class> _primitiveClasses;

  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, HashMap<String,Object> variableMap, Path pwd)
    throws RegistryException
  {
    String className = node.getString("class-name", null);
      
    if (className == null)
      throw error(node, L.l("Bean create needs a `class-name' attribute"));

    return createBean(node, variableMap, pwd, className);
  }

  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, VariableResolver env)
    throws RegistryException
  {
    String className = node.getELString("class-name", null, env);
      
    if (className == null)
      throw error(node, L.l("Bean create needs a `class-name' attribute"));

    Object obj = instantiate(node, className);

    return configure(obj, node, env, true, true, true);
  }

  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, VariableResolver env, boolean init)
    throws RegistryException
  {
    String className = node.getELString("class-name", null, env);
      
    if (className == null)
      throw error(node, L.l("Bean create needs a `class-name' attribute"));

    Object obj = instantiate(node, className);

    return configure(obj, node, env, true, true, init);
  }
  
  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, HashMap<String,Object> variableMap,
	     Path pwd, boolean init)
    throws RegistryException
  {
    String className = node.getString("class-name", null);
      
    if (className == null)
      throw error(node, L.l("Bean create needs a `class-name' attribute"));

    return createBean(node, variableMap, pwd, className, init);
  }
  
  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, HashMap<String,Object> variableMap,
             Path pwd, String className)
    throws RegistryException
  {
    if (className == null)
      throw new NullPointerException();

    Object obj = instantiate(node, className);

    return configure(obj, node, variableMap, pwd, true, true, true);
  }
  
  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, VariableResolver env, String className)
    throws RegistryException
  {
    if (className == null)
      throw new NullPointerException();

    Object obj = instantiate(node, className);

    return configure(obj, node, env, true, true, true);
  }
  
  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, HashMap<String,Object> variableMap,
             Path pwd, String className, boolean callInit)
    throws RegistryException
  {
    if (className == null)
      throw new NullPointerException();

    Object obj = instantiate(node, className);

    return configure(obj, node, variableMap, pwd, true, true, callInit);
  }
  
  /**
   * Instantiate and configure a bean.
   */
  public static Object
  createBean(RegistryNode node, VariableResolver env,
             String className, boolean callInit)
    throws RegistryException
  {
    if (className == null)
      throw new NullPointerException();

    Object obj = instantiate(node, className);

    return configure(obj, node, env, true, true, callInit);
  }

  public static Object
  instantiate(RegistryNode node, String className)
    throws RegistryException
  {
    Class cl;
          
    try {
      cl = CauchoSystem.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw error(node, e);
    }

    if (! Modifier.isPublic(cl.getModifiers()))
      throw error(node, L.l("Custom bean `{0}' must be public.", className));
    
    if (Modifier.isAbstract(cl.getModifiers()))
      throw error(node, L.l("Custom bean `{0}' must not be abstract.",
                            className));

    Object obj = null;
    try {
      return cl.newInstance();
    } catch (InstantiationException e) {
      throw error(node, L.l("Can't create custom bean `{0}'.  The class must implement a public zero-arg constructor.", className));
    } catch (IllegalAccessException e) {
      throw error(node, L.l("Can't create custom bean `{0}'.  Access denied.", className));
    } catch (Error e) {
      throw error(node, L.l("Can't create custom bean `{0}'.  Unknown error {1}.", className, e));
    }
  }

  /**
   * Configure but allow implicit init-param.
   */
  public static Object
  configure(Object obj, RegistryNode config)
    throws RegistryException
  {
    return configure(obj, config, null, null, true, true, true);
  }

  /**
   * Configure but allow implicit init-param.
   */
  public static Object
  configureOptional(Object obj, RegistryNode config)
    throws RegistryException
  {
    return configure(obj, config, null, null, true, false, true);
  }
  
  /**
   * Configures a bean
   *
   * @param obj the bean to configure
   * @param config the registry configuration
   * @param pathVariableMap the variable map
   * @param pwd the current directory
   * @param allowImplicit allows non-init-param
   * @param isRequired all fields must match
   * @param callInit call init when done
   */
  public static Object
  configure(Object obj, RegistryNode config,
            HashMap<String,Object> pathVariableMap, Path pwd,
            boolean allowImplicit, boolean isRequired, boolean callInit)
    throws RegistryException
  {
    if (pathVariableMap == null)
      pathVariableMap = new HashMap<String,Object>();
    
    VariableResolver resolver = new SystemPropertiesResolver();
    resolver = new EnvironmentVariableResolver(resolver);
    resolver = new MapVariableResolver(pathVariableMap, resolver);
    Object oldPwd = pathVariableMap.get("resin:pwd");

    if (pwd != null)
      pathVariableMap.put("resin:pwd", pwd);
    else if (oldPwd == null)
      pathVariableMap.put("resin:pwd", Vfs.lookup("."));

    configure(obj, config, resolver, allowImplicit, isRequired, callInit);

    if (oldPwd == null)
      pathVariableMap.remove("resin:pwd");
    else
      pathVariableMap.put("resin:pwd", oldPwd);

    return obj;
  }
  
  /**
   * Sets the properties for a bean by calling setXXX for each init-param.
   *
   * @param obj the bean to configure
   * @param config the registry configuration
   * @param env the variable environment
   * @param callInit call init when done
   */
  public static Object
  configure(Object obj, RegistryNode config,
            VariableResolver env,
            boolean callInit)
    throws RegistryException
  {
    return configure(obj, config, env, true, true, callInit);
  }
  
  /**
   * Sets the properties for a bean by calling setXXX for each init-param.
   *
   * @param obj the bean to configure
   * @param config the registry configuration
   * @param env the variable environment
   * @param allowImplicit allows non-init-param
   * @param isRequired all fields must match
   * @param callInit call init when done
   */
  public static Object
  configure(Object obj, RegistryNode config,
            VariableResolver env,
            boolean allowImplicit, boolean isRequired, boolean callInit)
    throws RegistryException
  {
    try {
      BeanInfo info = Introspector.getBeanInfo(obj.getClass());

      if (config == null)
        return obj;
    
      String initClass = config.getELString("init-class", null, env);
      if (initClass != null) {
        RegistryNode initNode = config.lookup("init-class");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Object initObject = Beans.instantiate(loader, initClass);

        if (initObject instanceof Map) {
          Map map = (Map) initObject;
        
          Iterator names = map.keySet().iterator();
          while (names.hasNext()) {
            String name = (String) names.next();
            String value = (String) map.get(name);

            setBeanProperty(obj, name, value, info, initNode, env, isRequired);
          }
        }
        else if (initObject instanceof Hashtable) {
          Hashtable map = (Hashtable) initObject;
        
          Enumeration names = map.keys();
          while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = (String) map.get(name);

            setBeanProperty(obj, name, value, info, initNode, env, isRequired);
          }
        }
        else
          throw error(initNode, L.l("`{0}' must be a Map", initClass));
      }
    
      Iterator iter = config.iterator();
      while (iter.hasNext()) {
        RegistryNode subnode = (RegistryNode) iter.next();
                
        String name = subnode.getName();
        if (name.equals("init-param")) {
          name = subnode.getString("param-name", null);
          String value = subnode.getString("param-value", null);

          if (name != null || value != null) {
            setBeanProperty(obj, name, value, info, subnode,
                            env, isRequired);
            continue;
          }

          Iterator subiter = subnode.iterator();
          while (subiter.hasNext()) {
            RegistryNode paramNode = (RegistryNode) subiter.next();
            
            setBeanProperty(obj, paramNode.getName(), paramNode.getValue(),
                            info, paramNode, env, isRequired);
          }
        }
        else if (allowImplicit) {
          Class propertyClass = getBeanPropertyClass(obj, name);

          if (propertyClass == null) {
          }
          else if (subnode.getFirstChild() != null) {
            Object child = propertyClass.newInstance();

            configure(child, subnode);
            
            Method setter = getBeanPropertyMethod(obj, name);

            setter.invoke(obj, new Object[] { child });
          }
          else {
            String value = subnode.getValue();

            setBeanProperty(obj, name, value, info, subnode, env, false);
          }
        }
      }

      try {
        Class cl = obj.getClass();
        Method method = cl.getMethod("setRegistry",
                                     new Class[] { RegistryNode.class });

        if (method != null)
          method.invoke(obj, new Object[] { config });
      } catch (Exception e) {
      }

      if (callInit) {
        Method method = null;

        try {
          Class cl = obj.getClass();
          method = cl.getMethod("init", new Class[0]);
        } catch (Exception e) {
        }

        if (method != null)
          method.invoke(obj, new Object[] { });
      }
    } catch (RegistryException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw error(config, e.getTargetException());
    } catch (Exception e) {
      throw error(config, e);
    }

    return obj;
  }

  /**
   * Configures a single bean property.
   *
   * @param obj the bean to configure
   * @param config the configuration node (for error messages)
   * @param name the property name
   * @param value the property name
   */
  public static void
  setBeanProperty(Object obj, String name, String value)
    throws RegistryException
  {
    try {
      BeanInfo info = Introspector.getBeanInfo(obj.getClass());

      setBeanProperty(obj, name, value, info, null, null, null, true);
    } catch (RegistryException e) {
      throw e;
    } catch (Exception e) {
      throw new RegistryException(e);
    }
  }
  
  /**
   * Configures a single bean property.
   *
   * @param obj the bean to configure
   * @param name the property name
   * @param value the property value
   * @param info the bean's introspected info
   * @param config the configuration node (for error messages)
   */
  public static void
  setBeanProperty(Object obj, String name, String stringValue,
                  BeanInfo info, RegistryNode config,
                  HashMap<String,Object> pathVariableMap, Path pwd,
                  boolean isRequired)
    throws RegistryException
  {
    VariableResolver resolver = new MapVariableResolver(pathVariableMap);
    Object oldPwd = pathVariableMap.get("resin:pwd");

    if (pwd != null)
      pathVariableMap.put("resin:pwd", pwd);
    else if (oldPwd == null)
      pathVariableMap.put("resin:pwd", Vfs.lookup("."));

    setBeanProperty(obj, name, stringValue, info, config,
                    resolver, isRequired);

    pathVariableMap.put("resin:pwd", oldPwd);
  }

  /**
   * Returns the bean property type.
   *
   * @param obj the bean object
   * @param name the property name
   */
  public static Class
  getBeanPropertyClass(Object obj, String name)
  {
    Method method = getBeanPropertyMethod(obj, name);

    if (method == null)
      return null;

    Class []paramTypes = method.getParameterTypes();
    if (paramTypes.length == 1)
      return paramTypes[0];
    else
      return null;
  }

  /**
   * Returns the bean property type.
   *
   * @param obj the bean object
   * @param name the property name
   */
  public static Method
  getBeanPropertyMethod(Object obj, String name)
  {
    name = configToBeanName(name);

    Class beanClass = obj.getClass();
    Method method = getSetMethod(beanClass, name);

    if (method == null)
      method = getAddMethod(beanClass, name);

    return method;
  }

  public static void
  validateClass(Class cl, Class parent)
    throws RegistryException
  {
    if (parent.isAssignableFrom(cl)) {
    }
    else if (parent.isInterface())
      throw new RegistryException(L.l("{0} must implement {1}",
                                      cl.getName(), parent.getName()));
    else
      throw new RegistryException(L.l("{0} must extend {1}",
                                      cl.getName(), parent.getName()));

    if (cl.isInterface())
      throw new RegistryException(L.l("{0} must be a concrete class.",
                                      cl.getName()));
    
    if (Modifier.isAbstract(cl.getModifiers()))
      throw new RegistryException(L.l("{0} must not be abstract.",
                                      cl.getName()));
    
    if (! Modifier.isPublic(cl.getModifiers()))
      throw new RegistryException(L.l("{0} must be public.",
                                      cl.getName()));

    Constructor zero = null;
    try {
      zero = cl.getConstructor(new Class[0]);
    } catch (Throwable e) {
    }

    if (zero == null)
      throw new RegistryException(L.l("{0} must have a public zero-arg constructor.",
                                      cl.getName()));
  }
  
  /**
   * Configures a single bean property.
   *
   * @param obj the bean to configure
   * @param name the property name
   * @param value the property value
   * @param info the bean's introspected info
   * @param config the configuration node (for error messages)
   */
  public static void
  setBeanProperty(Object obj, String name, String stringValue,
                  BeanInfo info, RegistryNode config,
                  VariableResolver env,
                  boolean isRequired)
    throws RegistryException
  {
    if (name == null || stringValue == null)
      throw error(config, L.l("unknown name"));

    if (name.equals("res-ref-name") || name.equals("res-type"))
      return;

    Object value = stringValue;
    Expr expr;

    try {
      expr = new ELParser(stringValue).parse();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      expr = new StringLiteral(stringValue);
    }

    try {
      name = configToBeanName(name);
      
      Method method = getSetMethod(info, name);

      if (method == null)
        method = getAddMethod(info.getBeanDescriptor().getBeanClass(), name);
      
      if (method == null) {
        setBeanPropertyMethod(obj, name, expr.evalString(env),
                              config, isRequired);
        return;
      }

      Class type = method.getParameterTypes()[0];
      String typeName = type.getName();

      if (typeName.equals("boolean") ||
          typeName.equals("java.lang.Boolean"))
        method.invoke(obj, new Object[] { new Boolean(expr.evalBoolean(env)) });
      else if (typeName.equals("int") ||
               typeName.equals("java.lang.Integer"))
        method.invoke(obj, new Object[] { new Integer((int) expr.evalLong(env)) });
      else if (typeName.equals("double") ||
               typeName.equals("java.lang.Double"))
        method.invoke(obj, new Object[] { new Double(expr.evalDouble(env)) });
      else if (long.class.equals(type) || Long.class.equals(type)) {
        long period = expr.evalPeriod(env);
        method.invoke(obj, new Object[] { new Long(period) });
      }
      else if (typeName.equals("com.caucho.util.InetNetwork")) {
        InetNetwork network = RegistryNode.parseNetwork(expr.evalString(env));
        
        method.invoke(obj, new Object[] { network });
      }
      else if (type.equals(Class.class)) {
        String className = expr.evalString(env);
        
        Class cl = (Class) _primitiveClasses.get(className);

        if (cl == null)
          cl = CauchoSystem.loadClass(className);
        
        method.invoke(obj, new Object[] { cl });
      }
      else if (Path.class.equals(type)) {
        Path pwd = (Path) env.resolveVariable("resin:pwd");
        if (pwd == null)
          pwd = Vfs.lookup(".");
        Path path = pwd.lookup(expr.evalString(env));
        method.invoke(obj, new Object[] { path });
      }
      else if (String.class.equals(type))
        method.invoke(obj, new Object[] { expr.evalString(env) });
      else {
        value = expr.evalObject(env);

        if (value == null)
          method.invoke(obj, new Object[] { null });
        else if (type.isAssignableFrom(value.getClass()))
          method.invoke(obj, new Object[] { value });
        else if (value instanceof String) {
          try {
            Method setTextValue = type.getMethod("setTextValue",
                                                 new Class[] { String.class });

            if (setTextValue != null) {
              Object subobj = type.newInstance();
              setTextValue.invoke(subobj, new Object[] { value });

              try {
                Method init = type.getMethod("init", new Class[] {});
                if (init != null)
                  init.invoke(subobj, new Object[0]);
              }
              catch (Exception e) {
              }

              method.invoke(obj, new Object[] { subobj });
            }
            else
              throw error(config, L.l("value `{0}' cannot be assigned to `{1}' for {2}", value, type.getName(), method));
          } catch (Exception e) {
            throw error(config, L.l("value `{0}' cannot be assigned to `{1}' for {2}", value, type.getName(), method));
          }
        }
        else
          throw error(config, L.l("value `{0}' cannot be assigned to `{1}' for {2}", value, type.getName(), method));
      }
    } catch (RegistryException e) {
      throw e;
    } catch (Exception e) {
      throw error(config, e);
    }
  }

  /**
   * Returns the native path for a configured path name.  The special cases
   * $app-dir and $resin-home specify the root directory.
   *
   * @param pathName the configuration path name.
   * @param varMap the map of path variables.
   * @param pwd the default path.
   *
   * @return a real path corresponding to the path name
   */
  public static Path lookupPath(String pathName, HashMap varMap, Path pwd)
  {
    if (pwd == null)
      pwd = Vfs.lookup();
    
    if (pathName.startsWith("$")) {
      int p = pathName.indexOf('/');
      String prefix;
      String suffix;
      
      if (p > 0) {
        prefix = pathName.substring(1, p);
        suffix = pathName.substring(p + 1);
      }
      else {
        prefix = pathName.substring(1);
        suffix = null;
      }

      Object value = varMap != null ? varMap.get(prefix) : null;
      if (value instanceof Path) {
        pwd = (Path) value;
        pathName = suffix;
      }
    }

    if (pathName == null)
      return pwd;
    else if (pathName.indexOf('$') < 0)
      return pwd.lookup(pathName);
    
    CharBuffer cb = CharBuffer.allocate();
    int head = 0;
    int tail = 0;
    while ((tail = pathName.indexOf('$', head)) >= 0) {
      cb.append(pathName.substring(head, tail));

      if (tail + 1 == pathName.length()) {
        cb.append('$');
        continue;
      }

      int ch = pathName.charAt(tail + 1);
      
      if (ch >= '0' && ch <= '9') {
        for (head = tail + 1; head < pathName.length(); head++) {
          ch = pathName.charAt(head);
        
          if (ch < '0' || ch > '9')
            break;
        }
      }
      else {
        for (head = tail + 1; head < pathName.length(); head++) {
          ch = pathName.charAt(head);
        
          if (ch == '/' || ch == '\\' || ch == '$' || ch == ' ')
            break;
        }
      }

      String key = pathName.substring(tail + 1, head);
      Object value = varMap != null ? varMap.get(key) : null;

      if (value == null)
        value = System.getProperty(key);

      if (value != null)
        cb.append(value);
      else
        cb.append(pathName.substring(tail, head));
    }

    if (head > 0 && head < pathName.length())
      cb.append(pathName.substring(head));
    
    return pwd.lookupNative(cb.close());
  }

  /**
   * Translates a configuration name to a bean name.
   *
   * <pre>
   * foo-bar maps to fooBar
   * </pre>
   */
  private static String configToBeanName(String name)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      
      if (ch == '-')
        cb.append(Character.toUpperCase(name.charAt(++i)));
      else
        cb.append(ch);
    }

    return cb.close();
  }

  /**
   * Returns an add method matching the name.
   */
  private static Method getAddMethod(Class cl, String name)
  {
    name = "add" + name;

    Method []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! Modifier.isPublic(methods[i].getModifiers()))
        continue;

      if (! name.equalsIgnoreCase(methods[i].getName()))
        continue;

      if (methods[i].getParameterTypes().length == 1)
        return methods[i];
    }

    return null;
  }

  /*
   * Sets the property for a translated bean method.
   *
   * @param obj the bean to configure
   * @param name the property name
   * @param value the property value
   * @param node the configure node (for errors)
   */
  private static void
  setBeanPropertyMethod(Object obj, String name, String value,
                        RegistryNode node, boolean isRequired)
    throws Exception
  {
    Method []methods = obj.getClass().getMethods();

    Class []params = new Class[] { String.class, String.class };
    
    Method method = getMethod(methods, "setProperty", params);
    if (method == null)
      method = getMethod(methods, "setAttribute", params);
    if (method == null)
      method = getMethod(methods, "put", params);
    if (method == null)
      method = getMethod(methods, "set", params);

    if (method != null)
      method.invoke(obj, new Object[] { name, value });
    else if (isRequired)
      throw error(node, L.l("can't set property {0} on {1}",
                            name, obj.getClass()));
    
  }

  /**
   * Returns the method matching the name.
   */
  static private Method getMethod(Method []methods, String name)
  {
    Method method = null;
    for (int i = 0; i < methods.length; i++) {
      method = methods[i];

      if (! Modifier.isPublic(method.getModifiers()))
          continue;
      
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
          continue;
      
      if (method.getName().equals(name))
        return method;
    }

    return null;
  }

  /**
   * Returns the method matching the name.
   */
  static private Method getMethod(Method []methods, String name,
                                  Class []params)
  {
    Method method = null;

    loop:
    for (int i = 0; i < methods.length; i++) {
      method = methods[i];
      
      if (! Modifier.isPublic(method.getModifiers()))
        continue;
      
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;
      
      if (! method.getName().equals(name))
        continue;

      Class []actual = method.getParameterTypes();

      if (actual.length != params.length)
        continue;

      for (int j = 0; j < actual.length; j++) {
        if (! actual[j].isAssignableFrom(params[j]))
          continue loop;
      }
      
      return method;
    }

    return null;
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(BeanInfo info, String propertyName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    Method method = null;
    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getName().equals(propertyName) &&
          pds[i].getWriteMethod() != null) {
        method = pds[i].getWriteMethod();

        if (method.getParameterTypes()[0].equals(String.class))
          return method;
      }
    }

    if (method != null)
      return method;

    return getSetMethod(info.getBeanDescriptor().getBeanClass(), propertyName);
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(Class cl, String propertyName)
  {
    Method method = getSetMethod(cl, propertyName, false);

    if (method != null)
      return method;

    return getSetMethod(cl, propertyName, true);
  }

  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(Class cl,
                                    String propertyName,
                                    boolean ignoreCase)
  {
    String setName = "set" + propertyNameToMethodName(propertyName);

    Method bestMethod = null;
    
    for (Class ptrCl = cl; ptrCl != null; ptrCl = ptrCl.getSuperclass()) {
      Method method = getSetMethod(ptrCl.getMethods(),
                                   setName,
                                   ignoreCase);

      if (method != null && method.getParameterTypes()[0].equals(String.class))
        return method;
      else if (method != null)
        bestMethod = method;
    }

    if (bestMethod != null)
      return bestMethod;

    Class []interfaces = cl.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      Method method = getSetMethod(interfaces[i].getMethods(),
                                   setName,
                                   ignoreCase);

      if (method != null && method.getParameterTypes()[0].equals(String.class))
        return method;
      else if (method != null)
        bestMethod = method;
    }

    if (bestMethod != null)
      return bestMethod;

    return null;
  }

  /**
   * Finds the matching set method
   *
   * @param method the methods for the class
   * @param setName the method name
   */
  private static Method getSetMethod(Method []methods,
                                     String setName,
                                     boolean ignoreCase)
  {
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      // The method name must match
      if (! ignoreCase && ! method.getName().equals(setName))
        continue;
      
      // The method name must match
      if (ignoreCase && ! method.getName().equalsIgnoreCase(setName))
        continue;
      
      // The method must be public
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      // It must be in a public class or interface
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;

      // It must have a single parameter
      if (method.getParameterTypes().length != 1)
        continue;
      
      // It must return void
      if (method.getReturnType().equals(void.class)) {
        return method;
      }
    }

    return null;
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getGetMethod(BeanInfo info, String propertyName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getName().equals(propertyName) &&
          pds[i].getReadMethod() != null) {
	if (! Modifier.isPublic(pds[i].getReadMethod().getDeclaringClass().getModifiers())) {
	  try {
	    pds[i].getReadMethod().setAccessible(true);
	  } catch (Throwable e) {
	    continue;
	  }
	}
	
        return pds[i].getReadMethod();
    }
    }

    return getGetMethod(info.getBeanDescriptor().getBeanClass(), propertyName);
  }

  /**
   * Returns a get method matching the property name.
   */
  public static Method getGetMethod(Class cl, String propertyName)
  {
    Method method = getGetMethod(cl, propertyName, false);

    if (method != null)
      return method;
    
    return getGetMethod(cl, propertyName, true);
  }

  /**
   * Returns a get method matching the property name.
   */
  public static Method getGetMethod(Class cl,
                                    String propertyName,
                                    boolean ignoreCase)
  {
    String getName = "get" + propertyNameToMethodName(propertyName);
    String isName = "is" + propertyNameToMethodName(propertyName);

    for (Class ptrCl = cl; ptrCl != null; ptrCl = ptrCl.getSuperclass()) {
      Method method = getGetMethod(ptrCl.getDeclaredMethods(), getName,
                                   isName, ignoreCase);

      if (method != null)
        return method;

      Class []interfaces = ptrCl.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
	method = getGetMethod(interfaces[i].getDeclaredMethods(),
			      getName, isName, ignoreCase);

	if (method != null)
	  return method;
      }
    }

    return null;
  }

  /**
   * Finds the matching set method
   *
   * @param method the methods for the class
   * @param setName the method name
   */
  private static Method getGetMethod(Method []methods,
                                     String getName,
                                     String isName,
                                     boolean ignoreCase)
  {
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      // The method must be public
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      // It must be in a public class or interface
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;

      // It must have no parameters
      if (method.getParameterTypes().length != 0)
        continue;
      
      // It must not return void
      if (method.getReturnType().equals(void.class))
        continue;

      // If it matches the get name, it's the right method
      else if (! ignoreCase && methods[i].getName().equals(getName))
        return methods[i];
      
      // If it matches the get name, it's the right method
      else if (ignoreCase && methods[i].getName().equalsIgnoreCase(getName))
        return methods[i];

      // The is methods must return boolean
      else if (! methods[i].getReturnType().equals(boolean.class))
        continue;
      
      // If it matches the is name, it must return boolean
      else if (! ignoreCase && methods[i].getName().equals(isName))
        return methods[i];
      
      // If it matches the is name, it must return boolean
      else if (ignoreCase && methods[i].getName().equalsIgnoreCase(isName))
        return methods[i];
    }

    return null;
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param propertyName the user property name
   * @return the equivalent bean method name
   */
  public static String propertyNameToMethodName(String propertyName)
  {
    char ch = propertyName.charAt(0);
    if (Character.isLowerCase(ch))
      propertyName = Character.toUpperCase(ch) + propertyName.substring(1);

    return propertyName;
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param methodName the method name
   * @return the equivalent property name
   */
  public static String methodNameToPropertyName(BeanInfo info,
                                                String methodName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getReadMethod() != null &&
          pds[i].getReadMethod().getName().equals(methodName))
        return pds[i].getName();
      if (pds[i].getWriteMethod() != null &&
          pds[i].getWriteMethod().getName().equals(methodName))
        return pds[i].getName();
    }

    return methodNameToPropertyName(methodName);
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param methodName the method name
   * @return the equivalent property name
   */
  public static String methodNameToPropertyName(String methodName)
  {
    if (methodName.startsWith("get"))
      methodName = methodName.substring(3);
    else if (methodName.startsWith("set"))
      methodName = methodName.substring(3);
    else if (methodName.startsWith("is"))
      methodName = methodName.substring(2);

    if (methodName.length() == 0)
      return null;

    char ch = methodName.charAt(0);
    if (Character.isUpperCase(ch) &&
        (methodName.length() == 1 ||
         ! Character.isUpperCase(methodName.charAt(1)))) {
      methodName = Character.toLowerCase(ch) + methodName.substring(1);
    }

    return methodName;
  }

  /**
   * Returns a new configuration exception for the node.
   */
  static private RegistryException error(RegistryNode node, String msg)
  {
    if (node == null)
      return new RegistryException(msg);
    else
      return new RegistryException(node.getFilename() + ":" + node.getLine() +
                                   ": " + msg);
  }

  /**
   * Returns a new configuration exception for the node.
   */
  static private RegistryException error(RegistryNode node, Throwable e)
  {
    if (e instanceof RegistryException)
      return (RegistryException) e;
    else if (node == null)
      return new RegistryException(String.valueOf(e), e);
    else
      return new RegistryException(node.getFilename() + ":" + node.getLine() +
                                   ": " + String.valueOf(e), e);
  }

  static {
    _primitiveClasses = new HashMap<String,Class>();
    _primitiveClasses.put("boolean", boolean.class);
    _primitiveClasses.put("byte", byte.class);
    _primitiveClasses.put("short", short.class);
    _primitiveClasses.put("char", char.class);
    _primitiveClasses.put("int", int.class);
    _primitiveClasses.put("long", long.class);
    _primitiveClasses.put("float", float.class);
    _primitiveClasses.put("double", double.class);
  }
}
