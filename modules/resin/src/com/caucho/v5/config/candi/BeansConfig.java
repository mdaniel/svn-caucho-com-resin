/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.interceptor.Interceptor;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigName;
import com.caucho.v5.config.Configured;
import com.caucho.v5.config.LineConfigException;
import com.caucho.v5.config.custom.ConfigCustomBean;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Configuration for a classloader root containing webbeans
 */
@Module
public class BeansConfig implements EnvironmentBean {
  private static final L10N L = new L10N(BeansConfig.class);

  private static final String VERSION_1_0 = "1.0";
  private static final String VERSION_1_1 = "1.1";
  
  public static final String SCHEMA = "com/caucho/v5/config/cfg/resin-beans.rnc";

  private CandiManager _injectManager;
  private Path _root;
  private BeanManagerBase _beanManager;

  private Path _beansFile;

  private ArrayList<Class<?>> _alternativesList
    = new ArrayList<>();

  private ArrayList<Class<?>> _interceptorList
    = new ArrayList<>();

  private ArrayList<Class<?>> _decoratorList
    = new ArrayList<>();

  private ArrayList<Class<?>> _pendingClasses
    = new ArrayList<>();

  private ArrayList<Exclude> _excludeList
    = new ArrayList<>();

  private BeanArchive.DiscoveryMode _discoveryMode;

  private String _version = "1.0";

  private boolean _isConfigured;

  public BeansConfig(CandiManager injectManager, 
                     Path root)
  {
    this(injectManager, root, lookupFilePath(root));
  }

  public BeansConfig(CandiManager injectManager, 
                     Path rootPath,
                     Path filePath)
  {
    _injectManager = injectManager;

    _root = rootPath;
    _beansFile = filePath;
    
    _beanManager = injectManager.createBeanManager(rootPath);
  }

  public BeansConfig(CandiManager injectManager, 
                     BeanManagerBase beanManager)
  {
    _injectManager = injectManager;

    _beanManager = beanManager;
  }
  
  private static Path lookupFilePath(Path rootPath)
  {
    Path beansFile = rootPath.lookup("META-INF/beans.xml");
    beansFile.setUserPath(beansFile.getURL());
    
    return beansFile;
  }

