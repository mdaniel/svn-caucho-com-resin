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

package javax.management.modelmbean;

import java.lang.reflect.Method;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

import com.caucho.jmx.JobManager;

/**
 * The interface implements by the ModelMBeans.
 */
public class RequiredModelMBean extends NotificationBroadcasterSupport
  implements ModelMBean {
  private static final Logger log = Logger.getLogger(RequiredModelMBean.class.getName());
  private ModelMBeanInfo _modelInfo;
  private MBeanInfo _info;
  private Object _resource;
  private ObjectName _name;

  private transient int _sequence;
  
  /**
   * Zero-arg constructor.
   */
  public RequiredModelMBean()
    throws MBeanException, RuntimeOperationsException
  {
    ModelMBeanInfo info;
    info = new ModelMBeanInfoSupport("java.lang.Object", "null",
				     new ModelMBeanAttributeInfo[0],
				     new ModelMBeanConstructorInfo[0],
				     new ModelMBeanOperationInfo[0],
				     new ModelMBeanNotificationInfo[0]);

    setModelMBeanInfo(info);
  }
  
  /**
   * Constructor based on mbean info.
   */
  public RequiredModelMBean(ModelMBeanInfo info)
    throws MBeanException, RuntimeOperationsException
  {
    setModelMBeanInfo(info);
  }
     
  /**
   * Initializes a ModelMBean using the information provided.
   * The ModelMBean must be instantiated but not yet registered.
   */
  public void setModelMBeanInfo(ModelMBeanInfo info)
    throws MBeanException, RuntimeOperationsException
  {
    if (_name != null)
      throw new IllegalStateException("setModelMBeanInfo must occur before registration.");
    
    _modelInfo = (ModelMBeanInfo) info.clone();

    _info = new MBeanInfo(info.getClassName(),
			  info.getDescription(),
			  info.getAttributes(),
			  info.getConstructors(),
			  info.getOperations(),
			  info.getNotifications());
  }
  
  /**
   * Sets the instance of the object to execute methods.
   */
  public void setManagedResource(Object resource,
                                 String resourceType)
    throws MBeanException, RuntimeOperationsException,
    InstanceNotFoundException, InvalidTargetObjectTypeException
  {
    if (! "ObjectReference".equals(resourceType))
      throw new InvalidTargetObjectTypeException(resourceType);

    _resource = resource;
  }
  /**
   * Returns an attribute value.
   */
  public Object getAttribute(String attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    Method getter = getGetter(attribute);
    if (getter == null)
      throw new AttributeNotFoundException(attribute);

    try {
      return getter.invoke(_resource, null);
    } catch (Exception e) {
      throw new ReflectionException(e);
    }
  }
  
  /**
   * Sets an attribute value.
   */
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
	   MBeanException, ReflectionException
  {
    Method setter = getSetter(attribute.getName());
    if (setter == null)
      throw new AttributeNotFoundException(attribute.getName());

    try {
      setter.invoke(_resource, new Object[] { attribute.getValue() });
    } catch (Exception e) {
      throw new ReflectionException(e);
    }
  }
  
  /**
   * Returns matching attribute values.
   */
  public AttributeList getAttributes(String []attributes)
    throws RuntimeOperationsException
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.length; i++) {
      try {
	Object value = getAttribute(attributes[i]);

	list.add(new Attribute(attributes[i], value));
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }
  
  /**
   * Sets attribute values.
   */
  public AttributeList setAttributes(AttributeList attributes)
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.size(); i++) {
      try {
	Attribute attr = (Attribute) attributes.get(i);
	
	setAttribute(attr);

	list.add(attr);
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }
  
  /**
   * Invokes a method on the bean.
   */
  public Object invoke(String actionName,
                       Object []params,
                       String []signature)
    throws MBeanException, ReflectionException
  {
    Method method = getOperation(actionName, signature);
    if (method == null)
      return null;

    try {
      return method.invoke(_resource, params);
    } catch (Exception e) {
      Throwable cause = e.getCause();

      if (cause instanceof Exception)
	throw new ReflectionException((Exception) cause);
      else
	throw new ReflectionException(e);
    }
  }

  private Method getGetter(String attribute)
    throws MBeanException
  {
    ModelMBeanAttributeInfo info = _modelInfo.getAttribute(attribute);

    if (info == null)
      return null;

    String name = (String) info.getDescriptor().getFieldValue("getMethod");

    if (name == null)
      return null;

    try {
      Method method = _resource.getClass().getMethod(name, new Class[0]);

      return method;
    } catch (Exception e) {
      return null;
    }
  }

  private Method getSetter(String attribute)
    throws MBeanException
  {
    ModelMBeanAttributeInfo info = _modelInfo.getAttribute(attribute);

    if (info == null)
      return null;

    String name = (String) info.getDescriptor().getFieldValue("setMethod");

    if (name == null)
      return null;

    Method []methods = _resource.getClass().getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! method.getName().equals(name))
	continue;

      Class []param = method.getParameterTypes();
      if (param.length != 1)
	continue;

      return method;
    }

    return null;
  }

  private Method getOperation(String name, String []signature)
    throws MBeanException
  {
    ModelMBeanOperationInfo op = _modelInfo.getOperation(name);

    if (op == null)
      return null;

    Method []methods = _resource.getClass().getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! method.getName().equals(name))
	continue;

      Class []param = method.getParameterTypes();
      if (param.length != signature.length)
	continue;

      return method;
    }

    return null;
  }

  /**
   * Returns the introspection information for the MBean.
   */
  public MBeanInfo getMBeanInfo()
  {
    return _info;
  }

  /**
   * Called after the data has been deserialized.
   */
  public void load()
    throws MBeanException, RuntimeOperationsException,
	   InstanceNotFoundException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Called before the data should be serialized.
   */
  public void store()
    throws MBeanException, RuntimeOperationsException,
	   InstanceNotFoundException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sends notification text.
   *
   * The type will be jmx.modelmbean.general
   *
   * @param text text of notification to be passed to the listener.
   */
  public void sendNotification(String text)
    throws MBeanException, RuntimeOperationsException
  {
    Notification notif = new Notification("jmx.modelmbean.general",
					  this,
					  _sequence++,
					  JobManager.getCurrentTime(),
					  text);

    sendNotification(notif);
  }
  
  /**
   * Sends an attribute change notification
   *
   * @param notification notification to be passed to the listener.
   */
  public void
    sendAttributeChangeNotification(AttributeChangeNotification notification)
    throws MBeanException, RuntimeOperationsException
  {
    sendNotification(notification);
  }
  
  /**
   * Sends an attribute change notification
   *
   * The type will be jmx.attribute.change
   *
   * @param oldValue the old attribute binding.
   * @param newValue the new attribute binding.
   */
  public void sendAttributeChangeNotification(Attribute oldValue,
                                              Attribute newValue)
    throws MBeanException, RuntimeOperationsException
  {
    AttributeChangeNotification notif;
    notif = new AttributeChangeNotification(this, _sequence++,
					    JobManager.getCurrentTime(),
					    "attribute change",
					    oldValue.getName(),
					    "java.lang.Object",
					    oldValue.getValue(),
					    newValue.getValue());

    sendAttributeChangeNotification(notif);
  }
  
  /**
   * Adds a new listener
   *
   * @param listener the new listener
   * @param attributeName the attribute to listen
   * @param handback the handback
   */
  public void
    addAttributeChangeNotificationListener(NotificationListener listener,
					   String attributeName,
					   Object handback)
    throws MBeanException, RuntimeOperationsException
  {
    AttributeChangeNotificationFilter filter;
    filter = new AttributeChangeNotificationFilter();
    filter.enableAttribute(attributeName);
      
    addNotificationListener(listener, filter, handback);
  }
  
  /**
   * Remove a listener
   *
   * @param listener the new listener
   * @param attributeName the attribute to listen
   */
  public void
    removeAttributeChangeNotificationListener(NotificationListener listener,
					      String attributeName)
    throws MBeanException, RuntimeOperationsException,
	   ListenerNotFoundException
  {
    removeNotificationListener(listener);
  }
}

  
