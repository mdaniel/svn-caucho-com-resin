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

package com.caucho.config.inject;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.annotation.ServiceBinding;
import com.caucho.config.ServiceStartup;
import com.caucho.config.cfg.BeansConfig;
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
import javax.enterprise.inject.spi.ManagedBean;
import javax.enterprise.inject.spi.Plugin;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
public class XmlStandardPlugin implements Plugin
{
  private InjectManager _manager;
  private ClassLoader _classLoader;

  private HashSet<String> _configuredBeans = new HashSet<String>();
  
  private ArrayList<Path> _paths = new ArrayList<Path>();
  private ArrayList<Path> _pendingPaths = new ArrayList<Path>();
  
  private ArrayList<BeansConfig> _pendingBeans = new ArrayList<BeansConfig>();

  private Throwable _configException;

  public XmlStandardPlugin(InjectManager manager)
  {
    _manager = manager;
    
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  public void addRoot(Path root)
  {
    if (! _paths.contains(root)) {
      _pendingPaths.add(root);
    }
  }

  public void beforeDiscovery(@Observes BeforeBeanDiscovery event)
  {
    ArrayList<Path> paths = new ArrayList<Path>(_pendingPaths);
    _pendingPaths.clear();

    try {
      for (Path root : paths) {
	Path beansPath = root.lookup("META-INF/beans.xml");
	beansPath.setUserPath(beansPath.getURL());

	if (beansPath.canRead()) {
	  BeansConfig beans = new BeansConfig(_manager, beansPath);

	  new Config().configure(beans, beansPath);

	  _pendingBeans.add(beans);
	}
      }
    } catch (Exception e) {
      if (_configException == null)
	_configException = e;
      
      throw ConfigException.create(e);
    }
  }

  public void processType(@Observes ProcessAnnotatedType event)
  {
    AnnotatedType type = event.getAnnotatedType();

    if (type == null)
      return;

    if (type.isAnnotationPresent(Stateful.class)
	|| type.isAnnotationPresent(Stateless.class)
	|| type.isAnnotationPresent(MessageDriven.class)) {
      event.setAnnotatedType(null);
    }
  }

  public void processType(@Observes AfterBeanDiscovery event)
  {
    if (_configException != null)
      event.addDefinitionError(_configException);
  }

  public void processBean(@Observes ProcessBean event)
  {
    ProcessBeanImpl eventImpl = (ProcessBeanImpl) event;

    if (eventImpl.getManager() != _manager)
      return;
    
    Bean bean = event.getBean();
    Annotated annotated = event.getAnnotated();

    if (isStartup(annotated)) {
      _manager.addService(bean);
    }
  }

  private boolean isStartup(Annotated annotated)
  {
    if (annotated == null)
      return false;

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().equals(ServiceStartup.class))
	return true;

      if (ann.annotationType().isAnnotationPresent(ServiceStartup.class)) {
	return true;
      }
    }

    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
