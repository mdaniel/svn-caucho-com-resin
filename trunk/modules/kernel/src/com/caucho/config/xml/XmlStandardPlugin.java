/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.config.xml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ejb.DependsOn;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.ServiceStartup;
import com.caucho.config.bytecode.ScopeProxy;
import com.caucho.config.cfg.BeansConfig;
import com.caucho.config.extension.ProcessBeanImpl;
import com.caucho.config.inject.HandleAware;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ScheduleBean;
import com.caucho.config.inject.SingletonHandle;
import com.caucho.inject.LazyExtension;
import com.caucho.inject.Module;
import com.caucho.loader.EnvironmentBean;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
@Module
public class XmlStandardPlugin implements Extension {
  private static final L10N L = new L10N(XmlStandardPlugin.class);

  private static final String SCHEMA = "com/caucho/config/cfg/resin-beans.rnc";

  private InjectManager _cdiManager;
  private HashSet<String> _configuredBeans = new HashSet<String>();

  private ArrayList<Path> _roots = new ArrayList<Path>();
  private ArrayList<Path> _pendingRoots = new ArrayList<Path>();
  
  private ArrayList<Path> _pendingXml = new ArrayList<Path>();
  
  private HashSet<Path> _xmlSet = new HashSet<Path>();

  private ArrayList<BeansConfig> _pendingBeans = new ArrayList<BeansConfig>();

  private ArrayList<StartupItem> _pendingService = new ArrayList<StartupItem>();

  private Throwable _configException;

  public XmlStandardPlugin(InjectManager manager)
  {
    _cdiManager = manager;
  }

  public void addRoot(Path root)
  {
    if (!_roots.contains(root)) {
      _pendingRoots.add(root);
    }
  }
  
  public void addXmlPath(Path xmlPath)
  {
    if (! _xmlSet.contains(xmlPath)) {
      _xmlSet.add(xmlPath);
      _pendingXml.add(xmlPath);
    }
  }

  public void beforeDiscovery(@Observes BeforeBeanDiscovery event)
  {
    processRoots();
  }
  
  public boolean isPending()
  {
    return _pendingRoots.size() > 0 || _pendingXml.size() > 0;
  }
  
  public void processRoots()
  {
    try {
      ArrayList<Path> paths = new ArrayList<Path>(_pendingRoots);
      _pendingRoots.clear();
      
      for (Path root : paths) {
        configureRoot(root);
      }
      
      ArrayList<Path> xmlPaths = new ArrayList<Path>(_pendingXml);
      _pendingXml.clear();

      for (Path xml : xmlPaths) {
        configurePath(xml);
      }

      for (int i = 0; i < _pendingBeans.size(); i++) {
        BeansConfig config = _pendingBeans.get(i);

        ArrayList<Class<?>> deployList = config.getAlternativesList();

        if (deployList != null && deployList.size() > 0) {
          _cdiManager.setDeploymentTypes(deployList);
        }
      }
    } catch (Exception e) {
      if (_configException == null)
        _configException = e;

      throw ConfigException.create(e);
    }
  }

  private void configureRoot(Path root) throws IOException
  {
    List<Path> beansXmlOverride = _cdiManager.getBeansXmlOverride(root);

    if (beansXmlOverride == null) {
      addPath(root.lookup("META-INF/beans.xml"));
      addPath(root.lookup("META-INF/resin-beans.xml"));

      if (root.getFullPath().endsWith("WEB-INF/classes/")) {
        addPath(root.lookup("../beans.xml"));
        addPath(root.lookup("../resin-beans.xml"));
      } else if (!root.lookup("META-INF/beans.xml").canRead()
          && !root.lookup("META-INF/resin-beans.xml").canRead()) {
        // ejb/11h0
        addPath(root.lookup("beans.xml"));
        addPath(root.lookup("resin-beans.xml"));
      }
    } else {
      for (Path beansXMLPath : beansXmlOverride) {
        configureXmlOverridePath(beansXMLPath);
      }
    }
  }
  
  private void addPath(Path beansPath)
  {
    if (beansPath.canRead()
        && beansPath.getLength() > 0) {
      addXmlPath(beansPath);
    }
  }

  private void configurePath(Path beansPath) throws IOException
  {
    if (beansPath.canRead() && beansPath.getLength() > 0) {
      // ioc/0041 - tck allows empty beans.xml
      
      BeansConfig beans = new BeansConfig(_cdiManager, beansPath);

      beansPath.setUserPath(beansPath.getURL());
      new Config().configure(beans, beansPath, SCHEMA);

      _pendingBeans.add(beans);
    }
  }
  
  private void configureXmlOverridePath(Path beansPath) throws IOException
  {
    ContextConfig context = new ContextConfig(_cdiManager, beansPath);

    Config config = new Config();
    config.configure(context, beansPath, SCHEMA);
  }  

  public void addConfiguredBean(String className)
  {
    _configuredBeans.add(className);
  }

