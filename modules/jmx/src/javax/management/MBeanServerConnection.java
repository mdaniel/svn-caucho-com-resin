/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

package javax.management;

import java.util.Set;

import java.io.ObjectInputStream;
import java.io.IOException;

import javax.management.loading.ClassLoaderRepository;

/**
 * The main interface for retrieving and managing JMX objects.
 */
public interface MBeanServerConnection {
  
  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanException, NotCompliantMBeanException, IOException;
  
  
  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param loaderName the name of the class loader to user
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException,
	   MBeanException, NotCompliantMBeanException,
	   InstanceNotFoundException, IOException;
  
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
  public ObjectInstance createMBean(String className, ObjectName name,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
	   MBeanException, NotCompliantMBeanException, IOException;
  
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
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
	   MBeanException, NotCompliantMBeanException, IOException,
	   InstanceNotFoundException;
  
  /**
   * Unregisters an MBean from the server.
   *
   * @param name the name of the mbean.
   */
  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException,
	   MBeanRegistrationException, IOException;
  
  /**
   * Returns the MBean registered with the given name.
   *
   * @param name the name of the mbean.
   *
   * @return the matching mbean object.
   */
  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException, IOException;
  
  /**
   * Returns a set of MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the queryd to match.
   *
   * @return the set of matching mbean object.
   */
  public Set queryMBeans(ObjectName name, QueryExp query)
    throws IOException;
  
  /**
   * Returns a set of names for MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the queryd to match.
   *
   * @return the set of matching mbean names.
   */
  public Set queryNames(ObjectName name, QueryExp query)
    throws IOException;
  
  /**
   * Returns true if the given object is registered with the server.
   *
   * @param name the name of the mbean to test.
   *
   * @return true if the object is registered.
   */
  public boolean isRegistered(ObjectName name)
    throws IOException;
  
  /**
   * Returns the number of MBeans registered.
   *
   * @return the number of registered mbeans.
   */
  public Integer getMBeanCount()
    throws IOException;
  
  /**
   * Returns a specific attribute of a named MBean.
   *
   * @param name the name of the mbean to test
   * @param attribute the name of the attribute to retrieve
   *
   * @return the attribute value
   */
  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException,
	   InstanceNotFoundException, ReflectionException,
	   IOException;
  
  /**
   * Returns a list of several MBean attributes.
   *
   * @param name the name of the mbean
   * @param attributes the name of the attributes to retrieve
   *
   * @return the attribute value
   */
  public AttributeList getAttributes(ObjectName name, String []attribute)
    throws InstanceNotFoundException, ReflectionException,
	   IOException;
  
  /**
   * Sets an attribute in the MBean.
   *
   * @param name the name of the mbean
   * @param attribute the name/value of the attribute to set.
   */
  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException,
	   InvalidAttributeValueException, MBeanException,
	   ReflectionException, IOException;
  
  /**
   * Set an attributes in the MBean.
   *
   * @param name the name of the mbean
   * @param attributes the name/value list of the attribute to set.
   */
  public AttributeList setAttributes(ObjectName name, AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException;
  
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
    throws InstanceNotFoundException, MBeanException,
	   ReflectionException, IOException;
  
  /**
   * Returns the default domain for naming the MBean
   */
  public String getDefaultDomain()
    throws IOException;
  
  /**
   * Returns the domains for all registered MBeans
   *
   * @since JMX 1.2
   */
  public String []getDomains()
    throws IOException;
  
  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException;
  
  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  public void addNotificationListener(ObjectName name,
                                      ObjectName listenerName,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException;
  
  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   */
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException,
	   IOException;
  
  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   */
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName)
    throws InstanceNotFoundException, ListenerNotFoundException;
  
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
                                         ObjectName listenerName,
					 NotificationFilter filter,
					 Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException,
	   IOException;
  
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
    throws InstanceNotFoundException, ListenerNotFoundException,
	   IOException;
  
  /**
   * Returns the analyzed information for an MBean
   *
   * @param name the name of the mbean
   *
   * @return the introspected information
   */
  public MBeanInfo getMBeanInfo(ObjectName name)
    throws InstanceNotFoundException, IntrospectionException,
	   ReflectionException, IOException;
  
  /**
   * Returns true if the MBean is an instance of the specified class.
   *
   * @param name the name of the mbean
   * @param className the className to test.
   *
   * @return true if the bean is an instance
   */
  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException, IOException;
}
