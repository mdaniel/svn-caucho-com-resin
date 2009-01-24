/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.webbeans;

import com.caucho.config.ConfigException;
import com.caucho.config.annotation.ServiceBinding;
import com.caucho.config.annotation.OsgiServiceBinding;
import com.caucho.ejb.timer.EjbTimerService;
import com.caucho.jca.UserTransactionProxy;
import com.caucho.jmx.Jmx;
import com.caucho.remote.BamService;
import com.caucho.webbeans.*;
import com.caucho.webbeans.component.CauchoBean;
import com.caucho.webbeans.manager.WebBeansContainer;
import com.caucho.webbeans.manager.BeanStartupEvent;
import com.caucho.server.util.ScheduledThreadPool;
import com.caucho.transaction.*;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.ejb.*;
import javax.context.Conversation;
import javax.event.Observes;
import javax.management.*;
import javax.transaction.*;
import javax.inject.*;
import javax.inject.manager.Bean;
import javax.inject.manager.Manager;

import org.osgi.framework.BundleContext;

/**
 * Resin WebBeans producer for the main singletons.
 */

@CauchoDeployment
@Singleton
public class ResinWebBeansProducer
{
  private static final Logger log
    = Logger.getLogger(ResinWebBeansProducer.class.getName());
  private static final L10N L = new L10N(ResinWebBeansProducer.class);
  
  /**
   * Returns the web beans container.
   */
  @Produces
  @Standard
  public Manager getManager()
  {
    return WebBeansContainer.create();
  }
  
  /**
   * Returns the web beans conversation controller
   */
  @Produces
  @Standard
  public Conversation getConversation()
  {
    return WebBeansContainer.create().createConversation();
  }
  
  /**
   * Returns the MBeanServer
   */
  @Produces
  @CauchoDeployment
  public MBeanServer getMBeanServer()
  {
    return Jmx.getGlobalMBeanServer();
  }
  
  /**
   * Returns the TransactionManager
   */
  @Produces
  @CauchoDeployment
  public TransactionManager getTransactionManager()
  {
    return TransactionManagerImpl.getInstance();
  }
  
  /**
   * Returns the UserTransaction
   */
  @Produces
  @CauchoDeployment
  public UserTransaction getUserTransaction()
  {
    return UserTransactionProxy.getInstance();
  }
  
  /**
   * Returns the ScheduledExecutorService
   */
  @Produces
  @CauchoDeployment
  public ScheduledExecutorService getScheduledExecutorService()
  {
    return ScheduledThreadPool.getLocal();
  }
  
  /**
   * Returns the javax.ejb.TimerService
   */
  @Produces
  @CauchoDeployment
  public TimerService getTimerService()
  {
    return EjbTimerService.getCurrent();
  }

  //
  // event listeners
  //

  /**
   * Starts a bean based on a ServiceStartup event
   */
  public void serviceStartup(@Observes @ServiceBinding BeanStartupEvent beanEvent)
  {
    Bean bean = beanEvent.getBean();
    
    if (log.isLoggable(Level.FINER))
      log.fine(bean + " starting at initialization");
    
    WebBeansContainer webBeans = WebBeansContainer.create();

    webBeans.getInstance(bean);
  }

  /**
   * Registers a bean with OSGi based on a ServiceStartup event
   */
  public void osgiServiceStartup(@Observes @OsgiServiceBinding
				 BeanStartupEvent beanEvent)
  {
    Bean bean = beanEvent.getBean();
    
    if (log.isLoggable(Level.FINER))
      log.fine(bean + " starting at initialization");
    
    WebBeansContainer webBeans = WebBeansContainer.create();
    BundleContext bundle = webBeans.getInstanceByType(BundleContext.class);

    if (bundle == null)
      throw new ConfigException(L.l("The current environment does not have a BundleContext"));

    Object service = webBeans.getInstance(bean);

    Set<Class> typeSet = bean.getTypes();
      
    String types[] = new String[typeSet.size()];
    int i = 0;
    for (Class type : typeSet) {
      types[i++] = type.getName();
    }
    Dictionary properties = new Hashtable();

    String name = bean.getName();

    if (name != null)
      properties.put("javax.webbeans.Named", name);

    bundle.registerService(types, service, properties);
  }
}
