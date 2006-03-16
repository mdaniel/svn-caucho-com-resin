/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.jmx;

import java.lang.reflect.Method;

import java.lang.ref.WeakReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.ObjectName;
import javax.management.MBeanRegistration;
import javax.management.DynamicMBean;
import javax.management.ObjectInstance;
import javax.management.QueryExp;
import javax.management.StandardMBean;
import javax.management.MBeanServerDelegate;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.IntrospectionException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.NotCompliantMBeanException;
import javax.management.BadStringOperationException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadAttributeValueExpException;
import javax.management.InvalidApplicationException;

import javax.management.loading.ClassLoaderRepository;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.Environment;
import com.caucho.loader.WeakCloseListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;

/**
 * The context containing mbeans registered at a particular level.
 */
public class MBeanContext {
  private static final Logger log = Log.open(MBeanContext.class);
  private static final L10N L = new L10N(MBeanContext.class);

  // The owning MBeanServer
  private AbstractMBeanServer _mbeanServer;

  private MBeanServerDelegate _delegate;
  private long _seq;
  
  // class loader for this server
  private ClassLoader _loader;

  private String _domain = "resin";
  private LinkedHashMap<String,String> _properties =
    new LinkedHashMap<String,String>();

  private ClassLoaderRepositoryImpl _classLoaderRepository =
    new ClassLoaderRepositoryImpl();
  
  // map of all mbeans
  private Hashtable<ObjectName,MBeanWrapper> _mbeans
    = new Hashtable<ObjectName,MBeanWrapper>();

  private ArrayList<Listener> _listeners = new ArrayList<Listener>();

  // The local view associated with the context
  private MBeanView _view;
  // The global view associated with the context
  private MBeanView _globalView;

  MBeanContext(AbstractMBeanServer mbeanServer,
	       ClassLoader loader,
	       MBeanServerDelegate delegate)
  {
    _mbeanServer = mbeanServer;
    _loader = loader;
    _delegate = delegate;

    Environment.addClassLoaderListener(new WeakCloseListener(this), _loader);
    
    _classLoaderRepository.addClassLoader(_loader);

    _view = new MBeanView(mbeanServer, _loader, "resin");
    _globalView = new MBeanView(mbeanServer, _loader, "resin");
  }

  /**
   * Returns the parent view.
   */
  protected MBeanView getParentView()
  {
    return _mbeanServer.getParentView();
  }

  /**
   * Returns the ClassLoaderRepository.
   */
  public ClassLoaderRepository getClassLoaderRepository()
  {
    return _classLoaderRepository;
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Returns the view for this context.
   */
  MBeanView getView()
  {
    return _view;
  }

  /**
   * Returns the view for this context.
   */
  MBeanView getGlobalView()
  {
    return _globalView;
  }

  /**
   * Sets the properties.
   */
  public void setProperties(Map<String,String> props)
  {
    _properties.clear();
    _properties.putAll(props);
  }

  /**
   * Sets the properties.
   */
  public LinkedHashMap<String,String> copyProperties()
  {
    return new LinkedHashMap<String,String>(_properties);
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    if (name.indexOf(':') < 0) {
      LinkedHashMap<String,String> properties;
      properties = new LinkedHashMap<String,String>();

      properties.putAll(_properties);
      Jmx.parseProperties(properties, name);

      return Jmx.getObjectName(_domain, properties);
    }
    else
      return new ObjectName(name);
  }

  /**
   * Finds an admin object.
   */
  public MBeanWrapper getMBean(ObjectName name)
  {
    return _mbeans.get(name);
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param mbean the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  ObjectInstance registerMBean(MBeanWrapper mbean,
			       ObjectName name)
    throws InstanceAlreadyExistsException,
	   MBeanRegistrationException,
	   NotCompliantMBeanException
  {
    if (mbean == null)
      throw new NotCompliantMBeanException(L.l("{0} is a null mbean",
					       name));
    if (_mbeans.get(name) != null)
      throw new InstanceAlreadyExistsException(String.valueOf(name));

    Object object = mbean.getObject();
    
    MBeanRegistration registration = null;
   
    if (object instanceof MBeanRegistration)
      registration = (MBeanRegistration) object;
    
    try {
      if (registration != null)
        name = registration.preRegister(_mbeanServer, name);
    } catch (Exception e) {
      throw new MBeanRegistrationException(e);
    }

    addMBean(name, mbean);

    try {
      if (registration != null)
	registration.postRegister(new Boolean(true));
    } catch (Exception e) {
      throw new MBeanRegistrationException(e);
    }

    return mbean.getObjectInstance();
  }
  
  /**
   * Unregisters an MBean from the server.
   *
   * @param name the name of the mbean.
   */
  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException,
	   MBeanRegistrationException
  {
    if (_mbeans == null) {
      removeMBean(name);
      return;
    }

    if (name.getDomain().equals("JMImplementation"))
      return;
    
    MBeanWrapper mbean = _mbeans.get(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    Object obj = mbean.getObject();
    MBeanRegistration registration = null;
   
    if (obj instanceof MBeanRegistration)
      registration = (MBeanRegistration) obj;

    try {
      if (registration != null) {
	try {
	  registration.preDeregister();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString());
	}
      }

      removeMBean(name);
       
      if (registration != null) {
	try {
	  registration.postDeregister();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString());
	}
      }
    } catch (Exception e) {
      throw new MBeanRegistrationException(e);
    }
  }
  
  /**
   * Returns the MBean registered with the given name.
   *
   * @param name the name of the mbean.
   *
   * @return the matching mbean object.
   */
  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.getObjectInstance();
  }