  @LazyExtension
  public void processType(@Observes ProcessAnnotatedType<?> event)
  {
    AnnotatedType<?> type = event.getAnnotatedType();

    if (type == null)
      return;

    if (type.isAnnotationPresent(XmlCookie.class))
      return;

    if (_configuredBeans.contains(type.getJavaClass().getName())) {
      event.veto();
      return;
    }

    // XXX: managed by ResinStandardPlugin
    /*
     * if (type.isAnnotationPresent(Stateful.class) ||
     * type.isAnnotationPresent(Stateless.class) ||
     * type.isAnnotationPresent(MessageDriven.class)) { event.veto(); }
     */
  }

  @LazyExtension
  @SuppressWarnings({ "unchecked" })
  public void processTarget(@Observes ProcessInjectionTarget<?> event)
  {
    AnnotatedType<?> type = event.getAnnotatedType();

    XmlCookie cookie = type.getAnnotation(XmlCookie.class);

    if (cookie != null) {
      InjectionTarget target = _cdiManager
          .getXmlInjectionTarget(cookie.value());

      event.setInjectionTarget(target);
    }
  }

  public void processType(@Observes AfterBeanDiscovery event)
  {
    if (_configException != null)
      event.addDefinitionError(_configException);
  }

  @LazyExtension
  public void processBean(@Observes ProcessBean<?> event)
  {
    ProcessBeanImpl<?> eventImpl = (ProcessBeanImpl<?>) event;

    if (eventImpl.getManager() != _cdiManager)
      return;

    Annotated annotated = event.getAnnotated();
    Bean<?> bean = event.getBean();

    if (isStartup(annotated)) {
      _pendingService.add(new StartupItem(bean, annotated));
    }
  }

  public void processAfterValidation(@Observes AfterDeploymentValidation event)
  {
    ArrayList<StartupItem> startupBeans
      = new ArrayList<StartupItem>(_pendingService);

    _pendingService.clear();

    ArrayList<Bean<?>> runningBeans = new ArrayList<Bean<?>>();

    Bean<?> bean;

    while ((bean = nextStartup(startupBeans, runningBeans)) != null) {
      CreationalContext<?> env = _cdiManager.createCreationalContext(bean);

      Object value = _cdiManager.getReference(bean, bean.getBeanClass(), env);

      if (value instanceof ScopeProxy)
        ((ScopeProxy) value).__caucho_getDelegate();

      if (bean instanceof ScheduleBean) {
        ((ScheduleBean) bean).scheduleTimers(value);
      }

      if (value instanceof HandleAware && bean instanceof PassivationCapable) {
        String id = ((PassivationCapable) bean).getId();

        ((HandleAware) value).setSerializationHandle(new SingletonHandle(id));
      }
    }
  }

  private Bean<?> nextStartup(ArrayList<StartupItem> startupBeans,
      ArrayList<Bean<?>> runningBeans)
  {
    if (startupBeans.size() == 0)
      return null;

    for (StartupItem item : startupBeans) {
      Bean<?> bean = item.getBean();

      DependsOn dependsOn = item.getAnnotated().getAnnotation(DependsOn.class);

      if (dependsOn == null || isStarted(runningBeans, dependsOn)) {
        startupBeans.remove(item);
        runningBeans.add(bean);

        return bean;
      }
    }

    StringBuilder sb = new StringBuilder();

    for (StartupItem item : startupBeans) {
      sb.append("\n  " + item.getBean());
    }

    throw new ConfigException(L.l("@DependsOn circularity {0}", sb));
  }

  private boolean isStarted(ArrayList<Bean<?>> runningBeans, DependsOn depends)
  {
    for (String dep : depends.value()) {
      if (! isStarted(runningBeans, dep))
        return false;
    }

    return true;
  }

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

  private boolean isStartup(Annotated annotated)
  {
    if (annotated == null)
      return false;

    for (Annotation ann : annotated.getAnnotations()) {
      Class<?> annType = ann.annotationType();

      // @Stateless must be on the bean itself
      if (annType.equals(Stateless.class))
        return true;

      if (annType.equals(Startup.class))
        return true;

      if (annType.equals(ServiceStartup.class))
        return true;

      // @Startup & @ServiceStartup can be stereotyped
      if (annType.isAnnotationPresent(Stereotype.class)) {
        if (annType.isAnnotationPresent(ServiceStartup.class))
          return true;

        if (annType.isAnnotationPresent(Startup.class))
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

  static class StartupItem {
    private Bean<?> _bean;
    private Annotated _annotated;

    public StartupItem(Bean<?> bean, Annotated annotated)
    {
      _bean = bean;
      _annotated = annotated;
    }

    public Bean<?> getBean()
    {
      return _bean;
    }

    public Annotated getAnnotated()
    {
      return _annotated;
    }
  }
  
  private class ContextConfig extends BeansConfig implements EnvironmentBean {
    ContextConfig(InjectManager manager, Path root)
    {
      super(manager, root);
    }

    public ClassLoader getClassLoader()
    {
      return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("unused")
    public SystemContext createSystem()
    {
      return new SystemContext();
    }
  }

  private class SystemContext implements EnvironmentBean {
    public ClassLoader getClassLoader()
    {
      return ClassLoader.getSystemClassLoader();
    }
  }  
}