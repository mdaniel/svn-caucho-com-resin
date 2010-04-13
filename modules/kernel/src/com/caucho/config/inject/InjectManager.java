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

package com.caucho.config.inject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.naming.InitialContext;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import com.caucho.config.CauchoDeployment;
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.Configured;
import com.caucho.config.ContextDependent;
import com.caucho.config.LineConfigException;
import com.caucho.config.ModulePrivate;
import com.caucho.config.ModulePrivateLiteral;
import com.caucho.config.el.WebBeansContextResolver;
import com.caucho.config.j2ee.EjbHandler;
import com.caucho.config.j2ee.PersistenceContextHandler;
import com.caucho.config.j2ee.PersistenceUnitHandler;
import com.caucho.config.j2ee.ResourceHandler;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.BaseTypeFactory;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.DependentContext;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.SingletonScope;
import com.caucho.config.xml.XmlStandardPlugin;
import com.caucho.inject.Module;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentApply;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * The web beans container for a given environment.
 */
@ModulePrivate
@SuppressWarnings("serial")
public class InjectManager
  implements BeanManager, EnvironmentListener,
             java.io.Serializable, HandleAware
{
  private static final L10N L = new L10N(InjectManager.class);
  private static final Logger log
    = Logger.getLogger(InjectManager.class.getName());

  private static final EnvironmentLocal<InjectManager> _localContainer
    = new EnvironmentLocal<InjectManager>();

  private static final int DEFAULT_PRIORITY = 1;

  private static final Annotation []CURRENT_ANN
    = CurrentLiteral.CURRENT_ANN_LIST;

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
    "javax.servlet.Servlet",
    "javax.servlet.Filter",
    "javax.servlet.ServletContextListener",
    "javax.servlet.http.HttpSessionListener",
    "javax.servlet.ServletRequestListener",
    "javax.ejb.EnterpriseBean",
    "javax.faces.component.UIComponent",
    "javax.enterprise.inject.spi.Extension",
  };

  private static final Class<? extends Annotation> []_forbiddenAnnotations;
  private static final Class<?> []_forbiddenClasses;

  private String _id;

  private InjectManager _parent;

  private EnvironmentClassLoader _classLoader;
  
  private final InjectScanManager _scanManager;
  private final ExtensionManager _extensionManager
    = new ExtensionManager(this);
  
  private AtomicLong _version = new AtomicLong();

  private HashSet<String> _configuredClasses
    = new HashSet<String>();

  private HashSet<Class<?>> _specializedClasses
    = new HashSet<Class<?>>();

  private HashMap<Class<?>,Integer> _deploymentMap
    = new HashMap<Class<?>,Integer>();

  private BaseTypeFactory _baseTypeFactory = new BaseTypeFactory();

  private HashMap<Class<?>,InjectionPointHandler> _injectionMap
    = new HashMap<Class<?>,InjectionPointHandler>();

  //
  // self configuration
  //

  private HashMap<Class<?>,Set<TypedBean>> _selfBeanMap
    = new HashMap<Class<?>,Set<TypedBean>>();

  private HashMap<String,ArrayList<Bean<?>>> _selfNamedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<Class<?>,ObserverMap> _extObserverMap
    = new HashMap<Class<?>,ObserverMap>();

  private HashMap<String,Bean<?>> _selfPassivationBeanMap
    = new HashMap<String,Bean<?>>();

  //
  // combined visibility configuration
  //

  private HashMap<Class<?>,WebComponent> _beanMap
    = new HashMap<Class<?>,WebComponent>();

  private HashMap<String,ArrayList<Bean<?>>> _namedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<Class<?>,ObserverMap> _observerMap
    = new HashMap<Class<?>,ObserverMap>();

  private HashMap<Type,Bean<?>> _newBeanMap
    = new HashMap<Type,Bean<?>>();
  
  private HashSet<Class<? extends Annotation>> _qualifierSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _scopeTypeSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _normalScopeSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _passivatingScopeSet
    = new HashSet<Class<? extends Annotation>>();

  private HashMap<Class<?>,Context> _contextMap
    = new HashMap<Class<?>,Context>();

  private HashMap<Class<?>,ArrayList<ObserverMap>> _observerListCache
    = new HashMap<Class<?>,ArrayList<ObserverMap>>();

  private ArrayList<InterceptorEntry<?>> _interceptorList
    = new ArrayList<InterceptorEntry<?>>();

  private ArrayList<DecoratorEntry<?>> _decoratorList
    = new ArrayList<DecoratorEntry<?>>();

  private HashSet<Bean<?>> _beanSet = new HashSet<Bean<?>>();

  private boolean _isUpdateNeeded = true;

  private ArrayList<Path> _pendingPathList
    = new ArrayList<Path>();

  private ArrayList<AnnotatedType<?>> _pendingAnnotatedTypes
    = new ArrayList<AnnotatedType<?>>();

  private ArrayList<AbstractBean<?>> _pendingBindList
    = new ArrayList<AbstractBean<?>>();

  private ArrayList<Bean<?>> _pendingServiceList
    = new ArrayList<Bean<?>>();

  private boolean _isBeforeBeanDiscoveryComplete;
  private boolean _isAfterBeanDiscoveryComplete;

  // XXX: needs to be a local resolver
  private ELResolver _elResolver = new WebBeansContextResolver();

  private DependentContext _dependentContext = new DependentContext();
  private SingletonScope _singletonScope = new SingletonScope();
  private ApplicationScope _applicationScope = new ApplicationScope();
  private XmlStandardPlugin _xmlExtension;

  private RuntimeException _configException;

  private Object _serializationHandle;

  private InjectManager(String id,
                        InjectManager parent,
                        EnvironmentClassLoader loader,
                        boolean isSetLocal)
  {
    _id = id;

    _classLoader = loader;
    
    _parent = parent;
    
    _scanManager = new InjectScanManager(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (isSetLocal) {
        _localContainer.set(this, _classLoader);

        if (_parent == null) {
          _localContainer.setGlobal(this);
        }
      }

      if (_classLoader != null)
        _classLoader.getNewTempClassLoader();
      else
        new DynamicClassLoader(null);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void init(boolean isSetLocal)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      try {
        InitialContext ic = new InitialContext();
        ic.rebind("java:comp/BeanManager", new WebBeansJndiProxy());
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      addContext("com.caucho.server.webbeans.RequestScope");
      addContext("com.caucho.server.webbeans.SessionScope");
      addContext("com.caucho.server.webbeans.ConversationScope");
      addContext("com.caucho.server.webbeans.TransactionScope");
      addContext(_applicationScope);
      addContext(_singletonScope);
      addContext(_dependentContext);

      _injectionMap.put(PersistenceContext.class,
                        new PersistenceContextHandler(this));
      _injectionMap.put(PersistenceUnit.class,
                        new PersistenceUnitHandler(this));
      _injectionMap.put(Resource.class,
                        new ResourceHandler(this));
      _injectionMap.put(EJB.class,
                        new EjbHandler(this));

      _deploymentMap.put(CauchoDeployment.class, 0);
      // DEFAULT_PRIORITY
      _deploymentMap.put(Configured.class, 2);

      BeanFactory<InjectManager> factory = createBeanFactory(InjectManager.class);
      // factory.deployment(Standard.class);
      factory.type(InjectManager.class);
      factory.type(BeanManager.class);
      factory.annotation(ModulePrivateLiteral.create());
      addBean(factory.singleton(this));

      _xmlExtension = new XmlStandardPlugin(this);
      addExtension(_xmlExtension);
      _extensionManager.createExtension("com.caucho.server.webbeans.ResinStandardPlugin");
      

      if (_classLoader != null && isSetLocal) {
        // _classLoader.addScanListener(this);
        _classLoader.addScanListener(_scanManager);
      }

      Environment.addEnvironmentListener(this, _classLoader);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the modification version.
   */
  public long getVersion()
  {
    return _version.get();
  }
  
  InjectScanManager getScanManager()
  {
    return _scanManager;
  }

  private void addContext(String contextClassName)
  {
    try {
      Class<?> cl = Class.forName(contextClassName);
      Context context = (Context) cl.newInstance();

      addContext(context);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static InjectManager getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static InjectManager getCurrent(ClassLoader loader)
  {
    synchronized (_localContainer) {
      return _localContainer.get(loader);
    }
  }

  /**
   * Returns the current active container.
   */
  public static InjectManager create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current active container.
   */
  public static InjectManager create(ClassLoader loader)
  {
    InjectManager manager = null;

    manager = _localContainer.getLevel(loader);
    
    if (manager != null)
      return manager;
      
    EnvironmentClassLoader envLoader
      = Environment.getEnvironmentClassLoader(loader);

    // ejb doesn't create a new InjectManager even though it's a new
    // environment
    if (envLoader != null
        && Boolean.FALSE.equals(envLoader.getAttribute("caucho.inject"))) {
      manager = create(envLoader.getParent());
      
      if (manager != null)
        return manager;
    }

    String id;

    if (envLoader != null)
      id = envLoader.getId();
    else
      id = "";

    InjectManager parent = null;

    if (envLoader != null)
      parent = create(envLoader.getParent());

    synchronized (_localContainer) {
      manager = _localContainer.getLevel(envLoader);
        
      if (manager != null)
        return manager;
        
      manager = new InjectManager(id, parent, envLoader, true);
    }
      
    manager.init(true);
    
    return manager;
  }

  /**
   * Returns the current active container.
   */
  public InjectManager createParent(String prefix)
  {
    _parent = new InjectManager(prefix + _id,
                                _parent,
                                _classLoader,
                                false);
    _parent.init(false);

    return _parent;
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public InjectManager getParent()
  {
    return _parent;
  }

  public ApplicationScope getApplicationScope()
  {
    return _applicationScope;
  }

  public void setParent(InjectManager parent)
  {
    _parent = parent;
  }

  public void addPath(Path path)
  {
    _pendingPathList.add(path);
  }

  public void setDeploymentTypes(ArrayList<Class<?>> deploymentList)
  {
    _deploymentMap.clear();

    _deploymentMap.put(CauchoDeployment.class, 0);
    // DEFAULT_PRIORITY

    int priority = DEFAULT_PRIORITY + 1;

    if (! deploymentList.contains(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }

    for (int i = deploymentList.size() - 1; i >= 0; i--) {
      _deploymentMap.put(deploymentList.get(i), priority);
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

    beanList.add(bean);

    _namedBeanMap.remove(name);
  }

  /**
   * Adds a bean by the interface type
   *
   * @param type the interface type to expose the component
   * @param bean the component to register
   */
  private void addBeanByType(Type type, Bean<?> comp)
  {
    if (type == null)
      return;

    addBeanByType(createBaseType(type), comp);
  }

  private void addBeanByType(BaseType type, Bean<?> bean)
  {
    if (type == null)
      return;

    if (log.isLoggable(Level.FINEST))
      log.finest(bean + "(" + type + ") added to " + this);

    Class<?> rawType = type.getRawClass();

    Set<TypedBean> beanSet = _selfBeanMap.get(rawType);

    if (beanSet == null) {
      beanSet = new HashSet<TypedBean>();
      _selfBeanMap.put(rawType, beanSet);
    }
    _beanMap.remove(rawType);

    beanSet.add(new TypedBean(type, bean));
  }

  /**
   * Returns the scope context corresponding to the scope annotation type.
   *
   * @param scope the scope annotation type identifying the scope
   */
  public ScopeContext getScopeContext(Class<?> scope)
  {
    if (scope == null)
      throw new NullPointerException();
    else if (Dependent.class.equals(scope))
      return null;

    Context context = _contextMap.get(scope);

    if (context instanceof ScopeContext)
      return (ScopeContext) context;
    else
      return null;
  }

  /**
   * Finds a component by its component name.
   */
  protected ArrayList<Bean<?>> findByName(String name)
  {
    // #3334 - shutdown timing issues
    HashMap<String,ArrayList<Bean<?>>> namedBeanMap = _namedBeanMap;

    if (namedBeanMap == null)
      return null;

    ArrayList<Bean<?>> beanList = _namedBeanMap.get(name);

    if (beanList == null) {
      beanList = new ArrayList<Bean<?>>();

      if (_classLoader != null)
        _classLoader.applyVisibleModules(new FillByName(name, beanList));

      for (int i = beanList.size() - 1; i >= 0; i--) {
        if (getDeploymentPriority(beanList.get(i)) < 0) {
          beanList.remove(i);
        }
      }

      _namedBeanMap.put(name, beanList);
    }

    return beanList;
  }

  private void fillByName(String name, ArrayList<Bean<?>> beanList)
  {
    ArrayList<Bean<?>> localBeans = _selfNamedBeanMap.get(name);

    if (localBeans != null) {
      for (Bean<?> bean : localBeans) {
        if (getDeploymentPriority(bean) < 0)
          continue;

        if (! beanList.contains(bean))
          beanList.add(bean);
      }
    }
  }

  //
  // javax.webbeans.Container
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
    ManagedBeanImpl<T> bean = createManagedBean(type);

    // server/10gn
    //return factory.create(new ConfigContext());
    InjectionTarget<T> injectionTarget = bean.getInjectionTarget();

    CreationalContext<T> env = new CreationalContextImpl<T>(bean);

    T instance = injectionTarget.produce(env);
    injectionTarget.inject(instance, env);

    return instance;
  }

  public ELContext getELContext()
  {
    return new ConfigELContext();
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanFactory<T> createBeanFactory(ManagedBeanImpl<T> managedBean)
  {
    return new BeanFactory<T>(managedBean);
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanFactory<T> createBeanFactory(Class<T> type)
  {
    return createBeanFactory(createManagedBean(type));
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanFactory<T> createBeanFactory(AnnotatedType<T> type)
  {
    return createBeanFactory(createManagedBean(type));
  }

  //
  // enabled deployment types, scopes, and binding types
  //

  /**
   * Returns the enabled deployment types
   */
  public List<Class<? extends Annotation>> getEnabledDeploymentTypes()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
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
    NormalScope scope =  annotationType.getAnnotation(NormalScope.class);

    if (scope != null && scope.passivating())
      return true;
    
    return _passivatingScopeSet.contains(annotationType);
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
  public boolean isInterceptorBindingType(Class<? extends Annotation> annotationType)
  {
    return annotationType.isAnnotationPresent(Qualifier.class);
  }

  /**
   * Returns the bindings for an interceptor binding type
   */
  @Override
  public Set<Annotation> getInterceptorBindingTypeDefinition(Class<? extends Annotation> bindingType)
  {
    return new HashSet<Annotation>();
  }

  /**
   * Tests if an annotation is an enabled stereotype.
   */
  @Override
  public boolean isStereotype(Class<? extends Annotation> annotationType)
  {
    return annotationType.isAnnotationPresent(Stereotype.class);
  }

  /**
   * Returns the annotations associated with a stereotype
   */
  @Override
  public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // bean resolution and instantiation
  //

  /**
   * Creates a BaseType from a Type
   */
  public BaseType createBaseType(Type type)
  {
    return _baseTypeFactory.create(type);
  }

  /**
   * Creates a BaseType from a Type
   */
  public BaseType createClassBaseType(Class<?> type)
  {
    return _baseTypeFactory.createClass(type);
  }

  /**
   * Creates a BaseType from a Type
   */
  public BaseType createBaseType(Type type, HashMap<String,BaseType> paramMap)
  {
    return _baseTypeFactory.create(type, paramMap);
  }

  /**
   * Creates an annotated type.
   */
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> cl)
  {
    return ReflectionAnnotatedFactory.introspectType(cl);
  }

  /**
   * Creates an injection target
   */
  public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
  {
    InjectionTargetImpl<T> bean = new InjectionTargetImpl<T>(this, type);

    bean.introspect();

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public <T> InjectionTarget<T> createInjectionTarget(Class<T> type)
  {
    return createInjectionTarget(createAnnotatedType(type));
  }

  /**
   * Processes the discovered InjectionTarget
   */
  private <T> InjectionTarget<T> processInjectionTarget(InjectionTarget<T> target)
  {
    AnnotatedType<T> annotatedType = null;
    
    if (target instanceof InjectionTargetImpl<?>)
      annotatedType = ((InjectionTargetImpl<T>) target).getAnnotatedType();
    
    ProcessInjectionTargetImpl<T> processTarget
      = new ProcessInjectionTargetImpl<T>(target, annotatedType);
    
    BaseType eventType = createBaseType(ProcessInjectionTargetImpl.class);
    eventType = eventType.fill(createBaseType(annotatedType.getBaseType()));

    fireExtensionEvent(processTarget, eventType);

    return (InjectionTarget<T>) processTarget.getInjectionTarget();
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(AnnotatedType<T> type)
  {
    InjectionTarget<T> target = createInjectionTarget(type);

    ManagedBeanImpl<T> bean = new ManagedBeanImpl<T>(this, type, target);
    bean.introspect();

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(Class<T> cl)
  {
    AnnotatedType<T> type = createAnnotatedType(cl);

    return createManagedBean(type);
  }

  /**
   * Processes the discovered bean
   */
  public <T> Bean<T> processBean(Bean<T> bean)
  {
    ProcessBeanImpl<T> processBean = new ProcessBeanImpl<T>(this, bean);

    fireExtensionEvent(processBean);

    if (processBean.isVeto())
      return null;
    else
      return processBean.getBean();
  }

  /**
   * Adds a new bean definition to the manager
   */
  public <T> void addBean(Bean<T> bean)
  {
    bean = processBean(bean);

    if (bean == null)
      return;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " add bean " + bean);

    _version.incrementAndGet();

    // bean = new InjectBean<T>(bean, this);

    _beanSet.add(bean);

    for (Type type : bean.getTypes()) {
      addBeanByType(type, bean);
    }

    if (bean.getName() != null) {
      addBeanByName(bean.getName(), bean);
    }

    if (bean instanceof PassivationCapable) {
      PassivationCapable pass = (PassivationCapable) bean;

      if (pass.getId() != null)
        _selfPassivationBeanMap.put(pass.getId(), bean);
    }

    registerJmx(bean);
  }

  private void registerJmx(Bean<?> bean)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      /*
      WebBeanAdmin admin = new WebBeanAdmin(bean, _beanId);

      admin.register();
      */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the bean definitions matching a given name
   *
   * @param name the name of the bean to match
   */
  public Set<Bean<?>> getBeans(String name)
  {
    ArrayList<Bean<?>> beanList = findByName(name);

    if (beanList != null)
      return new LinkedHashSet<Bean<?>>(beanList);
    else
      return new LinkedHashSet<Bean<?>>();
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param bindings required @Qualifier annotations
   */
  public Set<Bean<?>> getBeans(Type type,
                               Annotation... bindings)
  {
    Set<Bean<?>> set = resolve(type, bindings);

    if (set != null)
      return (Set<Bean<?>>) set;
    else
      return new HashSet<Bean<?>>();
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set<Bean<?>> resolve(Type type, Annotation []bindings)
  {
    if (bindings == null || bindings.length == 0) {
      if (Object.class.equals(type))
        return resolveAllBeans();

      bindings = CURRENT_ANN;
    }

    BaseType baseType = createBaseType(type);

    return resolve(baseType, bindings);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set<Bean<?>> resolve(BaseType baseType,
                               Annotation []bindings)
  {
    WebComponent component = getWebComponent(baseType);

    if (component != null) {
      Set<Bean<?>> beans = component.resolve(baseType, bindings);

      if (beans != null && beans.size() > 0) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " bind(" + baseType.getSimpleName()
                     + "," + toList(bindings) + ") -> " + beans);

        return beans;
      }
    }
    else if (New.class.equals(bindings[0].annotationType())) {
      // ioc/0721
      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      AbstractBean<?> newBean = new NewBean(this, new AnnotatedTypeImpl(baseType.getRawClass(), baseType.getRawClass()));
      newBean.introspect();

      set.add(newBean);

      return set;
    }

    Class<?> rawType = baseType.getRawClass();

    if (Instance.class.equals(rawType)) {
      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new InstanceBeanImpl(this, beanType, bindings));
      return set;
    }
    else if (Event.class.equals(rawType)) {
      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new EventBeanImpl(this, beanType, bindings));
      return set;
    }

    if (_parent != null) {
      return _parent.resolve(baseType, bindings);
    }

    for (Annotation ann : bindings) {
      if (! ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        throw new IllegalArgumentException(L.l("'{0}' is an invalid binding annotation because it does not have a @Qualifier meta-annotation",
                                               ann));
      }
    }

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " bind(" + baseType.getSimpleName()
                + "," + toList(bindings) + ") -> none");
    }

    return null;
  }


  /**
   * Returns the web beans component with a given binding list.
   */
  public Set<Bean<?>> resolveAllByType(Class<?> type)
  {
    Annotation []bindings = new Annotation[0];

    WebComponent component = getWebComponent(createBaseType(type));

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

  private WebComponent getWebComponent(BaseType baseType)
  {
    if (_beanMap == null)
      return null;

    WebComponent beanSet = _beanMap.get(baseType.getRawClass());

    if (beanSet == null) {
      HashSet<TypedBean> typedBeans = new HashSet<TypedBean>();

      if (_classLoader != null) {
        FillByType fillByType = new FillByType(baseType, typedBeans, this);

        _classLoader.applyVisibleModules(fillByType);
      }

      Class<?> rawClass = baseType.getRawClass();
      
      beanSet = new WebComponent(this, rawClass);

      for (TypedBean typedBean : typedBeans) {
        if (getDeploymentPriority(typedBean.getBean()) < 0)
          continue;

        beanSet.addComponent(typedBean.getType(), typedBean.getBean());
      }
      
      _beanMap.put(rawClass, beanSet);
    }

    return beanSet;
  }

  private void fillByType(BaseType baseType,
                          HashSet<TypedBean> beanSet,
                          InjectManager beanManager)
  {
    Class<?> rawClass = baseType.getRawClass();
    
    InjectScanClass scanClass = _scanManager.getScanClass(rawClass.getName());
    
    if (scanClass != null) {
      discoverScanClass(scanClass);
      processPendingAnnotatedTypes();
    }
      
    Set<TypedBean> localBeans = _selfBeanMap.get(rawClass);
    
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
        if (getDeploymentPriority(bean.getBean()) < 0)
          continue;

        if (bean.isModulePrivate() && this != beanManager)
          continue;

        beanSet.add(bean);
      }
    }
  }

  public <X> Bean<X> getMostSpecializedBean(Bean<X> bean)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
  {
    Bean<? extends X> bestBean = null;
    Bean<? extends X> secondBean = null;
    int bestPriority = -1;

    for (Bean<? extends X> bean : beans) {
      int priority = getDeploymentPriority(bean);

      if (bestPriority < priority) {
        bestBean = bean;
        secondBean = null;
        bestPriority = priority;
      }
      else if (bestPriority == priority) {
        secondBean = bean;
      }
    }

    if (secondBean == null)
      return bestBean;
    else
      throw ambiguousException(beans, bestPriority);
  }

  public void validate(InjectionPoint ij)
  {
    //     throw new UnsupportedOperationException(getClass().getName());
  }

  public int getDeploymentPriority(Bean<?> bean)
  {
    int priority = DEFAULT_PRIORITY;

    Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();

    if (stereotypes != null) {
      for (Class<? extends Annotation> annType : stereotypes) {
        Integer value = _deploymentMap.get(annType);

        if (value != null && priority < value)
          priority = value;
      }
    }

    if (bean instanceof AbstractBean<?>) {
      // ioc/0213
      AbstractBean<?> absBean = (AbstractBean<?>) bean;

      if (absBean.getBeanManager() == this)
        priority += 1000000;
    }
    else
      priority += 1000000;

    return priority;
  }

  private Set<Bean<?>> resolveAllBeans()
  {
    synchronized (_beanMap) {
      LinkedHashSet<Bean<?>> beans = new LinkedHashSet<Bean<?>>();

      for (Set<TypedBean> comp : _selfBeanMap.values()) {
        for (TypedBean typedBean : comp) {
          beans.add(typedBean.getBean());
        }
      }

      return beans;
    }
  }

  @Override
  public <T> CreationalContextImpl<T> createCreationalContext(Contextual<T> bean)
  {
    return new CreationalContextImpl<T>(bean);
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(Class<T> type, Annotation... bindings)
  {
    Set<Bean<?>> beans = getBeans(type);
    Bean<?> bean = resolve(beans);

    if (bean == null)
      return null;

    CreationalContext<?> env = createCreationalContext(bean);

    return (T) getReference(bean, type, env);
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(String name)
  {
    Set<Bean<?>> beans = getBeans(name);
    Bean<?> bean = resolve(beans);

    if (bean == null)
      return null;

    CreationalContext<?> env = createCreationalContext(bean);

    return (T) getReference(bean, bean.getBeanClass(), env);
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
    if (bean instanceof ScopeAdapterBean<?>) {
      ScopeAdapterBean<?> simpleBean = (ScopeAdapterBean<?>) bean;

      Object adapter = simpleBean.getScopeAdapter(bean, null);

      if (adapter != null)
        return adapter;
    }
    
    CreationalContextImpl<?> env
      = (CreationalContextImpl<?>) createContext;

    if (env != null) {
      Object object = env.get(bean);

      if (object != null)
        return object;
    }

    Object object = getInstanceRec(bean, type, env, this);

    return object;
  }

  /**
   * Used by ScopeProxy
   */
  public Object getReference(Bean<?> bean)
  {
    CreationalContext<?> env = createCreationalContext(bean);

    return getReference(bean, bean.getBeanClass(), env);
  }

  /**
   * Used by ScopeProxy
   */
  public Object getInstance(Bean<?> bean)
  {
    CreationalContext<?> env = createCreationalContext(bean);

    return getInstanceRec(bean, bean.getBeanClass(), env, this);
  }

  private <T> T getInstanceRec(Bean<T> bean,
                               Type type,
                               CreationalContext<?> createContext,
                               InjectManager topManager)
  {
    if (createContext == null)
      throw new NullPointerException();

    CreationalContextImpl<T> env
      = (CreationalContextImpl<T>) createContext;

    Class<? extends Annotation> scopeType = bean.getScope();

    if (Dependent.class.equals(scopeType)) {
      // server/4764

      T instance = env.get(bean);

      if (instance != null)
        return instance;
      else
        return bean.create(env);
    }

    if (scopeType == null) {
      throw new IllegalStateException("Unknown scope for " + bean);
    }

    InjectManager ownerManager;

    if (bean instanceof AbstractBean<?>)
      ownerManager = ((AbstractBean<?>) bean).getBeanManager();
    else
      ownerManager = this;

    Context context = ownerManager.getContextImpl(scopeType);

    if (context == null)
      return null;

    //CreationalContextImpl<T> createContext
    //  = new CreationalContextImpl<T>(bean, parentEnv);

    /*
    // ioc/0b10
    if (bean instanceof InjectBean<?>)
      bean = ((InjectBean<T>) bean).getBean();
      */

    return context.get(bean, env);
  }

  private RuntimeException unsatisfiedException(Type type,
                                                Annotation []bindings)
  {
    WebComponent component = getWebComponent(createBaseType(type));

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
        return new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans match the type and bindings {1}.\nBeans:{2}",
                                                      type,
                                                      toList(bindings),
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
      sb.append("\n    ").append(line);
    }

    return sb.toString();
  }

  /**
   * Convert an annotation array to a list for debugging purposes
   */
  private ArrayList<Annotation> toList(Annotation []annList)
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

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public Object getInjectableReference(InjectionPoint ij,
                                       CreationalContext<?> parentCxt)
  {
    Bean<?> bean = resolveByInjectionPoint(ij);

    if (bean instanceof ScopeAdapterBean<?>) {
      ScopeAdapterBean simpleBean = (ScopeAdapterBean<?>) bean;

      Object adapter = simpleBean.getScopeAdapter(bean, parentCxt);

      if (adapter != null)
        return adapter;
    }

    CreationalContextImpl<?> parentEnv = null;
    
    if (parentCxt instanceof CreationalContextImpl<?>)
      parentEnv = (CreationalContextImpl<?>) parentCxt;

    Object value = CreationalContextImpl.find(parentEnv, bean);

    if (value != null)
      return value;

    CreationalContext<?> cxt
      = new CreationalContextImpl(bean, parentEnv, ij);

    /*
    if (cxt instanceof ConfigContext) {
      ConfigContext env = (ConfigContext) cxt;

      // ioc/0770
      env.setInjectionPoint(ij);
    }
    */

    return getReference(bean, ij.getType(), cxt);
  }

  public Bean<?> resolveByInjectionPoint(InjectionPoint ij)
  {
    Type type = ij.getType();
    Set<Annotation> qualifiers = ij.getQualifiers();

    return resolveByInjectionPoint(type, qualifiers, ij);
  }

  public Bean<?> resolveByInjectionPoint(Type type,
                                         Set<Annotation> bindingSet,
                                         InjectionPoint ij)
  {
    Annotation []bindings;

    if (bindingSet != null && bindingSet.size() > 0) {
      bindings = new Annotation[bindingSet.size()];
      bindingSet.toArray(bindings);

      if (bindings.length == 1
          && bindings[0].annotationType().equals(New.class)) {
        return createNewBean(type);
      }
    }
    else
      bindings = new Annotation[] { DefaultLiteral.DEFAULT };

    Set<Bean<?>> set = getBeans(type, bindings);

    if (set == null || set.size() == 0) {
      if (InjectionPoint.class.equals(type))
        return new InjectionPointBean(this, ij);
      
      throw unsatisfiedException(type, bindings);
    }

    return resolve(set);

    /*
    else if (set.size() == 1) {
      Iterator iter = set.iterator();

      if (iter.hasNext()) {
        Bean bean = (Bean) iter.next();

        return bean;
      }
    }
    else {
      throw new AmbiguousResolutionException(L.l("'{0}' with binding {1} matches too many configured beans{2}",
                                                 BaseType.create(type, null),
                                                 bindingSet,
                                                 toLineList(set)));
    }

    return null;
*/
  }

  private Bean<?> createNewBean(Type type)
  {
    Bean<?> bean = _newBeanMap.get(type);

    if (bean == null) {
      BaseType baseType = createBaseType(type);

      AbstractBean<?> newBean = new NewBean(this, new AnnotatedTypeImpl(baseType.getRawClass(), baseType.getRawClass()));
      newBean.introspect();

      _newBeanMap.put(type, bean);
      bean = newBean;
    }

    return bean;
  }

  private <X> AmbiguousResolutionException
    ambiguousException(Set<Bean<? extends X>> beanSet, int bestPriority)
  {
    ArrayList<Bean<?>> matchBeans = new ArrayList<Bean<?>>();

    for (Bean<?> bean : beanSet) {
      int priority = getDeploymentPriority(bean);

      if (priority == bestPriority)
        matchBeans.add(bean);
    }

    return new AmbiguousResolutionException(L.l("Too many beans match, because they all have equal precedence.  See the @Stereotype and <enable> tags to choose a precedence.  Beans:{0}",
                                                listToLines(matchBeans)));
  }

  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  @Override
  public ExpressionFactory
    wrapExpressionFactory(ExpressionFactory expressionFactory)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // scopes
  //

  /**
   * Adds a new scope context
   */
  public void addContext(Context context)
  {
    /*
    Class<? extends Annotation> scopeType = context.getScope();
    
    Context oldContext = _contextMap.get(scopeType);
    
    if (oldContext != null && oldContext != context) {
      throw new IllegalStateException(L.l("{0} is an invalid new context because @{1} is already registered as a scope",
                                          context, scopeType.getName()));
    }
    */
    
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
    
    /*
    else if (! isScope(scopeType)) {
      throw new IllegalStateException(L.l("'@{0}' is not a valid scope because it does not have a @Scope annotation",
                                          scopeType));
    }
    */
    
    throw new ContextNotActiveException(L.l("'@{0}' is not an active Java Injection context.",
                                            scopeType.getName()));
  }

  /**
   * Returns the scope context for the given type
   */
  Context getContextImpl(Class<? extends Annotation> scopeType)
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

  //
  // event management
  //

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer)
  {
    BaseType observedType = createBaseType(observer.getObservedType());
    Set<Annotation> qualifierSet = observer.getObservedQualifiers();

    Annotation[] qualifiers = new Annotation[qualifierSet.size()];
    int i = 0;
    for (Annotation qualifier : qualifierSet) {
      qualifiers[i++] = qualifier;
    }

    addObserver(observer, observedType, qualifiers);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer,
                          Type type,
                          Annotation... bindings)
  {
    BaseType eventType = createBaseType(type);

    addObserver(observer, eventType, bindings);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer,
                          BaseType eventBaseType,
                          Annotation... bindings)
  {
    Class<?> eventType = eventBaseType.getRawClass();

    checkActive();

    /*
    if (eventType.getTypeParameters() != null
        && eventType.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's a parameterized type.",
                                             eventType));
    }
    */

    synchronized (_observerMap) {
      ObserverMap map = _observerMap.get(eventType);

      if (map == null) {
        map = new ObserverMap(eventType);
        _observerMap.put(eventType, map);
      }

      map.addObserver(observer, eventBaseType, bindings);
    }

    synchronized (_observerListCache) {
      // XXX: mark the map as changed
      _observerListCache.clear();
    }
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  void addExtensionObserver(ObserverMethod<?> observer,
                            BaseType eventBaseType,
                            Annotation... bindings)
  {
    addObserver(_extObserverMap, observer, eventBaseType, bindings);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  private void addObserver(HashMap<Class<?>,ObserverMap> observerMap,
                           ObserverMethod<?> observer,
                           BaseType eventBaseType,
                           Annotation... bindings)
  {
    Class<?> eventType = eventBaseType.getRawClass();

    checkActive();

    /*
    if (eventType.getTypeParameters() != null
        && eventType.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's a parameterized type.",
                                             eventType));
    }
    */

    synchronized (observerMap) {
      ObserverMap map = observerMap.get(eventType);

      if (map == null) {
        map = new ObserverMap(eventType);
        observerMap.put(eventType, map);
      }

      map.addObserver(observer, eventBaseType, bindings);
    }

    synchronized (_observerListCache) {
      // XXX: mark the map as changed
      _observerListCache.clear();
    }
  }

  /**
   * Removes an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public void removeObserver(ObserverMethod<?> observer)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Registers an event observer
   *
   * @param observerMethod the observer method
   */
  /*
  public void addObserver(ObserverMethod<?,?> observerMethod)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  Annotation []getQualifiers(Set<Annotation> annotations)
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

  Annotation []getQualifiers(Annotation []annotations)
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
  public void fireEvent(Object event, Annotation... bindings)
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " fireEvent " + event);

    BaseType eventType = createBaseType(event.getClass());

    fireEventImpl(event, eventType, bindings);
  }

  protected void fireEventImpl(Object event,
                               BaseType eventType,
                               Annotation... bindings)
  {
    if (_parent != null)
      _parent.fireEventImpl(event, eventType, bindings);

    ArrayList<ObserverMap> observerList;

    synchronized (_observerListCache) {
      observerList = _observerListCache.get(event.getClass());

      if (observerList == null) {
        observerList = new ArrayList<ObserverMap>();

        fillLocalObserverList(_observerMap, observerList, eventType);

        _observerListCache.put(event.getClass(), observerList);
      }
    }

    int size = observerList.size();
    for (int i = 0; i < size; i++) {
      observerList.get(i).fireEvent(event, eventType, bindings);
    }
  }

  /**
   * Returns the observers listening for an event
   *
   * @param eventType event to resolve
   * @param bindings the binding set for the event
   */
  public <T> Set<ObserverMethod<? super T>>
    resolveObserverMethods(T event, Annotation... qualifiers)
  {
    HashSet<ObserverMethod<? super T>> set
      = new HashSet<ObserverMethod<? super T>>();

    BaseType eventType = createBaseType(event.getClass());
    
    for (Annotation qualifier : qualifiers) {
      if (! isQualifier(qualifier.annotationType()))
        throw new IllegalArgumentException(L.l("{0} is not a valid qualifier because it is missing a @Qualifier annotation",
                                               qualifier));
    }

    for (ObserverMap map : getLocalObserverList(event.getClass())) {
      map.resolveObservers(set, eventType, qualifiers);
    }

    return set;
  }

  private ArrayList<ObserverMap> getLocalObserverList(Class<?> cl)
  {
    ArrayList<ObserverMap> observerList;

    synchronized (_observerListCache) {
      observerList = _observerListCache.get(cl);

      if (observerList == null) {
        observerList = new ArrayList<ObserverMap>();

        BaseType eventType = createClassBaseType(cl);

        fillLocalObserverList(_observerMap, observerList, eventType);

        _observerListCache.put(cl, observerList);
      }
    }

    return observerList;
  }

  private void fireExtensionEvent(Object event, Annotation... bindings)
  {
    fireLocalEvent(_extObserverMap, event, bindings);
  }

  private void fireExtensionEvent(Object event, BaseType eventType, Annotation... bindings)
  {
    fireLocalEvent(_extObserverMap, event, eventType, bindings);
  }

  private void fireLocalEvent(HashMap<Class<?>,ObserverMap> localMap,
                              Object event, Annotation... bindings)
  {
    // ioc/0062 - class with type-param handled specially
    BaseType eventType = createClassBaseType(event.getClass());

    fireLocalEvent(localMap, event, eventType, bindings);
  }
  
  private void fireLocalEvent(HashMap<Class<?>,ObserverMap> localMap,
                              Object event, BaseType eventType,
                              Annotation... bindings)
  {
    ArrayList<ObserverMap> observerList = new ArrayList<ObserverMap>();

    fillLocalObserverList(localMap, observerList, eventType);

    int size = observerList.size();
    for (int i = 0; i < size; i++) {
      observerList.get(i).fireEvent(event, eventType, bindings);
    }
  }

  private void fillLocalObserverList(HashMap<Class<?>,ObserverMap> localMap,
                                     ArrayList<ObserverMap> list,
                                     BaseType eventType)
  {
    Class<?> cl = eventType.getRawClass();

    // XXX: generic
    if (cl.getSuperclass() != null)
      fillLocalObserverList(localMap, list, createBaseType(cl.getSuperclass()));

    for (Class<?> iface : cl.getInterfaces()) {
      fillLocalObserverList(localMap, list, createBaseType(iface));
    }

    ObserverMap map = localMap.get(eventType.getRawClass());
    
    if (map != null)
      list.add(map);
  }

  //
  // events
  //

  //
  // interceptor support
  //

  /**
   * Adds a new interceptor to the manager
   */
  public <X> void addInterceptor(Interceptor<X> interceptor)
  {
    _interceptorList.add(new InterceptorEntry<X>(interceptor));
  }

  public void setInterceptorList(List<Interceptor<?>> interceptorList)
  {
    for (Interceptor<?> interceptor : interceptorList)
      addInterceptor(interceptor);
  }

  /**
   * Resolves the interceptors for a given interceptor type
   *
   * @param type the main interception type
   * @param bindings qualifying bindings
   *
   * @return the matching interceptors
   */
  public List<Interceptor<?>> resolveInterceptors(InterceptionType type,
                                                  Annotation... bindings)
  {
    if (bindings == null || bindings.length == 0)
      throw new IllegalArgumentException(L.l("resolveInterceptors requires at least one @InterceptorBinding"));

    ArrayList<Interceptor<?>> interceptorList
      = new ArrayList<Interceptor<?>>();


    for (InterceptorEntry<?> entry : _interceptorList) {
      Interceptor<?> interceptor = entry.getInterceptor();

      if (! interceptor.intercepts(type)) {
        continue;
      }

      if (entry.isMatch(bindings)) {
        interceptorList.add(interceptor);
      }
    }

    return interceptorList;
  }

  //
  // decorator
  //

  /**
   * Adds a new decorator
   */
  public <X> BeanManager addDecorator(Decorator<X> decorator)
  {
    BaseType baseType = createBaseType(decorator.getDelegateType());

    _decoratorList.add(new DecoratorEntry<X>(decorator, baseType));

    return this;
  }
  
  /**
   * Called by the generated code.
   */
  public List<Decorator<?>> resolveDecorators(Class<?> type)
  {
    HashSet<Type> types = new HashSet<Type>();                                  
    types.add(type);                                                            

    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();            

    boolean isQualifier = false;                                                

    for (Annotation ann : type.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
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

    return resolveDecorators(types, bindings);                                  
  }


  /**
   * Resolves the decorators for a given set of types
   *
   * @param types the types to match for the decorator
   * @param bindings qualifying bindings
   *
   * @return the matching interceptors
   */
  public List<Decorator<?>> resolveDecorators(Set<Type> types,
                                              Annotation... bindings)
  {
    ArrayList<Decorator<?>> decorators = new ArrayList<Decorator<?>>();

    if (bindings == null || bindings.length == 0)
      bindings = CURRENT_ANN;

    if (_decoratorList == null)
      return decorators;

    for (DecoratorEntry<?> entry : _decoratorList) {
      Decorator<?> decorator = entry.getDecorator();

      // XXX: delegateTypes
      if (isTypeContained(types, entry.getDelegateType())
          && entry.isMatch(bindings)) {
        decorators.add(decorator);
      }
    }

    return decorators;
  }

  private boolean isTypeContained(Set<Type> types,
                                  BaseType delegateType)
  {
    for (Type type : types) {
      BaseType baseType = createBaseType(type);

      if (delegateType.isAssignableFrom(baseType))
        return true;
    }

    return false;
  }

  //
  // class loader updates
  //

  public void addConfiguredClass(String className)
  {
    _xmlExtension.addConfiguredBean(className);
//    _configuredClasses.add(className);
  }

  public void addLoader()
  {
    _isUpdateNeeded = true;
  }

  public void update()
  {
    if (! _isUpdateNeeded 
        && ! _scanManager.isPending()
        && _pendingAnnotatedTypes.size() == 0) {
      return;
    }

    _isUpdateNeeded = false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      _extensionManager.updateExtensions();

      ArrayList<ScanRootContext> rootContextList
        = _scanManager.getPendingScanRootList();

      for (ScanRootContext context : rootContextList) {
        _xmlExtension.addRoot(context.getRoot());
      }

      _isBeforeBeanDiscoveryComplete = true;
      fireExtensionEvent(new BeforeBeanDiscoveryImpl());
      
      /*
      // ioc/0061
      if (rootContextList.size() == 0)
        return;

      for (ScanRootContext context : rootContextList) {
        for (String className : context.getClassNameList()) {
          if (! _configuredClasses.contains(className)) {
            discoverBean(className);
          }
        }
      }
      */

      processPendingAnnotatedTypes();
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void processPendingAnnotatedTypes()
  {
    _scanManager.discover();
    
    ArrayList<AnnotatedType<?>> types = new ArrayList<AnnotatedType<?>>(_pendingAnnotatedTypes);
    _pendingAnnotatedTypes.clear();
    
    for (AnnotatedType<?> type : types) {
      discoverBean(type);
    }
  }

  void discoverScanClass(InjectScanClass scanClass)
  {
    scanClass.register();
    
    // processPendingAnnotatedTypes();
  }
  
  void discoverBean(String className)
  {
    try {
      Class<?> cl;

      cl = Class.forName(className, false, _classLoader);

      if (! isValidSimpleBean(cl))
        return;

      if (cl.getDeclaringClass() != null
          && ! Modifier.isStatic(cl.getModifiers()))
        return;

      for (Class<? extends Annotation> forbiddenAnnotation : _forbiddenAnnotations) {
        if (cl.isAnnotationPresent(forbiddenAnnotation))
          return;
      }

      for (Class<?> forbiddenClass : _forbiddenClasses) {
        if (forbiddenClass.isAssignableFrom(cl))
          return;
      }

      if (isDisabled(cl))
        return;

      AnnotatedType<?> type = createAnnotatedType(cl);
      
      processAnnotatedType(type);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
 
  /**
   * Creates a discovered annotated type.
   */
  private <T> void processAnnotatedType(AnnotatedType<T> type)
  {
    ProcessAnnotatedTypeImpl<T> processType
      = new ProcessAnnotatedTypeImpl<T>(type);

    BaseType baseType = createBaseType(ProcessAnnotatedTypeImpl.class);
    baseType = baseType.fill(createBaseType(type.getBaseType()));
    
    fireExtensionEvent(processType, baseType);

    if (processType.isVeto()) {
      return;
    }
    
    type = processType.getAnnotatedType();

    if (type == null)
      return;
    
    if (type.isAnnotationPresent(Specializes.class)) {
      Class<?> parent = type.getJavaClass().getSuperclass();

      if (parent != null)
        _specializedClasses.add(parent);
    }

    _pendingAnnotatedTypes.add(type);
  }

  private boolean isDisabled(Class<?> type)
  {
    boolean isDisabled = false;

    for (Annotation ann : type.getAnnotations()) {
      Class<?> annType = ann.annotationType();

      // check stereotypes
      if (_deploymentMap.containsKey(annType))
        return false;

      if (annType.equals(Alternative.class)
          || annType.isAnnotationPresent(Alternative.class)) {
        isDisabled = true;
      }
    }

    return isDisabled && ! _deploymentMap.containsKey(type);
  }

  private boolean isValidSimpleBean(Class<?> type)
  {
    if (type.isInterface())
      return false;
    else if (type.isAnonymousClass())
      return false;
    /*
    else if (type.isMemberClass())
      return false;
      */

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

  private <T> void discoverBean(AnnotatedType<T> type)
  {
    if (_specializedClasses.contains(type.getJavaClass()))
      return;
    
    // XXX: not sure this is correct.
    if (Throwable.class.isAssignableFrom(type.getJavaClass()))
      return;

    InjectionTarget<T> target = createInjectionTarget(type);

    if (target instanceof InjectionTargetImpl<?>) {
      InjectionTargetImpl<?> targetImpl = (InjectionTargetImpl<?>) target;

      targetImpl.setGenerateInterception(true);
    }

    target = processInjectionTarget(target);

    if (target == null)
      return;

    ManagedBeanImpl<T> bean = new ManagedBeanImpl<T>(this, type, target);

    bean.introspect();

    AnnotatedType<T> annType = bean.getAnnotatedType();

    // ioc/0i04
    if (annType.isAnnotationPresent(javax.decorator.Decorator.class))
      return;
    // ioc/0c1a
    if (annType.isAnnotationPresent(javax.interceptor.Interceptor.class))
      return;

    addDiscoveredBean(bean);
    
    fillProducerBeans(bean);

    // beans.addScannedClass(cl);
  }
  
  private void fillProducerBeans(ManagedBeanImpl<?> bean)
  {
  }

  private <X> void addDiscoveredBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);
    
    for (Bean<?> producerBean : managedBean.getProducerBeans()) {
      addBean(producerBean);
    }

    for (ObserverMethodImpl<X,?> observer : managedBean.getObserverMethods()) {
      // observer = processObserver(observer);

      if (observer != null) {
        Set<Annotation> annSet = observer.getObservedQualifiers();

        Annotation []bindings = new Annotation[annSet.size()];
        annSet.toArray(bindings);

        BaseType baseType = createBaseType(observer.getObservedEventType());

        addObserver(observer, baseType, bindings);
      }
    }
  }

  public <X> void addManagedBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);

    for (ObserverMethodImpl<X,?> observer : managedBean.getObserverMethods()) {
      // observer = processObserver(observer);

      if (observer != null) {
        Set<Annotation> annSet = observer.getObservedQualifiers();

        Annotation []bindings = new Annotation[annSet.size()];
        annSet.toArray(bindings);

        BaseType baseType = createBaseType(observer.getObservedEventType());

        addObserver(observer, baseType, bindings);
      }
    }

    for (Bean<?> producerBean : managedBean.getProducerBeans()) {
      addBean(producerBean);
    }
  }

  public <T> ArrayList<T> loadServices(Class<T> serviceClass)
  {
    return loadServices(serviceClass, new HashSet<URL>());
  }

  private <T> ArrayList<T> loadServices(Class<T> serviceApiClass,
                                        HashSet<URL> serviceSet)
  {
    ArrayList<T> services = new ArrayList<T>();

    try {
      ClassLoader loader = _classLoader;

      if (loader == null)
        return services;

      Enumeration<URL> e
        = loader.getResources("META-INF/services/" + serviceApiClass.getName());

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

              if (cl != null)
                services.add(createTransientObject(cl));
            }
          }

          in.close();
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        } finally {
          IoUtil.close(is);
        }
      }
    } catch (IOException e) {
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
        throw new InjectionException(L.l("'{0}' is not a valid servicebecause it does not implement {1}",
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
      
      if (_pendingBindList == null)
        return;

      ArrayList<AbstractBean<?>> bindList
        = new ArrayList<AbstractBean<?>>(_pendingBindList);

      _pendingBindList.clear();

      isBind = bindList.size() > 0 || ! _isAfterBeanDiscoveryComplete;

      if (isBind) {
        _isAfterBeanDiscoveryComplete = true;

        fireExtensionEvent(new AfterBeanDiscoveryImpl());
      }

      if (_configException != null)
        throw _configException;

      /*
      for (AbstractBean comp : bindList) {
        if (_deploymentMap.get(comp.getDeploymentType()) != null)
          comp.bind();
      }
      */

      if (isBind) {
        fireExtensionEvent(new AfterDeploymentValidationImpl());
      }
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Handles the case the environment config phase
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
    initialize();
  }

  /**
   * Handles the case the environment config phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
    initialize();
    bind();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
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
    initialize();

    bind();

    //     _wbWebBeans.init();

    /*
    if (_lifecycle.toActive()) {
      fireEvent(this, new AnnotationLiteral<Deployed>() {});
    }
    */

    startServices();
  }

  public void addConfiguredBean(String className)
  {
    _xmlExtension.addConfiguredBean(className);
  }

  void addService(Bean<?> bean)
  {
    _pendingServiceList.add(bean);
  }

  /**
   * Initialize all the services
   */
  private void startServices()
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

    /*
    for (ManagedBean bean : registerServices) {
      startRegistration(bean);
    }
    */
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public void destroy()
  {
    _parent = null;
    _classLoader = null;
    _deploymentMap = null;

    _selfBeanMap = null;
    _selfNamedBeanMap = null;
    _beanMap = null;
    _namedBeanMap = null;
    _observerMap = null;

    _contextMap = null;
    _observerListCache = null;

    _interceptorList = null;
    _decoratorList = null;
    _pendingBindList = null;
    _pendingServiceList = null;
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

  public static ConfigException error(Method method, String msg)
  {
    return new ConfigException(location(method) + msg);
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

  private void checkActive()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  static String getSimpleName(Type type)
  {
    if (type instanceof Class<?>)
      return ((Class<?>) type).getSimpleName();
    else
      return String.valueOf(type);
  }

  static class TypedBean {
    private final BaseType _type;
    private final Bean<?> _bean;
    private final boolean _isModulePrivate;

    TypedBean(BaseType type, Bean<?> bean)
    {
      _type = type;
      _bean = bean;

      _isModulePrivate = isModulePrivate(bean) || bean.isAlternative();
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

    static boolean isModulePrivate(Bean<?> bean)
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
      InjectManager beanManager = InjectManager.getCurrent(loader);

      beanManager.fillByName(_name, _beanList);
    }
  }

  static class FillByType implements EnvironmentApply
  {
    private BaseType _baseType;
    private HashSet<TypedBean> _beanSet;
    private InjectManager _manager;

    FillByType(BaseType baseType,
               HashSet<TypedBean> beanSet,
               InjectManager manager)
    {
      _baseType = baseType;
      _beanSet = beanSet;
      _manager = manager;
    }

    public void apply(EnvironmentClassLoader loader)
    {
      InjectManager beanManager = InjectManager.getCurrent(loader);

      beanManager.fillByType(_baseType, _beanSet, _manager);
    }
  }

  class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
  {
    public void addAnnotatedType(AnnotatedType<?> annType)
    {
    }
    
    @Override
    public void addQualifier(Class<? extends Annotation> qualifier)
    {
      _qualifierSet.add(qualifier);
    }

    @Override
    public void addScope(Class<? extends Annotation> scopeType,
                         boolean isNormal,
                         boolean isPassivating)
    {
      _scopeTypeSet.add(scopeType);
      
      if (isNormal)
        _normalScopeSet.add(scopeType);
      
      if (isPassivating)
        _passivatingScopeSet.add(scopeType);
    }

    public void addStereotype(Class<? extends Annotation> stereotype,
                              Annotation... stereotypeDef)
    {
    }

    public void addInterceptorBinding(Class<? extends Annotation> bindingType,
                                      Annotation... bindings)
    {
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + InjectManager.this + "]";
    }
  }

  class ProcessAnnotatedTypeImpl<X> implements ProcessAnnotatedType<X>
  {
    private AnnotatedType<X> _annotatedType;
    private boolean _isVeto;

    ProcessAnnotatedTypeImpl(AnnotatedType<X> annotatedType)
    {
      if (annotatedType == null)
        throw new NullPointerException();

      _annotatedType = annotatedType;
    }

    public AnnotatedType<X> getAnnotatedType()
    {
      return _annotatedType;
    }

    public void setAnnotatedType(AnnotatedType<X> type)
    {
      _annotatedType = type;
    }

    boolean isVeto()
    {
      return _isVeto;
    }

    @Override
    public void veto()
    {
      _isVeto = true;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _annotatedType + "]";
    }
  }

  class ProcessInjectionTargetImpl<X> implements ProcessInjectionTarget<X>
  {
    private InjectionTarget<X> _target;
    private AnnotatedType<X> _type;

    ProcessInjectionTargetImpl(InjectionTarget<X> target,
                               AnnotatedType<X> type)
    {
      _target = target;
      _type = type;
    }

    public AnnotatedType<X> getAnnotatedType()
    {
      return _type;
    }

    public InjectionTarget<X> getInjectionTarget()
    {
      return _target;
    }

    public void setInjectionTarget(InjectionTarget<X> target)
    {
      _target = target;
    }

    public void addDefinitionError(Throwable t)
    {
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _target + "]";
    }
  }

  class AfterBeanDiscoveryImpl implements AfterBeanDiscovery
  {
    public void addBean(Bean<?> bean)
    {
      InjectManager.this.addBean(bean);
    }

    @Override
    public void addContext(Context context)
    {
      InjectManager.this.addContext(context);
    }

    @Override
    public void addObserverMethod(ObserverMethod<?> observerMethod)
    {
      InjectManager.this.addObserver(observerMethod);
    }

    @Override
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

    public boolean hasDefinitionError()
    {
      return false;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + InjectManager.this + "]";
    }
  }

  class AfterDeploymentValidationImpl implements AfterDeploymentValidation
  {
    public void addDeploymentProblem(Throwable t)
    {
    }

    public boolean hasDeploymentProgram()
    {
      return false;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + InjectManager.this + "]";
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

    InjectBean(Bean<X> bean, InjectManager beanManager)
    {
      super(beanManager, bean);

      _loader = Thread.currentThread().getContextClassLoader();

      if (bean instanceof AbstractBean) {
        AbstractBean<X> absBean = (AbstractBean<X>) bean;
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

    public X getScopeAdapter(Bean<?> topBean, CreationalContext<X> cxt)
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

        return getBean().create(env);
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

  static {
    ArrayList<Class<?>> forbiddenAnnotations = new ArrayList<Class<?>>();
    ArrayList<Class<?>> forbiddenClasses = new ArrayList<Class<?>>();

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

    /*
    ClassLoader systemClassLoader = null;

    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Throwable e) {
      // a security manager may not allow this call

      log.log(Level.FINEST, e.toString(), e);
    }
    */
  }
}