  public void setSchemaLocation(String schema)
  {
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  @Configured
  public void setBeanDiscoveryMode(BeanArchive.DiscoveryMode discoveryMode)
  {
    _discoveryMode = discoveryMode;
  }

  public BeanArchive.DiscoveryMode getDiscoveryMode()
  {
    if (_discoveryMode != null) {
      return _discoveryMode;
    }

    if (VERSION_1_0.equals(_version)) {
      _discoveryMode = BeanArchive.DiscoveryMode.ALL;
    }
    else if (VERSION_1_1.equals(_version)) {
      _discoveryMode = BeanArchive.DiscoveryMode.ANNOTATED;
    }

    return _discoveryMode;
  }

  /**
   * returns the owning container.
   */
  public CandiManager getContainer()
  {
    return _injectManager;
  }
  
  public BeanManagerBase getBeanManager()
  {
    return _beanManager;
  }

  /**
   * Returns the owning classloader.
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return getContainer().getClassLoader();
  }

  /**
   * Gets the web beans root directory
   */
  public Path getRoot()
  {
    return _root;
  }

  /**
   * Adds a scanned class
   */
  public void addScannedClass(Class<?> cl)
  {
    _pendingClasses.add(cl);
  }

  /**
   * True if the configuration file has been passed.
   */
  public boolean isConfigured()
  {
    return _isConfigured;
  }

  /**
   * True if the configuration file has been passed.
   */
  public void setConfigured(boolean isConfigured)
  {
    _isConfigured = isConfigured;
  }

  public ArrayList<Class<?>> getAlternativesList()
  {
    return _alternativesList;
  }

  //
  // web-beans syntax
  //

  /**
   * Adds a namespace bean
   */
  public void addCustomBean(ConfigCustomBean<?> bean)
  {
  }

  /**
   * Adds a deploy
   */
  @ConfigName("Deploy")
  public DeployConfig createDeploy()
  {
    return new DeployConfig();
  }

  /**
   * Adds a deploy
   */
  public AlternativesConfig createAlternatives()
  {
    return new AlternativesConfig();
  }

  /**
   * Adds the interceptors
   */
  public Interceptors createInterceptors()
  {
    return new Interceptors();
  }

  /**
   * Adds the decorators
   */
  public Decorators createDecorators()
  {
    return new Decorators();
  }

  /**
   * Initialization and validation on parse completion.
   */
  @PostConstruct
  public void init()
  {
    for (Class<?> cl : _decoratorList) {
      // DecoratorBean<?> decorator = new DecoratorBean(_injectManager, cl);

      // _injectManager.addDecoratorClass(cl);
      getBeanManager().addDecoratorClass(cl);
    }
    
    //_decoratorList.clear();

    for (Class<?> cl : _interceptorList) {
      getBeanManager().addInterceptorClass(cl);
    }
    
    //_interceptorList.clear();

    update();
  }

  public void update()
  {
    BeanManagerBase beanManager = getBeanManager();

    try {
      if (_pendingClasses.size() > 0) {
        ArrayList<Class<?>> pendingClasses
          = new ArrayList<Class<?>>(_pendingClasses);
        _pendingClasses.clear();

        for (Class<?> cl : pendingClasses) {
          ManagedBeanImpl<?> bean;
          
          bean = beanManager.createManagedBean(cl);

          beanManager.addBeanDiscover(bean);

          bean.introspectProduces();
        }
      }
    } catch (Exception e) {
      throw LineConfigException.create(_beansFile.getURL(), 1, e);
    }
  }

  public <T> void addInterceptor(Class<T> cl)
  {
    if (_interceptorList == null)
      _interceptorList = new ArrayList<>();
    
    if (cl.isInterface()) {
      throw new ConfigException(L.l("'{0}' is not valid because <interceptors> can only contain interceptor implementations",
                                    cl.getName()));
    }

    if (_interceptorList.contains(cl)) {
      throw new ConfigException(L.l("'{0}' is a duplicate interceptor. Interceptors may not be listed twice in the beans.xml",
                                    cl.getName()));
    }
    
    if (! cl.isAnnotationPresent(Interceptor.class)) {
      // ioc/0c95
      throw new ConfigException(L.l("'{0}' is an invalid interceptor because it does not have an @Interceptor.",
                                    cl.getName()));
      
    }
      
    _interceptorList.add(cl);
  }

  @Configured
  public BeansConfig createScan()
  {
    return this;
  }

  @Configured
  public void addExclude(Exclude exclude)
  {
    _excludeList.add(exclude);
  }

  public boolean isExcluded(String className)
  {
    for (Exclude exclude : _excludeList) {
      if (exclude.isMatch(className))
        return true;
    }

    return false;
  }

  public List<Class<?>> getInterceptors()
  {
    return _interceptorList;
  }

  public List<Class<?>> getDecorators()
  {
    return _decoratorList;
  }

  @Override
  public String toString()
  {
    if (_root != null)
      return getClass().getSimpleName() + "[" + _root.getURL() + "]";
    else
      return getClass().getSimpleName() + "[]";
  }

  public class Interceptors
  {
    public void addClass(Class<?> cl)
    {
      addInterceptor(cl);
    }
  }

  public class Decorators {
    public void setConfigLocation(String location)
    {
    }

    public void addClass(Class<?> cl)
    {
      if (_decoratorList.contains(cl)) {
        throw new ConfigException(L.l("'{0}' is a duplicate decorator. Decorators may not be listed twice in the beans.xml",
                                      cl.getName()));
      }
        
      // addDecorator(cl);
      _decoratorList.add(cl);
    }

    public void addDecorator(Class<?> cl)
    {
      addClass(cl);
    }

    public void addCustomBean(ConfigCustomBean<?> config)
    {
      Class<?> cl = config.getClassType();

      if (cl.isInterface())
        throw new ConfigException(L.l("'{0}' is not valid because <Decorators> can only contain decorator implementations",
                                      cl.getName()));

      /*
      if (! comp.isAnnotationPresent(Decorator.class)) {
        throw new ConfigException(L.l("'{0}' must have an @Decorator annotation because it is a decorator implementation",
                                      cl.getName()));
      }
      */

      _decoratorList.add(cl);
    }
  }

  public class DeployConfig {
    public void setConfigLocation(String location)
    {
    }

    public void addAnnotation(Annotation ann)
    {
      Class<?> cl = ann.annotationType();

      /*
      if (! cl.isAnnotationPresent(DeploymentType.class))
        throw new ConfigException(L.l("'{0}' must have a @DeploymentType annotation because because <Deploy> can only contain @DeploymentType annotations",
                                      cl.getName()));
      */

      _alternativesList.add(cl);
    }
  }

  public class AlternativesConfig
  {
    public void addClass(Class<?> cl)
    {
      if (cl.isAnnotation() && ! cl.isAnnotationPresent(Stereotype.class)) {
        // CDI TCK allows the stereotype in <class>
        throw new ConfigException(L.l("'{0}' is an invalid alternative because it is an annotation.",
                                      cl.getName()));
      }
      
      if (! cl.isAnnotationPresent(Alternative.class))
        throw new ConfigException(L.l("'{0}' is an invalid alternative because it does not have an @Alternative annotation.",
                                      cl.getName()));
     
      if (_alternativesList.contains(cl))
        throw new ConfigException(L.l("'{0}' is an invalid alternative because it is listed twice.",
                                      cl.getName()));
        
      _alternativesList.add(cl);
    }

    public void addStereotype(Class<?> cl)
    {
      if (! cl.isAnnotation())
        throw new ConfigException(L.l("'{0}' is an invalid alternative because it is not an annotation.",
                                      cl.getName()));
      
      if (! cl.isAnnotationPresent(Alternative.class))
        throw new ConfigException(L.l("'{0}' is an invalid alternative because it is missing an @Alternative.",
                                      cl.getName()));
      
      _alternativesList.add(cl);
    }
  }

  static interface ExcludePredicate
  {
    boolean isMatch();
  }

  static class IfClassNotAvailable implements ExcludePredicate
  {
    private String _name;

    @Configured
    public void setName(String name)
    {
      _name = name;
    }

    @Override
    public boolean isMatch()
    {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      try {
        loader.loadClass(_name);
        return false;
      } catch (Exception e) {
        return true;
      }
    }
  }

  static class IfClassAvailable implements ExcludePredicate
  {
    private String _name;

    @Configured
    public void setName(String name)
    {
      _name = name;
    }

    @Override
    public boolean isMatch()
    {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      try {
        loader.loadClass(_name);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }

  static class IfSystemProperty implements ExcludePredicate
  {
    private String _name;
    private String _value;

    public String getName()
    {
      return _name;
    }

    @Configured
    public void setName(String name)
    {
      _name = name;
    }

    @Configured
    public void setValue(String value)
    {
      _value = value;
    }

    @Override
    public boolean isMatch()
    {
      String property = System.getProperty(_name);

      boolean isMatch;

      if (property == null)
        isMatch = false;
      else if (property.equals(_value))
        isMatch = true;
      else if (_value == null &&  "true".equals(property))
        isMatch = true;
      else
        isMatch = false;

      return isMatch;
    }
  }

  static class Exclude {
    private Pattern _pattern;
    private ArrayList<ExcludePredicate> _predicates =
      new ArrayList<>();

    @Configured
    public void setName(String name)
    {
      char []chars = name.toCharArray();
      StringBuilder regex = new StringBuilder();

      char xC = 0;
      for (char c : chars) {
        if (c == '.')
          regex.append("\\.");
        else if (c == '*' && xC == '*')
          regex.append("\\.");
        else if (c == '*')
          regex.append("[0-9a-zA-Z_");
        else
          regex.append(c);

        xC = c;
      }

      regex.append("]*");

      Pattern pattern = Pattern.compile(regex.toString());

      _pattern = pattern;
    }

    public boolean isMatch(String className) {
      if (! _pattern.matcher(className).matches())
        return false;

      for (ExcludePredicate predicate : _predicates) {
        if (! predicate.isMatch())
          return false;
      }

      return true;
    }

    @Configured
    public void addIfClassNotAvailable(IfClassNotAvailable predicate) {
      _predicates.add(predicate);
    }

    @Configured
    public void addIfClassAvailable(IfClassAvailable predicate) {
      _predicates.add(predicate);
    }

    @Configured
    public void addIfSystemProperty(IfSystemProperty predicate) {
      _predicates.add(predicate);
    }
  }
}