  /**
   * Returns true if the given object is registered with the server.
   *
   * @param name the name of the mbean to test.
   *
   * @return true if the object is registered.
   */
  public boolean isRegistered(ObjectName name)
  {
    return _mbeans.get(name) != null;
  }
  
  /**
   * Returns the number of MBeans registered.
   *
   * @return the number of registered mbeans.
   */
  public int getMBeanCount()
  {
    return _mbeans.size();
  }

  /**
   * Adds an object.
   */
  private void addMBean(ObjectName name, MBeanWrapper mbean)
  {
    if (mbean == null) {
      log.warning(L.l("'{0}' is an empty mbean", name));
      return;
    }
    
    log.fine(getDebugName(name, mbean) + " registered in " + this);
    //log.fine(L.l("{0} registered in {1}", getDebugName(name, mbean), this));

    _mbeans.put(name, mbean);

    _view.add(name, mbean, true);
    _globalView.add(name, mbean, true);

    sendRegisterNotification(name);

    MBeanContext context = this;
    while (context._loader != null) {
      ClassLoader parentLoader = context._loader.getParent();
      
      MBeanContext parentContext = _mbeanServer.getContext(parentLoader);

      if (parentContext == null ||
	  parentContext == context)
	break;

      if (parentContext._globalView == null) {
	log.finer("global view is empty");
      }
      else if (parentContext._globalView.add(name, mbean, false))
	parentContext.sendRegisterNotification(name);
      
      context = parentContext;
    }
  }

