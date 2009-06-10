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
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.TypeLiteral;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.deployment.Standard;
import javax.enterprise.inject.deployment.Production;
import javax.enterprise.inject.deployment.Specializes;
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
	     java.io.Serializable
{
  private static final L10N L = new L10N(InjectManager.class);
  private static final Logger log
    = Logger.getLogger(InjectManager.class.getName());

  private static final String SCHEMA = "com/caucho/config/cfg/webbeans.rnc";

  private static final EnvironmentLocal<InjectManager> _localContainer
    = new EnvironmentLocal<InjectManager>();
  
  private static final Annotation []NULL_ANN = new Annotation[0];

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

  private static final String []FACTORY_CLASSES = {
    "com.caucho.ejb.cfg.StatelessBeanConfigFactory",
  };

  private static final Class []_forbiddenAnnotations;
  private static final Class []_forbiddenClasses;
  private static final Class []_factoryClasses;

  private String _id;

  private InjectManager _parent;
  
  private EnvironmentClassLoader _classLoader;
  private ClassLoader _tempClassLoader;

  private HashSet<URL> _extensionSet = new HashSet<URL>();

  private AtomicInteger _beanId = new AtomicInteger();

  private HashMap<Path,BeansConfig> _beansConfigMap
    = new HashMap<Path,BeansConfig>();

  private HashSet<String> _configuredClasses
    = new HashSet<String>();

  private ArrayList<AnnotatedType> _pendingAnnotatedTypes
    = new ArrayList<AnnotatedType>();

  private HashSet<Class> _specializedClasses
    = new HashSet<Class>();

  private HashSet<Class<? extends Annotation>> _deploymentSet
    = new HashSet<Class<? extends Annotation>>();
    
  private HashMap<Class,Integer> _deploymentMap
    = new HashMap<Class,Integer>();

  private HashMap<Class,WebComponent> _componentMap
    = new HashMap<Class,WebComponent>();

  private HashMap<Type,Bean> _newBeanMap
    = new HashMap<Type,Bean>();

  private BaseTypeFactory _baseTypeFactory = new BaseTypeFactory();

  private HashMap<BaseType,WebComponent> _componentBaseTypeMap
    = new HashMap<BaseType,WebComponent>();

  private HashMap<String,Bean> _namedComponentMap
    = new HashMap<String,Bean>();

  private HashMap<Path,WebBeansRootContext> _rootContextMap
    = new HashMap<Path,WebBeansRootContext>();

  private HashMap<Class,Context> _contextMap
    = new HashMap<Class,Context>();

  private HashMap<Class,ObserverMap> _observerMap
    = new HashMap<Class,ObserverMap>();

  private HashMap<Class,ArrayList<ObserverMap>> _observerListCache
    = new HashMap<Class,ArrayList<ObserverMap>>();

  private ArrayList<InterceptorEntry> _interceptorList
    = new ArrayList<InterceptorEntry>();

  private ArrayList<DecoratorEntry> _decoratorList
    = new ArrayList<DecoratorEntry>();

  private HashMap<String,SoftReference<InjectionTarget>> _transientMap
    = new HashMap<String,SoftReference<InjectionTarget>>();

  private HashMap<FactoryBinding,Bean> _objectFactoryMap
    = new HashMap<FactoryBinding,Bean>();

  private boolean _isUpdateNeeded = true;
  
  private ArrayList<Path> _pendingPathList
    = new ArrayList<Path>();

  private ArrayList<WebBeansRootContext> _pendingRootContextList
    = new ArrayList<WebBeansRootContext>();

  private ArrayList<AbstractBean> _pendingBindList
    = new ArrayList<AbstractBean>();

  private ArrayList<Bean> _pendingServiceList
    = new ArrayList<Bean>();

  private HashMap<Class,InjectProgram> _injectMap
    = new HashMap<Class,InjectProgram>();

  private ArrayList<Bean> _pendingRegistrationList
    = new ArrayList<Bean>();

  private ArrayList<BeanConfigFactory> _customFactoryList
    = new ArrayList<BeanConfigFactory>();

  private Lifecycle _lifecycle = new Lifecycle();
  private boolean _isBeforeBeanDiscoveryComplete;
  private boolean _isAfterBeanDiscoveryComplete;

  private ELResolver _elResolver;

  private ApplicationScope _applicationScope = new ApplicationScope();
  private XmlStandardPlugin _xmlExtension;

  private RuntimeException _configException;

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
	ic.rebind("java:comp/Manager", new WebBeansJndiProxy());
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

      _deploymentMap.put(Standard.class, 0);
      _deploymentMap.put(CauchoDeployment.class, 1);
      _deploymentMap.put(Production.class, 2);
      _deploymentMap.put(Configured.class, 3);

      for (Class cl : _factoryClasses) {
	try {
	  BeanConfigFactory factory = (BeanConfigFactory) cl.newInstance();
	  _customFactoryList.add(factory);
	} catch (Exception e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }

      BeanFactory factory = createBeanFactory(InjectManager.class);
      factory.deployment(Standard.class);
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
    if (deploymentList.size() < 1
	|| ! deploymentList.get(0).equals(Standard.class)) {
      throw new ConfigException(L.l("<Deploy> must contain @javax.webbeans.Standard as its first element because @Standard is always an enabled @DeploymentType"));
    }

    _deploymentMap.clear();

    boolean hasConfigured = deploymentList.contains(Configured.class);

    int i = 0;
    for (Class deploymentType : deploymentList) {
      _deploymentMap.put(deploymentType, i++);

      if (! hasConfigured && deploymentType == Production.class) {
	_deploymentMap.put(Configured.class, i++);
      }
    }
  }
  
  public void addComponentByName(String name, Bean comp)
  {
    _namedComponentMap.put(name, comp);
  }

  /**
   * Adds a component by the interface type
   *
   * @param type the interface type to expose the component
   * @param comp the component to register
   */
  public void addComponentByType(Type type, Bean comp)
  {
    if (type == null)
      return;

    addComponentByType(createBaseType(type), comp);
  }
    
  private void addComponentByType(BaseType type, Bean bean)
  {
    if (type == null)
      return;
    
    if (log.isLoggable(Level.FINEST))
      log.finest(bean + "(" + type + ") added to " + this);

    if (isUnbound(bean)) {
      return;
    }

    Class rawType = type.getRawClass();
    
    WebComponent webComponent;

    synchronized (_componentMap) {
      webComponent = _componentMap.get(rawType);

      if (webComponent == null) {
	webComponent = new WebComponent(this, rawType);
      
	_componentMap.put(rawType, webComponent);
	// _componentMap.clear();
      }
    }

    webComponent.addComponent(type, bean);
  }

  private boolean isUnbound(Bean bean)
  {
    for (Object annObj : bean.getBindings()) {
      Annotation ann = (Annotation) annObj;
      
      if (Unbound.class.equals(ann.annotationType()))
	return true;
    }

    return false;
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

  /**
   * Creates an injection program for the given field
   */
  public void createProgram(ArrayList<ConfigProgram> injectList,
			    Field field,
			    boolean isOptional)
    throws ConfigException
  {
    // ioc/0i36
    if (field.isAnnotationPresent(Decorates.class))
      return;
    
    Annotation []bindings;
    
    if (field.isAnnotationPresent(New.class)) {
      bindings = new Annotation[] { NewLiteral.NEW };

      Set set = resolve(field.getGenericType(), bindings);

      if (set == null || set.size() == 0) {
	addBean(createNewBean(field.getType()));
      }
    }
    else
      bindings = getBindings(field.getAnnotations());

    /*
    if (field.isAnnotationPresent(Observable.class)) {
      injectList.add(new FieldEventProgram(this, field, bindings));
      return;
    }
    */
      
    Set set = resolve(field.getGenericType(), bindings);

    if (set != null && set.size() == 1) {
      Iterator iter = set.iterator();
      if (iter.hasNext()) {
	Bean bean = (Bean) iter.next();

	injectList.add(new FieldComponentProgram(this, bean, field));
	return;
      }
    }

    if (set != null && set.size() > 1) {
      throw injectError(field, L.l("Can't inject a bean for '{0}' because multiple beans match: {1}",
				   field.getType().getName(),
				   toLineList(set)));
    }
    
    if (! isOptional) {
      WebComponent component = getWebComponent(field.getGenericType());

      if (component == null) {
	throw injectError(field, L.l("Can't find a component for '{0}' because no beans implementing that class have been registered with the injection Manager.",
				   field.getType().getName()));
      }
      else {
	ArrayList<Bean<?>> enabledList = component.getEnabledBeanList();

	if (enabledList.size() == 0) {
	  throw injectError(field, L.l("Can't find a component for '{0}' because any matching beans are disabled, i.e. non-enabled Deploy.\nDisabled beans:{2}",
				       field.getType().getName(),
				       toList(bindings),
				       listToLines(component.getBeanList())));
	}
	else {
	  throw injectError(field, L.l("Can't find a component for '{0}' because no enabled beans match the bindings {1}.\nEnabled beans:{2}",
				       field.getType().getName(),
				       toList(bindings),
				       listToLines(enabledList)));
	}
      }
    }
  }

  private String listToLines(List list)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < list.size(); i++) {
      sb.append("\n    ").append(list.get(i));
    }

    return sb.toString();
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
  protected Bean findByName(String name)
  {
    // #3334 - shutdown timing issues
    HashMap<String,Bean> namedComponentMap = _namedComponentMap;

    if (namedComponentMap == null)
      return null;
    
    Bean comp = _namedComponentMap.get(name);
    
    if (comp != null)
      return comp;
    else if (_parent != null)
      return _parent.findByName(name);
    else
      return null;
  }

  /**
   * Injects an object
   */
  public void injectObject(Object obj)
  {
    if (obj == null)
      return;

    Class cl = obj.getClass();
    InjectProgram program;

    synchronized (_injectMap) {
      program = _injectMap.get(cl);

      if (program == null) {
	program = InjectIntrospector.introspectProgram(cl, null);
	_injectMap.put(cl, program);
      }
    }

    program.configure(obj);
  }
  

  //
  // javax.webbeans.Container
  //

  public Conversation createConversation()
  {
    return (Conversation) _contextMap.get(ConversationScoped.class);
  }

  private void fillObserverList(ArrayList<ObserverMap> list, Class cl)
  {
    if (cl == null)
      return;

    // XXX: generic
    fillObserverList(list, cl.getSuperclass());

    for (Class iface : cl.getInterfaces())
      fillObserverList(list, iface);

    ObserverMap map = _observerMap.get(cl);

    if (map != null)
      list.add(map);
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

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  /*
  public <T> BeanInstance<T> createTransientInstance(Class<T> type)
  {

    Contextual<T> factory = createTransient(type);

    // server/10gn
    T value = factory.create(new ConfigContext());

    return new BeanInstance(factory, value);
  }
  */

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> T createTransientObjectNoInit(Class<T> type)
  {
    /*
    AbstractBean comp = (AbstractBean) createManagedBean(type);

    return (T) comp.createNoInit();
    */

    throw new UnsupportedOperationException();
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> BeanInstance<T> createTransientInstanceNoInit(Class<T> type)
  {
    /*
    ComponentImpl<T> bean = (ComponentImpl) createTransient(type);

    // server/10gn
    T value = (T) bean.createNoInit();

    return new BeanInstance<T>(bean, value);
    */
    return null;
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

    fireEvent(processType);

    return processType.getAnnotatedType();
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
      addComponentByType(type, bean);
    }

    if (bean.getName() != null) {
      addComponentByName(bean.getName(), bean);
    }

    /*
    if (bean instanceof ManagedBeanImpl) {
      ManagedBeanImpl<?> mBean = (ManagedBeanImpl) bean;

      for (ProducesBean producerBean : mBean.getProducerBeans()) {
	addBean(producerBean);
      }
    }
    */

    registerJmx(bean);
  }

  public void addBeanConfigFactory(BeanConfigFactory factory)
  {
    _customFactoryList.add(factory);
  }

  /*
  public BeanManager addConfigBean(Bean configBean)
  {
    for (BeanConfigFactory factory : _customFactoryList) {
      if (factory.createBean(configBean))
	return this;
    }
    
    addBean(configBean);

    return this;
  }
  */

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
    HashSet<Bean<?>> beans = new HashSet<Bean<?>>();

    Bean bean = findByName(name);

    if (bean != null)
      beans.add(bean);

    return beans;
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

  public <X> Bean<X> getMostSpecializedBean(Bean<X> bean)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public <X> Bean<? extends X> getHighestPrecedenceBean(Set<Bean<? extends X>> beans)
  {
    // XXX:
    for (Bean bean : beans) {
      return bean;
    }

    return null;
  }

  public void validate(InjectionPoint ij)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set resolve(Type type,
		     Set<Annotation> bindingSet)
  {
    Annotation []bindings = new Annotation[bindingSet.size()];
    bindingSet.toArray(bindings);

    return resolve(type, bindings);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set resolve(Type type,
		     Annotation []bindings)
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
  public Set resolve(BaseType baseType,
		     Annotation []bindings)
  {
    WebComponent component = getWebComponent(baseType);

    if (component != null) {
      Set beans = component.resolve(baseType, bindings);

      if (log.isLoggable(Level.FINER))
	log.finer(this + " bind(" + baseType.getSimpleName()
		  + "," + toList(bindings) + ") -> " + beans);

      if (beans != null && beans.size() > 0)
	return beans;
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
      
      HashSet set = new HashSet();
      set.add(new InstanceBeanImpl(this, beanType, bindings));
      return set;
    }
    else {
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
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " bind(" + baseType.getSimpleName()
		+ "," + toList(bindings) + ") -> none");
    }

    return null;
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
      

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set<Bean> resolveAllByType(Class type)
  {
    Annotation []bindings = new Annotation[0];
    
    WebComponent component = getWebComponent(type);

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

  public int getDeploymentPriority(Class deploymentType)
  {
    Integer value = _deploymentMap.get(deploymentType);

    if (value != null)
      return value;
    else
      return -1;
  }

  private Set resolveAllBeans()
  {
    Annotation []bindings = new Annotation[0];
    
    synchronized (_componentMap) {
      LinkedHashSet beans = new LinkedHashSet();

      for (WebComponent comp : _componentMap.values()) {
	Set set = comp.resolve(Object.class, bindings);

	beans.addAll(set);
      }

      return beans;
    }
  }

  public WebComponent getWebComponent(Type type)
  {
    BaseType baseType = createBaseType(type);

    return getWebComponent(baseType);
  }

  public WebComponent getWebComponent(BaseType baseType)
  {
    Class rawClass = baseType.getRawClass();
    
    WebComponent comp = _componentMap.get(baseType.getRawClass());

    return comp;
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
  
  private Object getInstanceRec(Bean<?> bean,
				Type type,
				CreationalContext createContext,
				InjectManager topManager)
  {
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
    WebComponent component = getWebComponent(type);

    if (component == null) {
      throw new UnsatisfiedResolutionException(L.l("Can't find a component for '{0}' because no beans implementing that class have been registered with the injection Manager.",
						   type));
    }
    else {
      ArrayList<Bean<?>> enabledList = component.getEnabledBeanList();

      if (enabledList.size() == 0) {
	return new UnsatisfiedResolutionException(L.l("Can't find a component for '{0}' because any matching beans are disabled, i.e. non-enabled Deploy.\nDisabled beans:{2}",
						     type,
						     toList(bindings),
						     listToLines(component.getBeanList())));
      }
      else {
	return new UnsatisfiedResolutionException(L.l("Can't find a component for '{0}' because no enabled beans match the bindings {1}.\nEnabled beans:{2}",
						      type,
						      toList(bindings),
						      listToLines(enabledList)));
      }
    }
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

  private String toLineList(Iterable list)
  {
    StringBuilder sb = new StringBuilder();

    for (Object item : list) {
      sb.append("\n  ").append(item);
    }

    return sb.toString();
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

    Annotation []bindings;

    if (bindingSet != null) {
      bindings = new Annotation[bindingSet.size()];
      bindingSet.toArray(bindings);
    }
    else
      bindings = new Annotation[] { CurrentLiteral.CURRENT };

    Set set = getBeans(type, bindings);

    if (set == null || set.size() == 0) {
      throw unsatisfiedException(type, bindings);
    }
    else if (set.size() == 1) {
      Iterator iter = set.iterator();

      if (iter.hasNext()) {
	Bean bean = (Bean) iter.next();

	/*
	if (bean instanceof ComponentImpl)
 	  bean = ((ComponentImpl) bean).bindInjectionPoint(ij);
	*/

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
    throw new UnsupportedOperationException(getClass().getSimpleName());
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
    BaseType observerType = baseType.findClass(Observer.class);

    Class eventType = observerType.getParameters()[0].getRawClass();

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

      map.addObserver(observer, bindings);
    }

    synchronized (_observerListCache) {
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

    fireEventImpl(event, bindings);
  }
  
  protected void fireEventImpl(Object event, Annotation... bindings)
  {
    if (_parent != null)
      _parent.fireEventImpl(event, bindings);

    ArrayList<ObserverMap> observerList;

    synchronized (_observerListCache) {
      observerList = _observerListCache.get(event.getClass());

      if (observerList == null) {
	observerList = new ArrayList<ObserverMap>();
	
	fillObserverList(observerList, event.getClass());
	
	_observerListCache.put(event.getClass(), observerList);
      }
    }

    int size = observerList.size();
    for (int i = 0; i < size; i++) {
      observerList.get(i).fireEvent(event, bindings);
    }
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
    
    ObserverMap map = _observerMap.get(event);

    if (map != null)
      map.resolveObservers(set, bindings);

    return set;
  }

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
    
    ArrayList<Interceptor<?>> interceptorList = new ArrayList<Interceptor<?>>();

    for (InterceptorEntry entry : _interceptorList) {
      Interceptor interceptor = entry.getInterceptor();
      
      if (! interceptor.intercepts(type))
	continue;

      if (entry.isMatch(bindings))
	interceptorList.add(interceptor);
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
    _decoratorList.add(new DecoratorEntry(decorator));

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
      /*
      if (isTypeContained(types, decorator.getDelegateType())
	  && entry.isMatch(bindings)) {
	decorators.add(decorator);
      }
      */
    }

    return decorators;
  }

  public List<Decorator> resolveDecorators(Class type)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : type.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }
    
    if (bindingList.size() == 0)
      bindingList.add(new CurrentLiteral());

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);
    
    ArrayList<Decorator> decorators = new ArrayList<Decorator>();

    for (DecoratorEntry entry : _decoratorList) {
      Decorator decorator = entry.getDecorator();

      // XXX: delegateTypes
      /*
      if (decorator.getDelegateTypes().isAssignableFrom(type)
	  && entry.isMatch(bindings)) {
	decorators.add(decorator);
      }
      */
    }

    return decorators;
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

  private boolean isTypeContained(Set<Class<?>> types, Class delegateType)
  {
    for (Class type : types) {
      if (delegateType.isAssignableFrom(type))
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

  /*
  public void update()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      updateExtensions();

      if (! _isBeforeBeanDiscoveryComplete) {
	_isBeforeBeanDiscoveryComplete = true;

	fireEvent(new BeforeBeanDiscoveryImpl());
      }

      ArrayList<Path> pathList
	= new ArrayList<Path>(_pendingPathList);
      
      _pendingPathList.clear();

      for (Path path : pathList) {
	if (_beansConfigMap.get(path) != null)
	  continue;
	
	if (path.canRead()) {
	  BeansConfig beans = new BeansConfig(this, path);
	  _beansConfigMap.put(path, beans);
	  
	  path.setUserPath(path.getURL());
	    
	  new Config().configure(beans, path, SCHEMA);
	}
      }

      ArrayList<WebBeansRootContext> rootContextList
	= new ArrayList<WebBeansRootContext>(_pendingRootContextList);
      
      _pendingRootContextList.clear();

      for (WebBeansRootContext context : rootContextList) {
	Path root = context.getRoot();
      
	BeansConfig beans = _beansConfigMap.get(root);

	if (beans == null) {
	  beans = new BeansConfig(this, root);
	  _beansConfigMap.put(root, beans);

	  Path path = root.lookup("META-INF/beans.xml");
	  
	  if (path.canRead()) {
	    path.setUserPath(path.getURL());
	    
	    new Config().configure(beans, path, SCHEMA);
	  }
	}

	class_loop:
	for (String className : context.getClassNameList()) {
	  try {
	    Class cl = Class.forName(className, false, _classLoader);

	    if (! SimpleBean.isValid(cl))
	      continue;
	    
	    if (cl.getDeclaringClass() != null
		&& ! Modifier.isStatic(cl.getModifiers()))
	      continue;

	    for (Class forbiddenAnnotation : _forbiddenAnnotations) {
	      if (cl.isAnnotationPresent(forbiddenAnnotation))
		continue class_loop;
	    }

	    for (Class forbiddenClass : _forbiddenClasses) {
	      if (forbiddenClass.isAssignableFrom(cl))
		continue class_loop;
	    }
	    
	    beans.addScannedClass(cl);
	  } catch (ClassNotFoundException e) {
	    log.log(Level.FINER, e.toString(), e);
	  }
	}

	beans.update();

	beans.init();
      }
    } catch (ConfigException e) {
      if (_configException == null)
	_configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  */
  
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

      if (rootContextList.size() == 0)
	return;
      
      for (WebBeansRootContext context : rootContextList) {
	_xmlExtension.addRoot(context.getRoot());
      }
      
      _isBeforeBeanDiscoveryComplete = true;
      fireEvent(new BeforeBeanDiscoveryImpl());

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
	  
	// XXX: addObserver(observer, bindings);
      }
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

    /* XXX:
    Observer observer = new ExtensionObserver(extension, method, args);

    addObserver(observer, param[0], getBindings(paramAnn[0]));

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

      /*
      registerServices = new ArrayList<ManagedBean>(_pendingRegistrationList);
      _pendingRegistrationList.clear();
      */
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

    _beansConfigMap = null;
    _deploymentSet = null;
    _deploymentMap = null;
    
    _componentMap = null;
    _componentBaseTypeMap = null;
    _namedComponentMap = null;

    _rootContextMap = null;
    _contextMap = null;
    _observerMap = null;
    _observerListCache = null;

    _interceptorList = null;
    _decoratorList = null;
    _transientMap = null;
    _objectFactoryMap = null;
    _pendingRootContextList = null;
    _pendingBindList = null;
    _pendingServiceList = null;
    _injectMap = null;
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
    if (! root.lookup("META-INF/beans.xml").canRead()) {
      if (root.getFullPath().endsWith("WEB-INF/classes/")
	  && ! root.lookup("../beans.xml").canRead()) {
	return false;
      }
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

  /**
   * Serialization rewriting
   */
  public Object writeReplace()
  {
    return new SingletonHandle(BeanManager.class);
  }

  private void checkActive()
  {
    if (_beansConfigMap == null)
      throw new IllegalStateException(L.l("{0} is closed", this));
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

    ProcessAnnotatedTypeImpl(AnnotatedType<X> annotatedType)
    {
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

    public void veto()
    {
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

    public void notify(Object event)
    {
      try {
	Object []args = new Object[_args.length];
	args[0] = event;

	for (int i = 1; i < args.length; i++) {
	  args[i] = _args[i].eval(null);
	}

	_method.invoke(_extension, args);
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
    ArrayList<Class> factoryClasses = new ArrayList<Class>();

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

    for (String className : FACTORY_CLASSES) {
      try {
	Class cl = Class.forName(className);

	if (cl != null)
	  factoryClasses.add(cl);
      } catch (Throwable e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    }

    _forbiddenAnnotations = new Class[forbiddenAnnotations.size()];
    forbiddenAnnotations.toArray(_forbiddenAnnotations);

    _forbiddenClasses = new Class[forbiddenClasses.size()];
    forbiddenClasses.toArray(_forbiddenClasses);

    _factoryClasses = new Class[factoryClasses.size()];
    factoryClasses.toArray(_factoryClasses);
  }
}
