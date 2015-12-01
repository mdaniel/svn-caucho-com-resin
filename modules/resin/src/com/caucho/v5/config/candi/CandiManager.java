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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.New;
import javax.enterprise.inject.ResolutionException;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;

import com.caucho.v5.amp.cdi.CdiProducerBaratine;
import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.CauchoDeployment;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configured;
import com.caucho.v5.config.ContextDependent;
import com.caucho.v5.config.LineConfigException;
import com.caucho.v5.config.ModulePrivate;
import com.caucho.v5.config.ModulePrivateLiteral;
import com.caucho.v5.config.annotation.ProxyProduces;
import com.caucho.v5.config.custom.CookieCustomBean;
import com.caucho.v5.config.custom.CookieCustomBeanLiteral;
import com.caucho.v5.config.custom.ExtensionCustomBean;
import com.caucho.v5.config.el.CandiElResolver;
import com.caucho.v5.config.el.CandiExpressionFactory;
import com.caucho.v5.config.event.EventManager;
import com.caucho.v5.config.extension.ExtensionManager;
import com.caucho.v5.config.inject.InjectContext;
import com.caucho.v5.config.inject.InjectManager;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.reflect.AnnotatedTypeImpl;
import com.caucho.v5.config.reflect.AnnotatedTypeUtil;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.config.reflect.BaseTypeFactory;
import com.caucho.v5.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.v5.config.scope.ApplicationContext;
import com.caucho.v5.config.scope.DependentContext;
import com.caucho.v5.config.scope.ErrorContext;
import com.caucho.v5.config.scope.SingletonScope;
import com.caucho.v5.config.xml.ConfigXml;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.loader.EnvironmentApply;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.EnvironmentListener;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.IoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;

/**
 * The CDI container for a given environment.
 */
