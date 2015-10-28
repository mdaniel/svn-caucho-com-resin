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

package com.caucho.v5.config.j2ee;

import java.util.ArrayList;

import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.ServiceStartup;
import com.caucho.v5.config.cfg.BeansConfig;
import com.caucho.v5.config.custom.ExtensionCustomBean;
import com.caucho.v5.config.inject.InjectManager;
import com.caucho.v5.config.inject.ScheduleBean;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
@Module
@CauchoBean
public class ExtensionCustomBeanResin extends ExtensionCustomBean
{
  private static final L10N L = new L10N(ExtensionCustomBeanResin.class);

  private static final String SCHEMA = "com/caucho/v5/config/cfg/resin-beans.rnc";

  public ExtensionCustomBeanResin(InjectManager manager)
  {
    super(manager);
  }

  public ExtensionCustomBeanResin()
  {
    throw new IllegalStateException();
  }

  /*
  @Override
  protected void addRootPaths(Path root)
  {
    super.addRootPaths(root);
    
    addPath(root.lookup("META-INF/beans.xml"));
    addPath(root.lookup("META-INF/resin-beans.xml"));

    if (root.getFullPath().endsWith("WEB-INF/classes/")) {
      addPath(root.lookup("../beans.xml"));
      addPath(root.lookup("../resin-beans.xml"));
    } else if (! root.lookup("META-INF/beans.xml").canRead()
               && ! root.lookup("META-INF/resin-beans.xml").canRead()) {
      // ejb/11h0
      addPath(root.lookup("beans.xml"));
      addPath(root.lookup("resin-beans.xml"));
    }
  }

  @Override
  protected void configurePath(BeansConfig beans, Path beansPath)
    throws IOException
  {
    new ConfigXml().configure(beans, beansPath, SCHEMA);
  }
  
  protected void configureXmlOverridePath(Path beansPath)
    throws IOException
  {
    ContextConfigJavaee context = new ContextConfigJavaee(getCdiManager(), beansPath);

    ConfigXml config = new ConfigXml();
    config.configure(context, beansPath, SCHEMA);
  }
  */
  
  @Override
  protected void startupBean(Bean<?> bean, Object value)
  {
    super.startupBean(bean, value);

    if (bean instanceof ScheduleBean) {
      ((ScheduleBean) bean).scheduleTimers(value);
    }
  }

  /*
  @Override
  protected boolean startDependencies(ArrayList<StartupItem> startupBeans,
                                      ArrayList<Bean<?>> runningBeans,
                                      StartupItem item)
  {
    Bean<?> bean = item.getBean();

    DependsOn dependsOn = item.getAnnotated().getAnnotation(DependsOn.class);

    if (dependsOn == null || isStarted(runningBeans, dependsOn)) {
      return super.startDependencies(startupBeans, runningBeans, item);
    }

    return false;
  }

  private boolean isStarted(ArrayList<Bean<?>> runningBeans, DependsOn depends)
  {
    for (String dep : depends.value()) {
      if (! isStarted(runningBeans, dep))
        return false;
    }

    return true;
  }
  */

  private boolean isStarted(ArrayList<Bean<?>> runningBeans, String dep)
  {
    for (Bean<?> bean : runningBeans) {
      String name = bean.getName();
      String className = bean.getBeanClass().getSimpleName();

      if (dep.equals(name) || dep.equals(className))
        return true;
    }

    return false;
  }

  @Override
  protected boolean isStartup(Class<?> annType)
  {
    if (super.isStartup(annType)) {
      return true;
    }

    // @Stateless must be on the bean itself
    /*
    if (annType.equals(Stateless.class)) {
      return true;
    }

    if (annType.equals(javax.ejb.Startup.class)) {
      return true;
    }
    */

    if (annType.equals(ServiceStartup.class)) {
      return true;
    }
    
    // @Startup & @ServiceStartup can be stereotyped
    if (annType.isAnnotationPresent(Stereotype.class)) {
      if (annType.isAnnotationPresent(ServiceStartup.class)) {
        return true;
      }

      /*
      if (annType.isAnnotationPresent(javax.ejb.Startup.class)) {
        return true;
      }
      */
    }

    return false;
  }
  
  private class ContextConfigJavaee extends BeansConfig implements EnvironmentBean {
    ContextConfigJavaee(InjectManager manager, Path root)
    {
      super(manager, root);
    }

    public ClassLoader getClassLoader()
    {
      return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("unused")
    public SystemContextJavaee createSystem()
    {
      return new SystemContextJavaee();
    }
  }

  private class SystemContextJavaee implements EnvironmentBean {
    public ClassLoader getClassLoader()
    {
      return ClassLoader.getSystemClassLoader();
    }
  }  
}