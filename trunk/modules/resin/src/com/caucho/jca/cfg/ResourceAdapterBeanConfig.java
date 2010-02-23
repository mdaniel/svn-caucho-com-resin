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

package com.caucho.jca.cfg;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.types.*;
import com.caucho.jca.*;
import com.caucho.jca.cfg.JavaMailConfig;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.StartListener;
import com.caucho.naming.Jndi;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the init-param pattern.
 */
public class ResourceAdapterBeanConfig extends BeanConfig {
  private static final Logger log
    = Logger.getLogger(ResourceAdapterBeanConfig.class.getName());
  
  private static L10N L = new L10N(ResourceAdapterBeanConfig.class);

  public ResourceAdapterBeanConfig()
  {
    setBeanConfigClass(ResourceAdapter.class);
  }

  public void init()
  {
    super.init();

    Class type = getClassType();

    ResourceArchive ra
      = ResourceArchiveManager.findResourceArchive(type.getName());

    ResourceAdapterController controller
      = new ResourceAdapterController(getComponent(), ra);

    InjectManager beanManager = InjectManager.create();
    BeanFactory factory = beanManager.createBeanFactory(controller.getClass());

    factory.name(type.getName());
    factory.binding(Names.create(type.getName()));
    factory.binding(CurrentLiteral.CURRENT);

    beanManager.addBean(factory.singleton(controller));
  }
}
