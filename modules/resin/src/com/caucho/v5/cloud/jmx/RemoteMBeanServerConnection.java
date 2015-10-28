/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import io.baratine.core.ServiceExceptionConnect;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import com.caucho.v5.util.L10N;
import com.caucho.v5.util.TimedCache;

/**
 * Remote administration API
 */
public class RemoteMBeanServerConnection
  implements MBeanServerConnection
{
  private static final L10N L = new L10N(RemoteMBeanServerConnection.class);
  private static final Logger log
    = Logger.getLogger(RemoteMBeanServerConnection.class.getName());

  private final JmxClient _client;

  private TimedCache<String,Item> _itemCache
    = new TimedCache<String,Item>(1024, 5000);

  private TimedCache<String,MBeanInfo> _infoCache
    = new TimedCache<String,MBeanInfo>(256, 60000);

  /**
   * Creates the MBeanClient.
   */
  public RemoteMBeanServerConnection(String serverId)
  {
    _client = new JmxClient(serverId);
  }

  /**
   * Creates the MBeanClient.
   */
  public RemoteMBeanServerConnection(String host,
                                     int port,
                                     String user,
                                     String password)
  {
    _client = new JmxClient(host, port, user, password);
  }

  /**
   * Returns the mbean info
   */
  @Override
  public MBeanInfo getMBeanInfo(ObjectName objectName)
    throws InstanceNotFoundException, IntrospectionException,
           ReflectionException, IOException
  {
    String name = objectName.toString();
    
    Item item = getItem(name);
    
    if (item == null)
      return null;
    
    String className = (String) item.getValue("__caucho_info_class");

    if (className == null)
      return _client.getMBeanInfo(objectName.toString());
    
    MBeanInfo info = _infoCache.get(className);
    
    if (info == null) {
      info = _client.getMBeanInfo(objectName.toString());
      _infoCache.put(className, info);
    }
    
    return info;
  }

  @Override
  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets an attribute.
   */
  @Override
  public Object getAttribute(ObjectName objectName, String attrName)
    throws MBeanException, AttributeNotFoundException,
           InstanceNotFoundException, ReflectionException,
           IOException
  {
    Item item = getItem(objectName.toString());

    if (item == null)
      throw new InstanceNotFoundException(L.l("'{0}' is unknown or unreachable for server '{1}'",
                                              String.valueOf(objectName), _client));


    Object value = item.getValue(attrName);

    return value;
  }
  
  private Item getItem(String name)
    throws IOException
  {
    Item item = _itemCache.get(name);
    
    if (item == null) {
      HashMap<?,?> values = _client.lookup(name);
      
      if (values != null) {
        item = new Item(values);
        
        _itemCache.put(name, item);
      }
    }
    
    return item;
  }

  public AttributeList getAttributes(ObjectName name, String[] attributes)
    throws InstanceNotFoundException, ReflectionException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException,
           AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException,
           ReflectionException,
           IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public AttributeList setAttributes(ObjectName name, AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object invoke(ObjectName objectName,
                       String operationName,
                       Object params[],
                       String sig[])
    throws InstanceNotFoundException,
           MBeanException,
           ReflectionException,
           IOException
  {
    return _client.invoke(objectName.toString(), operationName, params, sig);
  }

  public String getDefaultDomain()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public String[] getDomains()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void addNotificationListener(ObjectName name,
                                      ObjectName listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void removeNotificationListener(ObjectName name, ObjectName listener)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           IOException
  {
    throw new UnsupportedOperationException();
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    ObjectName loaderName)
    throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           InstanceNotFoundException,
           IOException
  {
    throw new UnsupportedOperationException();
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    Object params[],
                                    String signature[])
    throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           IOException
  {

    // XXX: unimplemented
    throw new UnsupportedOperationException("unimplemented");
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    ObjectName loaderName,
                                    Object params[],
                                    String signature[])
    throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           InstanceNotFoundException,
           IOException
  {
    throw new UnsupportedOperationException();
  }

  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException, MBeanRegistrationException, IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the object instance
   */
  public ObjectInstance getObjectInstance(ObjectName objectName)
    throws InstanceNotFoundException, IOException
  {
    throw new UnsupportedOperationException();
  }

  public Set queryMBeans(ObjectName name, QueryExp query)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
    throws IOException
  {
    String []names = _client.query(name.toString());

    if (names == null)
      return null;

    try {
      HashSet<ObjectName> set = new HashSet<ObjectName>();
      for (int i = 0; i < names.length; i++)
        set.add(new ObjectName(names[i]));

      return set;
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return true if registered.
   */
  public boolean isRegistered(ObjectName objectName)
    throws IOException
  {
    try {
      return getItem(objectName.toString()) != null;
    } catch (ServiceExceptionConnect e) {
      log.log(Level.FINEST, e.toString(), e);

      throw e;
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      return false;
    }
  }

  public Integer getMBeanCount()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client + "]";
  }

  static class Item {
    private HashMap<?,?> _values;
    
    Item(HashMap<?,?> values)
    {
      _values = values;
    }
    
    public Object getValue(String name)
    {
      return _values.get(name);
    }
  }
}

