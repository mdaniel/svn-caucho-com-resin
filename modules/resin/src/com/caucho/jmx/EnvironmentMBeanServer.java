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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.ObjectName;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.StandardMBean;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.ListenerNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.RuntimeOperationsException;

import javax.management.loading.ClassLoaderRepository;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentLocal;

/**
 * JNDI object for the Resin mbean server.
 */
public class EnvironmentMBeanServer extends AbstractMBeanServer {
  private static final L10N L = new L10N(EnvironmentMBeanServer.class);
  private static final Logger log = Log.open(EnvironmentMBeanServer.class);

  private EnvironmentLocal<MBeanContext> _localContext =
    new EnvironmentLocal<MBeanContext>();
  
  private MBeanServerDelegate _globalDelegate;
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public EnvironmentMBeanServer(String domain, MBeanServerDelegate delegate)
  {
    super(domain);
    
    if (Jmx.getMBeanServer() == null)
      Jmx.setMBeanServer(this);

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

    MBeanContext context = new MBeanContext(this, systemLoader, delegate);

    _localContext.set(context, systemLoader);
	
    try {
      IntrospectionMBean mbean;
      mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

      MBeanWrapper mbeanWrapper;
      mbeanWrapper = new MBeanWrapper(context, SERVER_DELEGATE_NAME,
				      delegate, mbean);

      context.registerMBean(mbeanWrapper, SERVER_DELEGATE_NAME);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    synchronized (_localContext) {
      MBeanContext context = _localContext.getLevel(loader);

      if (context == null) {
	MBeanServerDelegate delegate;
	delegate = new MBeanServerDelegateImpl("Resin-JMX");

	context = new MBeanContext(this, loader, delegate);

	MBeanContext parent = null;

	if (loader != null)
	  parent = getContext(loader.getParent());

	if (parent != null)
	  context.setProperties(parent.copyProperties());

	_localContext.set(context, loader);
	
	try {
	  IntrospectionMBean mbean;
	  mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

	  MBeanWrapper mbeanWrapper;
	  mbeanWrapper = new MBeanWrapper(context, SERVER_DELEGATE_NAME,
					  delegate, mbean);

	  context.registerMBean(mbeanWrapper, SERVER_DELEGATE_NAME);
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      return context;
    }
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext getExistingContext(ClassLoader loader)
  {
    if (loader == null)
      return _localContext.get(ClassLoader.getSystemClassLoader());
    
    synchronized (_localContext) {
      return _localContext.getLevel(loader);
    }
  }

  /**
   * Returns the local context.
   */
  protected void removeContext(MBeanContext context, ClassLoader loader)
  {
    if (_localContext.get(loader) == context)
      _localContext.remove(loader);
  }

  /**
   * Returns the string form.
   */
  public String toString()
  {
    return "EnvironmentMBeanServer[]";
  }
}
