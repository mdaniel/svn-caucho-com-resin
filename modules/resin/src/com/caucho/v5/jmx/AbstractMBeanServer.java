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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jmx;

import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import com.caucho.v5.loader.Environment;
import com.caucho.v5.loader.WeakCloseListener;

/**
 * The main interface for retrieving and managing JMX objects.
 */
abstract public class AbstractMBeanServer implements MBeanServer {
  private static final Logger log
    = Logger.getLogger(AbstractMBeanServer.class.getName());

  static ObjectName SERVER_DELEGATE_NAME;

  private MBeanServer _outerServer;

  // default domain
  private String _defaultDomain;

  protected AbstractMBeanServer(String defaultDomain,
                                MBeanServer outer)
  {
    _defaultDomain = defaultDomain;
    _outerServer = outer;
  }

  protected MBeanServer getOuterServer()
  {
    return _outerServer;
  }

  /**
   * Creats a new MBeanServer implementation.
   */
  public AbstractMBeanServer(String defaultDomain)
  {
    _defaultDomain = defaultDomain;

    Environment.addClassLoaderListener(new WeakCloseListener(this));
  }

  /**
   * Returns the context implementation.
   */
  protected MBeanContext createContext()
  {
    return createContext(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the context implementation.
   */
  protected final MBeanContext getCurrentContext()
  {
    return getCurrentContext(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the context implementation.
   */
  protected final MBeanContext getGlobalContext()
  {
    return createContext(ClassLoader.getSystemClassLoader());
  }

  /**
   * Returns the context implementation, creating if necessary.
   */
  abstract protected MBeanContext createContext(ClassLoader loader);

  /**
   * Returns the context implementation.
   */
  abstract protected MBeanContext getCurrentContext(ClassLoader loader);

  /**
   * Sets the context implementation.
   */
  abstract protected void setCurrentContext(MBeanContext context,
                                            ClassLoader loader);

  /**
   * Returns the context implementation.
   */
  abstract protected MBeanContext getContext(ClassLoader loader);

  /**
   * Removes the context implementation.
   */
  protected void removeContext(MBeanContext context, ClassLoader loader)
  {
  }

  /**
   * Instantiate an MBean object to be registered with the server.
   *
   * @param className the className to be instantiated.
   *
   * @return the instantiated object.
   */
  @Override
  public Object instantiate(String className)
    throws ReflectionException, MBeanException
  {
    try {
      Class<?> cl = getClassLoaderRepository().loadClass(className);

      return cl.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    } catch (InstantiationException e) {
      throw new ReflectionException(e);
    } catch (ExceptionInInitializerError e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception)
        throw new MBeanException((Exception) cause);
      else
        throw e;
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Instantiate an MBean object to be registered with the server.
   *
   * @param className the className to be instantiated.
   * @param loaderName names the classloader to be used
   *
   * @return the instantiated object.
   */
  @Override
  public Object instantiate(String className, ObjectName loaderName)
    throws ReflectionException, MBeanException, InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(loaderName);
    
    return getOuterServer().instantiate(className, contextName);
  }

  /**
   * Instantiate an MBean object with the given arguments to be
   * passed to the constructor.
   *
   * @param className the className to be instantiated.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  @Override
  public Object instantiate(String className,
                            Object []params, String []signature)
    throws ReflectionException, MBeanException
  {
    return getOuterServer().instantiate(className, params, signature);
  }

  /**
   * Instantiate an MBean object with the given arguments to be
   * passed to the constructor.
   *
   * @param className the className to be instantiated.
   * @param loaderName names the classloader to be used
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  @Override
  public Object instantiate(String className,
                            ObjectName loaderName,
                            Object []params,
                            String []signature)
    throws ReflectionException, MBeanException, InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(loaderName);
    
    return getOuterServer().instantiate(className, contextName, params, signature);
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException
  {
    return registerMBean(instantiate(className), name);
  }


  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param loaderName the name of the class loader to user
   *
   * @return the instantiated object.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException, InstanceNotFoundException
  {
    return registerMBean(instantiate(className, loaderName), name);
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException
  {
    return registerMBean(instantiate(className, params, signature),
                         name);
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param loaderName the loader name for the mbean.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  @Override
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException, InstanceNotFoundException
  {
    return registerMBean(instantiate(className, loaderName, params, signature),
                         name);
  }

  protected boolean isRegisterByContextName()
  {
    return true;
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  @Override
  public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    MBeanContext context = createContext();

    ObjectName contextName = context.getContextObjectName(name);
    ObjectInstance instance = null;
    
    if (isRegisterByContextName() && contextName != null) {
      instance = getOuterServer().registerMBean(object, contextName);
      context.addMBean(contextName);
    }
    else {
      instance = getOuterServer().registerMBean(object, name);
      context.addMBean(name);
    }

    return instance;
  }

  /**
   * Unregisters an MBean from the server.
   *
   * @param name the name of the mbean.
   */
  @Override
  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException,
           MBeanRegistrationException
  {
    MBeanContext context = getCurrentContext();

    ObjectName contextName = null;
    
    if (context != null) {
      contextName = context.getContextObjectName(name);
    }

    try {
      getOuterServer().unregisterMBean(name);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    try {
      if (contextName != null) {
        getOuterServer().unregisterMBean(contextName);
      }
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    if (context != null) {
      context.removeMBean(name);
      
      if (contextName != null) {
        context.removeMBean(contextName);
      }
    }
  }

  /**
   * Returns the MBean registered with the given name.
   *
   * @param name the name of the mbean.
   *
   * @return the matching mbean object.
   */
  @Override
  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    return getOuterServer().getObjectInstance(contextName);
  }

  /**
   * Returns a set of MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the queryd to match.
   *
   * @return the set of matching mbean object.
   */
  @Override
  public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
  {
    return _outerServer.queryMBeans(name, query);
  }

  /**
   * Returns a set of names for MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the query to match.
   *
   * @return the set of matching mbean names.
   */
  @Override
  public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
  {
    Set<ObjectName> names = _outerServer.queryNames(name, query);
    
    if (names != null && names.size() > 0) {
      return names;
    }
    
    ObjectName contextName = findContextName(name);
    if (contextName != null) {
      return _outerServer.queryNames(contextName, query);
    }
    else {
      return names;
    }
  }

  private Set<ObjectName> queryNamesImpl(ObjectName name, QueryExp query)
  {
    return _outerServer.queryNames(name, query);
  }

  /**
   * Returns true if the given object is registered with the server.
   *
   * @param name the name of the mbean to test.
   *
   * @return true if the object is registered.
   */
  @Override
  public boolean isRegistered(ObjectName name)
  {
    if (name != null) {
      return getOuterServer().isRegistered(name);
    }
    else {
      return false;
    }
  }

  /**
   * Returns the number of MBeans registered.
   *
   * @return the number of registered mbeans.
   */
  @Override
  public Integer getMBeanCount()
  {
    return _outerServer.getMBeanCount();
  }

  /**
   * Returns a specific attribute of a named MBean.
   *
   * @param name the name of the mbean to test
   * @param attribute the name of the attribute to retrieve
   *
   * @return the attribute value
   */
  @Override
  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException,
           InstanceNotFoundException, ReflectionException
  {
    return getOuterServer().getAttribute(getContextInstanceName(name), attribute);
  }
  
  protected ObjectName getContextInstanceName(ObjectName name)
  {
    if (! isRegisterByContextName()) {
      return name;
    }
    else if (getOuterServer().isRegistered(name)) {
      return name;
    }
    else {
      MBeanContext context = getCurrentContext();
      
      if (context == null) {
        return name;
      }
      
      ObjectName contextName = findContextName(name);

      if (contextName != null && getOuterServer().isRegistered(contextName)) {
        return contextName;
      }
      else {
        return name;
      }
    }
  }
  
  protected ObjectName findContextName(ObjectName name)
  {
    try {
      return findContextName(name.toString());
    } catch (MalformedObjectNameException e) {
      log.log(Level.FINER, e.toString(), e);
      return null;
    }
  }
  
  public ObjectName findContextName(String name)
    throws MalformedObjectNameException
  {
    MBeanContext context = createContext();
    
    Map<String,String> props = context.getProperties();
    
    ObjectName patternName;
    
    boolean isQuery = false;
    if (name.indexOf("*") < 0) {
      patternName = new ObjectName(name + ",*");
    }
    else {
      patternName = new ObjectName(name);
      isQuery = true;
    }
    
    ObjectName bestName = null;
    int bestLength = Integer.MAX_VALUE;

    for (ObjectName matchName : queryNamesImpl(patternName, null)) {
      if (bestLength < matchName.toString().length()) {
        continue;
      }
      
      if (! isMatch(matchName, patternName, props)) {
        continue;
      }
      
      bestName = matchName;
      bestLength = matchName.toString().length();
    }
    
    if (isQuery && bestName != null) {
      return new ObjectName(bestName.toString() + ",*");
    }
    
    return bestName;
  }
  
  private static boolean isMatch(ObjectName matchName, 
                                 ObjectName patternName,
                                 Map<String,String> contextProps)
  {
    
    Hashtable<String,String> propList = matchName.getKeyPropertyList();
    
    for (Map.Entry<String,String> entry : propList.entrySet()) {
      if (patternName.getKeyProperty(entry.getKey()) != null) {
        continue;
      }
      
      String propValue = contextProps.get(entry.getKey());
      
      // if (propValue == null || ! propValue.equals(entry.getValue())) {
      if (propValue != null && ! propValue.equals(entry.getValue())) {
        return false;
      }
    }
    
    return true;
  }

  /**
   * Returns a list of several MBean attributes.
   *
   * @param name the name of the mbean
   * @param attributes the name of the attributes to retrieve
   *
   * @return the attribute value
   */
  @Override
  public AttributeList getAttributes(ObjectName name, String []attributes)
    throws InstanceNotFoundException, ReflectionException
  {
    return getOuterServer().getAttributes(getContextInstanceName(name), attributes);
  }

  /**
   * Sets an attribute in the MBean.
   *
   * @param name the name of the mbean
   * @param attribute the name/value of the attribute to set.
   */
  @Override
  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException,
           InvalidAttributeValueException, MBeanException, ReflectionException
  {
    getOuterServer().setAttribute(getContextInstanceName(name), attribute);
  }

  /**
   * Set an attributes in the MBean.
   *
   * @param name the name of the mbean
   * @param attributes the name/value list of the attribute to set.
   */
  @Override
  public AttributeList setAttributes(ObjectName name, AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException
  {
    return getOuterServer().setAttributes(getContextInstanceName(name), attributes);
  }

  /**
   * Invokers an operation on an MBean.
   *
   * @param name the name of the mbean
   * @param operationName the name of the method to invoke
   * @param params the parameters for the invocation
   * @param signature the signature of the operation
   */
  public Object invoke(ObjectName name,
                       String operationName,
                       Object []params,
                       String []signature)
    throws InstanceNotFoundException, MBeanException, ReflectionException
  {
    return getOuterServer().invoke(getContextInstanceName(name),
                                   operationName, params, signature);
  }

  /**
   * Returns the default domain for naming the MBean
   */
  @Override
  public String getDefaultDomain()
  {
    return _defaultDomain;
  }

  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  @Override
  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    getOuterServer().addNotificationListener(contextName, listener, filter, handback);

    createContext().addNotificationListener(contextName, listener, filter, handback);
  }

  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  @Override
  public void addNotificationListener(ObjectName name,
                                      ObjectName listenerName,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    ObjectName contextListener = getContextInstanceName(listenerName);
    
    getOuterServer().addNotificationListener(contextName, contextListener,
                                             filter, handback);
    /*
    createContext().addNotificationListener(contextName, contextListener,
                                            filter, handback);
                                            */
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   */
  @Override
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    getOuterServer().removeNotificationListener(contextName, listener);

    createContext().removeNotificationListener(contextName, listener);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   */
  @Override
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    ObjectName contextListener = getContextInstanceName(listenerName);
    
    getOuterServer().removeNotificationListener(contextName, contextListener);
    
    // createContext().removeNotificationListener(contextName, contextListener);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter the notification filter
   * @param handback context to the listener
   *
   * @since JMX 1.2
   */
  @Override
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    ObjectName contextListener = getContextInstanceName(listenerName);
    
    getOuterServer().removeNotificationListener(contextName, contextListener, filter, handback);

    createContext().removeNotificationListener(contextName, contextListener, filter, handback);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter the notification filter
   * @param handback context to the listener
   *
   * @since JMX 1.2
   */
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    getOuterServer().removeNotificationListener(contextName, listener, filter, handback);

    createContext().removeNotificationListener(contextName, listener, filter, handback);
  }

  /**
   * Returns the analyzed information for an MBean
   *
   * @param name the name of the mbean
   *
   * @return the introspected information
   */
  @Override
  public MBeanInfo getMBeanInfo(ObjectName name)
    throws InstanceNotFoundException, IntrospectionException,
           ReflectionException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    return getOuterServer().getMBeanInfo(contextName);
  }

  /**
   * Returns true if the MBean is an instance of the specified class.
   *
   * @param name the name of the mbean
   * @param className the className to test.
   *
   * @return true if the bean is an instance
   */
  @Override
  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    return getOuterServer().isInstanceOf(contextName, className);
  }

  /**
   * Returns the ClassLoader that was used for loading the MBean.
   *
   * @param mbeanName the name of the mbean
   *
   * @return the class loader
   *
   * @since JMX 1.2
   */
  @Override
  public ClassLoader getClassLoaderFor(ObjectName name)
    throws InstanceNotFoundException
  {
    ObjectName contextName = getContextInstanceName(name);
    
    return getOuterServer().getClassLoaderFor(contextName);
  }

  /**
   * Returns the named ClassLoader.
   *
   * @param loaderName the name of the class loader
   *
   * @return the class loader
   *
   * @since JMX 1.2
   */
  @Override
  public ClassLoader getClassLoader(ObjectName loaderName)
    throws InstanceNotFoundException
  {
    return _outerServer.getClassLoader(loaderName);
  }

  /**
   * Returns the ClassLoaderRepository for this MBeanServer
   *
   * @since JMX 1.2
   */
  @Override
  public ClassLoaderRepository getClassLoaderRepository()
  {
    return createContext().getClassLoaderRepository();
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param name the name of the mbean
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  @Override
  public ObjectInputStream deserialize(ObjectName name, byte []data)
    throws InstanceNotFoundException, OperationsException
  {
    return _outerServer.deserialize(name,  data);
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param className the className matches to the loader
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  @Override
  public ObjectInputStream deserialize(String className, byte []data)
    throws OperationsException, ReflectionException
  {
    return _outerServer.deserialize(className, data);
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param className the className matches to the loader
   * @param loaderName the loader to use for deserialization
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  public ObjectInputStream deserialize(String className,
                                       ObjectName loaderName,
                                       byte []data)
    throws OperationsException, ReflectionException,
           InstanceNotFoundException
  {
    return _outerServer.deserialize(className, loaderName, data);
  }

  /**
   * Returns the domains for all registered MBeans
   *
   * @since JMX 1.2
   */
  @Override
  public String []getDomains()
  {
    return _outerServer.getDomains();
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void destroy()
  {
    try {
      MBeanServerFactory.releaseMBeanServer(this);
    } catch (IllegalArgumentException e) {
      log.log(Level.FINEST, e.toString(), e);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the string form.
   */
  @Override
  public String toString()
  {
    if (_defaultDomain != null)
      return getClass().getSimpleName() + "[domain=" + _defaultDomain + "]";
    else
      return getClass().getSimpleName() + "[]";
  }

  static {
    try {
      SERVER_DELEGATE_NAME = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
