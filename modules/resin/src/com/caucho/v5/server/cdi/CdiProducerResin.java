/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.server.cdi;

import java.security.Principal;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.CauchoDeployment;
import com.caucho.v5.config.ContextDependent;
import com.caucho.v5.http.security.SecurityContextException;
import com.caucho.v5.jmx.JmxUtil;
import com.caucho.v5.security.SecurityContext;
import com.caucho.v5.server.util.ScheduledThreadPool;
import com.caucho.v5.transaction.TransactionManagerImpl;
import com.caucho.v5.transaction.UserTransactionProxy;

/**
 * Resin CDI producer for the main singletons.
 */

@CauchoDeployment
@Singleton
@CauchoBean
public class CdiProducerResin
{
  private static final Logger log
    = Logger.getLogger(CdiProducerResin.class.getName());
  
  public CdiProducerResin()
  {
  }

  /**
   * Returns the web beans conversation controller
   */
  @Produces
  @Named("javax.enterprise.context.conversation")
  @RequestScoped
  @CauchoBean
  public ConversationContext getConversation()
  {
    return new ConversationContext();
  }
  
  public void destroy(@Disposes @Named("javax.enterprise.context.conversation") ConversationContext conversation)
  {
    conversation.destroy();
  }

  /**
   * Returns the MBeanServer
   */
  @Produces
  @CauchoDeployment
  @CauchoBean
  public MBeanServer getMBeanServer()
  {
    return JmxUtil.getMBeanServer();
  }

  /**
   * Returns the TransactionManager
   */
  @Produces
  @CauchoDeployment
  @CauchoBean
  public TransactionManager getTransactionManager()
  {
    return TransactionManagerImpl.getInstance();
  }

  /**
   * Returns the UserTransaction
   */
  @Produces
  @CauchoDeployment
  @CauchoBean
  public UserTransaction getUserTransaction()
  {
    return UserTransactionProxy.getInstance();
  }

  /**
   * Returns the TransactionSynchronizationRegistry
   */
  @Produces
  @CauchoDeployment
  @CauchoBean
  public TransactionSynchronizationRegistry getSyncRegistry()
  {
    return TransactionManagerImpl.getInstance().getSyncRegistry();
  }

  /**
   * Returns the ScheduledExecutorService
   */
  @Produces
  @CauchoDeployment
  @ContextDependent
  @CauchoBean
  public ScheduledExecutorService getScheduledExecutorService()
  {
    return ScheduledThreadPool.getLocal();
  }
  

  /**
   * Returns the ScheduledExecutorService
   */
  @Produces
  @CauchoDeployment
  @ContextDependent
  @CauchoBean
  public Principal getPrincipal()
  {
    try {
      return SecurityContext.getUserPrincipal();
    } catch (SecurityContextException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
  
  /*
  @Produces
  @CauchoDeployment
  @ContextDependent
  public Bean getBean(InjectionPoint injectionPoint)
  {
    return injectionPoint.getBean();
  }
  */

  /**
   * Returns the AmpManager
   */
  /*
  @Produces
  @CauchoDeployment
  @ContextDependent
  @CauchoBean
  public ServiceManager getServiceManager()
  {
    return ActorManagerImpl.getCurrent();
  }
  */

  /**
   * Returns the AmpManager
   */
  /*
  @Produces
  @CauchoDeployment
  @ContextDependent
  @Lookup
  @ProxyProduces
  @CauchoBean
  public Object createProxy(InjectionPoint ip)
  {
    Class<?> type = (Class<?>) ip.getType();
    Lookup lookup = ip.getAnnotated().getAnnotation(Lookup.class);
    
    ServiceManager manager = ActorManagerImpl.getCurrent();
    
    ServiceRef serviceRef = manager.lookup(lookup.value());
    
    if (ServiceRef.class.equals(type)) {
      return serviceRef;
    }
    else {
      return manager.lookup(lookup.value()).as(type);
    }
  }
  */

  /**
   * Returns the AmpEventBus
   */
  /*
  @Produces
  @CauchoDeployment
  @ContextDependent
  @CauchoBean
  public EventService getEventService()
  {
    ServiceManager ampManager = ActorManagerImpl.getCurrent();
    
    return ampManager.lookup("event:").as(EventService.class);
  }
  */

  /**
   * Returns the OhanaStore
   */
  /*
  @Produces
  @CauchoDeployment
  @CauchoBean
  public StoreService getAmpStore(InjectionPoint ip)
  {
    String name = null;
    
    Annotated ann = ip.getAnnotated();
    
    if (ann instanceof AnnotatedMember) {
      AnnotatedMember<?> annMember = (AnnotatedMember<?>) ann;
      
      AnnotatedType<?> owner = annMember.getDeclaringType();
      
      Service service = owner.getAnnotation(Service.class);
      
      if (service != null) {
        String []paths = service.value();
        
        name = (paths != null && paths.length > 0) ? paths[0] : null;
      }
      
      if (name == null) {
        name = owner.getJavaClass().getName();
      }
    }
    
    ServiceManager manager = getServiceManager();
    
    return manager.lookup("store:///" + name).as(StoreService.class);
  }
  */

  /**
   * Adds the bean validation producer to CDI. This uses reflection in case
   * the validation jars don't exist.
   */
  public static Class<?> createResinValidatorProducer()
  {
    try {
      Class<?> cl = Class.forName("com.caucho.v5.server.cdi.ResinValidatorProducer");
      
      cl.getMethods();
      cl.getDeclaredFields();
      
      return cl;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (NoClassDefFoundError e) {
      log.finer(e.toString());
      log.log(Level.FINEST, e.toString(), e);
    }
    
    return null;
  }
  
  /*
  @javax.inject.Singleton
  @Produces()
  @CauchoBean
  public CacheManager createCacheManager()
  {
    return Caching.getCacheManager();
  }
  */
}
