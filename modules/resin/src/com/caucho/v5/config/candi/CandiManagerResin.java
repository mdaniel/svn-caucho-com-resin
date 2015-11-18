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

package com.caucho.v5.config.candi;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.naming.InitialContext;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.ModulePrivate;
import com.caucho.v5.config.custom.CookieCustomBean;
import com.caucho.v5.config.custom.ExtensionCustomBean;
import com.caucho.v5.config.event.EventManager;
import com.caucho.v5.config.event.EventManagerResin;
import com.caucho.v5.config.j2ee.DataSourceDefinitionHandler;
import com.caucho.v5.config.j2ee.ExtensionCustomBeanResin;
import com.caucho.v5.config.j2ee.PersistenceContextHandler;
import com.caucho.v5.config.j2ee.PersistenceUnitHandler;
import com.caucho.v5.config.j2ee.ResourceHandler;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ResourceProgramManager;
import com.caucho.v5.config.scope.RequestContext;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.server.cdi.CdiExtensionResin;
import com.caucho.v5.server.cdi.ConversationContext;
import com.caucho.v5.server.cdi.SessionScopeImpl;
import com.caucho.v5.server.cdi.TransactionScope;

/**
 * The CDI container for a given environment.
 */
@ModulePrivate
@CauchoBean
@SuppressWarnings("serial")
public class CandiManagerResin extends CandiManager
{
  private static final Logger log
    = Logger.getLogger(CandiManagerResin.class.getName());
  
  private ResourceProgramManager _resourceManager
    = new ResourceProgramManager();
  
  public CandiManagerResin(String id,
                             CandiManager parent,
                             EnvironmentClassLoader loader,
                             boolean isSetLocal)
  {
    super(id, parent, loader, isSetLocal);
  }
  
  public static CandiManagerResin getCurrent()
  {
    return (CandiManagerResin) CandiManager.getCurrent();
  }
  
  public ResourceProgramManager getResourceManager()
  {
    return _resourceManager;
  }
  
  @Override
  protected EventManager createEventManager()
  {
    return new EventManagerResin(this);
  }

  @Override
  protected ExtensionCustomBean createExtensionCustomBean()
  {
    return new ExtensionCustomBeanResin(this);
  }
  
  @Override
  protected void initScopes()
  {
    super.initScopes();
  
    // addContext(SessionScopeImpl.class);
    // addContext(ConversationContext.class);
    // addContext(TransactionScope.class);
    
    addContext(ConversationContext.class);
    addContext(SessionScopeImpl.class);
    addContext(TransactionScope.class);
  }

  @Override
  protected void initManagerBeans()
  {
    super.initManagerBeans();
    
    try {
      InitialContext ic = new InitialContext();
      ic.rebind("java:comp/BeanManager", new ObjectFactoryNamingCdi());
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    
    addContext(new RequestContext());

    try {
      addInjection("javax.persistence.PersistenceContext",
                   new PersistenceContextHandler(this));
      addInjection("javax.persistence.PersistenceUnit",
                   new PersistenceUnitHandler(this));
    } catch (Throwable e) {
      log.finest(this + ": " + e.toString());
      log.log(Level.ALL, e.toString(), e);
    }

    addInjection(Resource.class,
                      new ResourceHandler(this));
    /*
    addInjection(EJB.class,
                 new EjbHandler(this));
    addInjection(EJBs.class,
                 new EjbHandler(this));
                 */
    addInjection(DataSourceDefinition.class,
                 new DataSourceDefinitionHandler(this));
    addInjection(DataSourceDefinitions.class,
                 new DataSourceDefinitionHandler(this));
    
    //getExtensionManager().addExtension(new ExtensionCustomBeanResin(this));

    getExtensionManager().addExtension(new CdiExtensionResin(this));
  }
  
  /*
  @Override
  protected boolean isUpdateRequired()
  {
    return (super.isUpdateRequired()
            || _xmlExtension.isPending());
  }
  */
  
  /*
  @Override
  protected boolean isStateful(Bean<?> bean)
  {
    return (super.isStateful(bean)
            || bean.getBeanClass().isAnnotationPresent(Stateful.class));
  }
  */
  
  @Override
  protected boolean isValidSimpleBean(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(CookieCustomBean.class)) {
      // ioc/04d0
      return true;
    }

    return super.isValidSimpleBean(type);
  }

  @Override
  public void buildInject(Class<?> rawType,
                          ArrayList<ConfigProgram> injectProgramList)
  {
    _resourceManager.buildInject(rawType, injectProgramList);
  }
  
  protected void beanPostBuildXml(Bean<?> bean)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClassLoader());

      /*
      WebBeanAdmin admin = new WebBeanAdmin(bean, _beanId);

      admin.register();
      */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