@ModulePrivate
@CauchoBean
@SuppressWarnings("serial")
public class CandiManager
  implements BeanManager, EnvironmentListener,
             Serializable, HandleAware
{
  private static final L10N L = new L10N(CandiManager.class);
  private static final Logger log
    = Logger.getLogger(CandiManager.class.getName());

  private static final EnvironmentLocal<CandiManager> _localContainer
    = new EnvironmentLocal<>();

  static final int DEFAULT_PRIORITY = 0;

  static final Annotation []DEFAULT_ANN
    = DefaultLiteral.DEFAULT_ANN_LIST;

  private static final String []FORBIDDEN_ANNOTATIONS = {
    "javax.persistence.Entity",
    /*
    "javax.ejb.Stateful",
    "javax.ejb.Stateless",
    "javax.ejb.Singleton",
    "javax.ejb.MessageDriven"
    */
  };

  private static final String []FORBIDDEN_CLASSES = {
    //"javax.servlet.Servlet",
    //"javax.servlet.Filter",
    //"javax.servlet.ServletContextListener",
    "javax.servlet.http.HttpSessionListener",
    "javax.servlet.ServletRequestListener",
    "javax.ejb.EnterpriseBean",
    "javax.faces.component.UIComponent",
    //"javax.enterprise.inject.spi.Extension",
  };

  private static final Class<? extends Annotation> []_forbiddenAnnotations;
  private static final Class<?> []_forbiddenClasses;

  private static final ClassLoader _systemClassLoader;

  private final String _id;

  private final CandiManager _parent;

  private EnvironmentClassLoader _classLoader;
  private ClassLoader _jndiClassLoader;
  private boolean _isChildManager;

  private final ScanListenerInject _scanManager;
  private final ExtensionManager _extensionManager;
  private EventManager _eventManager;

  private AtomicLong _version = new AtomicLong();

  private HashMap<Class<?>,Class<?>> _specializedMap
    = new HashMap<>();

  private HashMap<Class<?>,Integer> _deploymentMap
    = new HashMap<>();

  private BaseTypeFactory _baseTypeFactory = new BaseTypeFactory();

  private HashMap<Class<?>,InjectionPointHandler> _injectionMap
    = new HashMap<>();

  //
  // self configuration
  //

  private ConcurrentHashMap<Class<?>,ArrayList<TypedBean>> _selfBeanMap
    = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String,ArrayList<Bean<?>>> _selfNamedBeanMap
    = new ConcurrentHashMap<>();

  private HashMap<String,Bean<?>> _selfPassivationBeanMap
    = new HashMap<>();

  private HashSet<TypedBean> _selfProxySet
    = new HashSet<>();

  //
  // combined visibility configuration
  //
    
  private BeanManagerBase _beanManagerApplication;
  
  private HashMap<Path,BeanManagerBase> _beanManagerMap = new HashMap<>();

  private ConcurrentHashMap<String,ArrayList<Bean<?>>> _namedBeanMap
    = new ConcurrentHashMap<>();

  private HashMap<Type,Bean<?>> _newBeanMap
    = new HashMap<>();

  private WebComponent _proxySet;

  private HashSet<Class<? extends Annotation>> _qualifierSet
    = new HashSet<>();

  private HashSet<Class<? extends Annotation>> _scopeTypeSet
    = new HashSet<>();

  private HashSet<Class<? extends Annotation>> _normalScopeSet
    = new HashSet<>();

  private HashSet<Class<? extends Annotation>> _passivatingScopeSet
    = new HashSet<>();

  private HashMap<Class<? extends Annotation>, Set<Annotation>> _stereotypeMap
    = new HashMap<>();

  private HashMap<Class<?>,Context> _contextMap
    = new HashMap<>();

  // private InterceptorsBuilder _interceptorsBuilder;

  /*  
  private List<InterceptorEntry<?>> _interceptorList
    = new ArrayList<>();
    */

  // private DecoratorsBuilder _decoratorsBuilder;

  // private List<DecoratorEntry<?>> _decoratorList = new ArrayList<>();

  private HashSet<Bean<?>> _beanSet = new HashSet<>();

  private boolean _isEnableAutoUpdate = true;
  private boolean _isUpdateNeeded = true;
  private boolean _isAfterValidationNeeded = true;

  private ConcurrentHashMap<Path, List<Path>> _beansXMLOverrides
    = new ConcurrentHashMap<>();

  private ArrayList<AnnotatedType<?>> _pendingAnnotatedTypes
    = new ArrayList<>();

  private ArrayList<AnnotatedType<?>> _pendingExtensions
    = new ArrayList<>();

  private ArrayList<BeanBase<?>> _pendingBindList
    = new ArrayList<>();

  private ArrayList<Bean<?>> _pendingValidationBeans
    = new ArrayList<>();

  private ArrayList<Bean<?>> _pendingServiceList
    = new ArrayList<>();

  private ArrayList<ConfigProgram> _globalProgram
    = new ArrayList<>();

  private ArrayList<SyntheticAnnotatedType> _pendingSyntheticTypes
    = new ArrayList<>();

  private ArrayList<SyntheticAnnotatedType> _syntheticTypes
    = new ArrayList<>();

  private ConcurrentHashMap<String,ReferenceFactory<?>> _namedRefFactoryMap
  = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Member,AtomicBoolean> _staticMemberMap
  = new ConcurrentHashMap<>();

  private ThreadLocal<CreationalContextImpl<?>> _proxyThreadLocal
    = new ThreadLocal<>();

  private ConcurrentHashMap<Class<?>,ManagedBeanImpl<?>> _transientMap
    = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Long,InjectionTarget<?>> _injectionTargetCustomMap
    = new ConcurrentHashMap<>();

  private boolean _isStarted;
  private boolean _isBeforeBeanDiscoveryFired;
  private boolean _isAfterTypeDiscoverFired;
  private boolean _isAfterBeanDiscoveryComplete;

  //private AlternativesBuilder _alternativesBuilder = new AlternativesBuilder();
  //private List<Class<?>> _appAlternatives;

  // XXX: needs to be a local resolver
  private ELResolver _elResolver = new CandiElResolver(this);

  private final DependentContext _dependentContext = new DependentContext();
  private SingletonScope _singletonScope;
  private ApplicationContext _applicationScope;
  // private final XmlStandardPlugin _xmlExtension;

  private RuntimeException _configException;

  private final AtomicLong _xmlCookieSequence
    = new AtomicLong(CurrentTime.getCurrentTime());
  
  private ExtensionCustomBean _extCustomBean;

  private Object _serializationHandle;

  protected CandiManager(String id,
                          CandiManager parent,
                          EnvironmentClassLoader loader,
                          boolean isSetLocal)
  {
    _id = id;

    _classLoader = loader;

    _parent = parent;
    
    _extensionManager = new ExtensionManager(this);
    _scanManager = new ScanListenerInject(this);
    // _xmlExtension = new XmlStandardPlugin(this);
    _eventManager = createEventManager();
    _beanManagerApplication = new BeanManagerBase(this, null);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (isSetLocal) {
        _localContainer.set(this, _classLoader);

        /*
        if (_parent == null) {
          _localContainer.setGlobal(this);
        }
        */
      }

      if (_classLoader != null) {
        _classLoader.getNewTempClassLoader();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected EventManager createEventManager()
  {
    return new EventManager(this);
  }

  private void init(boolean isSetLocal)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      _singletonScope = new SingletonScope();
      _applicationScope = new ApplicationContext();

      initScopes();

      _deploymentMap.put(CauchoDeployment.class, 0);
      // DEFAULT_PRIORITY
      _deploymentMap.put(Configured.class, 2);

      BeanBuilder<CandiManager> factory = createBeanBuilder(CandiManager.class);
      // factory.deployment(Standard.class);
      factory.type(CandiManager.class);
      factory.type(BeanManager.class);
      factory.annotation(ModulePrivateLiteral.create());
      addBean(factory.singletonSimple(this));

      // ioc/0162
      addBean(new InjectionPointStandardBean());

      _extCustomBean = createExtensionCustomBean();

      initManagerBeans();

      if (_classLoader != null && isSetLocal) {
        // _classLoader.addScanListener(this);
        _classLoader.addScanListener(_scanManager);
      }
      
      Environment.addEnvironmentListener(this, _classLoader);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public String getId()
  {
    return _id;
  }
  
  protected void initScopes()
  {
    addContext(_applicationScope);
    addContext(_singletonScope);
    addContext(_dependentContext);
  }
  
  protected ExtensionCustomBean createExtensionCustomBean()
  {
    return new ExtensionCustomBean(this);
  }
  
  public void scanRoot(ClassLoader loader)
  {
    _scanManager.scanRoot(loader);
  }
  
  protected void initManagerBeans()
  {
    addExtension(_extCustomBean);
    
    // getExtensionManager().addExtension(new CdiExtensionBaratine());
    addManagedBeanDiscover(createManagedBean(CdiProducerBaratine.class));
  }

  protected void addInjection(String className, InjectionPointHandler handler)
    throws ClassNotFoundException
  {
    addInjection(Class.forName(className), handler);
  }

  protected void addInjection(Class<?> cl, InjectionPointHandler handler)
  {
    _injectionMap.put(cl, handler);
  }

  /**
   * Returns the modification version.
   */
  public long getVersion()
  {
    return _version.get();
  }

  public ScanListenerInject getScanManager()
  {
    return _scanManager;
  }

  public void setIsCustomExtension(boolean isCustom)
  {
    getScanManager().setIsCustomExtension(isCustom);
  }

  @Module
  public EventManager getEventManager()
  {
    return _eventManager;
  }

  @Module
  public ExtensionManager getExtensionManager()
  {
    return _extensionManager;
  }

  protected void addContext(String className)
  {
    try {
      Class cl = Class.forName(className);

      addContext(cl);
    } catch (NoClassDefFoundError e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  protected void addContext(Class<? extends Context> contextClass)
  {
    try {
      Context context = contextClass.newInstance();

      addContext(context);
    } catch (NoClassDefFoundError e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static CandiManager getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static CandiManager getCurrent(ClassLoader loader)
  {
    return _localContainer.get(loader);
  }

  /**
   * Returns the current active container.
   */
  public static CandiManager create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current active container.
   */
  public static CandiManager create(ClassLoader loader)
  {
    if (loader == null) {
      loader = _systemClassLoader;
    }

    CandiManager manager = null;

    manager = _localContainer.getLevel(loader);

    if (manager != null) {
      return manager;
    }

    EnvironmentClassLoader envLoader
      = Environment.getEnvironmentClassLoader(loader);

    // ejb doesn't create a new InjectManager even though it's a new
    // environment
    // XXX: yes it does, because of the SessionContext
    // ejb/2016 vs ejb/12h0
    /*
    if (envLoader != null
        && Boolean.FALSE.equals(envLoader.getAttribute("caucho.inject"))) {
      manager = create(envLoader.getParent());
      
      if (manager != null)
        return manager;
    }
    */

    String id;

    if (envLoader != null)
      id = envLoader.getId();
    else
      id = "";

    CandiManager parent = null;

    if (envLoader != null && envLoader != _systemClassLoader) {
      // parent = create(envLoader.getParent());
      parent = _localContainer.getLevel(envLoader.getParent());
    }

    synchronized (_localContainer) {
      manager = _localContainer.getLevel(envLoader);

      if (manager != null) {
        return manager;
      }

      manager = newInjectManager(id, parent, envLoader, true);
    }

    manager.init(true);

    return manager;
  }
  
  private static CandiManager newInjectManager(String id,
                                                CandiManager parent,
                                                EnvironmentClassLoader loader,
                                                boolean isSetLocal)
  {
    try {
      Class<?> cl = Class.forName("com.caucho.v5.config.candi.CandiManagerResin");
      
      Constructor<?> ctor = cl.getConstructor(String.class,
                                              CandiManager.class,
                                              EnvironmentClassLoader.class,
                                              boolean.class);
      
      return (CandiManager) ctor.newInstance(id, parent, loader, isSetLocal);
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    return new CandiManager(id, parent, loader, isSetLocal);
  }

  /**
   * Returns the current active container.
   */
  /*
  public InjectManager createParent(String prefix)
  {
    _parent = new InjectManager(prefix + _id,
                                _parent,
                                _classLoader,
                                false);
    _parent.init(false);

    return _parent;
  }
  */

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public ClassLoader getJndiClassLoader()
  {
    return _jndiClassLoader;
  }

  public boolean isChildManager()
  {
    return _isChildManager;
  }

  public void setJndiClassLoader(ClassLoader loader)
  {
    if (_parent == null)
      throw new IllegalStateException();

    _isChildManager = true;
    _jndiClassLoader = loader;
  }

  public CandiManager getParent()
  {
    return _parent;
  }

  public ApplicationContext getApplicationScope()
  {
    return _applicationScope;
  }

  /*
  public void setParent(InjectManager parent)
  {
    _parent = parent;
  }
  */

  public void addBeansXmlOverride(Path path, Path beansXmlPath)
  {
    if (path == null)
      throw new NullPointerException();

    List<Path> beansXmlPaths = _beansXMLOverrides.get(path);

    if (beansXmlPaths == null) {
      beansXmlPaths = new ArrayList<Path>();
    }

    beansXmlPaths.add(beansXmlPath);

    _beansXMLOverrides.put(path, beansXmlPaths);
  }

  public List<Path> getBeansXmlOverride(Path path)
  {
    return _beansXMLOverrides.get(path);
  }

  public void setEnableAutoUpdate(boolean isEnable)
  {
    _isEnableAutoUpdate = isEnable;
  }

  public void setDeploymentTypes(BeanManagerBase beanManager,
                                 ArrayList<Class<?>> deploymentList)
  {
//    _deploymentMap.clear();

//    _deploymentMap.put(CauchoDeployment.class, 0);

    if (! _deploymentMap.containsKey(CauchoDeployment.class)) {
      _deploymentMap.put(CauchoDeployment.class, 0);
    }
    // DEFAULT_PRIORITY

    final int priority = DEFAULT_PRIORITY + 1;

/*
    if (! deploymentList.contains(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }
*/

    for (int i = deploymentList.size() - 1; i >= 0; i--) {
      _deploymentMap.put(deploymentList.get(i), priority);
    }

    if (_deploymentMap.containsKey(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }

    for (Class<?> type : deploymentList) {
      // _alternativesBuilder.addRegistered(type);
      
      beanManager.addAlternative(type);
    }
  }

  /**
   * Adds the bean to the named bean map.
   */
  private void addBeanByName(String name, Bean<?> bean)
  {
    ArrayList<Bean<?>> beanList = _selfNamedBeanMap.get(name);

    if (beanList == null) {
      beanList = new ArrayList<Bean<?>>();
      _selfNamedBeanMap.put(name, beanList);
    }
    else if (bean.isAlternative()) {
    }
    else if (_specializedMap.get(bean.getBeanClass()) != null) {
    }
    else {
      // ioc/0n18 vs ioc/0g30
      for (Bean<?> testBean : beanList) {
        if (testBean.isAlternative()) {
        }
        else if (bean.isAlternative()) {
        }
        else if (bean.getBeanClass().isAnnotationPresent(Specializes.class)
                 && testBean.getBeanClass().isAssignableFrom(bean.getBeanClass())) {
        }
        else if (testBean.getBeanClass().isAnnotationPresent(Specializes.class)
                  && bean.getBeanClass().isAssignableFrom(testBean.getBeanClass())) {
        }
        else if ((bean instanceof IntrospectedBeanBase<?>)
                 && ((IntrospectedBeanBase<?>) bean).getAnnotated().isAnnotationPresent(Specializes.class)) {
          // ioc/07a2
        }
        else if ((testBean instanceof IntrospectedBeanBase<?>)
                 && ((IntrospectedBeanBase<?>) testBean).getAnnotated().isAnnotationPresent(Specializes.class)) {
        }
        else {
          throw new DeploymentException(L.l("@Named('{0}') is a duplicate name for\n  {1}\n  {2}",
                                            name, testBean, bean));
        }
      }
    }

    beanList.add(bean);

    _namedBeanMap.remove(name);

    // ioc/0g31
    int p = name.indexOf('.');
    if (p > 0) {
      addBeanByName(name.substring(0, p), bean);
    }
  }

  /**
   * Adds a bean by the interface type
   *
   * @param type the interface type to expose the component
   * @param bean the component to register
   */
  private void addBeanByType(Type type,
                             Annotated annotated,
                             Bean<?> bean)
  {
    if (type == null) {
      return;
    }

    BaseType baseType = createSourceBaseType(type);

    addBeanByType(baseType, annotated, bean);
  }

  private void addBeanByType(BaseType type,
                             Annotated annotated,
                             Bean<?> bean)
  {
    if (type == null) {
      return;
    }

    if (isSpecialized(bean.getBeanClass())) {
      return;
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(bean + "(" + type + ") added to " + this);

    Class<?> rawType = type.getRawClass();
    
    ArrayList<TypedBean> beanSet = _selfBeanMap.get(rawType);

    if (beanSet == null) {
      beanSet = new ArrayList<TypedBean>();
      _selfBeanMap.put(rawType, beanSet);
    }
    
    // BeanManager beanManager = getBeanManager(bean);
    // XXX: beanManager.removeBean(rawType);

    TypedBean typedBean = new TypedBean(type, annotated, bean);

    if (! beanSet.contains(typedBean)) {
      beanSet.add(typedBean);
    }

    if (annotated != null && annotated.isAnnotationPresent(ProxyProduces.class)) {
      if (! _selfProxySet.contains(typedBean)) {
        _selfProxySet.add(typedBean);
      }

      _proxySet = null;
    }
  }

  /**
   * Finds a component by its component name.
   */
  protected ArrayList<Bean<?>> findByName(String name)
  {
    // #3334 - shutdown timing issues
    ConcurrentHashMap<String,ArrayList<Bean<?>>> namedBeanMap = _namedBeanMap;

    if (namedBeanMap == null) {
      return null;
    }

    ArrayList<Bean<?>> beanList = _namedBeanMap.get(name);

    if (beanList == null) {
      beanList = new ArrayList<Bean<?>>();

      if (_classLoader != null) {
        _classLoader.applyVisible(new FillByName(name, beanList));
      }

      // fillByName(name, beanList);

      // ioc/0680 
      /*
      for (int i = beanList.size() - 1; i >= 0; i--) {
        if (getDeploymentPriority(beanList.get(i)) < 0) {
          beanList.remove(i);
        }
      }
      */

      _namedBeanMap.put(name, beanList);
    }

    return beanList;
  }

  private void fillByName(String name, ArrayList<Bean<?>> beanList)
  {
    updatePending();
    
    ArrayList<Bean<?>> localBeans = _selfNamedBeanMap.get(name);
    
    if (localBeans != null) {
      for (Bean<?> bean : localBeans) {
        /*
        if (getDeploymentPriority(bean) < 0)
          continue;
          */

        // ioc/0g20
        if (bean.isAlternative() && ! isEnabled(bean))
          continue;

        if (_specializedMap.containsKey(bean.getBeanClass()))
          continue;

        if (! beanList.contains(bean)) {
          beanList.add(bean);
        }
      }
    }
  }

  //
  // javax.enterprise.context.Conversation
  //

  public Conversation createConversation()
  {
    return (Conversation) _contextMap.get(ConversationScoped.class);
  }
  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> T createTransientObject(Class<T> type)
  {
    ManagedBeanImpl<T> bean = createCachedManagedBean(type);

    validate(bean);

    // server/10gn
    //return factory.create(new ConfigContext());
    InjectionTarget<T> injectionTarget = bean.getInjectionTarget();

    CreationalContext<T> env = new OwnerCreationalContext<T>(bean);

    T instance = injectionTarget.produce(env);
    injectionTarget.inject(instance, env);
    // jsp/1o60
    injectionTarget.postConstruct(instance);

    return instance;
  }

  public <T> Bean<T> createTransientManagedBean(Class<T> type)
  {
    Bean<T> bean = createCachedManagedBean(type);

    validate(bean);

    return bean;
  }

  public <T> Bean<T> createTransientBean(Class<T> cl)
  {
    AnnotatedType<T> type = createAnnotatedType(cl);
    BeanAttributes<T> attributes = createBeanAttributes(type);
    InjectionTargetFactory<T> factory = getInjectionTargetFactory(type);
    Bean<T> bean = createBean(attributes, cl, factory);
    
    return bean;
  }

  public <T> Supplier<T> createBeanSupplier(Class<T> type)
  {
    Bean<T> bean = createCachedManagedBean(type);

    validate(bean);

    return new BeanSupplier<T>(bean);
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  private <T> ManagedBeanImpl<T> createCachedManagedBean(Class<T> type)
  {
    ManagedBeanImpl<T> bean = (ManagedBeanImpl<T>) _transientMap.get(type);

    if (bean == null) {
      bean = createManagedBean(type);

      validate(bean);

      _transientMap.putIfAbsent(type, bean);

      bean = (ManagedBeanImpl<T>) _transientMap.get(type);
    }

    return bean;
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanBuilder<T> createBeanFactory(ManagedBeanImpl<T> managedBean)
  {
    return new BeanBuilder<T>(managedBean);
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanBuilder<T> createBeanBuilder(Class<T> type)
  {
    ManagedBeanImpl<T> managedBean = createManagedBean(type);

    if (managedBean != null)
      return createBeanFactory(managedBean);
    else
      return null;
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with CDI.
   */
  public <T> BeanBuilder<T> createBeanFactory(AnnotatedType<T> type)
  {
    return createBeanFactory(createManagedBean(type));
  }

  public <T> Bean<T> addSingleton(T obj)
  {
    BeanBuilder<T> builder = createBeanBuilder((Class<T>) obj.getClass());

    Bean<T> bean = builder.singleton(obj);

    addBeanDiscover(bean);

    return bean;
  }

  @Override
  public boolean areInterceptorBindingsEquivalent(Annotation ib1,
                                                  Annotation ib2)
  {
    return areAnnotationsEquivalent(ib1, ib2);
  }

  @Override
  public boolean areQualifiersEquivalent(Annotation q1, Annotation q2)
  {
    return areAnnotationsEquivalent(q1, q2);
  }

  private boolean areAnnotationsEquivalent(Annotation a1, Annotation a2) {
    if (a1 == a2)
      return true;

    QualifierBinding binding1 = new QualifierBinding(a1);

    boolean isEquivalent = binding1.isMatch(a2);

    return isEquivalent;

  }
//
  // enabled deployment types, scopes, and qualifiers
  //

  @Module
  public void addScope(Class<? extends Annotation> scopeType,
                       boolean isNormal,
                       boolean isPassivating)
  {
    if (isPassivating && ! isNormal)
      throw new DefinitionException(L.l("@{0} must be 'normal' because it's using 'passivating'",
                                        scopeType.getName()));

    _scopeTypeSet.add(scopeType);

    if (isNormal)
      _normalScopeSet.add(scopeType);

    if (isPassivating)
      _passivatingScopeSet.add(scopeType);

    if (isNormal) {
      // TCK - force validation of all methods
      _scanManager.setIsCustomExtension(true);
    }
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isScope(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Scope.class)
            || annotationType.isAnnotationPresent(NormalScope.class)
            || _scopeTypeSet.contains(annotationType));
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isNormalScope(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(NormalScope.class)
            || _normalScopeSet.contains(annotationType));
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
  {
    NormalScope scope = annotationType.getAnnotation(NormalScope.class);

    if (scope != null)
      return scope.passivating();

    return _passivatingScopeSet.contains(annotationType);
  }

  @Module
  public void addQualifier(Class<? extends Annotation> qualifier)
  {
    _qualifierSet.add(qualifier);
  }

  @Module
  public void addQualifier(AnnotatedType<? extends Annotation> qualifier)
  {
    _qualifierSet.add(qualifier.getJavaClass());
  }

  /**
   * Tests if an annotation is an enabled binding type
   */
  @Override
  public boolean isQualifier(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Qualifier.class)
            || _qualifierSet.contains(annotationType));
  }

  /**
   * Tests if an annotation is an enabled interceptor binding type
   */
  @Override
  public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
  {
    return annotationType.isAnnotationPresent(InterceptorBinding.class);
  }

  /**
   * Returns the bindings for an interceptor binding type
   */
  @Override
  public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType)
  {
    LinkedHashSet<Annotation> annSet = new LinkedHashSet<Annotation>();

    for (Annotation ann : bindingType.getAnnotations()) {
      annSet.add(ann);
    }

    return annSet;
  }

  @Override
  public int getInterceptorBindingHashCode(Annotation binding)
  {
    return getAnnotationHashCode(binding);
  }

  @Override
  public int getQualifierHashCode(Annotation qualififer)
  {
    return getAnnotationHashCode(qualififer);
  }

  public int getAnnotationHashCode(Annotation binding)
  {
    Method []methods = binding.annotationType().getDeclaredMethods();

    int hashCode = 0;

    for (Method method : methods) {
      if (method.getParameterTypes().length > 0
          || Annotation.class == method.getDeclaringClass()
          || Object.class == method.getDeclaringClass())
        continue;

      if (method.isAnnotationPresent(Nonbinding.class))
        continue;

      try {
        final Object value = method.invoke(binding);
        int valueHashCode;
        if (value.getClass().isArray())
          valueHashCode = Arrays.hashCode((Object[]) value);
        else
          valueHashCode = value.hashCode();

        hashCode += (127 * method.getName().hashCode()) ^ valueHashCode;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return hashCode;
  }

  @Module
  public void addStereotype(Class<? extends Annotation> annotationType,
                            Annotation []annotations)
  {
    LinkedHashSet<Annotation> annSet = new LinkedHashSet<Annotation>();

    for (Annotation ann : annotations)
      annSet.add(ann);

    _stereotypeMap.put(annotationType, annSet);
  }

  /**
   * Tests if an annotation is an enabled stereotype.
   */
  @Override
  public boolean isStereotype(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Stereotype.class)
            || _stereotypeMap.get(annotationType) != null);
  }

  /**
   * Returns the annotations associated with a stereotype
   */
  @Override
  public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
  {
    Set<Annotation> mapAnnSet = _stereotypeMap.get(stereotype);

    if (mapAnnSet != null)
      return mapAnnSet;

    if (! stereotype.isAnnotationPresent(Stereotype.class))
      return null;

    LinkedHashMap<Class<?>, Annotation> annMap
      = new LinkedHashMap<Class<?>, Annotation>();

    addStereotypeDefinitions(annMap, stereotype);

    mapAnnSet = new LinkedHashSet<Annotation>(annMap.values());

    _stereotypeMap.put(stereotype, mapAnnSet);

    return mapAnnSet;
  }

  private void addStereotypeDefinitions(Map<Class<?>,Annotation> annMap,
                                        Class<? extends Annotation> stereotype)
  {
    for (Annotation ann : stereotype.getAnnotations()) {
      if (annMap.get(ann.annotationType()) == null)
        annMap.put(ann.annotationType(), ann);
    }

    for (Annotation ann : stereotype.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      if (annType.isAnnotationPresent(Stereotype.class)) {
        addStereotypeDefinitions(annMap, annType);
      }
    }
  }

  //
  // bean resolution and instantiation
  //

  public BaseTypeFactory getTypeFactory()
  {
    return _baseTypeFactory;
  }
  
  /**
   * Creates a BaseType from a Type used as a target, for example
   * an injection point.
   */
  public BaseType createTargetBaseType(Type type)
  {
    return _baseTypeFactory.createForTarget(type);
  }

  /**
   * Creates a BaseType from a Type used as a source, for example a Bean.
   */
  public BaseType createSourceBaseType(Type type)
  {
    return _baseTypeFactory.createForSource(type);
  }

  /**
   * Creates an annotated type.
   */
  @Override
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> cl)
  {
    Objects.requireNonNull(cl);

    AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(cl);

    // TCK:
    // return getExtensionManager().processAnnotatedType(annType);

    return annType;
  }

  @Override
  public <T> Bean<T> createBean(BeanAttributes<T> attributes,
                                Class<T> beanClass,
                                InjectionTargetFactory<T> factory)
  {
    Bean<T> bean;

    if (attributes.getStereotypes().contains(javax.decorator.Decorator.class)) {
      bean = createDecoratorBean(attributes, beanClass, factory);
    } else {
      AnnotatedType type = createAnnotatedType(beanClass);

      Bean<T> cdiBean = createManagedBean(type, attributes);

      bean = new FactoryInjectedBean<T>(cdiBean, factory);
    }

    return bean;
  }

  private <T> Bean<T> createDecoratorBean(BeanAttributes<T> attributes,
                                          Class<T> type,
                                          InjectionTargetFactory<T> factory)
  {
    return DecoratorBean.create(this, attributes, type, factory);
  }

    @Override
  public <T,X> Bean<T> createBean(BeanAttributes<T> attributes,
                                  Class<X> beanClass,
                                  ProducerFactory<X> factory)
  {
    return new BeanSynthetic<T,X>(attributes, beanClass, factory);
    
    /*
    BeanAttributesImpl beanAttributes = (BeanAttributesImpl) attributes;
    Annotated annotated = beanAttributes.getAnnotated();
    
    Bean cdiBean;

    if (annotated instanceof AnnotatedField) {
      AnnotatedField producesField = (AnnotatedField) annotated;
      AnnotatedType type = createAnnotatedType(beanClassFactory);
      Bean producesBean = createManagedBean(type);
      ProducesBuilder builder = new ProducesBuilder(this);
      cdiBean = builder.createProducesField(producesBean, type, producesField);
    }
    else if (annotated instanceof AnnotatedMethod) {
      AnnotatedMethod producesMethod = (AnnotatedMethod) annotated;
      AnnotatedType type = createAnnotatedType(beanClassFactory);
      Bean producesBean = createManagedBean(type);

      ProducesBuilder builder = new ProducesBuilder(this);

      Annotation []qualifiers = builder.getQualifiers(producesMethod);
      AnnotatedMethod disposesMethod
        = builder.findDisposesMethod(type,
                                     producesMethod.getBaseType(),
                                     qualifiers);

      cdiBean = builder.createProducesMethod(producesBean,
                                       type,
                                       producesMethod,
                                       disposesMethod);
    }
    else {
      throw new UnsupportedOperationException();
    }

    Bean factoryProducesBean
      = new FactoryProducedBean(cdiBean, factory);
    
    //return factory.createProducer(beanFactory);
    
    return factoryProducesBean;
    */
  }

  /**
   * Creates an injection target
   */
  @Override
  public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
  {
    InjectionTargetBuilder<T> target = new InjectionTargetBuilder<>(this, type);

    target.validate();

    // ioc/0p14 - createInjectionTarget doesn't trigger the event, the
    // initial scan does
    // return getExtensionManager().processInjectionTarget(target, type);

    return target;
  }

  @Override
  public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> member)
  {
    if (member instanceof AnnotatedField) {
    }
    else if (member instanceof AnnotatedMethod) {
    }
    else {
      throw new IllegalArgumentException(L.l("AnnotatedField or AnnotatedMethod expected. '{0}' received.", member));
    }

    BeanAttributes attributes = new BeanAttributesImpl(member, this);

    return attributes;
  }

  @Override
  public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type)
  {
    BeanAttributes attributes = new BeanAttributesImpl(type, this);

    return attributes;
  }

  /**
   * Creates an injection target
   */
  public <T> InjectionTarget<T> discoverInjectionTarget(AnnotatedType<T> type)
  {
    InjectionTarget<T> target = createInjectionTarget(type);

    return getExtensionManager().processInjectionTarget(target, type);
  }

  /**
   * Creates a managed bean.
   */
  public <T> InjectionTarget<T> createInjectionTarget(Class<T> type)
  {
    // ioc/0062 (vs discover)
    try {
      AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(type);
      // special call from servlet, etc.
      return createInjectionTarget(annType);
    } catch (Exception e) {
      throw ConfigException.createConfig(e);
    }
  }

  /**
   * Creates a managed bean.
   */
  public <T> InjectionTarget<T> discoverInjectionTarget(Class<T> type)
  {
    //fireBeforeBeanDiscovery();

    try {
      AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(type);

      AnnotatedType<T> enhAnnType
        = getExtensionManager().processAnnotatedType(annType);

      if (enhAnnType != null)
        return discoverInjectionTarget(enhAnnType);
      else {
        // special call from servlet, etc.
        return discoverInjectionTarget(annType);
      }
    } catch (Exception e) {
      throw ConfigException.createConfig(e);
    }
  }

  public <T,X> void addObserver(ObserverMethod<T> observer,
                                AnnotatedMethod<X> method)
  {
    _extensionManager.processObserver(observer, method);

    getEventManager().addObserver(observer);
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(AnnotatedType<T> type)
  {
    if (type == null)
      throw new NullPointerException();

    BeanAttributes attributes = createBeanAttributes(type);

    ManagedBeanImpl<T> bean = createManagedBean(type, attributes);

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(AnnotatedType<T> type,
                                                  BeanAttributes<T> attributes)
  {
    return createManagedBean(type, attributes, getBeanManager());
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(AnnotatedType<T> type,
                                                  BeanAttributes<T> attributes,
                                                  BeanManagerBase beanManager)
  {
    Objects.requireNonNull(type);
    Objects.requireNonNull(attributes);
    Objects.requireNonNull(beanManager);

    ManagedBeanImpl<T> bean
      = new ManagedBeanImpl<>(this, beanManager, type, false, attributes);

    bean.introspect();

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(Class<T> cl)
  {
    Objects.requireNonNull(cl);

    AnnotatedType<T> type = createAnnotatedType(cl);

    return createManagedBean(type);
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> discoverManagedBean(Class<T> cl)
  {
    Objects.requireNonNull(cl);
    
    //fireBeforeBeanDiscovery();

    AnnotatedType<T> type = createAnnotatedType(cl);

    AnnotatedType<T> extType = getExtensionManager().processAnnotatedType(type);

    if (extType != null)
      return createManagedBean(extType);
    else
      return createManagedBean(type);
  }

  /**
   * Processes the discovered bean
   */
  public <T> void addBeanDiscover(Bean<T> bean)
  {
    Objects.requireNonNull(bean);
    
    addBeanDiscover(bean, (Annotated) null);
  }

  /**
   * Processes the discovered bean
   */
  public <T> void addBean(Bean<T> bean)
  {
    Objects.requireNonNull(bean);

    addBean(bean, (Annotated) null);
  }

  /**
   * Processes the discovered bean
   */
  @Module
  public <T> void addBeanDiscover(Bean<T> bean, Annotated ann)
  {
    //fireBeforeBeanDiscovery();

    if (ann != null) {
    }
    else if (bean instanceof BeanBase<?>) {
      ann = ((BeanBase<T>) bean).getAnnotatedType();
    }
    else if (bean instanceof InjectionPointStandardBean) {
      ann = ((InjectionPointStandardBean) bean).getAnnotated();
    }
    else if (bean instanceof InterceptorBean) {
      ann = ((InterceptorBean)bean).getAnnotatedType();
    }

    bean = processBean(bean, ann);

    addBean(bean, ann);
  }

  private <T> Bean<T> processBean(Bean<T> bean, Annotated ann)
  {
    if (bean instanceof ManagedBeanImpl<?>) {
      ManagedBeanImpl<T> managedBean = (ManagedBeanImpl<T>) bean;

      bean = getExtensionManager().processManagedBean(managedBean, ann);
    }
    else if (bean instanceof ProducesMethodBean<?,?>) {
      ProducesMethodBean<?,T> methodBean = (ProducesMethodBean<?,T>) bean;

      bean = getExtensionManager().processProducerMethod(methodBean);
    }
    else if (bean instanceof ProducesFieldBean<?,?>) {
      ProducesFieldBean<?,T> fieldBean = (ProducesFieldBean<?,T>) bean;

      bean = getExtensionManager().processProducerField(fieldBean);
    }
    else
      bean = getExtensionManager().processBean(bean, ann);

    return bean;
  }

  @Module
  public <T> void addBean(Bean<T> bean, Annotated ann)
  {
    addBeanImpl(bean, ann);
  }

  /**
   * Adds a new bean definition to the manager
   */
  public <T> void addBean(Bean<T> bean, ProcessBean<T> process)
  {
    bean = getExtensionManager().processBean(bean, process);

    if (bean != null) {
      addBeanImpl(bean, process.getAnnotated());
    }
  }

  /**
   * Adds a new bean definition to the manager
   */
  public <T> void addBeanImpl(Bean<T> bean, Annotated ann)
  {
    if (bean == null) {
      return;
    }

    if (_specializedMap.containsKey(bean.getBeanClass())) {
      return;
    }

    if (log.isLoggable(Level.FINER)) {
      if (isCauchoBean(bean)) {
        log.finest("add bean " + bean);
      } else {
        log.finer("add bean " + bean);
      }
    }

    _isAfterValidationNeeded = true;

    _version.incrementAndGet();

    if (bean instanceof Interceptor<?>) {
      AnnotatedType<?> annType = (AnnotatedType<?>) ann;
      
      addInterceptor(annType, (Interceptor<?>) bean, false);
      return;
    }
    else if (bean instanceof Decorator<?>) {
      AnnotatedType<?> annType = (AnnotatedType<?>) ann;
      
      addDecorator(annType, (Decorator<?>) bean);
      return;
    }

    // bean = new InjectBean<T>(bean, this);

    _beanSet.add(bean);

    for (Type type : bean.getTypes()) {
      addBeanByType(type, ann, bean);
    }

    if (bean.getName() != null) {
      addBeanByName(bean.getName(), bean);
    }

    // XXX: required for TCK, although we use lazily
    boolean isNullable = bean.isNullable();

    if (bean instanceof PassivationCapable) {
      PassivationCapable pass = (PassivationCapable) bean;

      if (pass.getId() != null)
        _selfPassivationBeanMap.put(pass.getId(), bean);
    }

    // server/1aj1
    clearBeanCache();

    beanPostBuild(bean);
  }
  
  private boolean isCauchoBean(Bean<?> bean)
  {
    if (bean instanceof AnnotatedBean) {
      AnnotatedBean annBean = (AnnotatedBean) bean;

      if (annBean.getAnnotated() != null)
        return annBean.getAnnotated().isAnnotationPresent(CauchoBean.class);
    }

    return false;
  }

  public BeanManagerBase getBeanManager()
  {
    return _beanManagerApplication;
  }

  public BeanManagerBase getBeanManager(Class<?> cl)
  {
    ScanClassInject scanClass = getScanManager().getScanClass(cl.getName());
    
    if (scanClass != null) {
      return scanClass.getBeanManager();
    }
    else {
      return _beanManagerApplication;
    }
  }
  
  protected BeanManagerBase getBeanManager(Bean<?> bean)
  {
    if (bean instanceof WithBeanManager) {
      WithBeanManager mBean = (WithBeanManager) bean;
      
      return mBean.getBeanManager();
    }
    else {
      return _beanManagerApplication;
    }
  }
  
  protected BeanManagerBase getBeanManager(InjectionPoint ij)
  {
    if (ij != null) {
      return getBeanManager(ij.getBean());
    }
    else {
      return _beanManagerApplication;
    }
  }

  public BeanManagerBase getBeanManager(Annotated type)
  {
    /*
    if (type.isAnnotationPresent(Priority.class)) {
      return getBeanManager();
    }
    else
    */
    if (type instanceof AnnotatedTypeImpl) {
      AnnotatedTypeImpl annType = (AnnotatedTypeImpl) type;
    
      return annType.getBeanManager();
    }
    else {
      return getBeanManager();
    }
  }

  public BeanManagerBase createBeanManager(Path root)
  {
    BeanManagerBase beanManager = _beanManagerMap.get(root);

    if (beanManager == null) {
      Path pwd = Vfs.getPwd();
      
      if (pwd.equals(root)) {
        beanManager = getBeanManager();
      }
      else {
        beanManager = new BeanManagerArchive(this, root);
      }
      
      _beanManagerMap.put(root, beanManager);
    }
    
    return beanManager;
  }

  public void addConfigPath(Path path)
  {
    BeansConfig beansConfig
    = new BeansConfig(this, getBeanManager());

    path.setUserPath(path.getURL());

    ConfigXml configXml = new ConfigXml();

    // beansConfig; //, BeansConfig.SCHEMA);
    configXml.configure(beansConfig, path, BeansConfig.SCHEMA);
  }

  public void buildInject(Class<?> rawType,
                          ArrayList<ConfigProgram> injectProgramList)
  {
  }

  protected void beanPostBuild(Bean<?> bean)
  {
  }

  void addGlobalProgram(ConfigProgram program)
  {
    if (program != null) {
      if (_isChildManager) {
        _parent.addGlobalProgram(program);
      }
      else {
        _globalProgram.add(program);
      }
    }
  }

  /**
   * Returns the bean definitions matching a given name
   *
   * @param name the name of the bean to match
   */
  @Override
  public Set<Bean<?>> getBeans(String name)
  {
    ArrayList<Bean<?>> beanList = findByName(name);

    if (beanList != null)
      return new LinkedHashSet<Bean<?>>(beanList);
    else
      return new LinkedHashSet<Bean<?>>();
  }

  @Module
  public AtomicBoolean getStaticMemberBoolean(Member member)
  {
    AtomicBoolean flag = _staticMemberMap.get(member);

    if (flag == null) {
      flag = new AtomicBoolean();

      _staticMemberMap.putIfAbsent(member, flag);

      flag = _staticMemberMap.get(member);
    }

    return flag;
  }

  @Module
  public ReferenceFactory<?> getReferenceFactory(String name)
  {
    // ioc/23n3
    update();

    ReferenceFactory<?> refFactory = _namedRefFactoryMap.get(name);

    if (refFactory == null) {
      Set<Bean<?>> beanSet = getBeans(name);

      if (beanSet != null && beanSet.size() > 0) {
        Bean<?> bean = resolve(beanSet);
        
        BeanManagerBase beanManager = getBeanManager(bean);

        // server/10sx
        if (name.equals(bean.getName())) {
          refFactory = beanManager.getReferenceFactory(bean);

          // ioc/0301
          if (refFactory instanceof ReferenceFactoryDependentImpl<?>)
            refFactory = new ReferenceFactoryDependentEl((ManagedBeanImpl<?>) bean);
        }
      }

      if (refFactory == null) {
        refFactory = new ReferenceFactoryUnresolved();
      }

      _namedRefFactoryMap.put(name, refFactory);
    }

    return refFactory;
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifiers required @Qualifier annotations
   */
  @Override
  public Set<Bean<?>> getBeans(Type type,
                               Annotation... qualifiers)
  {
    return getBeanManager().getBeans(type, qualifiers);
  }

  void filter(Set<Bean<?>> beans, Bean<?> context)
  {
    for (Iterator<Bean<?>> it = beans.iterator(); it.hasNext(); ) {
      Bean<?> bean = it.next();

      if (! bean.isAlternative()) {
        continue;
      }
      
      BeanManagerBase beanManager = getBeanManager(bean);
      
      if (beanManager.isAlternativeEnabled(bean)) {
        continue;
      }

      /*
      if (isAppScopeAlternative(bean))
        continue;
        */

      if (context == null) {
      }
      else if (isBeanArchiveAlternative(context, bean)) {
        continue;
      }

      it.remove();
    }
  }

  void filterInterceptors(List<Interceptor<?>> beans, Bean<?> context) {
    final Class contextClass;

    if (context == null) {
      contextClass = null;
    }
    else {
      contextClass = context.getBeanClass();
    }

    filterInterceptors(beans, contextClass);
  }

  public void filterInterceptors(List<Interceptor<?>> beans, Class context) {
    for (Iterator<Interceptor<?>> it = beans.iterator(); it.hasNext(); ) {
      Interceptor<?> interceptor = it.next();

      if (isAppScopeInterceptor(interceptor))
        continue;

      if (context == null) {
      }
      else if (isBeanArchiveInterceptor(context, interceptor))
        continue;

      it.remove();
    }
  }

  private boolean isAppScopeInterceptor(Interceptor<?> interceptor)
  {
    boolean isAppScope
      = (interceptor.getBeanClass().isAnnotationPresent(Priority.class));

    return isAppScope;
  }

  void filterDecorators(List<Decorator<?>> beans, Bean<?> context)
  {
    final Class contextClass;

    if (context == null) {
      contextClass = null;
    }
    else {
      contextClass = context.getBeanClass();
    }

    filterDecorators(beans, contextClass);
  }

  public void filterDecorators(List<Decorator<?>> beans, Class context)
  {
    for (Iterator<Decorator<?>> it = beans.iterator(); it.hasNext(); ) {
      Decorator<?> decorator = it.next();

      if (isAppScopeDecorator(decorator))
        continue;

      if (context == null) {
      }
      else if (isBeanArchiveDecorator(context, decorator))
        continue;

      it.remove();
    }
  }

  private boolean isAppScopeDecorator(Decorator<?> decorator)
  {
    boolean isAppScope
      = (decorator.getBeanClass().isAnnotationPresent(Priority.class));

    return isAppScope;
  }
  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifierSet required @Qualifier annotations
   */
  private Set<Bean<?>> getBeans(Type type,
                                Set<Annotation> qualifierSet)
  {
    Annotation []qualifiers = new Annotation[qualifierSet.size()];
    qualifierSet.toArray(qualifiers);

    return getBeans(type, qualifiers);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set<Bean<?>> resolveAllByType(Class<?> type)
  {
    Annotation []bindings = new Annotation[0];
    
    BeanManagerBase beanManager = getBeanManager();

    WebComponent component = beanManager.getWebComponent(createTargetBaseType(type));

    if (component != null) {
      Set<Bean<?>> beans = component.resolve(type, bindings);

      if (log.isLoggable(Level.FINEST))
        log.finest(this + " bind(" + getSimpleName(type)
                  + "," + toList(bindings) + ") -> " + beans);

      if (beans != null && beans.size() > 0)
        return beans;
    }

    if (_parent != null) {
      return _parent.resolveAllByType(type);
    }

    return null;
  }
  
  void addPendingValidationBean(Bean<?> bean)
  {
    _pendingValidationBeans.add(bean);
  }

  private WebComponent createComponent(String className,
                                       HashSet<TypedBean> typedBeans)
  {
    WebComponent beanSet = new WebComponent(this, getBeanManager(), className);
    
    BeanManagerBase beanManager = getBeanManager();

    for (TypedBean typedBean : typedBeans) {
      if (beanManager.getDeploymentPriority(typedBean.getBean()) < 0) {
        continue;
      }

      // _pendingValidationBeans.add(typedBean.getBean());

      beanSet.addComponent(typedBean.getType(),
                           typedBean.getAnnotated(),
                           typedBean.getBean());
    }

    return beanSet;
  }

  WebComponent getProxyComponent()
  {
    if (_proxySet == null && _selfProxySet != null) {
      _proxySet = createComponent(Object.class.getName(), _selfProxySet);
    }

    return _proxySet;
  }

  private void clearBeanCache()
  {
    _namedRefFactoryMap.clear();
    
    // XXX: need to clear for all archives
    // _beanMap.clear();
    
    // cloud/0156
    getBeanManager().clearCache();
  }

  void fillByType(BaseType baseType,
                  HashSet<TypedBean> beanSet,
                  BeanManagerBase beanManager)
  {
    Class<?> rawClass = baseType.getRawClass();

    ScanClassInject scanClass
      = _scanManager.getScanClass(rawClass.getName());

    if (scanClass == null) {
    }
/*
    else if (! scanClass.isBeanCandidate()) {
    }
*/
    else if (! scanClass.isRegistered()) {
      discoverScanClass(scanClass);
      processPendingAnnotatedTypes();
    }

    ArrayList<TypedBean> localBeans = _selfBeanMap.get(rawClass);

    if (localBeans != null) {
      // ioc/0k00, ioc/0400 - XXX: not exactly right.  want local beans to have
      // priority if type and binding match
      /*
      if (this == beanManager)
        beanSet.clear();
      else if (beanSet.size() > 0) {
        return;
      }
      */
      
      for (TypedBean bean : localBeans) {
        if (beanManager.getDeploymentPriority(bean.getBean()) < 0) {
          continue;
        }

        if (bean.isModulePrivate() && this != beanManager.getDelegate()) {
          continue;
        }

        beanSet.add(bean);
      }
    }
  }

  //@Override
  public <X> Bean<X> getMostSpecializedBean(Bean<X> bean)
  {
    throw new UnsupportedOperationException();
    /*
    Bean<?> special = _specializedMap.get(bean.getBeanClass());
    
    if (special != null)
      return (Bean<X>) special;
    else
      return bean;
      */
  }

  @Module
  public boolean isSpecialized(Class<?> beanClass)
  {
    return _specializedMap.get(beanClass) != null;
  }

  @Override
  public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
  {
    return getBeanManager().resolve(beans);
  }

  void validate(Type type)
  {
    BaseType baseType = createTargetBaseType(type);
    
    BeanManagerBase beanManager = getBeanManager(baseType.getClass());

    WebComponent comp = beanManager.getWebComponent(baseType);
  }

  private void validate(Bean<?> bean)
  {
    if (bean.isAlternative() && ! isEnabled(bean))
      return;

    boolean isPassivating = isPassivatingScope(bean.getScope());

    if (bean instanceof InjectEnvironmentBean) {
      InjectEnvironmentBean envBean = (InjectEnvironmentBean) bean;

      if (envBean.getCdiManager() != this) {
        envBean.getCdiManager().validate(bean);
        return;
      }
    }

    if (bean instanceof CdiStatefulBean)
      isPassivating = true;

    if (bean instanceof ManagedBeanImpl
        && ((ManagedBeanImpl) bean).validate()) {
    }

    for (InjectionPoint ip : bean.getInjectionPoints()) {
      ReferenceFactory<?> factory = validateInjectionPoint(ip);

      if (ip.isDelegate() && ! (bean instanceof Decorator))
        throw new ConfigException(L.l("'{0}' is an invalid delegate because {1} is not a Decorator.",
                                      ip.getMember().getName(),
                                      bean));

      RuntimeException exn = validatePassivation(ip);

      if (exn != null && ! factory.isProducer())
        throw exn;
    }

    if (isNormalScope(bean.getScope())) {
      validateNormal(bean);
    }
  }

  RuntimeException validatePassivation(InjectionPoint ip)
  {
    Bean<?> bean = ip.getBean();

    if (bean == null)
      return null;

    boolean isPassivating = isPassivatingScope(bean.getScope());

    if (isStateful(bean)) {
      isPassivating = true;
    }

    if (isPassivating && ! ip.isTransient()) {
      Class<?> cl = getRawClass(ip.getType());

      Bean<?> prodBean = resolve(getBeans(ip.getType(), ip.getQualifiers()));

      // TCK conflict
      if (! cl.isInterface()
          && ! cl.isPrimitive()
          && ! Serializable.class.isAssignableFrom(cl)
          && ! isPassivationCapable(prodBean)) {
        RuntimeException exn;

        if (isProduct(prodBean))
          exn = new IllegalProductException(L.l("'{0}' is an invalid injection point of type {1} because it's not serializable for {2}",
                                                ip.getMember().getName(),
                                                ip.getType(),
                                                bean));
        else
          exn = new DeploymentException(L.l("'{0}.{1}' is an invalid injection point of type {2} ({3}) because it's not serializable for {4}",
                                        bean.getBeanClass().getName(),
                                        ip.getMember().getName(),
                                        ip.getType(),
                                        prodBean,
                                        bean));

        return exn;
      }
    }

    return null;
  }
  
  protected boolean isStateful(Bean<?> bean)
  {
    return bean instanceof CdiStatefulBean;
  }

  private boolean isPassivationCapable(Bean<?> bean)
  {
    if (isNormalScope(bean.getScope()))
      return true;

    // ioc/05e2
    if (bean instanceof PassivationCapable
        && ((PassivationCapable) bean).getId() != null) {
      return true;
    }

    return false;
  }

  private boolean isProduct(Bean<?> bean)
  {
    return ((bean instanceof ProducesFieldBean<?,?>)
            || (bean instanceof ProducesMethodBean<?,?>));
  }

  private Class<?> getRawClass(Type type)
  {
    if (type instanceof Class<?>)
      return (Class<?>) type;

    BaseType baseType = createSourceBaseType(type);

    return baseType.getRawClass();
  }

  void validateNormal(Bean<?> bean)
  {
    Annotated ann = null;

    if (bean instanceof BeanBase) {
      BeanBase absBean = (BeanBase) bean;

      ann = absBean.getAnnotated();
    }

    if (ann == null)
      return;

    Type baseType = ann.getBaseType();

    Class<?> cl = createTargetBaseType(baseType).getRawClass();

    if (cl.isInterface())
      return;

    int modifiers = cl.getModifiers();

    if (Modifier.isFinal(modifiers)) {
      throw new DeploymentException(L.l("'{0}' is an invalid @{1} bean because it's a final class, for {2}.",
                                        cl.getSimpleName(), bean.getScope().getSimpleName(),
                                        bean));
    }

    Constructor<?> ctor = null;

    for (Constructor<?> ctorPtr : cl.getDeclaredConstructors()) {
      if (ctorPtr.getParameterTypes().length > 0
          && ! ctorPtr.isAnnotationPresent(Inject.class)) {
        // ioc/05am
        continue;
      }

      if (Modifier.isPrivate(ctorPtr.getModifiers())) {
        throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because its constructor is private for {2}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      bean));

      }

      ctor = ctorPtr;
    }

    if (ctor == null) {
      throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because it doesn't have a zero-arg constructor for {2}.",
                                    cl.getName(), bean.getScope().getSimpleName(),
                                    bean));

    }


    for (Method method : cl.getMethods()) {
      if (method.getDeclaringClass() == Object.class)
        continue;

      if (Modifier.isFinal(method.getModifiers())) {
        throw new DeploymentException(L.l("'{0}' is an invalid @{1} bean because {2} is a final method for {3}.",
                                          cl.getSimpleName(), bean.getScope().getSimpleName(),
                                          method.getName(),
                                          bean));

      }
    }

    for (Field field : cl.getFields()) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      if (Modifier.isPublic(field.getModifiers())) {
        throw new DefinitionException(L.l("'{0}' is an invalid @{1} bean because {2} is a public field for {3}.",
                                          cl.getSimpleName(), bean.getScope().getSimpleName(),
                                          field.getName(),
                                          bean));
      }
    }

    for (InjectionPoint ip : bean.getInjectionPoints()) {
      if (ip.getType().equals(InjectionPoint.class))
        throw new DefinitionException(L.l("'{0}' is an invalid @{1} bean because '{2}' injects an InjectionPoint for {3}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      ip.getMember().getName(),
                                      bean));

    }
  }

  @Override
  public InjectionPoint createInjectionPoint(AnnotatedField<?> field)
  {
    return new InjectionPointImpl(this, (Bean) null, field);
  }

  @Override
  public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter)
  {
    return new InjectionPointImpl(this, (Bean) null, parameter);
  }

  @Override
  public void validate(InjectionPoint ij)
  {
    validateInjectionPoint(ij);
  }

  public ReferenceFactory<?> validateInjectionPoint(InjectionPoint ij)
  {
    try {
      if (ij.isDelegate()) {
        if (! (ij.getBean() instanceof Decorator<?>))
          throw new DefinitionException(L.l("'{0}' is an invalid @Delegate because {1} is not a decorator",
                                            ij.getMember().getName(), ij.getBean()));
      }
      else {
        return getBeanManager(ij).getReferenceFactory(ij);
      }
    } catch (AmbiguousResolutionException e) {
      throw new AmbiguousResolutionException(L.l("{0}.{1}: {2}",
                                       ij.getMember().getDeclaringClass().getName(),
                                       ij.getMember().getName(),
                                       e.getMessage()),
                                   e);
    } catch (UnsatisfiedResolutionException e) {
      throw new UnsatisfiedResolutionException(L.l("{0}.{1}: {2}",
                                                   ij.getMember().getDeclaringClass().getName(),
                                                   ij.getMember().getName(),
                                                   e.getMessage()),
                                   e);
    } catch (IllegalProductException e) {
      throw new IllegalProductException(L.l("{0}.{1}: {2}",
                                            ij.getMember().getDeclaringClass().getName(),
                                            ij.getMember().getName(),
                                            e.getMessage()),
                                   e);
    } catch (Exception e) {
      throw new InjectionException(L.l("{0}.{1}: {2}",
                                       ij.getMember().getDeclaringClass().getName(),
                                       ij.getMember().getName(),
                                       e.getMessage()),
                                   e);
    }

    return null;
  }


  Integer getPriority(Class<?> cl)
  {
    Integer value = _deploymentMap.get(cl);

    if (value != null) {
      return value;
    }
    else if (_parent != null) {
      return _parent.getPriority(cl);
    }
    else {
      return null;
    }
  }

  Set<Bean<?>> resolveAllBeans()
  {
    LinkedHashSet<Bean<?>> beans = new LinkedHashSet<Bean<?>>();

    for (ArrayList<TypedBean> comp : _selfBeanMap.values()) {
      for (TypedBean typedBean : comp) {
        beans.add(typedBean.getBean());
      }
    }

    return beans;
  }

  @Override
  public <T> CreationalContext<T> createCreationalContext(Contextual<T> bean)
  {
    return new OwnerCreationalContext<T>(bean);
  }

  /**
   * Convenience-method
   */
  public <T> T getReference(Class<T> type, Annotation... qualifiers)
  {
    Set<Bean<?>> beans = getBeans(type, qualifiers);
    
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null) {
      return null;
    }

    return getReference(bean);
  }

  /**
   * Convenience
   */
  public <T> T getReference(Bean<T> bean)
  {
    BeanManagerBase beanManager = getBeanManager(bean);
    
    ReferenceFactory<T> factory = beanManager.getReferenceFactory(bean);

    if (factory != null) {
      return factory.create(null, null, null);
    }
    else {
      return null;
    }
  }

  /**
   * Convenience
   */
  public <T> T findReference(Bean<T> bean)
  {
    Context context = getContext(bean.getScope());

    if (context != null)
      return context.get(bean);
    else
      return null;
  }

  /**
   * Convenience
   */
  public <T> T getReference(Bean<T> bean, CreationalContextImpl<?> parentEnv)
  {
    BeanManagerBase beanManager = getBeanManager(bean);
    
    ReferenceFactory<T> factory = beanManager.getReferenceFactory(bean);

    return factory.create(null, parentEnv, null);
  }

  /**
   * Convenience
   */
  public <T> T getReference(String name)
  {
    Set<Bean<?>> beans = getBeans(name);
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null)
      return null;

    BeanManagerBase beanManager = getBeanManager(bean);
    
    ReferenceFactory<T> factory = beanManager.getReferenceFactory(bean);

    return factory.create(null, null, null);
  }

  /**
   * Convenience
   */
  public <T> T getReference(String name, CreationalContextImpl parentEnv)
  {
    Set<Bean<?>> beans = getBeans(name);
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null) {
      return null;
    }
    
    BeanManagerBase beanManager = getBeanManager(bean);

    ReferenceFactory<T> factory = beanManager.getReferenceFactory(bean);

    return factory.create(null, parentEnv, null);
  }

  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  @Override
  public Object getReference(Bean<?> bean,
                             Type type,
                             CreationalContext<?> createContext)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());
      
      BeanManagerBase beanManager = getBeanManager(bean);

      ReferenceFactory factory = beanManager.getReferenceFactory(bean);

      if (factory == null) {
        throw new IllegalStateException(L.l("{0} is an uninstantiable bean",
                                            bean));
      }

      if (createContext instanceof CreationalContextImpl<?>) {
        return factory.create((CreationalContextImpl) createContext, null, null);
      }
      else {
        return factory.create(null, null, null);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Used by ScopeProxy
   */
  private <T> T getInstanceForProxy(Bean<T> bean)
  {
    CreationalContextImpl<?> oldEnv = _proxyThreadLocal.get();

    T value;

    if (oldEnv != null) {
      value = oldEnv.get(bean);

      if (value != null)
        return value;
    }

    try {
      CreationalContextImpl<T> env = new OwnerCreationalContext(bean, oldEnv);

      _proxyThreadLocal.set(env);

      value = bean.create(env);

      return value;
    } finally {
      _proxyThreadLocal.set(oldEnv);
    }
  }

  public RuntimeException unsatisfiedException(Type type,
                                               Annotation []qualifiers)
  {
    BeanManagerBase beanManager = getBeanManager();
    
    WebComponent component = beanManager.getWebComponent(createTargetBaseType(type));

    if (component == null) {
      throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection manager {1}.",
                                                   type, this));
    }
    else {
      ArrayList<Bean<?>> enabledList = component.getEnabledBeanList();

      if (enabledList.size() == 0) {
        throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection manager {1}.",
                                                     type, this));
      }
      else {
        return new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans match the type and qualifiers {1}.\nBeans:{2}",
                                                      type,
                                                      toList(qualifiers),
                                                      listToLines(enabledList)));
      }
    }
  }

  private String listToLines(List<?> list)
  {
    StringBuilder sb = new StringBuilder();

    ArrayList<String> lines = new ArrayList<String>();

    for (int i = 0; i < list.size(); i++) {
      lines.add(list.get(i).toString());
    }

    Collections.sort(lines);

    for (String line : lines) {
      for (String split : line.split("\n")) {
        sb.append("\n    ").append(split);
      }
    }

    return sb.toString();
  }

  /**
   * Convert an annotation array to a list for debugging purposes
   */
  ArrayList<Annotation> toList(Annotation []annList)
  {
    ArrayList<Annotation> list = new ArrayList<Annotation>();

    if (annList != null) {
      for (Annotation ann : annList) {
        list.add(ann);
      }
    }

    return list;
  }

  InjectionPointHandler getInjectionPointHandler(AnnotatedField<?> field)
  {
    // InjectIntrospector.introspect(_injectProgramList, field);

    for (Annotation ann : field.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      InjectionPointHandler handler = _injectionMap.get(annType);

      if (handler != null) {
        return handler;
      }
    }

    return null;
  }

  InjectionPointHandler getInjectionPointHandler(AnnotatedMethod<?> method)
  {
    // InjectIntrospector.introspect(_injectProgramList, field);

    for (Annotation ann : method.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      InjectionPointHandler handler = _injectionMap.get(annType);

      if (handler != null) {
        return handler;
      }
    }

    return null;
  }

  public InjectionPointHandler
  getInjectionPointHandler(Class<? extends Annotation> annType)
  {
    return _injectionMap.get(annType);
  }

  /**
   * Internal callback during creation to get a new injection instance.
   */
  @Override
  public Object getInjectableReference(InjectionPoint ij,
                                       CreationalContext<?> parentCxt)
  {
    CreationalContextImpl<?> parentEnv = null;

    if (parentCxt instanceof CreationalContextImpl<?>)
      parentEnv = (CreationalContextImpl<?>) parentCxt;

    if (InjectionPoint.class.equals(ij.getType())) {
      if (parentEnv != null) {
        return parentEnv.findInjectionPoint();
      }
    }

    ReferenceFactory<?> factory = getBeanManager(ij).getReferenceFactory(ij);

    return factory.create(null, parentEnv, ij);
  }

  ReferenceFactory getInterceptedBeanReferenceFactory(InjectionPoint ij) {
    Type type = ij.getType();
    ParameterizedType parameterizedType = null;

    if (type instanceof ParameterizedType)
      parameterizedType = (ParameterizedType) type;

    if (Object.class.equals(type)) {
    }
    else if (parameterizedType != null
             && Bean.class.equals(parameterizedType.getRawType())
             && parameterizedType.getActualTypeArguments()[0] instanceof WildcardType) {
    }
    else {
      throw new DefinitionException(L.l("{0} must be of Type Bean<?>", ij));
    }

    return new ReferenceFactoryInterceptedBean();
  }

  ReferenceFactory getDecoratedBeanReferenceFactory(InjectionPoint ij)
  {
    Type ijType = ij.getType();

    Type actualType = null;
    if (Object.class.equals(ijType)) {
    }
    else if (ijType instanceof ParameterizedType &&
             Bean.class.equals(((ParameterizedType) ijType).getRawType())) {
      actualType = ((ParameterizedType) ijType).getActualTypeArguments()[0];
    }
    else
      throw new DefinitionException(L.l("{0} should be of Type Bean<SomeType>",
                                        ij));

    Set<InjectionPoint> points = ij.getBean().getInjectionPoints();

    Set<InjectionPoint> delegates = new HashSet<>();

    for (InjectionPoint point : points) {
      if (point.isDelegate())
        delegates.add(point);
    }

    boolean isDelegateMatchFound = false;
    for (InjectionPoint delegate : delegates) {
      if (Object.class.equals(ijType)
          || delegate.getType().equals(actualType)) {
        Set<Annotation> ijQualifiers = ij.getQualifiers();
        ijQualifiers.remove(DecoratedLiteral.DECORATED);

        Set<Annotation> pointQualifiers = delegate.getQualifiers();
        pointQualifiers.remove(DefaultLiteral.DEFAULT);

        if (ijQualifiers.size() == pointQualifiers.size()) {
          isDelegateMatchFound = true;
          for (Annotation ijQualifier : ijQualifiers) {

            for (Annotation pointQualifier : pointQualifiers) {
              if (areQualifiersEquivalent(ijQualifier, pointQualifier)) {
                isDelegateMatchFound = true;
                break;
              }
            }

            if (! isDelegateMatchFound)
              break;
          }
        }
      }

      if (isDelegateMatchFound)
        break;
    }

    if (isDelegateMatchFound)
      return new ReferenceFactoryDecoratedBean();
    else {
      String message = L.l("{0} has no matching Delegates in the defined set of {1}", ij, delegates);

      throw new DefinitionException(message);
    }
  }

  boolean isBeanArchiveAlternative(Bean<?> bean, Bean<?> candidate)
  {
    ScanClassInject beanScanClass
      = getScanManager().getScanClass(bean.getBeanClass().getName());
    ScanRootInject beanRoot = beanScanClass.getContext();

    List<Class<?>> alternatives = beanRoot.getAlternatives();

    if (alternatives.contains(candidate.getBeanClass()))
      return true;

    for (Class<? extends Annotation> stereotype : candidate.getStereotypes()) {
      for (Class<?> alternative : alternatives) {
        if (alternative.equals(stereotype))
          return true;
      }
    }

    return false;
  }

  boolean isBeanArchiveInterceptor(Class context, Interceptor<?> candidate)
  {
    ScanClassInject beanScanClass
      = getScanManager().getScanClass(context.getName());
    ScanRootInject beanRoot = beanScanClass.getContext();

    List<Class<?>> interceptors = beanRoot.getInterceptors();

    boolean isRegistered = interceptors.contains(candidate.getBeanClass());

    return isRegistered;
  }

  boolean isBeanArchiveDecorator(Class context, Decorator<?> candidate)
  {
    ScanClassInject beanScanClass
      = getScanManager().getScanClass(context.getName());
    ScanRootInject beanRoot = beanScanClass.getContext();

    List<Class<?>> decorators = beanRoot.getDecorators();

    boolean isRegistered = decorators.contains(candidate.getBeanClass());

    return isRegistered;
  }


  /*
  void filterByBeanArchive(Bean<?> context, Set<Bean<?>> beans)
  {
    for (Iterator<Bean<?>> it = beans.iterator(); it.hasNext(); ) {
      Bean<?> next = it.next();

      if (! next.isAlternative())
        continue;

      if (isAppScopeAlternative(next))
        continue;

      if (isBeanArchiveAlternative(context, next))
        continue;

      it.remove();
    }
  }
  */

  <T> Bean<?> createNewBean(Type type, New newQualifier)
  {
    Class<?> newClass = newQualifier.value();

    if (newClass == null
        || void.class.equals(newClass)
        || New.class.equals(newClass)) {
      BaseType baseType = createTargetBaseType(type);
      newClass = (Class<T>) baseType.getRawClass();
    }

    Bean<?> bean = _newBeanMap.get(newClass);

    if (bean == null) {
      AnnotatedType<T> annType = (AnnotatedType<T>) ReflectionAnnotatedFactory.introspectType(newClass);

      BaseType newType = createSourceBaseType(type);

      NewBean<T> newBean = new NewBean<T>(this, newType.getRawClass(), annType);
      newBean.introspect();

      _newBeanMap.put(type, bean);
      bean = newBean;
    }

    return bean;
  }

  <X> AmbiguousResolutionException
    ambiguousException(Set<Bean<? extends X>> beanSet,
                       int bestPriority)
  {
    BeanManagerBase beanManager = getBeanManager();
    
    // ArrayList<Bean<?>> matchBeans = new ArrayList<Bean<?>>();
    ArrayList<String> matchBeans = new ArrayList<String>();
    
    for (Bean<?> bean : beanSet) {
      int priority = beanManager.getDeploymentPriority(bean);

      if (priority == bestPriority) {
        matchBeans.add(toDisplayString(bean));
      }
    }

    return new AmbiguousResolutionException(L.l("Too many beans match, because they all have equal precedence.  Beans:{0}\nfor {1}. You may need to use the @Alternative or <alternatives> to select one.",
                                                listToLines(matchBeans), this));
  }

  private String toDisplayString(Bean<?> bean)
  {
    if (bean instanceof BeanBase<?>) {
      BeanBase<?> absBean = (BeanBase<?>) bean;

      return absBean.toDisplayString();
    }
    else
      return String.valueOf(bean);
  }

  @Override
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  @Override
  public ExpressionFactory
    wrapExpressionFactory(ExpressionFactory expressionFactory)
  {
    return new CandiExpressionFactory(expressionFactory);
  }

  //
  // scopes
  //

  /**
   * Adds a new scope context
   */
  public void addContext(Context context)
  {
    Class<? extends Annotation> scopeType = context.getScope();

    Context oldContext = _contextMap.get(scopeType);

    if (oldContext == null) {
      _contextMap.put(context.getScope(), context);
    }
    else {
      // ioc/0p41 - CDI TCK

      RuntimeException exn
        = new IllegalStateException(L.l("{0} is an invalid new context because @{1} is already registered as a scope",
                                        context, scopeType.getName()));

      _contextMap.put(context.getScope(), new ErrorContext(exn, context));
    }
  }

  public void replaceContext(Context context)
  {
    _contextMap.put(context.getScope(), context);
  }

  /**
   * Returns the scope context for the given type
   */
  @Override
  public Context getContext(Class<? extends Annotation> scopeType)
  {
    Context context = _contextMap.get(scopeType);

    if (context != null && context.isActive()) {
      return context;
    }

    if (context instanceof ErrorContext) {
      ErrorContext cxt = (ErrorContext) context;

      throw cxt.getException();
    }
    
    /*
    if (! isScope(scopeType)) {
      throw new IllegalStateException(L.l("'@{0}' is not a valid scope because it does not have a @Scope annotation",
                                          scopeType));
    }
    */

    throw new ContextNotActiveException(L.l("'@{0}' is not an active Java Injection context.",
                                            scopeType.getName()));
  }

  /**
   * Required for TCK. Returns the scope context for the given type.
   */
  public Context getContextImpl(Class<? extends Annotation> scopeType)
  {
    return _contextMap.get(scopeType);
  }

  /**
   * Returns the bean for the given passivation id.
   */
  public Bean<?> getPassivationCapableBean(String id)
  {
    return _selfPassivationBeanMap.get(id);
  }

  @Override
  public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> type)
  {
    InjectionTargetBuilder<T> target = new InjectionTargetBuilder<>(this, type);
    
    // ioc/0p18
    return (InjectionTargetFactory) processInjectionTarget(target, type);
  }

  @Override
  public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field,
                                                   Bean<X> bean)
  {
    if (bean == null && ! field.isStatic()) {
      throw new IllegalArgumentException(L.l("Can't create a producer with null bean for non-static member {0}",
                                             field));
    }
    
    try {
      ProducesBuilder builder = new ProducesBuilder(this);
    
      AnnotatedType<X> beanType = null;
    
      return builder.createProducesField(bean, beanType, field);
    } catch (InjectionException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
    
    // return new ProducerFactoryField<X>(getBeanManager(), declaringBean, field);
  }

  @Override
  public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> producesMethod,
                                                   Bean<X> bean)
  {
    if (bean == null && ! producesMethod.isStatic()) {
      throw new IllegalArgumentException(L.l("Can't create a producer with null bean for non-static member {0}",
                                             producesMethod));
    }
    
    ProducesBuilder builder = new ProducesBuilder(this);
    
    AnnotatedType<X> beanType = null;
    AnnotatedMethod<? super X> disposesMethod = null;
    
    return builder.createProducesMethod(bean, beanType, producesMethod, disposesMethod);
    
    //return new ProducerFactoryMethod<X>(getBeanManager(), declaringBean, method);
  }

  public Annotation []getQualifiers(Set<Annotation> annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  public Annotation []getQualifiers(Annotation []annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  /**
   * Sends the specified event to any observer instances in the scope
   */
  @Override
  public void fireEvent(Object event, Annotation... qualifiers)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " fireEvent " + event);
    }

    EventManager eventManager = getEventManager();
    
    if (eventManager != null) {
      eventManager.fireEvent(event, qualifiers);
    }
  }

  public void fireEventImpl(InjectionPoint ip,
                            Object event,
                            Annotation... qualifiers)
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " fireEvent " + event);

    getEventManager().fireEvent(ip, event, qualifiers);
  }

  /**
   * Returns the observers listening for an event
   *
   * @param event to resolve
   * @param qualifiers the binding set for the event
   */
  @Override
  public <T> Set<ObserverMethod<? super T>>
  resolveObserverMethods(T event, Annotation... qualifiers)
  {
    return getEventManager().resolveObserverMethods(event, qualifiers);
  }

  //
  // interceptor support
  //

  /**
   * Adds a new interceptor class
   */
  /*
  public void addInterceptorClass(Class<?> interceptorClass)
  {
    getBeanManager().addInterceptorClass(interceptorClass);
  }
  */

  /**
   * Adds a new interceptor to the manager
   */
  private <X> InterceptorEntry<X> addInterceptor(AnnotatedType<?> annType,
                                                 Interceptor<X> interceptor, 
                                                 boolean isSafe)
  {
/*
    if (isSafe) {
    }
    else if (! _interceptorClassList.contains(interceptor.getBeanClass()))
      return null;
*/

    InterceptorEntry<X> entry = new InterceptorEntry<X>(interceptor);
    
    BeanManagerBase beanManager;
    
    if (isAppScopeInterceptor(interceptor)) {
      beanManager = getBeanManager();
    }
    else {
      beanManager = getBeanManager(interceptor);
    }
    
    beanManager.addInterceptor(annType, entry);

    //_interceptorList.add(entry);

    return entry;
  }

  /**
   * Resolves the interceptors for a given interceptor type
   *
   * @param type the main interception type
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Interceptor<?>> resolveInterceptors(InterceptionType type,
                                                  Annotation... qualifiers)
  {
    return getBeanManager().resolveInterceptors(type, qualifiers);
  }

  //
  // decorator
  //

  /**
   * Adds a new decorator
   */
  private <X> DecoratorEntry<X> addDecorator(AnnotatedType<?> annType,
                                             Decorator<X> decorator)
  {
    BaseType baseType = createTargetBaseType(decorator.getDelegateType());

    DecoratorEntry<X> entry = new DecoratorEntry<X>(this, decorator, baseType);

    // addDecorator(annType, entry);

    for (Type type : decorator.getDecoratedTypes()) {
      if (type instanceof Class<?>) {
        Class<?> cl = (Class<?>) type;

        if (Object.class.equals(cl)
            || Serializable.class.equals(cl)) {
          continue;
        }

        String javaClassName = cl.getName();

        ScanClassInject scanClass = getScanManager().getScanClass(javaClassName);

        if (scanClass != null && ! scanClass.isRegistered()) {
          discoverScanClass(scanClass);
        }
      }
    }

    processPendingAnnotatedTypes();
    
    // ioc/0i80 -- added after finalDecorators
    addDecorator(annType, entry);

    return entry;
  }
  
  private <X> void addDecorator(AnnotatedType<?> type, DecoratorEntry<X> entry)
  {
    for (BeanManagerBase manager : _beanManagerMap.values()) {
      manager.addDecorator(type, entry);
    }
    
    // ioc/0i81
    //_beanManagerApplication.addDecorator(type, entry);
  }
  

  /**
   * Adds a new decorator class
   */
  /*
  public void addDecoratorClass(Class<?> decoratorClass)
  {
    _decoratorsBuilder.addRegistered(decoratorClass);
  }
  */

  /**
   * Called by the generated code.
   */
  public List<Decorator<?>> resolveDecorators(Class<?> type)
  {
    HashSet<Type> types = new HashSet<>();
    types.add(type);

    ArrayList<Annotation> bindingList = new ArrayList<>();

    boolean isQualifier = false;

    for (Annotation ann : type.getAnnotations()) {
      if (isQualifier(ann.annotationType())) {
        bindingList.add(ann);

        if (! Named.class.equals(ann.annotationType())) {
          isQualifier = true;
        }
      }
    }

    if (! isQualifier)
      bindingList.add(DefaultLiteral.DEFAULT);
    bindingList.add(AnyLiteral.ANY);

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    List<Decorator<?>> decorators = resolveDecorators(types, bindings);

    filterDecorators(decorators, type);

    // XXX: 4.0.7
    // log.info("DECORATORS: " + decorators + " " + types + " " + this);

    return decorators;
  }

  /**
   * Resolves the decorators for a given set of types
   *
   * @param types the types to match for the decorator
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Decorator<?>> resolveDecorators(Set<Type> types,
                                              Annotation... qualifiers)
  {
    return getBeanManager().resolveDecorators(types, qualifiers);
  }
  /*
    if (types.size() == 0)
      throw new IllegalArgumentException(L.l("type set must contain at least one type"));

    if (qualifiers != null) {
      for (int i = 0; i < qualifiers.length; i++) {
        for (int j = i + 1; j < qualifiers.length; j++) {
          if (qualifiers[i].annotationType() == qualifiers[j].annotationType())
            throw new IllegalArgumentException(L.l("resolveDecorators may not have a duplicate qualifier '{0}'",
                                          qualifiers[i]));
        }
      }
    }

    ArrayList<Decorator<?>> decorators = new ArrayList<Decorator<?>>();

    if (qualifiers == null || qualifiers.length == 0)
      qualifiers = DEFAULT_ANN;

    if (_decoratorList == null)
      return decorators;

    for (Annotation ann : qualifiers) {
      if (! isQualifier(ann.annotationType()))
        throw new IllegalArgumentException(L.l("@{0} must be a qualifier", ann.annotationType()));
    }

    ArrayList<BaseType> targetTypes = new ArrayList<BaseType>();

    for (Type type : types) {
      targetTypes.add(createSourceBaseType(type));
    }

    for (DecoratorEntry<?> entry : _decoratorList) {
      Decorator<?> decorator = entry.getDecorator();

      // XXX: delegateTypes
      if (isDelegateAssignableFrom(entry.getDelegateType(), targetTypes)
          && entry.isMatch(qualifiers)) {
        decorators.add(decorator);
      }
    }

    return decorators;
  }
  */

  @Override
  public <T extends Extension> T getExtension(Class<T> extensionClass)
  {
    T extension = this.getReference(extensionClass);

    if (extension == null)
      throw new IllegalArgumentException(L.l("extension `{0}' is not registered", extensionClass.getName()));

    return extension;
  }

  private boolean isDelegateAssignableFrom(BaseType delegateType,
                                           ArrayList<BaseType> sourceTypes)
  {
    for (BaseType sourceType : sourceTypes) {
      if (delegateType.isAssignableFrom(sourceType)) {
        return true;
      }
    }

    return false;
  }

  public void addLoader()
  {
    _isUpdateNeeded = true;
  }

  public void updatePending()
  {
    if (! _isEnableAutoUpdate) {
      return;
    }
    
    update();
    
    processPendingAnnotatedTypes();

    if (_isAfterBeanDiscoveryComplete) {
      processStartupBeans();
    }
  }

  public void update()
  {
    // ioc/0044
    if (! _isEnableAutoUpdate) {
      return;
    }

    if (! isUpdateRequired()) {
      return;
    }

    _isUpdateNeeded = false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      _extensionManager.updateExtensions();

      ArrayList<ScanRootInject> rootContextList
        = _scanManager.getPendingScanRootList();

      for (ScanRootInject context : rootContextList) {
        BeansConfig config = context.getBeansConfig();
        
        if (config != null) {
          addScanConfig(config);
        }
        else {
          addScanConfigRoot(context.getRoot());
        }
      }

      processRoots();

      processPendingAnnotatedTypes();

      if (_isAfterBeanDiscoveryComplete) {
        processStartupBeans();
      }
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected boolean isUpdateRequired()
  {
    return (_isUpdateNeeded
            || _scanManager.isPending()
            || _pendingAnnotatedTypes.size() == 0
            || _extCustomBean.isPending());
  }
  
  protected void setUpdateRequired()
  {
    _isUpdateNeeded = true;
  }

/*
  public void addXmlPath(Path path)
  {
    setUpdateRequired();

    _extCustomBean.addXmlPath(path);
  }
*/

  protected void addScanConfig(BeansConfig config)
  {
    _extCustomBean.add(config);
  }
  
  protected void addScanConfigRoot(Path path)
  {
    _extCustomBean.addRoot(path);
  }
  
  protected void processStartupBeans()
  {
    _extCustomBean.startupBeans();
  }

  protected void processRoots()
  {
    _extCustomBean.processRoots();
  }

  public void fireBeforeBeanDiscovery()
  {
    if (getEventManager() == null) {
      throw new IllegalStateException();
    }
    
    if (_isBeforeBeanDiscoveryFired) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (_classLoader != null) {
        _classLoader.updateScan();
      }

      _extensionManager.updateExtensions();

      if (getEventManager() != null) {
        getExtensionManager().fireBeforeBeanDiscovery();
      }

      _isBeforeBeanDiscoveryFired = true;
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void afterTypeDiscovery()
  {
    if (_isAfterTypeDiscoverFired)
      return;

    _isAfterTypeDiscoverFired = true;

    AfterTypeDiscovery event
      = getExtensionManager().fireAfterTypeDiscovery();

    // XXX:
    getBeanManager().setFinalDiscovered(event);
    
    // _alternativesBuilder.setFinalDiscovered(event.getAlternatives());
  }

  public void updateResources()
  {
  }

  public void addPendingAnnotatedType(AnnotatedType<?> annType)
  {
    _pendingAnnotatedTypes.add(annType);
  }

  public void addPendingExtension(AnnotatedType<?> annType)
  {
    _pendingExtensions.add(annType);
  }

  public void processPendingAnnotatedTypes()
  {
    //fireBeforeBeanDiscovery();

    _scanManager.discover();

    processPendingExtensions();

    ArrayList<AnnotatedType<?>> types = new ArrayList<>(_pendingAnnotatedTypes);
    _pendingAnnotatedTypes.clear();

    ArrayList<AnnotatedType<?>> processedTypes = new ArrayList<>(types.size());

    for (AnnotatedType<?> beanType : types) {
      if (getExtensionManager().isExtension(beanType)) {
        continue;
      }
      
      AnnotatedType<?> processedType
        = getExtensionManager().processAnnotatedType(beanType);

      if (processedType != null)
        processedTypes.add(processedType);
    }

    ArrayList<SyntheticAnnotatedType> syntheticTypes
      = new ArrayList<>(_pendingSyntheticTypes);

    _pendingSyntheticTypes.clear();

    for (SyntheticAnnotatedType syntheticType : syntheticTypes) {
      // ioc/0pe0
      /*
      if (syntheticType.isVetoedAnnotated()) {
        continue;
      }
      */

      AnnotatedType type = syntheticType.getType();

      if (syntheticType.isForced()) {
        type = processSyntethicAnnotatedType(syntheticType);
      }

      if (type == null) {
        continue;
      }

      if (syntheticType.isForced()) {

      }
      else if (isAlternative(type)
               || isInterceptorAnnotated(type)
               || isDecoratorAnnotated(type)) {
        continue;
      }

      _syntheticTypes.add(syntheticType);
      processedTypes.add(syntheticType.getType());
    }

    // _decoratorsBuilder.discover(processedTypes);
    // _interceptorsBuilder.discover(processedTypes);
    // _alternativesBuilder.discover(processedTypes);
    
    
    for (AnnotatedType type : processedTypes) {
      BeanManagerBase beanManager = getBeanManager(type);

      /* ioc/0pc9 */
      if (type.isAnnotationPresent(Priority.class)) {
        beanManager = getBeanManager();
      }
      
      beanManager.discover(type);
    }
    
    Collections.sort(processedTypes, ComparatorAnnotatedType.CMP);

    for (AnnotatedType<?> type : processedTypes) {
      discoverBeanImpl(type);
    }

    // ioc/0pe4
    if (! _isAfterTypeDiscoverFired) {
      afterTypeDiscovery();
    }

    _extensionManager.processPendingEvents();
  }

  private boolean isPriorityAnnotated(final AnnotatedType type)
  {
    return type.getAnnotation(Priority.class) != null;
  }

  private boolean isDecoratorAnnotated(final AnnotatedType type)
  {
    return type.isAnnotationPresent(javax.decorator.Decorator.class);
  }

  private boolean isInterceptorAnnotated(final AnnotatedType type)
  {
    return type.isAnnotationPresent(javax.interceptor.Interceptor.class);
  }

  private boolean isAlternative(final AnnotatedType type)
  {
    Alternative alternative = type.getAnnotation(Alternative.class);

    boolean isAlternative = alternative != null
                            || isAlternativeStereotype(type);

    return isAlternative;
  }

  public void processPendingExtensions()
  {
    ArrayList<AnnotatedType<?>> types = new ArrayList<>(_pendingExtensions);
    _pendingExtensions.clear();

    for (AnnotatedType<?> beanType : types) {
      getExtensionManager().processAnnotatedType(beanType);
    }
  }

  void discoverScanClass(ScanClassInject scanClass)
  {
    scanClass.register();

    // processPendingAnnotatedTypes();
  }

  void discoverBean(ScanClassInject scanClass)
  {
    // ioc/0640
    if (_extCustomBean.isConfiguredBean(scanClass.getClassName())) { {
      return;
    }
    }

    AnnotatedType<?> type = createDiscoveredType(scanClass.getClassName());

    if (type != null) {
      BeanManagerBase beanManager = scanClass.getBeanManager();
      
      /*
      if (type.isAnnotationPresent(Priority.class)) {
        beanManager = getBeanManager();
      }
      */
      
      type = AnnotatedTypeImpl.create(type, beanManager);
      
      discoverBean(type, beanManager);
    }
  }

  void discoverBean(String className)
  {
    AnnotatedType<?> type = createDiscoveredType(className);

    if (type != null) {
      discoverBean(type);
    }
  }

  private AnnotatedType<?> createDiscoveredType(String className)
  {
    try {
      Class<?> cl;

      cl = Class.forName(className, false, _classLoader);

      /*
      if (! isValidSimpleBean(cl))
        return;
        */

      if (cl.getDeclaringClass() != null
          && ! Modifier.isStatic(cl.getModifiers()))
        return null;

      for (Class<? extends Annotation> forbiddenAnnotation : _forbiddenAnnotations) {
        if (cl.isAnnotationPresent(forbiddenAnnotation))
          return null;
      }

      for (Class<?> forbiddenClass : _forbiddenClasses) {
        if (forbiddenClass.isAssignableFrom(cl)) {
          return null;
        }
      }

      // ioc/0619
      /*
      if (isDisabled(cl))
        return;
        */

      if (cl.isInterface()) {
        if (Annotation.class.isAssignableFrom(cl)
            && cl.isAnnotationPresent(Qualifier.class)) {
          // validateQualifier(cl);
          QualifierBinding.validateQualifier(cl, null);
        }
      }

      return createAnnotatedType(cl);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (NoClassDefFoundError e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return null;
  }

  public <X> void discoverBean(AnnotatedType<X> beanType)
  {
    BeanManagerBase beanManager = getBeanManager(beanType);
    
    discoverBean(beanType, beanManager);
  }

  public <X> void discoverBean(AnnotatedType<X> beanType,
                               BeanManagerBase beanManager)
  {
    Class<X> cl;

    // ioc/07fb
    cl = beanType.getJavaClass();

    if (cl.isAnnotationPresent(Specializes.class)) {
      Class<?> parent = cl.getSuperclass();

      if (parent != null) {
        addSpecialize(cl, parent);
      }
    }

    addPendingAnnotatedType(beanType);
  }

  private void addSpecialize(Class<?> specializedType, Class<?> parentType)
  {
    Class<?> oldSpecialized = _specializedMap.get(parentType);

    if (oldSpecialized != null)
      throw new DeploymentException(L.l("@Specialized on '{0}' is invalid because it conflicts with an older specialized '{1}'",
                                        specializedType.getName(),
                                        oldSpecialized.getName()));

    if (! isValidSimpleBean(parentType))
      throw new DeploymentException(L.l("@Specialized on '{0}' is invalid because its parent '{1}' is not a managed bean.",
                                        specializedType.getName(),
                                        parentType.getName()));

    _specializedMap.put(parentType, specializedType);
  }

  boolean isEnabled(Bean<?> bean)
  {
    if (! bean.isAlternative())
      return true;

    Class beanClass = bean.getBeanClass();

    if (_deploymentMap.containsKey(beanClass))
      return true;

    if (beanClass.getAnnotation(Priority.class) != null) {
      return true;
    }
    
    BeanManagerBase beanManager = getBeanManager(bean);

    if (beanManager.isAlternativeEnabled(bean)) {
      return true;
    }

    for (Class<?> stereotype : bean.getStereotypes()) {
      if (_deploymentMap.containsKey(stereotype)) {
        return true;
      }
    }

    return false;
  }

  boolean isIntrospectObservers(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(Specializes.class))
      return true;

    String javaClassName = type.getJavaClass().getName();

    ScanClassInject scanClass = getScanManager().getScanClass(javaClassName);

    if (scanClass == null)
      return true;

    return scanClass.isObserves();
  }

  private <T> void discoverBeanImpl(AnnotatedType<T> type)
  {
    // ioc/0n18
    /*
    if (_specializedMap.get(type.getJavaClass()) != null)
      return;
      */

    final Class beanClass = type.getJavaClass();
    // XXX: not sure this is correct.
    if (Throwable.class.isAssignableFrom(beanClass)) {
      return;
    }
    
    BeanManagerBase beanManager = getBeanManager(type);

    boolean isDecorator = isDecoratorAnnotated(type);
    boolean isInterceptor = isInterceptorAnnotated(type);
    boolean isAlternative = isAlternative(type);

    if (isDecorator) {
      if (! isDecoratorEnabled(type)) {
        return;
      }
    }
    else if (isDecorator) {
    }
    else if (isInterceptor && ! beanManager.isInterceptorEnabled(type)) {
      return;
    }
    else if (! isValidSimpleBean(type)) {
      return;
    }

    /* XXX: selected later
    if (isAlternative && ! beanManager.isAlternativeEnabled(type)) {
      return;
    }
    */

    BeanAttributes beanAttributes = createBeanAttributes(type);

    final boolean isExtension = getExtensionManager().isExtension(type);

    if (! isExtension) {
      beanAttributes
        = getExtensionManager().processBeanAttributes(beanAttributes, type);
    }
    
    if (beanAttributes == null) {
      //_decoratorsBuilder.disable(type);
      getBeanManager().disable(type);
      // _alternativesBuilder.disable(type);

      return;
    }

    ManagedBeanImpl<T> bean
      = new ManagedBeanImpl<>(this, beanManager, type, false, beanAttributes);

    InjectionTarget<T> target = bean.getInjectionTarget();

    if (target instanceof InjectionTargetBuilder<?>) {
      InjectionTargetBuilder<?> targetImpl = (InjectionTargetBuilder<?>) target;

      targetImpl.setGenerateInterception(true);
    }

    if (! isExtension)
      target = processInjectionTarget(target, type);

    if (target == null) {
      return;
    }

    if (target instanceof InjectionTargetBuilder<?>) {
      InjectionTargetBuilder<T> targetImpl = (InjectionTargetBuilder<T>) target;

      targetImpl.setBean(bean);
    }

    bean.setInjectionTarget(target);

    bean.introspect();

    AnnotatedType<T> annType = bean.getAnnotatedType();

    // ioc/0i04
    if (annType.isAnnotationPresent(javax.decorator.Decorator.class)) {
      if (annType.isAnnotationPresent(javax.interceptor.Interceptor.class))
        throw new ConfigException(L.l("'{0}' bean may not have both a @Decorator and @Interceptor annotation.",
                                      annType.getJavaClass()));

      Bean<?> newBean = processBean(bean, annType);

      
      if (beanAttributes.getStereotypes().contains(javax.decorator.Decorator.class)) {
        DecoratorBean decoratorBean = DecoratorBean.create(this, newBean);

        addDecorator(annType, decoratorBean).setEnabled(true);
      }

      // addBean(decoratorBean);

      return;
    }
    // ioc/0c1a
    if (annType.isAnnotationPresent(javax.interceptor.Interceptor.class)) {
      InterceptorBean interceptorBean
        = new InterceptorBean(this, annType.getJavaClass(), beanManager);

      addBeanDiscover(interceptorBean);
      return;
    }

    addDiscoveredBean(bean);

    fillProducerBeans(bean);

    // beans.addScannedClass(cl);
  }

  protected boolean isValidSimpleBean(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(CookieCustomBean.class)) {
      // ioc/04d0
      return true;
    }

    return isValidSimpleBean(type.getJavaClass());
  }

  private boolean isValidSimpleBean(Class<?> type)
  {
    try {
      if (type.isInterface())
        return false;
      else if (type.isAnonymousClass())
        return false;
      /*
    else if (type.isMemberClass())
      return false;
       */

      if (Modifier.isAbstract(type.getModifiers()))
        return false;

      /* XXX: ioc/024d */
      // ioc/070c, ioc/0j0g
      /*
    if (type.getTypeParameters() != null
        && type.getTypeParameters().length > 0) {
      return false;
    }
       */

      if (! isValidConstructor(type))
        return false;

      return true;
    } catch (Throwable e) {
      log.finer("CDI scanning " + type + ": " + e);
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    }
  }

  private boolean isValidConstructor(Class<?> type)
  {
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
        return true;

      if (ctor.isAnnotationPresent(Inject.class))
        return true;
    }

    return false;
  }

  /*private boolean isEnabledAlternative(Class cl) {
    boolean isEnabled = false;
    if (_appAlternatives != null)
      isEnabled = _appAlternatives.contains(cl);
    else if (cl.getAnnotation(Priority.class) != null)
      isEnabled = true;
    else if (_deploymentMap.containsKey(cl))
      isEnabled = true;

    return isEnabled;
  }*/
  
  private boolean isDecoratorEnabled(AnnotatedType<?> type)
  {
    for (BeanManagerBase manager : this._beanManagerMap.values()) {
      if (manager.isDecoratorEnabled(type)) {
        return true;
      }
    }

    return false;
  }

  public <T> InjectionPoint processInjectionPoint(InjectionPoint ip)
  {
    ip = _extensionManager.processInjectionPoint(ip);

    return ip;
  }

  public <T> InjectionTarget<T> processInjectionTarget(InjectionTarget<T> target,
                                                       AnnotatedType<T> ann)
  {
    return getExtensionManager().processInjectionTarget(target, ann);
  }

  private void fillProducerBeans(ManagedBeanImpl<?> bean)
  {
  }

  private <X> void addDiscoveredBean(ManagedBeanImpl<X> managedBean)
  {
    /*
     // ioc/04d0
    if (! isValidSimpleBean(managedBean.getBeanClass()))
      return;
      */

    // ioc/0680
    if (true || ! managedBean.isAlternative() || isEnabled(managedBean)) {
      // XXX: bean alternative is discovered but not filtered for active
      // until later
      
      // ioc/0680
      addBeanDiscover(managedBean);

      // ioc/0b0f
      if (! _specializedMap.containsKey(managedBean.getBeanClass())) {
        // ioc/0b84 - alternative not observer yet
        if (! managedBean.isAlternative() || isEnabled(managedBean)) {
          managedBean.introspectObservers();
        }
      }
    }

    // ioc/07d2
    if (! _specializedMap.containsKey(managedBean.getBeanClass())
        && isEnabled(managedBean)) {
      managedBean.introspectProduces();
    }
  }

  public <X> void addProduces(Bean<X> bean, AnnotatedType<X> beanType)
  {
    ProducesBuilder builder = new ProducesBuilder(this);

    builder.introspectProduces(bean, beanType);
  }

  public <X> void addManagedProduces(Bean<X> bean, AnnotatedType<X> beanType)
  {
    ProducesBuilder builder = new ManagedProducesBuilder(this);

    builder.introspectProduces(bean, beanType);
  }

  public <X,T> void addProducesBean(ProducesMethodBean<X,T> bean)
  {
    AnnotatedMethod<X> producesMethod
    = (AnnotatedMethod<X>) bean.getProducesMethod();

    Producer<T> producer = bean.getProducer();

    producer = getExtensionManager().processProducer(producesMethod, producer);

    bean.setProducer(producer);

    //addBean(bean, producesMethod);
     addBeanDiscover(bean, producesMethod);
  }

  public <X,T> void addProducesFieldBean(ProducesFieldBean<X,T> bean)
  {
    AnnotatedField<X> producesField
      = (AnnotatedField<X>) bean.getField();

    Producer<T> producer = bean.getProducer();

    producer = getExtensionManager().processProducer(producesField, producer);

    bean.setProducer(producer);

    //addBean(bean, producesField);
    addBeanDiscover(bean, producesField);
  }

  public <X> void addManagedBeanDiscover(ManagedBeanImpl<X> managedBean)
  {
    addBeanDiscover(managedBean);

    managedBean.introspectProduces();
  }

  public <X> void addManagedBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);

    managedBean.introspectProduces();
  }

  public <T> ArrayList<T> loadServices(Class<T> serviceClass)
  {
    return loadServices(serviceClass, new HashSet<URL>(), false);
  }

  public <T> ArrayList<T> loadLocalServices(Class<T> serviceClass)
  {
    return loadServices(serviceClass, new HashSet<URL>(), true);
  }

  private <T> ArrayList<T> loadServices(Class<T> serviceApiClass,
                                        HashSet<URL> serviceSet,
                                        boolean isLocal)
  {
    ArrayList<T> services = new ArrayList<T>();

    try {
      DynamicClassLoader loader = _classLoader;

      if (loader == null)
        return services;

      String serviceName = "META-INF/services/" + serviceApiClass.getName();

      Enumeration<URL> e;

      if (isLocal) {
        e = loader.findResources(serviceName);
      }
      else {
        e = loader.getResources(serviceName);
      }

      //makes sense to have the order preserved when multiples are defined in a single file
      HashSet<Class<T>> classSet = new LinkedHashSet<Class<T>>();

      while (e.hasMoreElements()) {
        URL url = e.nextElement();

        if (serviceSet.contains(url))
          continue;

        serviceSet.add(url);

        InputStream is = null;
        try {
          is = url.openStream();
          ReadStream in = Vfs.openRead(is);

          String line;

          while ((line = in.readLine()) != null) {
            int p = line.indexOf('#');
            if (p >= 0)
              line = line.substring(0, p);
            line = line.trim();

            if (line.length() > 0) {
              Class<T> cl = loadServiceClass(serviceApiClass, line);

              if (cl != null) {
                classSet.add(cl);
              }
            }
          }

          in.close();
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        } finally {
          IoUtil.close(is);
        }
      }

      for (Class<T> cl : classSet) {
        services.add(createTransientObject(cl));
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return services;
  }

  private <T> Class<T> loadServiceClass(Class<T> serviceApi,
                                        String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> serviceClass = Class.forName(className, false, loader);

      if (! serviceApi.isAssignableFrom(serviceClass))
        throw new InjectionException(L.l("'{0}' is not a valid service because it does not implement {1}",
                                         serviceClass, serviceApi.getName()));

      return (Class<T>) serviceClass;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  public void addExtension(Extension extension)
  {
    _extensionManager.addExtension(extension);
  }

  public CookieCustomBean generateCustomBeanCookie()
  {
    return new CookieCustomBeanLiteral(_xmlCookieSequence.incrementAndGet());
  }

  /**
   * Starts the bind phase
   */
  public void bind()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isBind = false;

    try {
      thread.setContextClassLoader(_classLoader);

      processPendingAnnotatedTypes();

      if (_pendingBindList != null) {
        ArrayList<BeanBase<?>> bindList
          = new ArrayList<BeanBase<?>>(_pendingBindList);

        _pendingBindList.clear();

        if (bindList.size() > 0) {
          isBind = true;
        }
      }

      if (! _isAfterBeanDiscoveryComplete) {
        isBind = true;
      }

      if (isBind) {
        _isAfterBeanDiscoveryComplete = true;

        getExtensionManager().fireAfterBeanDiscovery();
      }

      if (_configException != null)
        throw _configException;

      /*
      for (AbstractBean comp : bindList) {
        if (_deploymentMap.get(comp.getDeploymentType()) != null)
          comp.bind();
      }
      */

      // buildDecorators();
      // buildInterceptors();

      bindGlobals();
      validate();
      
      /*
      if (isBind) {
        getExtensionManager().fireAfterDeploymentValidation();
      }
      */
    } catch (RuntimeException e) {
      if (_configException == null)
        _configException = e;
      else {
        log.log(Level.WARNING, e.toString(), e);
      }

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void bindGlobals()
  {
    if (_globalProgram.size() > 0) {
      ArrayList<ConfigProgram> programList
        = new ArrayList<ConfigProgram>(_globalProgram);
      _globalProgram.clear();

      for (ConfigProgram program : programList) {
        program.inject(this, null);
      }
    }
  }

  /*
  private void buildInterceptors()
  {
    // _interceptorList = _interceptorsBuilder.build();
  }
  */

  /*
  private void buildDecorators()
  {
    _decoratorList = _decoratorsBuilder.build();

    // ioc/0i57 - validation must be early
    for (DecoratorEntry<?> entry : _decoratorList) {
      if (entry.isEnabled()) {
        for (Type type : entry.getDelegateType().getTypeClosure(this)) {
          validate(type);
        }
      }
    }
  }
  */

 private void validate()
  {
    ArrayList<ArrayList<TypedBean>> typeValues
      = new ArrayList<>(_selfBeanMap.values());

    for (int i = typeValues.size() - 1; i >= 0; i--) {
      ArrayList<TypedBean> beans = typeValues.get(i);

      validateSpecializes(beans);
    }

    for (int i = typeValues.size() - 1; i >= 0; i--) {
      ArrayList<TypedBean> beans = typeValues.get(i);

      if (beans == null)
        continue;

      for (int j = beans.size() - 1; j >= 0; j--) {
        TypedBean typedBean = beans.get(j);

        typedBean.validate();
      }
    }
  }

  private void validateSpecializes(ArrayList<TypedBean> beans)
  {
    if (beans == null)
      return;

    for (int i = beans.size() - 1; i >= 0; i--) {
      TypedBean bean = beans.get(i);

      Annotated ann = bean.getAnnotated();

      if (ann == null || ! ann.isAnnotationPresent(Specializes.class))
        continue;

      for (int j = beans.size() - 1; j >= 0; j--) {
        if (i == j)
          continue;

        TypedBean bean2 = beans.get(j);

        // XXX:

        Annotated ann2 = bean.getAnnotated();

        if (ann2 == null)
          continue;

        if (isSpecializes(ann, ann2) && isMatchInject(bean, bean2)) {
          beans.remove(j);
          i = 0;
        }
      }
    }
  }

  private boolean isMatchInject(TypedBean typedBeanA, TypedBean typedBeanB)
  {
    Bean<?> beanA = typedBeanA.getBean();
    Bean<?> beanB = typedBeanB.getBean();

    return (beanA.getTypes().equals(beanB.getTypes())
            && beanA.getQualifiers().equals(beanB.getQualifiers()));
  }

  private boolean isSpecializes(Annotated childAnn, Annotated parentAnn)
  {
    if (childAnn instanceof AnnotatedMethod<?>
        && parentAnn instanceof AnnotatedMethod<?>) {
      Method childMethod = ((AnnotatedMethod<?>) childAnn).getJavaMember();
      Method parentMethod = ((AnnotatedMethod<?>) parentAnn).getJavaMember();

      if (! AnnotatedTypeUtil.isMatch(childMethod, parentMethod)) {
        return false;
      }

      Class<?> childClass = childMethod.getDeclaringClass();
      Class<?> parentClass = parentMethod.getDeclaringClass();

      if (parentClass.isAssignableFrom(childClass))
        return true;
    }

    return false;
  }

  public List<Class<?>> getAlternatives()
  {
    return getBeanManager().getAlternatives();
  }

  private boolean isAlternative(final Class c)
  {
    Alternative alternative = (Alternative) c.getAnnotation(Alternative.class);

    boolean isAlternative = alternative != null || isAlternativeStereotype(c);

    return isAlternative;
  }

  public boolean isEnabledWithAlternativeStereotype(Annotated annotated)
  {
    Map<Class<? extends Annotation>,WrappedAnnotaton> map = new HashMap<>();

    boolean result = true;

    Set<WrappedAnnotaton> alternativeStereotypes = null;

    for (Annotation annotation : annotated.getAnnotations()) {
      WrappedAnnotaton wAnn = new WrappedAnnotaton(annotation.annotationType(), map);

      if (wAnn.isStereotype() && wAnn.isAlternative()) {
        if (alternativeStereotypes == null)
          alternativeStereotypes = new HashSet<>();

        alternativeStereotypes.add(wAnn);
      }
    }

    if (alternativeStereotypes != null) {
      for (WrappedAnnotaton wAnn : alternativeStereotypes) {
        if (wAnn.isEnabled())
          result = true;
        else
          result = false;

        if (result)
          break;
      }
    }

    return result;
  }

  public BeanAttributes processBeanAttributes(BeanAttributes attributes, Annotated ann)
  {
    BeanAttributes beanAttributes
      = getExtensionManager().processBeanAttributes(attributes, ann);

    return beanAttributes;
  }

  public <T> AnnotatedType<T> getAnnotatedType(Class<T> type, String id)
  {
    AnnotatedType<T> annotatedType = getSyntheticAnnotatedType(type, id);

    if (annotatedType == null && id == null)
      annotatedType = getAnnotatedType(type);

    return annotatedType;
  }

  public <T> java.lang.Iterable<AnnotatedType<T>> getAnnotatedTypes(java.lang.Class<T> type)
  {
    ArrayList<AnnotatedType<T>> types = getSyntheticAnnotatedTypes(type);

    if (types.size() == 0)
      types.add(getAnnotatedType(type));

    return types;
  }

  private <T> AnnotatedType<T> getAnnotatedType(java.lang.Class<T> type)
  {
    ScanClassInject injectScanClass
      = _scanManager.getScanClass(type.getName());

    if (injectScanClass != null)
      return createAnnotatedType(type);

    return null;
  }

  public void addSyntheticBeforeBeanDiscovery(AnnotatedType<?> type,
                                              String id)
  {
    Extension extension = _extensionManager.getCurrentExtension();

    _pendingSyntheticTypes.add(new SyntheticAnnotatedType(extension,
                                                          type,
                                                          id,
                                                          true));
  }

  public void addSyntheticAfterTypeDiscovery(AnnotatedType<?> type, String id)
  {
    Extension extension = _extensionManager.getCurrentExtension();

    type = _extensionManager.processSyntheticAnnotatedType(extension, type);

    if (type == null) {
    }
    else if (isAlternative(type)) {

    }
    else if (isInterceptorAnnotated(type)) {

    }
    else if (isDecoratorAnnotated(type)) {

    }
    else {
      _pendingSyntheticTypes.add(new SyntheticAnnotatedType(extension,
                                                            type,
                                                            id,
                                                            false));
    }
  }

  public <T> AnnotatedType<T> getSyntheticAnnotatedType(Class<T> type,
                                                        String id)
  {
    if (id == null)
      id = type.getName();

    for (SyntheticAnnotatedType synthetic : _syntheticTypes) {
      if (synthetic.getId().equals(id)
          && synthetic.getType().getJavaClass().equals(type))
        return synthetic.getType();
    }

    return null;
  }

  public <T> ArrayList<AnnotatedType<T>> getSyntheticAnnotatedTypes(java.lang.Class<T> type)
  {
    ArrayList<AnnotatedType<T>> types = new ArrayList<>();

    for (SyntheticAnnotatedType synthetic : _syntheticTypes) {
      if (synthetic.getType().getJavaClass().equals(type))
        types.add(synthetic.getType());
    }

    return types;
  }

  class WrappedAnnotaton
  {
    Class<? extends Annotation> _annotation;
    WrappedAnnotaton []_annotated;

    public WrappedAnnotaton(final Class<? extends Annotation> annotation,
                            final Map<Class<? extends Annotation>,WrappedAnnotaton> map)
    {
      _annotation = annotation;

      Annotation []annotations = _annotation.getAnnotations();
      _annotated = new WrappedAnnotaton[annotations.length];

      map.put(annotation, this);

      for (int i = 0; i < annotations.length; i++) {
        Annotation a = annotations[i];
        Class<? extends Annotation> annotationType = a.annotationType();

        WrappedAnnotaton wAnn = map.get(annotationType);
        if (wAnn == null)
          wAnn = new WrappedAnnotaton(annotationType, map);

        _annotated[i] = wAnn;
      }
    }

    boolean isStereotype() {
      Set<WrappedAnnotaton> set = new HashSet<>();

      return isStereotype(set);
    }

    private boolean isStereotype(Set<WrappedAnnotaton> set) {
      if (set.contains(this))
        return false;
      set.add(this);

      if (Stereotype.class.equals(_annotation))
        return true;
      for (WrappedAnnotaton wAnn : _annotated) {
        if (wAnn.isStereotype(set))
          return true;
      }

      return false;
    }

    boolean isAlternative() {
      Set<WrappedAnnotaton> set = new HashSet<>();

      return isAlternative(set);
    }

    private boolean isAlternative(Set<WrappedAnnotaton> set) {
      if (set.contains(this))
        return false;

      set.add(this);

      if (Alternative.class.equals(_annotation))
        return true;
      for (WrappedAnnotaton wAnn : _annotated) {
        if (wAnn.isAlternative(set))
          return true;
      }

      return false;
    }

    private boolean isEnabled() {
      Set<WrappedAnnotaton> set = new HashSet<>();

      return isEnabled(set);
    }

    private boolean isEnabled(Set<WrappedAnnotaton> set) {
      if (set.contains(this))
        return false;

      set.add(this);

/*
      if (_appAlternatives.contains(_annotation))
        return true;
*/

      if (_deploymentMap.containsKey(_annotation))
        return true;

      for (WrappedAnnotaton wAnn : _annotated) {
        if (wAnn.isEnabled(set))
          return true;
      }

      return false;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      WrappedAnnotaton wAnn = (WrappedAnnotaton) o;

      if (!_annotation.equals(wAnn._annotation)) return false;

      return true;
    }

    @Override
    public int hashCode()
    {
      return _annotation.hashCode();
    }
  }

  public boolean isAlternativeStereotype(final Class c) {
    LinkedList<Annotation > annotations = new LinkedList<>();

    for (Annotation a: c.getAnnotations()) {
      if (Alternative.class.isAssignableFrom(a.annotationType()))
        return true;
      else if (a.annotationType().isAnnotationPresent(Stereotype.class))
        annotations.add(a);
    }

    while (! annotations.isEmpty()) {
      Annotation annotation = annotations.removeFirst();

      if (annotation.annotationType().isAssignableFrom(Alternative.class))
        return true;

      if (annotation.annotationType().isAnnotationPresent(Stereotype.class)) {
        for (Annotation a: annotation.annotationType().getAnnotations()) {
          if (Alternative.class.isAssignableFrom(a.annotationType()))
            return true;
          else if (a.annotationType().isAnnotationPresent(Stereotype.class))
            annotations.add(a);
        }
      }
    }

    return false;
  }

  private boolean isAlternativeStereotype(final AnnotatedType type)
  {
    LinkedList<Annotation> annotations = new LinkedList<>();

    for (Annotation a : type.getAnnotations()) {
      if (Alternative.class.isAssignableFrom(a.annotationType()))
        return true;
      else if (a.annotationType().isAnnotationPresent(Stereotype.class))
        annotations.add(a);
    }

    while (! annotations.isEmpty()) {
      Annotation annotation = annotations.removeFirst();

      if (annotation.annotationType().isAssignableFrom(Alternative.class))
        return true;

      if (annotation.annotationType().isAnnotationPresent(Stereotype.class)) {
        for (Annotation a : annotation.annotationType().getAnnotations()) {
          if (Alternative.class.isAssignableFrom(a.annotationType()))
            return true;
          else if (a.annotationType().isAnnotationPresent(Stereotype.class))
            annotations.add(a);
        }
      }
    }

    return false;
  }

  /*
  public List<Class<?>> getInterceptors()
  {
    return _interceptorsBuilder.getDiscovered();
  }
  */

  /*
  public List<Class<?>> getDecorators()
  {
    return _decoratorsBuilder.getDiscovered();
  }
  */

  /**
   * Handles the case the environment config phase
   */
  @Override
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
    try {
      initialize();
    } catch (ConfigException e) {
      throw e;
    } catch (DefinitionException e) {
      throw ConfigException.createConfig(e);
    } catch (DeploymentException e) {
      throw ConfigException.createConfig(e);
    }
  }

  /**
   * Handles the case the environment config phase
   */
  @Override
  public void environmentBind(EnvironmentClassLoader loader)
  {
    try {
      fireBeforeBeanDiscovery();
      initialize();
      bind();
    } catch (ConfigException e) {
      throw e;
    } catch (ResolutionException e) {
      throw ConfigException.createConfig(e);
    } catch (DeploymentException e) {
      throw ConfigException.createConfig(e);
    }
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  @Override
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  public void initialize()
  {
    update();

    /*
    if (_lifecycle.toInit()) {
      fireEvent(this, new AnnotationLiteral<Initialized>() {});
    }
    */
  }

  public void start()
  {
    if (_isStarted)
      return;

    _isStarted = true;

    //initialize();

    //bind();

    startServices();

    if (_configException != null) {
      // ioc/0p91
      throw _configException;
    }

    notifyStart();
  }

  public void notifyStart()
  {

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      update();

      // cloud/0300
      if (_isAfterValidationNeeded) {
        _isAfterValidationNeeded = false;
        getExtensionManager().fireAfterDeploymentValidation();
      }
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    // ioc/0p30
    if (_configException != null)
      throw _configException;
  }

  public void addDefinitionError(Throwable t)
  {
    if (_configException != null) {
      log.log(Level.WARNING, t.toString(), t);
    }
    else if (t instanceof RuntimeException) {
      _configException = (RuntimeException) t;
    }
    else {
      _configException = ConfigException.create(t);
    }
  }

  public RuntimeException getConfigException()
  {
    return _configException;
  }

  public void addConfiguredBean(String className)
  {
    _extCustomBean.addConfiguredBean(className);
  }

  public void addInjectionTargetCustom(long cookie, InjectionTarget<?> target)
  {
    _injectionTargetCustomMap.put(cookie, target);
  }

  public InjectionTarget<?> getInjectionTargetCustomBean(long cookie)
  {
    return _injectionTargetCustomMap.get(cookie);
  }

  void addService(Bean<?> bean)
  {
    _pendingServiceList.add(bean);
  }

  /**
   * Initialize all the services
   */
  protected void startServices()
  {
    ArrayList<Bean<?>> services;
    // ArrayList<ManagedBean> registerServices;

    synchronized (_pendingServiceList) {
      services = new ArrayList<Bean<?>>(_pendingServiceList);
      _pendingServiceList.clear();
    }

    for (Bean<?> bean : services) {
      CreationalContext<?> env = createCreationalContext(bean);

      getReference(bean, bean.getBeanClass(), env);
    }
    
    _extCustomBean.startupBeans();

    /*
    for (ManagedBean bean : registerServices) {
      startRegistration(bean);
    }
    */
  }

  /**
   * Handles the case where the environment is stopping
   */
  @Override
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public void destroy()
  {
    _singletonScope.closeContext();

    // _parent = null;
    _classLoader = null;
    _deploymentMap = null;

    _selfBeanMap = null;
    _selfProxySet= null;
    _selfNamedBeanMap = null;
    // _beanMap = null;
    _namedBeanMap = null;
    _contextMap = null;
    _proxySet = null;

    //_interceptorsBuilder = null;
    // _interceptorList = null;
    //_decoratorList = null;
    _pendingBindList = null;
    _pendingServiceList = null;

    _eventManager = null;
    //_decoratorsBuilder = null;
  }

  public static ConfigException injectError(AccessibleObject prop, String msg)
  {
    String location = "";

    if (prop instanceof Field) {
      Field field = (Field) prop;
      String className = field.getDeclaringClass().getName();

      location = className + "." + field.getName() + ": ";
    }
    else if (prop instanceof Method) {
      Method method = (Method) prop;
      String className = method.getDeclaringClass().getName();

      location = className + "." + method.getName() + ": ";
    }

    return new ConfigException(location + msg);
  }

  public static String location(Field field)
  {
    return field.getDeclaringClass().getName() + "." + field.getName() + ": ";
  }

  public static String location(Method method)
  {
    return LineConfigException.loc(method);
  }

  public static DefinitionException error(Method method, String msg)
  {
    return new DefinitionException(location(method) + msg);
  }

  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serialization rewriting
   */
  public Object writeReplace()
  {
    return _serializationHandle;
  }

  public void checkActive()
  {
  }

  /**
   * @return
   */
  public boolean isClosed()
  {
    // return _beanMap == null;
    return false;
  }

  public String toString()
  {
    //String className = "InjectManager"; // XXX: temp for QA
    String className = getClass().getSimpleName();
    
    if (_classLoader != null)
      return className + "[" + _classLoader.getId() + "]";
    else
      return className + "[" + _id + "]";
  }

  static String getSimpleName(Type type)
  {
    if (type instanceof Class<?>)
      return ((Class<?>) type).getSimpleName();
    else
      return String.valueOf(type);
  }

  private AnnotatedType<?> processSyntethicAnnotatedType(SyntheticAnnotatedType syntheticType)
  {
    AnnotatedType<?> type =
      _extensionManager.processSyntheticAnnotatedType(syntheticType.getExtension(),
                                                      syntheticType.getType());

    return type;
  }

  class TypedBean {
    private final BaseType _type;
    private final Annotated _annotated;
    private final Bean<?> _bean;
    private final boolean _isModulePrivate;
    private boolean _isValidated;

    TypedBean(BaseType type, Annotated annotated, Bean<?> bean)
    {
      _type = type;
      _annotated = annotated;
      _bean = bean;

      _isModulePrivate = isModulePrivate(bean) || bean.isAlternative();
    }

    public Annotated getAnnotated()
    {
      return _annotated;
    }

    /**
     *
     */
    public void validate()
    {
      if (! _isValidated) {
        _isValidated = true;

        CandiManager.this.validate(_bean);
        /*
        for (InjectionPoint ip : _bean.getInjectionPoints()) {
          InjectManager.this.validate(ip);
        }
        */
      }
    }

    boolean isModulePrivate()
    {
      return _isModulePrivate;
    }

    BaseType getType()
    {
      return _type;
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    boolean isModulePrivate(Bean<?> bean)
    {
      if (! (bean instanceof AnnotatedBean))
        return false;

      Annotated annotated = ((AnnotatedBean) bean).getAnnotated();

      if (annotated == null)
        return false;

      for (Annotation ann : annotated.getAnnotations()) {
        Class<?> annType = ann.annotationType();

        if (annType.equals(ModulePrivate.class)
            || annType.isAnnotationPresent(ModulePrivate.class)
            || annType.equals(Module.class)
            || annType.isAnnotationPresent(Module.class)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public int hashCode()
    {
      return 65521 * _type.hashCode() + _bean.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (! (o instanceof TypedBean))
        return false;

      TypedBean bean = (TypedBean) o;

      return _type.equals(bean._type) && _bean.equals(bean._bean);
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _type + "," + _bean + "]";
    }
  }

  static class FillByName implements EnvironmentApply
  {
    private String _name;
    private ArrayList<Bean<?>> _beanList;

    FillByName(String name, ArrayList<Bean<?>> beanList)
    {
      _name = name;
      _beanList = beanList;
    }

    public void apply(EnvironmentClassLoader loader)
    {
      CandiManager beanManager = CandiManager.getCurrent(loader);

      beanManager.fillByName(_name, _beanList);
    }
  }

  static class FillByType implements EnvironmentApply
  {
    private BaseType _baseType;
    private HashSet<TypedBean> _beanSet;
    private CandiManager _manager;

    FillByType(BaseType baseType,
               HashSet<TypedBean> beanSet,
               CandiManager manager)
    {
      _baseType = baseType;
      _beanSet = beanSet;
      _manager = manager;
    }

    @Override
    public void apply(EnvironmentClassLoader loader)
    {
      CandiManager injectManager = CandiManager.getCurrent(loader);
      BeanManagerBase beanManager = injectManager.getBeanManager();

      injectManager.fillByType(_baseType, _beanSet, beanManager);
    }
  }

  static class FactoryBinding {
    private static final Annotation []NULL = new Annotation[0];

    private final Type _type;
    private final Annotation []_ann;

    FactoryBinding(Type type, Annotation []ann)
    {
      _type = type;

      if (ann != null)
        _ann = ann;
      else
        _ann = NULL;
    }

    @Override
    public int hashCode()
    {
      int hash = _type.hashCode();

      for (Annotation ann : _ann)
        hash = 65521 * hash + ann.hashCode();

      return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (! (obj instanceof FactoryBinding))
        return false;

      FactoryBinding binding = (FactoryBinding) obj;

      if (_type != binding._type)
        return false;

      if (_ann.length != binding._ann.length)
        return false;

      for (int i = 0; i < _ann.length; i++) {
        if (! _ann[i].equals(binding._ann[i]))
          return false;
      }

      return true;
    }
  }

  static class InjectBean<X> extends BeanWrapper<X>
    implements PassivationCapable, ScopeAdapterBean<X>
  {
    private ClassLoader _loader;

    InjectBean(Bean<X> bean, CandiManager beanManager)
    {
      super(beanManager, bean);

      _loader = Thread.currentThread().getContextClassLoader();

      if (bean instanceof BeanBase) {
        BeanBase<X> absBean = (BeanBase<X>) bean;
        Annotated annotated = absBean.getAnnotated();

        if (annotated != null
            && annotated.isAnnotationPresent(ContextDependent.class)) {
          // ioc/0e17
          _loader = null;
        }
      }
    }

    public String getId()
    {
      Bean<?> bean = getBean();

      if (bean instanceof PassivationCapable)
        return ((PassivationCapable) bean).getId();
      else
        return null;
    }

    public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> cxt)
    {
      Bean<?> bean = getBean();

      if (bean instanceof ScopeAdapterBean<?>)
        return (X) ((ScopeAdapterBean) bean).getScopeAdapter(topBean, cxt);
      else
        return null;
    }

    @Override
    public X create(CreationalContext<X> env)
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        if (_loader != null) {
          // ioc/0e17
          thread.setContextClassLoader(_loader);
        }

        X value = getBean().create(env);

        return value;
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    @Override
    public int hashCode()
    {
      return getBean().hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof InjectBean<?>))
        return false;

      InjectBean<?> bean = (InjectBean<?>) o;

      return getBean().equals(bean.getBean());
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + getBean() + "]";
    }
  }

  private class BeanSupplier<T> implements Supplier<T>
  {
    private final Bean<T> _bean;

    BeanSupplier(Bean<T> bean)
    {
      _bean = bean;
    }

    @Override
    public final T get()
    {
      return getReference(_bean);
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _bean + "]";
    }
  }

  static private class SyntheticAnnotatedType
  {
    private Extension _extension;
    private AnnotatedType _type;
    private String _id;
    private boolean _isForced;

    private SyntheticAnnotatedType(Extension extension,
                                   AnnotatedType type,
                                   String id,
                                   boolean isForced)
    {
      _extension = extension;
      _type = type;
      _id = id;
      _isForced = isForced;
    }

    private Extension getExtension()
    {
      return _extension;
    }

    private AnnotatedType getType()
    {
      return _type;
    }

    public String getId()
    {
      if (_id == null)
        _id = _type.getJavaClass().getName();

      return _id;
    }

    public boolean isForced()
    {
      return _isForced;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + '[' + _type + ']';
    }

    public boolean isVetoedAnnotated()
    {
      return _type.isAnnotationPresent(Vetoed.class);
    }
  }

  static class PriorityComparator implements Comparator<Class<?>>
  {
    @Override
    public int compare(Class<?> a, Class<?> b)
    {
      Priority aPriorityAnn = a.getAnnotation(Priority.class);
      Priority bPriorityAnn = b.getAnnotation(Priority.class);

      int aPriority = Integer.MAX_VALUE;
      if (aPriorityAnn != null)
        aPriority = aPriorityAnn.value();

      int bPriority = Integer.MAX_VALUE;
      if (bPriorityAnn != null)
        bPriority = bPriorityAnn.value();

      return aPriority - bPriority;
    }
  }
  
  static class ComparatorAnnotatedType implements Comparator<AnnotatedType<?>> {
    private static final ComparatorAnnotatedType CMP = new ComparatorAnnotatedType();
    
    @Override
    public int compare(AnnotatedType<?> a, AnnotatedType<?> b)
    {
      return a.getBaseType().toString().compareTo(b.getBaseType().toString());
    }
  }

  static {
    ArrayList<Class<?>> forbiddenAnnotations = new ArrayList<>();
    ArrayList<Class<?>> forbiddenClasses = new ArrayList<>();

    for (String className : FORBIDDEN_ANNOTATIONS) {
      try {
        Class<?> cl = Class.forName(className);

        if (cl != null)
          forbiddenAnnotations.add(cl);
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    for (String className : FORBIDDEN_CLASSES) {
      try {
        Class<?> cl = Class.forName(className);

        if (cl != null)
          forbiddenClasses.add(cl);
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    _forbiddenAnnotations = new Class[forbiddenAnnotations.size()];
    forbiddenAnnotations.toArray(_forbiddenAnnotations);

    _forbiddenClasses = new Class[forbiddenClasses.size()];
    forbiddenClasses.toArray(_forbiddenClasses);

    ClassLoader systemClassLoader = null;

    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Throwable e) {
      // a security manager may not allow this call

      log.log(Level.FINEST, e.toString(), e);
    }

    _systemClassLoader = systemClassLoader;
  }
}

