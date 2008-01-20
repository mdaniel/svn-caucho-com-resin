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

package com.caucho.ejb.session;

import com.caucho.config.*;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import javax.ejb.*;
import javax.webbeans.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Server container for a session bean.
 */
abstract public class SessionServer extends AbstractServer
{
  protected final static Logger log
    = Logger.getLogger(SessionServer.class.getName());

  public SessionServer(EjbContainer manager)
  {
    super(manager);
  }

  @Override
  protected String getType()
  {
    return "session:";
  }

  /**
   * Initialize the server
   */
  @Override
  public void init()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      super.init();
      
      WebBeansContainer webBeans = WebBeansContainer.create();

      SingletonComponent comp
        = new SingletonComponent(webBeans, getSessionContext());
      comp.setTargetType(SessionContext.class);
      comp.init();
      webBeans.addComponent(comp);

      _localHome = getSessionContext().createLocalHome();
      _remoteHomeView = getSessionContext().createRemoteHomeView();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerWebBeans();

    log.fine(this + " initialized");
  }

  private void registerWebBeans()
  {
    Class beanClass = getBeanSkelClass();
    ArrayList<Class> localApiList = getLocalApiList();

    if (beanClass != null && localApiList != null) {
      WebBeansContainer webBeans = WebBeansContainer.create();

      String beanName = getEJBName();
      Named named = (Named) beanClass.getAnnotation(Named.class);

      if (named != null)
	beanName = named.value();
      
      for (Class api : localApiList) {
	ComponentImpl comp = createSessionComponent(api);

	comp.setTargetType(getEjbClass());

	comp.setName(beanName);
	comp.addNameBinding(beanName);

	comp.init();
	webBeans.addComponentByName(beanName, comp);
	webBeans.addComponentByType(api, comp);
      }
    }
  }
  
  abstract protected ComponentImpl createSessionComponent(Class api);

  /**
   * Returns the object key from a handle.
   */
  @Override
  public Class getPrimaryKeyClass()
  {
    return null;
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  @Override
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  @Override
  public EJBHome getEJBHome()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the EJBRemote stub for the container
   */
  @Override
  public Object getRemoteObject()
  {
    return getRemoteObject(null);
  }

  public AbstractContext getContext()
  {
    return getSessionContext();
  }
}
