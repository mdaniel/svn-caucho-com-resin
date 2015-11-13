/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.custom;

import io.baratine.core.Startup;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.bytecode.ScopeProxy;
import com.caucho.v5.config.candi.BeansConfig;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.HandleAware;
import com.caucho.v5.config.candi.SingletonHandle;
import com.caucho.v5.config.extension.ProcessBeanImpl;
import com.caucho.v5.config.xml.ConfigXml;
import com.caucho.v5.config.xml.ContextConfigXml;
import com.caucho.v5.inject.LazyExtension;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Standard bean behavior for custom configured beans.
 */
@Module
@CauchoBean
public class ExtensionCustomBean implements Extension {
  private static final L10N L = new L10N(ExtensionCustomBean.class);
  
  private static final String SCHEMA = "com/caucho/v5/config/cfg/resin-beans.rnc";

  private CandiManager _cdiManager;
  private HashSet<String> _configuredBeans = new HashSet<String>();

  private ArrayList<Path> _roots = new ArrayList<Path>();
  private ArrayList<Path> _pendingRoots = new ArrayList<Path>();
  
  private ArrayList<Path> _pendingXml = new ArrayList<>();
  
  private HashSet<Path> _xmlSet = new HashSet<Path>();

  private ArrayList<BeansConfig> _pendingBeans = new ArrayList<BeansConfig>();

  private ArrayList<StartupItem> _pendingStartup = new ArrayList<StartupItem>();

  private Throwable _configException;

  public ExtensionCustomBean(CandiManager manager)
  {
    _cdiManager = manager;
  }

  public ExtensionCustomBean()
  {
    throw new IllegalStateException();
  }
  
  protected CandiManager getCdiManager()
  {
    return _cdiManager;
  }

  public void addRoot(Path root)
  {
    if (! _roots.contains(root)) {
      _pendingRoots.add(root);
    }
  }

  public void add(BeansConfig config)
  {
    _pendingBeans.add(config);
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
      ArrayList<Path> paths = new ArrayList<>(_pendingRoots);
      _pendingRoots.clear();
      
      for (Path root : paths) {
        configureRoot(root);
      }

      ArrayList<Path> xmlPaths = new ArrayList<>(_pendingXml);
      _pendingXml.clear();

      for (Path xml : xmlPaths) {
        configurePath(xml);
      }

      for (int i = 0; i < _pendingBeans.size(); i++) {
        BeansConfig config = _pendingBeans.get(i);

        ArrayList<Class<?>> deployList = config.getAlternativesList();

        if (deployList != null && deployList.size() > 0) {
          config.getBeanManager().setDeploymentTypes(deployList);
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
      addRootPaths(root);
    } 
    else {
      for (Path beansXMLPath : beansXmlOverride) {
        configureXmlOverridePath(beansXMLPath);
      }
    }
  }

  protected void addRootPathsConfigFile(Path root)
  {
    addPath(root.lookup("META-INF/beans.cf"));

    if (root.getFullPath().endsWith("WEB-INF/classes/")) {
      addPath(root.lookup("../beans.cf"));
    }
    else if (! root.lookup("META-INF/beans.cf").canRead()) {
      // ejb/11h0
      addPath(root.lookup("beans.cf"));
    }
  }

  protected void addRootPaths(Path root)
  {
    addRootPathsConfigFile(root);
    
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
  
  protected void addPath(Path beansPath)
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
      
      Path root = beansPath.lookup("../..");
      
      BeansConfig beans = new BeansConfig(_cdiManager, 
                                          root);

      beansPath.setUserPath(beansPath.getURL());
      
      configurePath(beans, beansPath);

      _pendingBeans.add(beans);
    }
  }

  protected void configurePath(BeansConfig beans, Path beansPath)
    throws IOException
  {
    new ConfigXml().configure(beans, beansPath, SCHEMA);
  }
  
  protected void configureXmlOverridePath(Path beansPath)
    throws IOException
  {
    //ContextConfigXml context = new ContextConfigXml(getCdiManager(), beansPath);

    ConfigXml config = new ConfigXml();
    ContextConfigXml context = new ContextConfigXml(config);
    
    config.configure(context, beansPath, SCHEMA);
  }
  
  /*
  protected void configurePath(BeansConfig beans, Path path)
    throws IOException
  {
    new Config().configure2(beans, path);
  }
  
  protected void configureXmlOverridePath(Path beansPath) throws IOException
  {
    ContextConfig context = new ContextConfig(_cdiManager, beansPath);

    Config config = new Config();
    config.configure2(context, beansPath);
  }
  */

