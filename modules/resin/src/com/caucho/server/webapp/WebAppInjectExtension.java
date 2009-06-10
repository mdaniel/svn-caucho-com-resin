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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.remote.annotation.ServiceType;
import com.caucho.remote.annotation.ProxyType;
import com.caucho.remote.client.ProtocolProxyFactory;
import com.caucho.remote.server.ProtocolServletFactory;
import com.caucho.server.dispatch.ServletProtocolConfig;
import com.caucho.server.dispatch.ServletMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.event.Observes;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
public class WebAppInjectExtension implements Extension
{
  private InjectManager _beanManager;
  private WebApp _webApp;

  public WebAppInjectExtension(InjectManager manager,
			       WebApp webApp)
  {
    _beanManager = manager;
    _webApp = webApp;
  }

  public void processAnnotatedType(@Observes ProcessAnnotatedType event)
  {
    try {
      AnnotatedType annotatedType = event.getAnnotatedType();
      // Bean<?> bean = event.getBean();

      if (annotatedType == null || annotatedType.getAnnotations() == null)
	return;

      for (Annotation ann : annotatedType.getAnnotations()) {
	Class annType = ann.annotationType();
	
	if (annType.equals(WebServlet.class)) {
	  WebServlet webServlet = (WebServlet) ann;
      
	  ServletMapping mapping = new ServletMapping();

	  for (String value : webServlet.value()) {
	    mapping.addURLPattern(value);
	  }
	  
	  for (String value : webServlet.urlPatterns()) {
	    mapping.addURLPattern(value);
	  }
	  
	  mapping.setBean(_beanManager.createManagedBean(annotatedType));
	
	  mapping.init();

	  _webApp.addServletMapping(mapping);

	  event.veto();
	}
	else if (annType.isAnnotationPresent(ServiceType.class)) {
	  ServiceType serviceType
	    = (ServiceType) annType.getAnnotation(ServiceType.class);

	  Class factoryClass = serviceType.defaultFactory();
	  ProtocolServletFactory factory
	    = (ProtocolServletFactory) factoryClass.newInstance();

	  factory.setServiceType(ann);
	  factory.setAnnotated(annotatedType);

	  Method urlPatternMethod = annType.getMethod("urlPattern");

	  String urlPattern = (String) urlPatternMethod.invoke(ann);
      
	  ServletMapping mapping = new ServletMapping();
	  mapping.addURLPattern(urlPattern);
	  mapping.setBean(_beanManager.createManagedBean(annotatedType));

	  mapping.setProtocolFactory(factory);
	
	  mapping.init();

	  _webApp.addServletMapping(mapping);

	  event.veto();
	}
	else if (annType.isAnnotationPresent(ProxyType.class)) {
	  ProxyType proxyType
	    = (ProxyType) annType.getAnnotation(ProxyType.class);

	  Class factoryClass = proxyType.defaultFactory();
	  ProtocolProxyFactory proxyFactory
	    = (ProtocolProxyFactory) factoryClass.newInstance();

	  proxyFactory.setProxyType(ann);
	  proxyFactory.setAnnotated(annotatedType);

	  /*
	  HessianProtocolProxyFactory proxyFactory
	    = new HessianProtocolProxyFactory();
	  proxyFactory.setURL(client.url());
	  */
	
	  Object proxy = proxyFactory.createProxy((Class) annotatedType.getBaseType());

	  BeanFactory factory
	    = _beanManager.createBeanFactory(annotatedType);

	  /*
	  factory.name(bean.getName());

	  for (Type type : bean.getTypes()) {
	    factory.type(type);
	  }

	  for (Annotation binding : bean.getBindings()) {
	    factory.binding(binding);
	  }

	  factory.deployment(bean.getDeploymentType());
	  */

	  _beanManager.addBean(factory.singleton(proxy));

	  //event.setBean(null);
	}
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
