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

import com.caucho.ejb.timer.EjbTimerService;
import com.caucho.jca.UserTransactionProxy;
import com.caucho.jmx.Jmx;
import com.caucho.webbeans.*;
import com.caucho.webbeans.manager.WebBeansContainer;
import com.caucho.server.util.ScheduledThreadPool;
import com.caucho.transaction.*;

import java.util.concurrent.*;
import javax.ejb.*;
import javax.management.*;
import javax.transaction.*;
import javax.webbeans.*;

/**
 * Resin WebBeans producer for the main singletons.
 */

@Standard
@Singleton
public class ResinWebBeansProducer
{
  /**
   * Returns the web beans container.
   */
  @Produces
  @Standard
  public Container getContainer()
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
  @Standard
  public MBeanServer getMBeanServer()
  {
    return Jmx.getGlobalMBeanServer();
  }
  
  /**
   * Returns the TransactionManager
   */
  @Produces
  @Standard
  public TransactionManager getTransactionManager()
  {
    return TransactionManagerImpl.getInstance();
  }
  
  /**
   * Returns the UserTransaction
   */
  @Produces
  @Standard
  public UserTransaction getUserTransaction()
  {
    return UserTransactionProxy.getInstance();
  }
  
  /**
   * Returns the ScheduledExecutorService
   */
  @Produces
  @Standard
  public ScheduledExecutorService getScheduledExecutorService()
  {
    return ScheduledThreadPool.getLocal();
  }
  
  /**
   * Returns the javax.ejb.TimerService
   */
  @Produces
  @Standard
  public TimerService getTimerService()
  {
    return EjbTimerService.getCurrent();
  }
}