  public void addConfiguredBean(String className)
  {
    _configuredBeans.add(className);
  }

  public boolean isConfiguredBean(String className)
  {
    return _configuredBeans.contains(className);
  }

  @LazyExtension
  @SuppressWarnings({ "unchecked" })
  public void processTarget(@Observes ProcessInjectionTarget<?> event)
  {
    AnnotatedType<?> type = event.getAnnotatedType();

    CookieCustomBean cookie = type.getAnnotation(CookieCustomBean.class);

    if (cookie != null) {
      InjectionTarget target = _cdiManager
          .getInjectionTargetCustomBean(cookie.value());

      event.setInjectionTarget(target);
    }
  }

  public void processType(@Observes AfterBeanDiscovery event)
  {
    if (_configException != null) {
      event.addDefinitionError(_configException);
    }
  }

  @LazyExtension
  public void processBean(@Observes ProcessBean<?> event)
  {
    ProcessBeanImpl<?> eventImpl = (ProcessBeanImpl<?>) event;

    if (eventImpl.getManager() != _cdiManager) {
      return;
    }

    Annotated annotated = event.getAnnotated();
    Bean<?> bean = event.getBean();

    if (isStartup(annotated)) {
      _pendingStartup.add(new StartupItem(bean, annotated));
    }
  }

  public void processAfterValidation(@Observes AfterDeploymentValidation event)
  {
    startupBeans();
  }
  
  public void startupBeans()
  {
    ArrayList<StartupItem> startupBeans = new ArrayList<>(_pendingStartup);

    _pendingStartup.clear();

    ArrayList<Bean<?>> runningBeans = new ArrayList<Bean<?>>();
    
    Collections.sort(startupBeans, new StartupComparator());

    Bean<?> bean;

    while ((bean = nextStartup(startupBeans, runningBeans)) != null) {
      CreationalContext<?> env = _cdiManager.createCreationalContext(bean);

      Object value = _cdiManager.getReference(bean, bean.getBeanClass(), env);

      startupBean(bean, value);
    }
  }
  
  protected void startupBean(Bean<?> bean, Object value)
  {
    if (value instanceof ScopeProxy) {
      ((ScopeProxy) value).__caucho_getDelegate();
    }

    if (value instanceof HandleAware && bean instanceof PassivationCapable) {
      String id = ((PassivationCapable) bean).getId();

      ((HandleAware) value).setSerializationHandle(new SingletonHandle(id));
    }
  }

  private Bean<?> nextStartup(ArrayList<StartupItem> startupBeans,
                              ArrayList<Bean<?>> runningBeans)
  {
    if (startupBeans.size() == 0) {
      return null;
    }

    for (StartupItem item : startupBeans) {
      Bean<?> bean = item.getBean();

      if (startDependencies(startupBeans, runningBeans, item)) {
        return bean;
      }
    }

    StringBuilder sb = new StringBuilder();

    for (StartupItem item : startupBeans) {
      sb.append("\n  " + item.getBean());
    }

    throw new ConfigException(L.l("@DependsOn circularity {0}", sb));
  }
  
  protected boolean startDependencies(ArrayList<StartupItem> startupBeans,
                                      ArrayList<Bean<?>> runningBeans,
                                      StartupItem item)
  {
    startupBeans.remove(item);
    runningBeans.add(item.getBean());

    return true;
  }

  private boolean isStartup(Annotated annotated)
  {
    if (annotated == null)
      return false;

    for (Annotation ann : annotated.getAnnotations()) {
      Class<?> annType = ann.annotationType();

      if (isStartup(annType)) {
        return true;
      }
    }

    return false;
  }
  
  protected boolean isStartup(Class<?> annType)
  {
    if (annType.equals(Startup.class)) {
      return true;
    }
    
    /*
    if (annType.isAnnotationPresent(Stereotype.class)) {
      if (annType.isAnnotationPresent(Startup.class)) {
        return true;
      }
    }
    */
    
    if (annType.isAnnotationPresent(Startup.class)) {
      return true;
    }
    
    return false;
  }
  

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }

  public static class StartupItem {
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
    ContextConfig(CandiManager manager, Path root)
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
  
  private static class StartupComparator implements Comparator<StartupItem> {
    @Override
    public int compare(StartupItem a , StartupItem b)
    {
      String aName = a.getBean().getBeanClass().getName();
      String bName = b.getBean().getBeanClass().getName();
      
      return aName.compareTo(bName);
    }
    
  }

  private class SystemContext implements EnvironmentBean {
    public ClassLoader getClassLoader()
    {
      return ClassLoader.getSystemClassLoader();
    }
  }  
}