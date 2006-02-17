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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.ObjectInputStream;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.MBeanServerFactory;
import javax.management.MBeanRegistration;
import javax.management.MBeanInfo;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.StandardMBean;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationEmitter;
import javax.management.QueryExp;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.IntrospectionException;
import javax.management.MBeanRegistrationException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.NotCompliantMBeanException;
import javax.management.OperationsException;
import javax.management.ListenerNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.AttributeNotFoundException;

import javax.management.loading.ClassLoaderRepository;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.DynamicClassLoader;

/**
 * The main interface for retrieving and managing JMX objects.
 */
class MBeanServerImpl extends AbstractMBeanServer {
  private static final L10N L = new L10N(MBeanServerImpl.class);
  
  private static final Logger log = Log.open(MBeanServerImpl.class);

  private MBeanContext _context;

  /**
   * Creats a new MBeanServer implementation.
   */
  public MBeanServerImpl(String domain, MBeanServerDelegate delegate)
  {
    super(domain);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    _context = new MBeanContext(this, loader, delegate);

    try {
      IntrospectionMBean mbean;
      mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

      registerMBean(mbean, SERVER_DELEGATE_NAME);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    return _context;
  }

  /**
   * Returns the context.
   */
  protected MBeanContext getExistingContext(ClassLoader loader)
  {
    return _context;
  }
}
