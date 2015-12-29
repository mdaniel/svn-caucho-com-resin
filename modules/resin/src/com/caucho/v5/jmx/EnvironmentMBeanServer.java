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

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;

import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.L10N;

/**
 * JNDI object for the mbean server.
 */
public class EnvironmentMBeanServer extends AbstractMBeanServer implements java.io.Serializable {
  private static final L10N L = new L10N(EnvironmentMBeanServer.class);
  
  private EnvironmentLocal<MBeanContext> _localContext
    = new EnvironmentLocal<MBeanContext>();
  
  private MBeanContext _globalContext;
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  EnvironmentMBeanServer(String domain,
                         MBeanServer outerServer,
                         MBeanServerDelegate delegate)
  {
    super(domain, outerServer);

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
    
    MBeanContext context = new MBeanContext(this, systemLoader, delegate,
                                            null);

    _globalContext = context;

    _localContext.setGlobal(context);

    /*
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
    */
  }
  
  public static EnvironmentMBeanServer getGlobal()
  {
    return EnvironmentMBeanServerBuilder.getGlobal();
  }

  /**
   * Returns the local context.
   */
  @Override
  protected MBeanContext createContext(ClassLoader loader)
  {
    synchronized (_localContext) {
      MBeanContext context = _localContext.getLevel(loader);

      if (context == null) {
        if (loader instanceof DynamicClassLoader
            && ((DynamicClassLoader) loader).isDestroyed()) {
          throw new IllegalStateException(L.l("JMX context {0} has been closed.",
                                              loader));
        }

        MBeanServerDelegate delegate;
        delegate = new MBeanServerDelegateImpl("Baratine-JMX");

        context = new MBeanContext(this, 
                                   loader, 
                                   delegate, 
                                   _globalContext);

        MBeanContext parent = null;

        if (loader != null) {
          parent = createContext(loader.getParent());
        }

        if (parent != null) {
          context.setProperties(parent.copyProperties());
        }

        _localContext.set(context, loader);

        /*
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
        */
      }

      return context;
    }
  }

  /**
   * Returns the local context.
   */
  @Override
  protected MBeanContext getCurrentContext(ClassLoader loader)
  {
    if (loader == null)
      loader = EnvLoader.getEnvironmentClassLoader(loader);
    
    return _localContext.getLevel(loader);
  }

  /**
   * Sets the local context.
   */
  @Override
  protected void setCurrentContext(MBeanContext context, ClassLoader loader)
  {
    if (loader == null)
      loader = EnvLoader.getEnvironmentClassLoader(loader);
    
    synchronized (_localContext) {
      if (_localContext.getLevel(loader) != null
          && _localContext.getLevel(loader) != context)
        throw new IllegalStateException(L.l("replacing context is forbidden"));
      
      _localContext.set(context, loader);
    }
  }
  
 /**
   * Returns the local context.
   */
  @Override
  protected MBeanContext getContext(ClassLoader loader)
  {
    return _localContext.get(loader);
  }

  /**
   * Returns the local context.
   */
  @Override
  protected void removeContext(MBeanContext context, ClassLoader loader)
  {
    if (_localContext.get(loader) == context)
      _localContext.remove(loader);
  }

  /**
   * Serialization.
   */
  /* XXX:
  private Object writeReplace()
  {
    return new SingletonBindingHandle(MBeanServer.class);
  }
  */

  /**
   * Returns the string form.
   */
  @Override
  public String toString()
  {
    return "EnvironmentMBeanServer[]";
  }
}
