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

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.loading.ClassLoaderRepository;

import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvLoader;

/**
 * The context containing mbeans registered at a particular level.
 */
public class MBeanContext implements AutoCloseable
{
  private static final Logger log
    = Logger.getLogger(MBeanContext.class.getName());

  /*
  private final EnvironmentLocal<MBeanClose> _mbeanClose
    = new EnvironmentLocal<MBeanClose>();
  */
  
  private MBeanContext _parent;
  
  // The owning MBeanServer
  private AbstractMBeanServer _mbeanServer;

  private MBeanServerDelegate _delegate;
  private long _seq;
  
  // class loader for this server
  private ClassLoader _loader;

  private String _domain = JmxUtilResin.DOMAIN;
  
  private LinkedHashMap<String,String> _properties
    = new LinkedHashMap<String,String>();

  private ClassLoaderRepositoryImpl _classLoaderRepository
    = new ClassLoaderRepositoryImpl();
  
  private ConcurrentSkipListSet<ObjectName> _mbeanSet
    = new ConcurrentSkipListSet<ObjectName>();

  private ArrayList<Listener> _listeners = new ArrayList<Listener>();
  
  private final Lifecycle _lifecycle = new Lifecycle(); 

  MBeanContext(EnvironmentMBeanServer mbeanServer,
               ClassLoader loader,
               MBeanServerDelegate delegate,
               MBeanContext globalContext)
  {
    _mbeanServer = mbeanServer;
    
    loader = EnvLoader.getEnvironmentClassLoader(loader);

    if (loader == null) {
      loader = ClassLoader.getSystemClassLoader();
    }
    
    _loader = loader;
    _delegate = delegate;

    _mbeanServer.setCurrentContext(this, loader);

    EnvLoader.addCloseListener(this, loader);

    //Environment.addClassLoaderListener(new WeakCloseListener(this), _loader);
    
    _classLoaderRepository.addClassLoader(_loader);

    if (_loader != null
        && _loader != ClassLoader.getSystemClassLoader()) {
      _parent = _mbeanServer.createContext(_loader.getParent());

      if (_parent == this) {
        _parent = null;
      }
    }
    
    _lifecycle.toActive();
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
  
  LinkedHashMap<String,String> getProperties()
  {
    return _properties;
  }
  
  ObjectName getContextObjectName(ObjectName objectName)
  {
    if (_properties.size() == 0) {
      return null;
    }
    else if (_loader == ClassLoader.getSystemClassLoader()) {
      return null;
    }
    
    try {
      Hashtable<String,String> props = new Hashtable<String,String>();
      
      props.putAll(_properties);
      
      props.putAll(objectName.getKeyPropertyList());
      
      return ObjectName.getInstance(objectName.getDomain(), props);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Returns the object name.
   */
  public ObjectName getContextObjectName(String name)
      throws MalformedObjectNameException
  {
    int len = name.length();
    String domain = _domain;

    for (int i = 0; i < len; i++) {
      char ch = name.charAt(i);

      if (ch == ':') {
        domain = name.substring(0, i);
        name = name.substring(i + 1);
        break;
      }
      else if ('a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || '0' <= ch && ch <= '9'
          || ch == '-' || ch == '_' || ch == '.') {
        continue;
      }
      else
        break;
    }

    LinkedHashMap<String,String> properties;
    properties = new LinkedHashMap<String,String>();

    properties.putAll(_properties);
    JmxUtilResin.parseProperties(properties, name);

    return JmxUtilResin.getObjectName(domain, properties);
  }
  
  public ObjectName getServerObjectName(String name)
      throws MalformedObjectNameException
  {
    int len = name.length();
    String domain = _domain;
    String tail = name;

    for (int i = 0; i < len; i++) {
      char ch = name.charAt(i);

      if (ch == ':') {
        domain = name.substring(0, i);
        tail= name.substring(i + 1);
        break;
      }
      else if ('a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || '0' <= ch && ch <= '9'
          || ch == '-' || ch == '_' || ch == '.') {
        continue;
      }
      else
        break;
    }
    
    if (! JmxUtilResin.DOMAIN.equals(domain) && ! "resin".equals(domain)) {
      return new ObjectName(name);
    }

    LinkedHashMap<String,String> properties;
    properties = new LinkedHashMap<String,String>();

    if (_properties.get("Server") != null) {
      properties.put("Server", _properties.get("Server"));
    }
    
    JmxUtilResin.parseProperties(properties, tail);

    return JmxUtilResin.getObjectName(domain, properties);
  }
  
  public ObjectName getObjectName(String name)
      throws MalformedObjectNameException
  {
    int len = name.length();

    for (int i = 0; i < len; i++) {
      char ch = name.charAt(i);

      if (ch == ':') {
        return new ObjectName(name);
      }
      else if ('a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || '0' <= ch && ch <= '9'
          || ch == '-' || ch == '_' || ch == '.') {
        continue;
      }
      else
        break;
    }

    LinkedHashMap<String,String> properties;
    properties = new LinkedHashMap<String,String>();

    properties.putAll(_properties);
    JmxUtilResin.parseProperties(properties, name);

    return JmxUtilResin.getObjectName(_domain, properties);
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param mbean the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  void addMBean(ObjectName name)
  {
    _mbeanSet.add(name);
  }
  
  /**
   * Unregisters an MBean from the server.
   *
   * @param name the name of the mbean.
   */
  public void removeMBean(ObjectName name)
  {
    _mbeanSet.remove(name);
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
    serverNotification(name,
                       MBeanServerNotification.REGISTRATION_NOTIFICATION);
  }

  /**
   * Sends the register notification.
   */
  void sendUnregisterNotification(ObjectName name)
  {
    serverNotification(name,
                       MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
  }

  /**
   * Sends the notification
   */
  private void serverNotification(ObjectName name, String type)
  {
    MBeanServerNotification notif;

    ObjectName delegateName = AbstractMBeanServer.SERVER_DELEGATE_NAME;
    
    notif = new MBeanServerNotification(type, delegateName, _seq++, name);

    _delegate.sendNotification(notif);
  }

  /**
   * Closes the context server.
   */
  public void close()
  {
    try {
      log.finest(this + " destroy");

      ArrayList<ObjectName> list = new ArrayList<ObjectName>(_mbeanSet);

      ArrayList<Listener> listeners = new ArrayList<Listener>(_listeners);

      for (int i = 0; i < listeners.size(); i++) {
        Listener listener = listeners.get(i);

        try {
          _mbeanServer.removeNotificationListener(listener.getName(),
                                                  listener.getListener(),
                                                  listener.getFilter(),
                                                  listener.getHandback());
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      for (int i = 0; i < list.size(); i++) {
        ObjectName name = list.get(i);

        try {
          _mbeanServer.unregisterMBean(name);
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }

      _mbeanServer.removeContext(this, _loader);
    } finally {
      _parent = null;
      _mbeanServer = null;
      _delegate = null;
      _loader = null;
      _classLoaderRepository = null;
      _listeners = null;
    }
  }

  /**
   * Display name.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _loader + "]";
  }

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

  class MBeanClose implements Closeable {
    private final ArrayList<ObjectName> _names = new ArrayList<ObjectName>();

    public void addName(ObjectName name)
    {
      _names.add(name);
    }

    public void removeName(ObjectName name)
    {
      _names.add(name);
    }

    public void close()
    {
      ArrayList<ObjectName> names = new ArrayList<ObjectName>(_names);
      _names.clear();

      for (ObjectName name : names) {
        try {
          _mbeanServer.unregisterMBean(name);
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
    }

    public String toString()
    {
      return getClass().getSimpleName();
    }
  }

  /**
   * @param name
   * @param listenerName
   * @param filter
   * @param handback
   */
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName,
                                         NotificationFilter filter,
                                         Object handback)
  {
    // TODO Auto-generated method stub
    
  }
}

