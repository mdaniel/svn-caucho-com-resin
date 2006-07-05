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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.mbeans.j2ee;

import com.caucho.ejb.AbstractSessionBean;
import com.caucho.jmx.Jmx;
import com.caucho.jmx.AbstractMBeanServer;

import javax.ejb.CreateException;
import javax.management.*;
import javax.management.j2ee.statistics.ListenerRegistration;
import java.rmi.RemoteException;
import java.util.Set;

public class ManagementImpl
  extends AbstractSessionBean
{
  private final ListenerRegistration _listenerRegistrationImpl
    = new ListenerRegistrationImpl();

  public void ejbCreate()
     throws CreateException
   {
   }

  private AbstractMBeanServer getMBeanServer()
  {
    return Jmx.getMBeanServer();
  }

  public Object getAttribute(ObjectName name, String attribute)
    throws
    MBeanException,
    AttributeNotFoundException,
    InstanceNotFoundException,
    ReflectionException,
    RemoteException
  {
    return getMBeanServer().getAttribute(name, attribute);
  }

  public AttributeList getAttributes(ObjectName name, String []attributes)
    throws InstanceNotFoundException, ReflectionException, RemoteException
  {
    return getMBeanServer().getAttributes(name, attributes);
  }

  public String getDefaultDomain()
    throws RemoteException
  {
    return getMBeanServer().getDefaultDomain();
  }

  public Integer getMBeanCount()
    throws RemoteException
  {
    return getMBeanServer().getMBeanCount();
  }

  public MBeanInfo getMBeanInfo(ObjectName objectName)
    throws
    IntrospectionException,
    InstanceNotFoundException,
    ReflectionException,
    RemoteException
  {
    return getMBeanServer().getMBeanInfo(objectName);
  }

  public Object invoke(ObjectName objectName,
                       String operationName,
                       Object []parameters,
                       String []signature)
    throws
    MBeanException,
    InstanceNotFoundException,
    ReflectionException,
    RemoteException
  {
    return getMBeanServer().invoke(objectName, operationName, parameters, signature);
  }

  public boolean isRegistered(ObjectName objectName)
    throws RemoteException
  {
    return getMBeanServer().isRegistered(objectName);
  }

  public Set queryNames(ObjectName objectName, QueryExp queryExp)
    throws RemoteException
  {
    return getMBeanServer().queryNames(objectName, queryExp);
  }

  public void setAttribute(ObjectName objectName, Attribute attribute)
    throws InstanceNotFoundException,
           AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException,
           ReflectionException,
           RemoteException
  {
    getMBeanServer().setAttribute(objectName, attribute);
  }

  public AttributeList setAttributes(ObjectName objectName,
                                     AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException, RemoteException
  {
    return getMBeanServer().setAttributes(objectName, attributes);
  }

  public ListenerRegistration getListenerRegistry()
    throws RemoteException
  {
    return _listenerRegistrationImpl;
  }

  public class ListenerRegistrationImpl
    implements ListenerRegistration
  {
    public void addNotificationListener(ObjectName objectName,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
      throws InstanceNotFoundException, RemoteException
    {
    }

    public void removeNotificationListener(ObjectName objectName,
                                           NotificationListener listener)
      throws InstanceNotFoundException, ListenerNotFoundException, RemoteException
    {
    }
  }
}
