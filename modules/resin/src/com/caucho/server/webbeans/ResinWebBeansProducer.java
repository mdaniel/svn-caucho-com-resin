/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.security.Principal;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Conversation;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import com.caucho.config.CauchoDeployment;
import com.caucho.config.ContextDependent;
import com.caucho.config.inject.InjectManager;
import com.caucho.jca.pool.UserTransactionProxy;
import com.caucho.jmx.Jmx;
import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextException;
import com.caucho.server.util.ScheduledThreadPool;
import com.caucho.transaction.TransactionManagerImpl;

/**
 * Resin WebBeans producer for the main singletons.
 */

@CauchoDeployment
@Singleton
public class ResinWebBeansProducer
{
  private static final Logger log
    = Logger.getLogger(ResinWebBeansProducer.class.getName());
  
  public ResinWebBeansProducer()
  {
  }

  /**
   * Returns the web beans container.
   */
  /*
  @Produces
  public BeanManager getManager()
  {
    return InjectManager.create();
  }
  */

  /**
   * Returns the web beans conversation controller
   */
  @Produces
  public Conversation getConversation()
  {
    return InjectManager.create().createConversation();
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
   * Returns the TransactionSynchronizationRegistry
   */
  @Produces
  @CauchoDeployment
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
  public Principal getPrincipal()
  {
    try {
      return SecurityContext.getUserPrincipal();
    } catch (SecurityContextException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
}
