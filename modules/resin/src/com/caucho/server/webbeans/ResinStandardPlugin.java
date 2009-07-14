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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.annotation.ServiceBinding;
import com.caucho.config.ServiceStartup;
import com.caucho.config.cfg.BeansConfig;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ProcessBeanImpl;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.inject.EjbGeneratedBean;
import com.caucho.jms.JmsMessageListener;
import com.caucho.vfs.Path;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
public class ResinStandardPlugin implements Extension
{
  private InjectManager _manager;
  private ClassLoader _classLoader;

  public ResinStandardPlugin(InjectManager manager)
  {
    _manager = manager;
    
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  public void processAnnotatedType(@Observes ProcessAnnotatedType event)
  {
    AnnotatedType annotatedType = event.getAnnotatedType();

    if (annotatedType == null)
      return;

    if (annotatedType.isAnnotationPresent(Stateful.class)
	|| annotatedType.isAnnotationPresent(Stateless.class)
	|| annotatedType.isAnnotationPresent(MessageDriven.class)
	|| annotatedType.isAnnotationPresent(JmsMessageListener.class)) {
      EjbContainer ejbContainer = EjbContainer.create();
      System.out.println("NULLSOZ:");
      ejbContainer.createBean(annotatedType, null);
      event.veto();
    }
  }

  public void processBean(@Observes ProcessBeanImpl event)
  {
    Annotated annotated = event.getAnnotated();
    Bean bean = event.getBean();

    if (annotated == null
	|| bean instanceof EjbGeneratedBean
	|| ! (bean instanceof AbstractBean)) {
      return;
    }
    
    AbstractBean absBean = (AbstractBean) bean;

    if (annotated.isAnnotationPresent(Stateful.class)
	|| annotated.isAnnotationPresent(Stateless.class)
	|| annotated.isAnnotationPresent(MessageDriven.class)
	|| annotated.isAnnotationPresent(JmsMessageListener.class)) {
      EjbContainer ejbContainer = EjbContainer.create();
      AnnotatedType annType = absBean.getAnnotatedType();
      
      if (annType != null) {
	ejbContainer.createBean(annType, absBean.getInjectionTarget());
	event.veto();
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
