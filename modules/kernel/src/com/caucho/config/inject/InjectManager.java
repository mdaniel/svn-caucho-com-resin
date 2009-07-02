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

import com.caucho.config.*;
import com.caucho.config.annotation.StartupType;
import com.caucho.config.el.WebBeansContextResolver;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.BeanArg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.program.FieldEventProgram;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.CreationContextImpl;
import com.caucho.config.scope.ScopeContext;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.ref.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

import javax.decorator.Decorates;
import javax.el.*;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observer;
import javax.enterprise.event.Observes;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.Conversation;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.PassivationCapable;
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.Disabled;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.TypeLiteral;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.stereotype.Stereotype;
import javax.naming.*;

/**
 * The web beans container for a given environment.
 */
public class InjectManager
  implements BeanManager, ScanListener, EnvironmentListener,
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
    "javax.ejb.Stateful",
    "javax.ejb.Stateless",
    "javax.ejb.Singleton",
    "javax.ejb.MessageDriven"
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

  private static final Class []_forbiddenAnnotations;
  private static final Class []_forbiddenClasses;

  private String _id;

  private InjectManager _parent;
  
  private EnvironmentClassLoader _classLoader;
  private ClassLoader _tempClassLoader;

  private HashSet<URL> _extensionSet = new HashSet<URL>();

  private AtomicInteger _beanId = new AtomicInteger();

  private HashSet<String> _configuredClasses
    = new HashSet<String>();

  private HashSet<Class> _specializedClasses
    = new HashSet<Class>();

  private HashMap<Class,Integer> _deploymentMap
    = new HashMap<Class,Integer>();

  private BaseTypeFactory _baseTypeFactory = new BaseTypeFactory();

  //
  // self configuration
  //

  private HashMap<Class,Set<TypedBean>> _selfBeanMap
    = new HashMap<Class,Set<TypedBean>>();

  private HashMap<String,ArrayList<Bean<?>>> _selfNamedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<Class,ObserverMap> _selfObserverMap
    = new HashMap<Class,ObserverMap>();

  private HashMap<String,Bean<?>> _selfPassivationBeanMap
    = new HashMap<String,Bean<?>>();

  //
  // combined visibility configuration
  //

  private HashMap<Class,WebComponent> _beanMap
    = new HashMap<Class,WebComponent>();

  private HashMap<String,ArrayList<Bean<?>>> _namedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<Class,ObserverMap> _observerMap
    = new HashMap<Class,ObserverMap>();
  
  private HashMap<Type,Bean> _newBeanMap
    = new HashMap<Type,Bean>();

  private HashMap<Class,Context> _contextMap
    = new HashMap<Class,Context>();

  private HashMap<Class,ArrayList<ObserverMap>> _observerListCache
    = new HashMap<Class,ArrayList<ObserverMap>>();

  private ArrayList<InterceptorEntry> _interceptorList
    = new ArrayList<InterceptorEntry>();

  private ArrayList<DecoratorEntry> _decoratorList
    = new ArrayList<DecoratorEntry>();

  private boolean _isUpdateNeeded = true;

  private HashMap<Path,WebBeansRootContext> _rootContextMap
    = new HashMap<Path,WebBeansRootContext>();
  
  private ArrayList<Path> _pendingPathList
    = new ArrayList<Path>();

  private ArrayList<WebBeansRootContext> _pendingRootContextList
    = new ArrayList<WebBeansRootContext>();

  private ArrayList<AnnotatedType> _pendingAnnotatedTypes
    = new ArrayList<AnnotatedType>();

  private ArrayList<AbstractBean> _pendingBindList
    = new ArrayList<AbstractBean>();

  private ArrayList<Bean> _pendingServiceList
    = new ArrayList<Bean>();

  private Lifecycle _lifecycle = new Lifecycle();
  private boolean _isBeforeBeanDiscoveryComplete;
  private boolean _isAfterBeanDiscoveryComplete;

  // XXX: needs to be a local resolver
  private ELResolver _elResolver = new WebBeansContextResolver();

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

    if (parent != null)
      _parent = parent;
    else if (_classLoader != null)
      _parent = InjectManager.create(_classLoader.getParent());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (isSetLocal)
	_localContainer.set(this, _classLoader);

      try {
	InitialContext ic = new InitialContext();
	ic.rebind("java:comp/BeanManager", new WebBeansJndiProxy());
      } catch (Throwable e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    
      if (_classLoader != null)
	_tempClassLoader = _classLoader.getNewTempClassLoader();
      else
	_tempClassLoader = new DynamicClassLoader(null);
    
      addContext("com.caucho.server.webbeans.RequestScope");
      addContext("com.caucho.server.webbeans.SessionScope");
      addContext("com.caucho.server.webbeans.ConversationScope");
      addContext(_applicationScope);

      _deploymentMap.put(CauchoDeployment.class, 0);
      // DEFAULT_PRIORITY
      _deploymentMap.put(Configured.class, 2);

      BeanFactory factory = createBeanFactory(InjectManager.class);
      // factory.deployment(Standard.class);
      factory.type(InjectManager.class);
      factory.type(BeanManager.class);
      factory.annotation(ModulePrivateLiteral.create());
      addBean(factory.singleton(this));

      _xmlExtension = new XmlStandardPlugin(this);
      addExtension(_xmlExtension);
      createExtension("com.caucho.server.webbeans.ResinStandardPlugin");

      if (_classLoader != null && isSetLocal) {
	_classLoader.addScanListener(this);
      }
    
      Environment.addEnvironmentListener(this, _classLoader);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void createExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);
      Constructor ctor= cl.getConstructor(new Class[] { InjectManager.class });

      Extension extension = (Extension) ctor.newInstance(this);

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }
  
  private void addContext(String contextClassName)
  {
    try {
      Class cl = Class.forName(contextClassName);
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
    InjectManager webBeans = null;

    synchronized (_localContainer) {
      webBeans = _localContainer.getLevel(loader);

      if (webBeans == null) {
	EnvironmentClassLoader envLoader
	  = Environment.getEnvironmentClassLoader(loader);

	String id;

	if (envLoader != null)
	  id = envLoader.getId();
	else
	  id = "";
	
	webBeans = new InjectManager(id, null, envLoader, true);
      
	// _localContainer.set(webBeans, loader);
      }
    }

    return webBeans;
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

  private void init()
  {
    try {
      _isUpdateNeeded = true;
      
      update();
    } catch (RuntimeException e) {
      _configException = e;
      
      throw _configException;
    } catch (Exception e) {
      _configException = ConfigException.create(e);

      throw _configException;
    }
    
    Environment.addEnvironmentListener(this);
  }

  public void addPath(Path path)
  {
    _pendingPathList.add(path);
  }

  public void setDeploymentTypes(ArrayList<Class> deploymentList)
  {
    _deploymentMap.clear();

    _deploymentMap.put(CauchoDeployment.class, 0);
    // DEFAULT_PRIORITY

    int priority = DEFAULT_PRIORITY + 1;
    
    if (! deploymentList.contains(Configured.class)) {
      _deploymentMap.put(Configured.class, priority++);
    }

    for (int i = deploymentList.size() - 1; i >= 0; i--) {
      _deploymentMap.put(deploymentList.get(i), priority++);
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
  private void addBeanByType(Type type, Bean comp)
  {
    if (type == null)
      return;

    addBeanByType(createBaseType(type), comp);
  }
    
  private void addBeanByType(BaseType type, Bean bean)
  {
    if (type == null)
      return;

    if (log.isLoggable(Level.FINEST))
      log.finest(bean + "(" + type + ") added to " + this);

    Class rawType = type.getRawClass();
    
    WebComponent webComponent;

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
  public ScopeContext getScopeContext(Class scope)
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

  Annotation []getBindings(Set<Annotation> annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }
  
  private Annotation []getBindings(Annotation []annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
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
    ManagedBeanImpl<T> factory = createManagedBean(type);

    // server/10gn
    //return factory.create(new ConfigContext());
    InjectionTarget<T> injectionTarget = factory.getInjectionTarget();

    ConfigContext env = ConfigContext.create();
    
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
    return new BeanFactory(managedBean);
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
  public boolean isScopeType(Class<? extends Annotation> annotationType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the scope definition for a scope type
   */
  public ScopeType getScopeDefinition(Class<? extends Annotation> scopeType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Tests if an annotation is an enabled binding type
   */
  public boolean isBindingType(Class<? extends Annotation> annotationType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Tests if an annotation is an enabled interceptor binding type
   */
  public boolean isInterceptorBindingType(Class<? extends Annotation> annotationType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bindings for an interceptor binding type
   */
  public Set<Annotation> getInterceptorBindingTypeDefinition(Class<? extends Annotation> bindingType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Tests if an annotation is an enabled stereotype.
   */
  public boolean isStereotype(Class<? extends Annotation> annotationType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the annotations associated with a stereotype
   */
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
  public BaseType createClassBaseType(Class type)
  {
    return _baseTypeFactory.createClass(type);
  }

  /**
   * Creates a BaseType from a Type
   */
  public BaseType createBaseType(Type type, HashMap paramMap)
  {
    return _baseTypeFactory.create(type, paramMap);
  }
  
  /**
   * Creates an annotated type.
   */
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> cl)
  {
    return new AnnotatedTypeImpl(cl, cl);
  }

  /**
   * Creates a discovered annotated type.
   */
  public <T> AnnotatedType<T> processAnnotatedType(AnnotatedType<T> type)
  {
    ProcessAnnotatedTypeImpl processType
      = new ProcessAnnotatedTypeImpl(type);

    fireLocalEvent(processType);

    if (! processType.isVeto())
      return processType.getAnnotatedType();
    else
      return null;
  }

  public void addAnnotatedType(AnnotatedType<?> type)
  {
    type = processAnnotatedType(type);

    if (type == null) {
      return;
    }

    /*
    if (type.isAnnotationPresent(Specializes.class)) {
      for (Class parent = cl.getSuperclass();
	   parent != null;
	   parent = parent.getSuperclass()) {
	_specializedClasses.add(parent);
      }
    }
    */

    _pendingAnnotatedTypes.add(type);
  }

  /**
   * Creates an injection target
   */
  public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
  {
    InjectionTargetImpl bean = new InjectionTargetImpl(this, type);
    
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
  public <T> InjectionTarget<T> processInjectionTarget(InjectionTarget<T> target)
  {
    ProcessInjectionTargetImpl processTarget
      = new ProcessInjectionTargetImpl(target);

    fireEvent(processTarget);

    return processTarget.getInjectionTarget();
  }

  /**
   * Creates a managed bean.
   */
  public ManagedBeanImpl createManagedBean(AnnotatedType type)
  {
    InjectionTarget target = createInjectionTarget(type);
    
    ManagedBeanImpl bean = new ManagedBeanImpl(this, type, target);
    bean.introspect();

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public ManagedBeanImpl createManagedBean(Class cl)
  {
    AnnotatedType type = new AnnotatedTypeImpl(cl, cl);
    
    return createManagedBean(type);
  }

  /**
   * Processes the discovered bean
   */
  public <T> Bean<T> processBean(Bean<T> bean)
  {
    ProcessBeanImpl processBean = new ProcessBeanImpl(this, bean);

    fireEvent(processBean);

    return processBean.getBean();
  }

  /**
   * Adds a new bean definition to the manager
   */
  public void addBean(Bean<?> bean)
  {
    bean = processBean(bean);

    if (bean == null)
      return;

    for (Type type : bean.getTypes()) {
      addBeanByType(type, bean);
    }

    if (bean.getName() != null) {
      addBeanByName(bean.getName(), bean);
    }


    /*
    if (bean instanceof ManagedBeanImpl) {
      ManagedBeanImpl<?> mBean = (ManagedBeanImpl) bean;

      for (ProducesBean producerBean : mBean.getProducerBeans()) {
	addBean(producerBean);
      }
    }
    */

    if (bean instanceof PassivationCapable) {
      PassivationCapable pass = (PassivationCapable) bean;

      if (pass.getId() != null)
	_selfPassivationBeanMap.put(pass.getId(), bean);
    }

    registerJmx(bean);
  }

  private void registerJmx(Bean bean)
  {
    int id = _beanId.incrementAndGet();

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

    return new LinkedHashSet<Bean<?>>(beanList);
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param bindings required @BindingType annotations
   */
  public Set<Bean<?>> getBeans(Type type,
			       Annotation... bindings)
  {
    Set set = resolve(type, bindings);

    if (set != null)
      return (Set<Bean<?>>) set;
    else
      return new HashSet<Bean<?>>();
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set resolve(Type type,
		      Set<Annotation> bindingSet)
  {
    Annotation []bindings = new Annotation[bindingSet.size()];
    bindingSet.toArray(bindings);

    return resolve(type, bindings);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set resolve(Type type, Annotation []bindings)
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
      Set beans = component.resolve(baseType, bindings);

      if (beans != null && beans.size() > 0) {
	if (log.isLoggable(Level.FINER))
	  log.finer(this + " bind(" + baseType.getSimpleName()
		     + "," + toList(bindings) + ") -> " + beans);

	return beans;
      }
    }
    else if (New.class.equals(bindings[0].annotationType())) {
      // ioc/0721
      HashSet set = new HashSet();
      AbstractBean newBean = new NewBean(this, new AnnotatedTypeImpl(baseType.getRawClass(), baseType.getRawClass()));
      newBean.introspect();
      
      set.add(newBean);
      
      return set;
    }

    Class rawType = baseType.getRawClass();

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
      if (! ann.annotationType().isAnnotationPresent(BindingType.class)) {
	throw new IllegalArgumentException(L.l("'{0}' is an invalid binding annotation because it does not have a @BindingType meta-annotation",
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
  public Set<Bean> resolveAllByType(Class type)
  {
    Annotation []bindings = new Annotation[0];
    
    WebComponent component = getWebComponent(createBaseType(type));

    if (component != null) {
      Set beans = component.resolve(type, bindings);

      if (log.isLoggable(Level.FINER))
	log.finer(this + " bind(" + getSimpleName(type)
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
    Class rawClass = baseType.getRawClass();
    
    WebComponent beanSet = _beanMap.get(baseType.getRawClass());

    if (beanSet == null) {
      HashSet<TypedBean> typedBeans = new HashSet<TypedBean>();

      if (_classLoader != null) {
	FillByType fillByType = new FillByType(baseType, typedBeans, this);
	
	_classLoader.applyVisibleModules(fillByType);
      }

      beanSet = new WebComponent(this, baseType.getRawClass());
      
      for (TypedBean typedBean : typedBeans) {
	if (getDeploymentPriority(typedBean.getBean()) < 0)
	  continue;

	beanSet.addComponent(typedBean.getType(), typedBean.getBean());
      }

      _beanMap.put(baseType.getRawClass(), beanSet);
    }

    return beanSet;
  }

  private void fillByType(BaseType baseType,
			  HashSet<TypedBean> beanSet,
			  InjectManager beanManager)
  {
    Set<TypedBean> localBeans = _selfBeanMap.get(baseType.getRawClass());

    if (localBeans != null) {
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
  
  public <X> Bean<? extends X> getHighestPrecedenceBean(Set<Bean<? extends X>> beans)
  {
    Bean bestBean = null;
    Bean secondBean = null;
    int bestPriority = -1;

    for (Bean bean : beans) {
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

  public int getDeploymentPriority(Bean bean)
  {
    Set<Annotation> stereotypes = bean.getStereotypes();

    int priority = DEFAULT_PRIORITY;

    if (stereotypes != null) {
      for (Annotation ann : stereotypes) {
	Integer value = _deploymentMap.get(ann.annotationType());

	if (value != null && priority < value)
	  priority = value;
      }
    }

    return priority;
  }

  private Set resolveAllBeans()
  {
    Annotation []bindings = new Annotation[0];
    
    synchronized (_beanMap) {
      LinkedHashSet beans = new LinkedHashSet();

      for (Set<TypedBean> comp : _selfBeanMap.values()) {
	for (TypedBean typedBean : comp) {
	  beans.add(typedBean.getBean());
	}
      }

      return beans;
    }
  }

  public CreationalContext<?> createCreationalContext()
  {
    return new ConfigContext();
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(Class<T> type, Annotation... bindings)
  {
    Set<Bean<?>> beans = getBeans(type);
    Bean<?> bean = getHighestPrecedenceBean(beans);

    if (bean == null)
      return null;

    CreationalContext<?> env = createCreationalContext();

    return (T) getReference(bean, type, env);
  }
  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  public Object getReference(Bean<?> bean,
			     Type type,
			     CreationalContext<?> createContext)
  {
    ConfigContext env = (ConfigContext) createContext;

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
  public Object create(Bean<?> bean)
  {
    CreationalContext<?> env = createCreationalContext();

    return getReference(bean, bean.getBeanClass(), env);
  }
  
  private Object getInstanceRec(Bean<?> bean,
				Type type,
				CreationalContext createContext,
				InjectManager topManager)
  {
    if (createContext == null)
      throw new NullPointerException();
    
    /* XXX: temp API change
    if (bean.getManager() != this) {
      if (getParent() == null) {
	throw new IllegalStateException(L.l("{0}: unknown bean {1} with owning manager {2}",
					    topManager,
					    bean,
					    bean.getManager()));
      }

      return getParent().getInstanceRec(bean, createContext, topManager);
    }
    else
    */
    Class scopeType = bean.getScopeType();

    if (Dependent.class.equals(scopeType)) {
      // server/4764
      Object instance = bean.create(createContext);

      return instance;
    }

    if (scopeType == null) {
      throw new IllegalStateException("Unknown scope for " + bean);
    }
      
    Context context = getContext(scopeType);

    if (context == null)
      return null;

    if (createContext != null) {
    }
    else if ((bean instanceof AbstractBean)
	     && (context instanceof ScopeContext)) {
      createContext = new ConfigContext((AbstractBean) bean,
					null,
					(ScopeContext) context);
    }
    else {
      ConfigContext parent = ConfigContext.getCurrent();
	
      createContext = new ConfigContext(parent);
    }

    return context.get(bean, createContext);
  }

  private RuntimeException unsatisfiedException(Type type,
						Annotation []bindings)
  {
    WebComponent component = getWebComponent(createBaseType(type));

    if (component == null) {
      throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection Manager.",
						   type));
    }
    else {
      ArrayList<Bean<?>> enabledList = component.getEnabledBeanList();

      if (enabledList.size() == 0) {
	throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection Manager.",
						     type));
      }
      else {
	return new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans match the type and bindings {1}.\nBeans:{2}",
						      type,
						      toList(bindings),
						      listToLines(enabledList)));
      }
    }
  }

  private String listToLines(List list)
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

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public Object getInjectableReference(InjectionPoint ij,
				       CreationalContext<?> cxt)
  {
    Bean bean = resolveByInjectionPoint(ij);

    if (bean instanceof ScopeAdapterBean) {
      ScopeAdapterBean simpleBean = (ScopeAdapterBean) bean;

      Object adapter = simpleBean.getScopeAdapter(cxt);

      if (adapter != null)
	return adapter;
    }
	
    return getReference(bean, ij.getType(), cxt);
  }

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public <T> T getInstanceToInject(InjectionPoint ij)
  {
    return (T) getInjectableReference(ij, new ConfigContext());
  }

  public Bean resolveByInjectionPoint(InjectionPoint ij)
  {
    Type type = ij.getType();
    Set<Annotation> bindingSet = ij.getBindings();

    return resolveByInjectionPoint(type, bindingSet);
  }

  public Bean resolveByInjectionPoint(Type type, Set<Annotation> bindingSet)
  {
    Annotation []bindings;

    if (bindingSet != null) {
      bindings = new Annotation[bindingSet.size()];
      bindingSet.toArray(bindings);

      if (bindings.length == 1
	  && bindings[0].annotationType().equals(New.class)) {
	return createNewBean(type);
      }
    }
    else
      bindings = new Annotation[] { CurrentLiteral.CURRENT };

    Set<Bean<?>> set = getBeans(type, bindings);

    if (set == null || set.size() == 0) {
      throw unsatisfiedException(type, bindings);
    }

    return getHighestPrecedenceBean(set);

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

  private Bean createNewBean(Type type)
  {
    Bean bean = _newBeanMap.get(type);

    if (bean == null) {
      BaseType baseType = createBaseType(type);
      
      AbstractBean newBean = new NewBean(this, new AnnotatedTypeImpl(baseType.getRawClass(), baseType.getRawClass()));
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

  //
  // scopes
  //

  /**
   * Adds a new scope context
   */
  public void addContext(Context context)
  {
    _contextMap.put(context.getScopeType(), context);
  }

  /**
   * Returns the scope context for the given type
   */
  public Context getContext(Class<? extends Annotation> scopeType)
  {
    Context context = _contextMap.get(scopeType);

    if (context != null && context.isActive())
      return context;
    else
      throw new ContextNotActiveException(L.l("'@{0}' is not an active WebBeans context.",
					      scopeType.getName()));
  }

  /**
   * Returns the bean for the given passivation id.
   */
  public Bean getPassivationCapableBean(String id)
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
  public void addObserver(Observer<?> observer,
			  Annotation... bindings)
  {
    BaseType baseType = createBaseType(observer.getClass());
    BaseType observerType = baseType.findClass(this, Observer.class);

    BaseType eventType = observerType.getParameters()[0];

    addObserver(observer, eventType, bindings);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(Observer<?> observer,
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
  public void addObserver(Observer<?> observer,
			  BaseType eventBaseType,
			  Annotation... bindings)
  {
    Class eventType = eventBaseType.getRawClass();

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
   * Removes an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public void removeObserver(Observer<?> observer)
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

  /**
   * Sends the specified event to any observer instances in the scope
   */
  public void fireEvent(Object event, Annotation... bindings)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " fireEvent " + event);

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
	
	fillLocalObserverList(observerList, eventType);
	
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
  public <T> Set<Observer<T>> resolveObservers(T event,
					       Annotation... bindings)
  {
    HashSet<Observer<T>> set = new HashSet<Observer<T>>();

    BaseType eventType = createBaseType(event.getClass());
    
    for (ObserverMap map : getLocalObserverList(event.getClass())) {
      map.resolveObservers(set, eventType, bindings);
    }

    return set;
  }

  private ArrayList<ObserverMap> getLocalObserverList(Class cl)
  {
    ArrayList<ObserverMap> observerList;

    synchronized (_observerListCache) {
      observerList = _observerListCache.get(cl);

      if (observerList == null) {
	observerList = new ArrayList<ObserverMap>();

	BaseType eventType = createClassBaseType(cl);

	fillLocalObserverList(observerList, eventType);
	
	_observerListCache.put(cl, observerList);
      }
    }

    return observerList;
  }
  
  private void fireLocalEvent(Object event, Annotation... bindings)
  {
    ArrayList<ObserverMap> observerList = new ArrayList<ObserverMap>();
    // ioc/0062 - class with type-param handled specially
    BaseType eventType = createClassBaseType(event.getClass());
	
    fillLocalObserverList(observerList, eventType);

    int size = observerList.size();
    for (int i = 0; i < size; i++) {
      observerList.get(i).fireEvent(event, eventType, bindings);
    }
  }

  private void fillLocalObserverList(ArrayList<ObserverMap> list,
				     BaseType eventType)
  {
    Class cl = eventType.getRawClass();
    
    // XXX: generic
    if (cl.getSuperclass() != null)
      fillLocalObserverList(list, createBaseType(cl.getSuperclass()));

    for (Class iface : cl.getInterfaces()) {
      fillLocalObserverList(list, createBaseType(iface));
    }

    ObserverMap map = _observerMap.get(eventType.getRawClass());

    if (map != null)
      list.add(map);
  }

  /**
   * Processes the discovered bean
   */
  /*
  public <T> ObserverMethod<T,?> processObserver(ObserverMethod<T,?> observer)
  {
    ProcessObserverImpl processObserver = new ProcessObserverImpl(observer);

    fireEvent(processObserver);

    return processObserver.getObserverMethod();
  }
  */

  //
  // events
  //

  //
  // interceptor support
  //

  /**
   * Adds a new interceptor to the manager
   */
  public void addInterceptor(Interceptor interceptor)
  {
    _interceptorList.add(new InterceptorEntry(interceptor));
  }

  public void setInterceptorList(List<Interceptor> interceptorList)
  {
    for (Interceptor interceptor : interceptorList)
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
      throw new IllegalArgumentException(L.l("resolveInterceptors requires at least one @InterceptorBindingType"));
    
    ArrayList<Interceptor<?>> interceptorList
      = new ArrayList<Interceptor<?>>();


    for (InterceptorEntry entry : _interceptorList) {
      Interceptor interceptor = entry.getInterceptor();
      
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
  public BeanManager addDecorator(Decorator decorator)
  {
    BaseType baseType = createBaseType(decorator.getDelegateType());
    
    _decoratorList.add(new DecoratorEntry(decorator, baseType));

    return this;
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

    for (DecoratorEntry entry : _decoratorList) {
      Decorator decorator = entry.getDecorator();

      // XXX: delegateTypes
      if (isTypeContained(types, entry.getDelegateType())
	  && entry.isMatch(bindings)) {
	decorators.add(decorator);
      }
    }

    return decorators;
  }

  public List<Decorator<?>> resolveDecorators(Class type)
  {
    HashSet<Type> types = new HashSet<Type>();
    types.add(type);
    
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : type.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }
    
    if (bindingList.size() == 0)
      bindingList.add(new CurrentLiteral());

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return resolveDecorators(types, bindings);
  }

  private Class getRawType(Type type)
  {
    if (type instanceof Class)
      return ((Class) type);
    else if (type instanceof ParameterizedType)
      return getRawType(((ParameterizedType) type).getRawType());
    else
      return null;
  }

  private Type getInstanceType(Type type)
  {
    if (type instanceof Class)
      return Object.class;
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      return pType.getActualTypeArguments()[0];
    }
    else
      throw new UnsupportedOperationException(String.valueOf(type));
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
  // Activity
  //

  //
  // Actitivities
  //

  /**
   * Creates a new activity
   */
  public BeanManager createActivity()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Associate the context with a scope
   */
  public BeanManager setCurrent(Class<? extends Annotation> scopeType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // class loader updates
  //
  
  public void addConfiguredClass(String className)
  {
    _configuredClasses.add(className);
  }

  public void addLoader()
  {
    _isUpdateNeeded = true;
  }
  
  public void update()
  {
    if (! _isUpdateNeeded)
      return;

    _isUpdateNeeded = false;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      updateExtensions();

      ArrayList<WebBeansRootContext> rootContextList
	= new ArrayList<WebBeansRootContext>(_pendingRootContextList);
      
      _pendingRootContextList.clear();
      
      for (WebBeansRootContext context : rootContextList) {
	_xmlExtension.addRoot(context.getRoot());
      }
      
      _isBeforeBeanDiscoveryComplete = true;
      fireEvent(new BeforeBeanDiscoveryImpl());

      // ioc/0061
      if (rootContextList.size() == 0)
	return;

      for (WebBeansRootContext context : rootContextList) {
	for (String className : context.getClassNameList()) {
	  if (! _configuredClasses.contains(className)) {
	    discoverBean(className);
	  }
	}
      }

      ArrayList<AnnotatedType> types
	= new ArrayList<AnnotatedType>(_pendingAnnotatedTypes);
      _pendingAnnotatedTypes.clear();

      for (AnnotatedType type : types) {
	discoverBean(type);
      }
    } catch (ConfigException e) {
      if (_configException == null)
	_configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void discoverBean(String className)
  {
    try {
      Class cl;

      cl = Class.forName(className, false, _classLoader);
      
      if (! isValidSimpleBean(cl))
	return;
	    
      if (cl.getDeclaringClass() != null
	  && ! Modifier.isStatic(cl.getModifiers()))
	return;

      for (Class forbiddenAnnotation : _forbiddenAnnotations) {
	if (cl.isAnnotationPresent(forbiddenAnnotation))
	  return;
      }

      for (Class forbiddenClass : _forbiddenClasses) {
	if (forbiddenClass.isAssignableFrom(cl))
	  return;
      }

      if (isDisabled(cl))
	return;

      AnnotatedType type = createAnnotatedType(cl);

      type = processAnnotatedType(type);

      if (type == null) {
	return;
      }

      if (type.isAnnotationPresent(Specializes.class)) {
	for (Class parent = cl.getSuperclass();
	     parent != null;
	     parent = parent.getSuperclass()) {
	  _specializedClasses.add(parent);
	}
      }

      _pendingAnnotatedTypes.add(type);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  private boolean isDisabled(Class type)
  {
    boolean isDisabled = false;
    
    for (Annotation ann : type.getAnnotations()) {
      Class annType = ann.annotationType();

      // check stereotypes
      if (_deploymentMap.containsKey(annType))
	return false;
      
      if (annType.equals(Disabled.class)
	  || annType.isAnnotationPresent(Disabled.class)) {
	isDisabled = true;
      }
    }

    return isDisabled && ! _deploymentMap.containsKey(type);
  }
  
  private boolean isValidSimpleBean(Class type)
  {
    if (type.isInterface())
      return false;

    /* XXX: ioc/024d
    if (type.getTypeParameters() != null
	&& type.getTypeParameters().length > 0) {
      return false;
    }
    */

    if (! isValidConstructor(type))
      return false;
    
    return true;
  }

  private boolean isValidConstructor(Class type)
  {
    for (Constructor ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
	return true;

      if (ctor.isAnnotationPresent(Initializer.class))
	return true;
    }

    return false;
  }

  private void discoverBean(AnnotatedType type)
  {
    if (_specializedClasses.contains(type.getJavaClass()))
      return;
      
    InjectionTarget target = createInjectionTarget(type);

    target = processInjectionTarget(target);

    if (target == null)
      return;

    ManagedBeanImpl bean = new ManagedBeanImpl(this, type, target);

    bean.introspect();

    addDiscoveredBean(bean);
	    
    // beans.addScannedClass(cl);
  }

  private <X> void addDiscoveredBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);

    for (ObserverMethodImpl observer : managedBean.getObserverMethods()) {
      // observer = processObserver(observer);

      if (observer != null) {
	Set<Annotation> annSet = observer.getObservedEventBindings();
	  
	Annotation []bindings = new Annotation[annSet.size()];
	annSet.toArray(bindings);

	BaseType baseType = createBaseType(observer.getObservedEventType());
	  
	addObserver(observer, baseType, bindings);
      }
    }

    for (ProducesBean producerBean : managedBean.getProducerBeans()) {
      addBean(producerBean);
    }
  }

  public <X> void addManagedBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);

    for (ObserverMethodImpl observer : managedBean.getObserverMethods()) {
      // observer = processObserver(observer);

      if (observer != null) {
	Set<Annotation> annSet = observer.getObservedEventBindings();
	  
	Annotation []bindings = new Annotation[annSet.size()];
	annSet.toArray(bindings);

	BaseType baseType = createBaseType(observer.getObservedEventType());
	  
	addObserver(observer, baseType, bindings);
      }
    }

    for (ProducesBean producerBean : managedBean.getProducerBeans()) {
      addBean(producerBean);
    }
  }

  public void updateExtensions()
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Enumeration e = loader.getResources("META-INF/services/" + Extension.class.getName());

      while (e.hasMoreElements()) {
	URL url = (URL) e.nextElement();

	if (_extensionSet.contains(url))
	  continue;

	_extensionSet.add(url);

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
	      loadExtension(line);
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
  }

  private void loadExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class cl = Class.forName(className, false, loader);

      if (! Extension.class.isAssignableFrom(cl))
	throw new InjectionException(L.l("'{0}' is not a valid extension because it does not implement {1}",
					 cl, Extension.class.getName()));

      Extension extension = (Extension) cl.newInstance();

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void addExtension(Extension extension)
  {
    for (Method method : extension.getClass().getMethods()) {
      bindObserver(extension, method);
    }
  }

  private void bindObserver(Extension extension, Method method)
  {
    Type []param = method.getGenericParameterTypes();

    if (param.length < 1)
      return;
    
    Annotation [][]paramAnn = method.getParameterAnnotations();

    if (! hasObserver(paramAnn))
      return;

    BeanArg []args = new BeanArg[param.length];

    for (int i = 1; i < param.length; i++) {
      Annotation []bindings = getBindings(paramAnn[i]);

      if (bindings.length == 0)
	bindings = new Annotation[] { CurrentLiteral.CURRENT };

      args[i] = new BeanArg(param[i], bindings);
    }

    Observer observer = new ExtensionObserver(extension, method, args);

    BaseType baseType = createBaseType(param[0]);
      
    addObserver(observer, baseType, getBindings(paramAnn[0]));

    /*
    // XXX: isAssignableFrom
    if (_isBeforeBeanDiscoveryComplete
	&& param[0].equals(BeforeBeanDiscovery.class)) {
      observer.notify(new BeforeBeanDiscoveryImpl());
    }
    
    if (_isAfterBeanDiscoveryComplete
	&& param[0].equals(AfterBeanDiscovery.class)) {
      observer.notify(new AfterBeanDiscoveryImpl());
    }
    */
  }

  private boolean hasObserver(Annotation [][]paramAnn)
  {
    for (int i = 0; i < paramAnn.length; i++) {
      for (int j = 0; j < paramAnn[i].length; j++) {
	if (paramAnn[i][j].annotationType().equals(Observes.class))
	  return true;
      }
    }

    return false;
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

      ArrayList<AbstractBean> bindList
	= new ArrayList<AbstractBean>(_pendingBindList);
      
      _pendingBindList.clear();

      isBind = bindList.size() > 0 || ! _isAfterBeanDiscoveryComplete;

      if (isBind) {
	_isAfterBeanDiscoveryComplete = true;

	fireEvent(new AfterBeanDiscoveryImpl());
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
	fireEvent(new AfterDeploymentValidationImpl());
      }
    } catch (ConfigException e) {
      if (_configException == null)
	_configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public Class loadClass(String className)
  {
    try {
      return Class.forName(className, false, _classLoader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
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
  
  void addService(Bean bean)
  {
    _pendingServiceList.add(bean);
  }
  
  /**
   * Initialize all the services
   */
  private void startServices()
  {
    ArrayList<Bean> services;
    // ArrayList<ManagedBean> registerServices;

    synchronized (_pendingServiceList) {
      services = new ArrayList<Bean>(_pendingServiceList);
      _pendingServiceList.clear();
    }

    for (Bean bean : services) {
      CreationalContext<?> env = createCreationalContext();
      
      getReference(bean, bean.getBeanClass(), env);
    }

    /*
    for (ManagedBean bean : registerServices) {
      startRegistration(bean);
    }
    */
  }

  private boolean isStartupPresent(Annotation []annList)
  {
    for (Annotation ann : annList) {
      Class<? extends Annotation> annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(StartupType.class)) {
	return true;
      }
      else if (annType.isAnnotationPresent(Stereotype.class)
	       && isStartupPresent(annType.getAnnotations())) {
	return true;
      }
    }

    return false;
  }

  private void registerBean(Bean bean, Annotation []annList)
  {
    for (Annotation ann : annList) {
      Class<? extends Annotation> annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(StartupType.class)) {
	registerBean(bean, ann);
      }
      else if (annType.isAnnotationPresent(Stereotype.class)) {
	registerBean(bean, annType.getAnnotations());
      }
    }
  }

  private void registerBean(Bean bean, Annotation ann)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation bindingAnn : ann.annotationType().getAnnotations()) {
      Class<? extends Annotation> annType = bindingAnn.annotationType();

      if (annType.isAnnotationPresent(BindingType.class))
	bindingList.add(bindingAnn);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    fireEvent(new BeanStartupEvent(this, bean), bindings);
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
    _tempClassLoader = null;

    _deploymentMap = null;
    
    _selfBeanMap = null;
    _selfNamedBeanMap = null;
    _selfObserverMap = null;
    
    _beanMap = null;
    _namedBeanMap = null;
    _observerMap = null;

    _rootContextMap = null;
    _contextMap = null;
    _observerListCache = null;

    _interceptorList = null;
    _decoratorList = null;
    _pendingRootContextList = null;
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

  //
  // ScanListener
  //

  /**
   * Since webbeans doesn't enhance, it's priority 1
   */
  public int getPriority()
  {
    return 1;
  }

  /**
   * Returns true if the root is a valid scannable root.
   */
  public boolean isRootScannable(Path root)
  {
    if (! (root.lookup("META-INF/beans.xml").canRead()
	   || (root.getFullPath().endsWith("WEB-INF/classes/")
	       && root.lookup("../beans.xml").canRead()))) {
      return false;
    }

    WebBeansRootContext context = _rootContextMap.get(root);

    if (context == null) {
      context = new WebBeansRootContext(root);
      _rootContextMap.put(root, context);
      _pendingRootContextList.add(context);
    }

    if (context.isScanComplete())
      return false;
    else {
      if (log.isLoggable(Level.FINER))
	log.finer("WebBeans scanning " + root.getURL());

      context.setScanComplete(true);
      return true;
    }
  }

  /**
   * Checks if the class can be a simple class
   */
  public ScanMatch isScanMatchClass(String className, int modifiers)
  {
    if (Modifier.isInterface(modifiers))
      return ScanMatch.DENY;
    else if (! Modifier.isPublic(modifiers))
      return ScanMatch.DENY;
    else if (Modifier.isAbstract(modifiers))
      return ScanMatch.ALLOW;
    else
      return ScanMatch.MATCH;
  }

  public boolean isScanMatchAnnotation(CharBuffer annotationName)
  {
    try {
      String className = annotationName.toString();

      if (className.startsWith("javax.enterprise.inject"))
	return true;
      
      Class cl = Class.forName(className, false, _tempClassLoader);

      Annotation []annList = cl.getAnnotations();

      if (annList != null) {
	for (Annotation ann : annList) {
	  Class annType = ann.annotationType();
	  
	  if (annType.getName().startsWith("javax.enterprise.inject"))
	    return true;
	}
      }
    } catch (ClassNotFoundException e) {
    }

    return false;
  }
  
  /**
   * Callback to note the class matches
   */
  public void classMatchEvent(EnvironmentClassLoader loader,
			      Path root,
			      String className)
  {
    WebBeansRootContext context = _rootContextMap.get(root);

    if (context == null) {
      context = new WebBeansRootContext(root);
      _rootContextMap.put(root, context);
      _pendingRootContextList.add(context);
    }
      
    context.addClassName(className);
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
    if (type instanceof Class)
      return ((Class) type).getSimpleName();
    else
      return String.valueOf(type);
  }

  static class TypedBean {
    private final BaseType _type;
    private final Bean _bean;
    private final boolean _isModulePrivate;

    TypedBean(BaseType type, Bean bean)
    {
      _type = type;
      _bean = bean;

      _isModulePrivate = isModulePrivate(bean);
    }

    boolean isModulePrivate()
    {
      return _isModulePrivate;
    }

    BaseType getType()
    {
      return _type;
    }

    Bean getBean()
    {
      return _bean;
    }

    static boolean isModulePrivate(Bean bean)
    {
      if (! (bean instanceof AnnotatedBean))
	return false;
      
      Annotated annotated = ((AnnotatedBean) bean).getAnnotated();

      if (annotated == null)
	return false;

      for (Annotation ann : annotated.getAnnotations()) {
	Class annType = ann.annotationType();

	if (annType.equals(ModulePrivate.class)
	    || annType.isAnnotationPresent(ModulePrivate.class)) {
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
    public void addBindingType(Class<? extends Annotation> bindingType)
    {
    }

    public void addScopeType(Class<? extends Annotation> scopeType,
			     boolean isNormal,
			     boolean isPassivating)
    {
    }

    public void addStereotype(Class<? extends Annotation> stereotype,
			      Annotation... stereotypeDef)
    {
    }

    public void addInterceptorBindingType(Class<? extends Annotation> bindingType,
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

    ProcessInjectionTargetImpl(InjectionTarget<X> target)
    {
      _target = target;
    }

    public AnnotatedType<X> getAnnotatedType()
    {
      return null;
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
    public void addBean(Bean bean)
    {
      InjectManager.this.addBean(bean);
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

  static class ExtensionObserver implements Observer<Object> {
    private Extension _extension;
    private Method _method;
    private BeanArg []_args;

    ExtensionObserver(Extension extension, Method method, BeanArg []args)
    {
      _extension = extension;
      _method = method;
      _args = args;
    }

    public boolean notify(Object event)
    {
      try {
	Object []args = new Object[_args.length];
	args[0] = event;

	for (int i = 1; i < args.length; i++) {
	  args[i] = _args[i].eval(null);
	}

	_method.invoke(_extension, args);

	return false;
      } catch (RuntimeException e) {
	throw e;
      } catch (InvocationTargetException e) {
	String loc = (_extension + "." + _method.getName() + ": ");
	
	throw new InjectionException(loc + e.getMessage(), e.getCause());
      } catch (Exception e) {
	String loc = (_extension + "." + _method.getName() + ": ");
	
	throw new InjectionException(loc + e.getMessage(), e);
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _extension + "," + _method.getName() + "]";
    }
  }

  static {
    ArrayList<Class> forbiddenAnnotations = new ArrayList<Class>();
    ArrayList<Class> forbiddenClasses = new ArrayList<Class>();

    for (String className : FORBIDDEN_ANNOTATIONS) {
      try {
	Class cl = Class.forName(className);

	if (cl != null)
	  forbiddenAnnotations.add(cl);
      } catch (Throwable e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    }

    for (String className : FORBIDDEN_CLASSES) {
      try {
	Class cl = Class.forName(className);

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
  }
}