  /**
   * Removes an object.
   */
  private MBeanWrapper removeMBean(ObjectName name)
  {
    MBeanWrapper mbean = null;

    if (_mbeans != null)
      mbean = _mbeans.remove(name);

    if (_view != null)
      _view.remove(name);
    
    if (_globalView != null && _globalView.remove(name) != null) {
      try {
	sendUnregisterNotification(name);
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    MBeanContext context = this;
    while (true) {
      ClassLoader parentLoader = context._loader.getParent();
      
      MBeanContext parentContext = _mbeanServer.getExistingContext(parentLoader);

      if (parentContext == null || parentContext == context) {
	break;
      }

      try {
	if (parentContext._globalView == null) {
	}
	else if (mbean == null &&
		 (mbean = parentContext._globalView.remove(name)) != null)
	  parentContext.sendUnregisterNotification(name);
	else if (mbean != null && 
		 parentContext._globalView.remove(name, mbean) != null)
	  parentContext.sendUnregisterNotification(name);
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      context = parentContext;
    }
    
    return mbean;
  }
  
  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  void addNotificationListener(ObjectName name,
			       NotificationListener listener,
			       NotificationFilter filter,
			       Object handback)
  {
    synchronized (_listeners) {
      _listeners.add(new Listener(name, listener, filter, handback));
    }
  }
  
  /**
   * Removes a listener to a registered MBean
   *
   * @param mbean the name of the mbean
   * @param listener the listener object
   */
  public void removeNotificationListener(ObjectName mbean,
					 NotificationListener listener)
  {
    synchronized (_listeners) {
      for (int i = _listeners.size() - 1; i >= 0; i--) {
	Listener oldListener = _listeners.get(i);

	if (oldListener.match(mbean, listener))
	  _listeners.remove(i);
      }
    }
  }
  
  /**
   * Removes a listener to a registered MBean
   *
   * @param mbean the name of the mbean
   * @param listener the listener object
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  public void removeNotificationListener(ObjectName mbean,
					 NotificationListener listener,
					 NotificationFilter filter,
					 Object handback)
  {
    synchronized (_listeners) {
      for (int i = _listeners.size() - 1; i >= 0; i--) {
	Listener oldListener = _listeners.get(i);

	if (oldListener.match(mbean, listener, filter, handback))
	  _listeners.remove(i);
      }
    }
  }

  /**
   * Sends the register notification.
   */
  void sendRegisterNotification(ObjectName name)
  {
    serverNotification(name, MBeanServerNotification.REGISTRATION_NOTIFICATION);
  }

  /**
   * Sends the register notification.
   */
  void sendUnregisterNotification(ObjectName name)
  {
    serverNotification(name, MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
  }

  /**
   * Sends the notification
   */
  private void serverNotification(ObjectName name, String type)
  {
    MBeanServerNotification notif;

    ObjectName delegateName = _mbeanServer.SERVER_DELEGATE_NAME;
    
    notif = new MBeanServerNotification(type, delegateName, _seq++, name);

    _delegate.sendNotification(notif);
  }

  /**
   * Closes the context server.
   */
  public void destroy()
  {
    if (_mbeans == null)
      return;
    
    ArrayList<ObjectName> list = new ArrayList<ObjectName>(_mbeans.keySet());

    ArrayList<Listener> listeners = new ArrayList<Listener>(_listeners);

    for (int i = 0; i < listeners.size(); i++) {
      Listener listener = listeners.get(i);

      try {
	MBeanWrapper mbean = _globalView.getMBean(listener.getName());

	if (mbean != null)
	  mbean.removeNotificationListener(listener.getListener(),
					   listener.getFilter(),
					   listener.getHandback());
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    for (int i = 0; i < list.size(); i++) {
      ObjectName name = list.get(i);

      try {
        unregisterMBean(name);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    _mbeanServer.removeContext(this, _loader);

    _listeners = null;
    _mbeans = null;
    _view = null;
    _globalView = null;
  }

  /**
   * Returns the debug name for a registered mbean.
   *
   * @param name the name of the mbean
   * @param mbean the mbean instance
   * @return
   */
  private String getDebugName(ObjectName name, MBeanWrapper mbean)
  {
    String className = mbean.getMBeanInfo().getClassName();

    int p = className.lastIndexOf('.');
    if (p > 0)
      className = className.substring(p + 1);

    return className + "[" + name + "]";
  }

  /**
   * Display name.
   */
  public String toString()
  {
    return "MBeanContext[" + _loader + "]";
  }

  /**
   * Finalizer.
   */
  /*
  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    destroy();
  }
  */

  /**
   * Listener references.
   */
  static class Listener {
    private ObjectName _name;
    private WeakReference<NotificationListener> _listenerRef;
    private WeakReference<NotificationFilter> _filterRef;
    private WeakReference<Object> _handbackRef;

    Listener(ObjectName name,
	     NotificationListener listener,
	     NotificationFilter filter,
	     Object handback)
    {
      _name = name;
      _listenerRef = new WeakReference<NotificationListener>(listener);

      if (filter != null)
	_filterRef = new WeakReference<NotificationFilter>(filter);

      if (handback != null)
	_handbackRef = new WeakReference<Object>(handback);
    }

    ObjectName getName()
    {
      return _name;
    }

    NotificationListener getListener()
    {
      return _listenerRef.get();
    }

    NotificationFilter getFilter()
    {
      return _filterRef != null ? _filterRef.get() : null;
    }

    Object getHandback()
    {
      return _handbackRef != null ? _handbackRef.get() : null;
    }

    boolean match(ObjectName name,
		  NotificationListener listener,
		  NotificationFilter filter,
		  Object handback)
    {
      if (! _name.equals(name))
	return false;

      else if (listener != _listenerRef.get())
	return false;

      else if (filter == null && _filterRef != null)
	return false;
      
      else if (_filterRef != null && _filterRef.get() != filter)
	return false;

      else if (handback == null && _handbackRef != null)
	return false;
      
      else if (_handbackRef != null && _handbackRef.get() != handback)
	return false;
      
      else
	return true;
    }

    boolean match(ObjectName name,
		  NotificationListener listener)
    {
      if (! _name.equals(name))
	return false;

      else if (listener != _listenerRef.get())
	return false;
      
      else
	return true;
    }
  }
}

