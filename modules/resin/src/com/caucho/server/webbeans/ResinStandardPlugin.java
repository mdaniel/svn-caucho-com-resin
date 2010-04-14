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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ProcessBeanImpl;
import com.caucho.ejb.inject.EjbGeneratedBean;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.inject.Jndi;
import com.caucho.inject.MBean;
import com.caucho.jms.JmsMessageListener;
import com.caucho.jmx.Jmx;
import com.caucho.remote.BamService;
import com.caucho.server.admin.AdminService;
import com.caucho.server.cluster.Server;
import com.caucho.util.L10N;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
public class ResinStandardPlugin implements Extension {
  private static final L10N L = new L10N(ResinStandardPlugin.class);
  private static final Logger log
    = Logger.getLogger(ResinStandardPlugin.class.getName());

  private InjectManager _injectManager;
  
  public ResinStandardPlugin(InjectManager manager) 
  {
    _injectManager = manager;
  }

  /**
   * Callback for discovered annotated types. EJBs are dispatched to
   * EJB and disabled for normal processing.
   */
  public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event) 
  {
    AnnotatedType<T> annotatedType = event.getAnnotatedType();

    if (annotatedType == null)
      return;

    // ioc/0j08
    boolean isXmlConfig = false;

    if (isXmlConfig
        && (annotatedType.isAnnotationPresent(Stateful.class)
            || annotatedType.isAnnotationPresent(Stateless.class)
            || annotatedType.isAnnotationPresent(Singleton.class)
            || annotatedType.isAnnotationPresent(MessageDriven.class)
            || annotatedType.isAnnotationPresent(JmsMessageListener.class))) {
      EjbContainer ejbContainer = EjbContainer.create();
      ejbContainer.createBean(annotatedType, null);
      event.veto();
    }
  }

  public <T> void processBean(@Observes ProcessBeanImpl<T> event) 
  {
    Annotated annotated = event.getAnnotated();
    Bean<T> bean = event.getBean();

    if (annotated == null || bean instanceof EjbGeneratedBean
        || !(bean instanceof AbstractBean<?>)) {
      return;
    }

    AbstractBean<T> absBean = (AbstractBean<T>) bean;

    if (annotated.isAnnotationPresent(Stateful.class)
        || annotated.isAnnotationPresent(Stateless.class)
        || annotated.isAnnotationPresent(Singleton.class)
        || annotated.isAnnotationPresent(MessageDriven.class)
        || annotated.isAnnotationPresent(JmsMessageListener.class)) {
      EjbContainer ejbContainer = EjbContainer.create();
      AnnotatedType<T> annType = absBean.getAnnotatedType();

      if (annType != null) {
        ejbContainer.createBean(annType, absBean.getInjectionTarget());
        
        if (event instanceof ProcessBeanImpl<?>)
          ((ProcessBeanImpl<?>) event).veto();
      }
    }
    
    if (annotated.isAnnotationPresent(Jndi.class)) {
      Jndi jndi = annotated.getAnnotation(Jndi.class);
      String jndiName = jndi.value();
      
      if ("".equals(jndiName)) {
        jndiName = bean.getBeanClass().getSimpleName();
      }
      
      JndiBeanProxy<T> proxy = new JndiBeanProxy<T>(_injectManager, bean);
      
      if (log.isLoggable(Level.FINE))
        log.fine("bind to JNDI '" + jndiName + "' for " + bean);
                 
      try {
        com.caucho.naming.Jndi.bindDeepShort(jndiName, proxy);
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    if (annotated.isAnnotationPresent(MBean.class)) {
      MBean manage = annotated.getAnnotation(MBean.class);
      
      String mbeanName = manage.value();
      if ("".equals(mbeanName))
        mbeanName = "type=" + bean.getBeanClass().getSimpleName();
      
      AnnotatedType<?> annType = (AnnotatedType<?>) annotated;
      
      try {
        Jmx.register(new BeanMBean(_injectManager, bean, annType), mbeanName);
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (annotated.isAnnotationPresent(BamService.class)) {
      BamService service = annotated.getAnnotation(BamService.class);

      HempBroker broker = HempBroker.getCurrent();

      broker.addStartupActor(event.getBean(), service.name(), service
          .threadMax());
    }

    if (annotated.isAnnotationPresent(AdminService.class)) {
      AdminService service = annotated.getAnnotation(AdminService.class);

      Server server = Server.getCurrent();

      if (server == null) {
        throw new ConfigException(L
            .l("@AdminService requires an active Resin Server."));
      }

      if (!server.isWatchdog()) {
        HempBroker broker = (HempBroker) server.getAdminBroker();

        broker.addStartupActor(event.getBean(), service.name(), service
            .threadMax());
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
