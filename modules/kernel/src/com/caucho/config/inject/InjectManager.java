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
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.MethodComponentProgram;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.program.FieldEventProgram;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.CreationContextImpl;
import com.caucho.config.scope.ScopeContext;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.ref.*;

import javax.annotation.Stereotype;
import javax.context.Context;
import javax.context.Contextual;
import javax.context.ContextNotActiveException;
import javax.context.Conversation;
import javax.context.ConversationScoped;
import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.decorator.Decorates;
import javax.el.*;
import javax.event.Observer;
import javax.event.Observable;
import javax.inject.AnnotationLiteral;
import javax.inject.BindingType;
import javax.inject.New;
import javax.inject.Production;
import javax.inject.Produces;
import javax.inject.Standard;
import javax.inject.TypeLiteral;
import javax.inject.AmbiguousDependencyException;
import javax.inject.UnsatisfiedDependencyException;
import javax.inject.manager.Bean;
import javax.inject.manager.Decorator;
import javax.inject.manager.Initialized;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.Interceptor;
import javax.inject.manager.InterceptionType;
import javax.inject.manager.Manager;
import javax.naming.*;

/**
 * The web beans container for a given environment.
 */
public class InjectManager
  implements Manager, ScanListener, EnvironmentListener,
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
    "javax.faces.component.UIComponent"
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

  private WbWebBeans _wbWebBeans;

  private int _beanId;

  private HashMap<Path,WbWebBeans> _webBeansMap
    = new HashMap<Path,WbWebBeans>();

  private HashSet<Class<? extends Annotation>> _deploymentSet
    = new HashSet<Class<? extends Annotation>>();

  private HashMap<Class,Integer> _deploymentMap
    = new HashMap<Class,Integer>();

  private HashMap<Type,WebComponent> _componentMap
    = new HashMap<Type,WebComponent>();

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

  private HashMap<String,SoftReference<SimpleBean>> _transientMap
    = new HashMap<String,SoftReference<SimpleBean>>();

  private HashMap<FactoryBinding,Bean> _objectFactoryMap
    = new HashMap<FactoryBinding,Bean>();

  private ArrayList<Path> _pendingPathList
    = new ArrayList<Path>();

  private ArrayList<WebBeansRootContext> _pendingRootContextList
    = new ArrayList<WebBeansRootContext>();

  private ArrayList<ComponentImpl> _pendingBindList
    = new ArrayList<ComponentImpl>();

  private ArrayList<CauchoBean> _pendingServiceList
    = new ArrayList<CauchoBean>();

  private HashMap<Class,InjectProgram> _injectMap
    = new HashMap<Class,InjectProgram>();

  private ArrayList<BeanRegistrationListener> _registrationListenerList
    = new ArrayList<BeanRegistrationListener>();

  private ArrayList<CauchoBean> _pendingRegistrationList
    = new ArrayList<CauchoBean>();

  private ArrayList<BeanConfigFactory> _customFactoryList
    = new ArrayList<BeanConfigFactory>();

  private ApplicationScope _applicationScope = new ApplicationScope();

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
    
      _wbWebBeans = new WbWebBeans(this, Vfs.lookup());

      addContext("com.caucho.server.webbeans.RequestScope");
      addContext("com.caucho.server.webbeans.SessionScope");
      addContext("com.caucho.server.webbeans.ConversationScope");
      addContext(_applicationScope);

      _deploymentMap.put(Standard.class, 0);
      _deploymentMap.put(CauchoDeployment.class, 1);
      _deploymentMap.put(Production.class, 2);

      if (_classLoader != null && isSetLocal) {
	_classLoader.addScanListener(this);
      }

      for (Class cl : _factoryClasses) {
	try {
	  BeanConfigFactory factory = (BeanConfigFactory) cl.newInstance();
	  _customFactoryList.add(factory);
	} catch (Exception e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }
    
      Environment.addEnvironmentListener(this, _classLoader);
    } finally {
      thread.setContextClassLoader(oldLoader);
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

  public WbWebBeans getWbWebBeans()
  {
    return _wbWebBeans;
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

  public WbComponentType createComponentType(Class cl)
  {
    return _wbWebBeans.createComponentType(cl);
  }

  public void addPath(Path path)
  {
    _pendingPathList.add(path);
  }

  public void setDeploymentTypes(ArrayList<Class> deploymentList)
  {
    if (deploymentList.size() < 1 ||
	! deploymentList.get(0).equals(Standard.class)) {
      throw new ConfigException(L.l("<Deploy> must contain @javax.webbeans.Standard as its first element because @Standard is always an enabled @DeploymentType"));
    }

    _deploymentMap.clear();

    for (int i = 0; i < deploymentList.size(); i++) {
      _deploymentMap.put(deploymentList.get(i), i);
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

    addComponentByType(BaseType.create(type, null), comp);
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

    WebComponent webComponent;

    synchronized (_componentMap) {
      webComponent = _componentBaseTypeMap.get(type);

      if (webComponent == null) {
	webComponent = new WebComponent(this, type);
      
	_componentBaseTypeMap.put(type, webComponent);
	_componentMap.clear();
      }
    }

    webComponent.addComponent(bean);
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

  public ArrayList<Bean> getBeansOfType(Type type)
  {
    ArrayList<Bean> beans = new ArrayList<Bean>();

    WebComponent webComponent = getWebComponent(type);

    if (webComponent == null)
      return beans;

    beans.addAll(webComponent.resolve(new Annotation[0]));

    return beans;
  }

  /**
   * Registers a singleton as a WebBean.  The value will be introspected
   * for its annotations like a SimpleBean to determine its attributes
   *
   * @param value the singleton value to register with the manager
   */
  public void addSingleton(Object value, Type... api)
  {
    addBean(new SingletonBean(value, null, api));
  }

  /**
   * Registers a singleton as a WebBean.  The value will be introspected
   * for its annotations like a SimpleBean to determine its attributes.
   * The name parameter override any introspected values.
   *
   * @param value the singleton value to register with the manager
   * @param name the WebBeans @Named value for the singleton
   */
  public void addSingleton(Object object,
			   String name,
			   Type ... api)
  {
    addBean(new SingletonBean(object,
			      null,
			      name,
			      new Annotation[] { Names.create(name),
						 CurrentLiteral.CURRENT },
			      api));
  }

  /**
   * Registers a singleton as a WebBean.  The value will be introspected
   * for its annotations like a SimpleBean to determine its attributes.
   * The name and api parameters override any introspected values.
   *
   * @param value the singleton value to register with the manager
   * @param name the WebBeans @Named value for the singleton
   * @param api the exported types for the singleton
   */
  /*
  public void addSingleton(Object object,
			   String name,
			   Type ...api)
  {
    addBean(new SingletonBean(object, name, api));
  }
  */

  /**
   * Registers a singleton as a WebBean.  The value will be introspected
   * for its annotations like a SimpleBean to determine its attributes.
   * The name, deployment type, and api parameters override any
   * introspected values.
   *
   * @param value the singleton value to register with the manager
   * @param deploymentType the WebBeans @DeploymentType value for the singleton
   * @param name the WebBeans @Named value for the singleton
   * @param api the exported types for the singleton
   */
  public void addSingleton(Object object,
			   Class deploymentType,
			   String name,
			   Type ...api)
  {
    addBean(new SingletonBean(object, deploymentType, name, api));
  }
  
  /**
   * Adds a singleton only to the name map
   * 
   * @param object the singleton value
   * @param name the singleton's name
   */
  public void addSingletonByName(Object object, String name)
  {
    addBean(new SingletonBean(object, name, new Type[0]));
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
	addBean(new NewBean(this, field.getType()));
      }
    }
    else
      bindings = getBindings(field.getAnnotations());
    
    if (field.isAnnotationPresent(Observable.class)) {
      injectList.add(new FieldEventProgram(this, field, bindings));
      return;
    }
      
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
				   set));
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
   * Creates an injection program for the given method
   */
  public void createProgram(ArrayList<ConfigProgram> injectList,
			    Method method)
    throws ConfigException
  {
    if (method.isAnnotationPresent(Produces.class))
      throw error(method, "An injection method may not have the @Produces annotation.");

    // XXX: lazy binding
    try {
      Type []paramTypes = method.getGenericParameterTypes();
      Annotation[][]paramAnn = method.getParameterAnnotations();
      
      ComponentImpl []args = new ComponentImpl[paramTypes.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = null;//bind(location(method), paramTypes[i], paramAnn[i]);

	if (args[i] == null) {
	  throw error(method,
		      L.l("Injection for type '{0}' of method parameter #{1} has no matching component.",
			  getSimpleName(paramTypes[i]), i));
	}
      }

      injectList.add(new MethodComponentProgram(method, args));
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw LineConfigException.create(method, e);
    }
  }

  /**
   * Finds a component by its component name.
   */
  public Bean findByName(String name)
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
   * Finds a component by its component name.
   */
  public Object getObjectByName(String name)
  {

    Bean bean = _namedComponentMap.get(name);
    if (bean != null)
      return getInstance(bean);
    else if (_parent != null)
      return _parent.getObjectByName(name);
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
    Bean<T> factory = createTransient(type);

    // server/10gn
    return factory.create(new ConfigContext());
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> BeanInstance<T> createTransientInstance(Class<T> type)
  {
    Contextual<T> factory = createTransient(type);

    // server/10gn
    T value = factory.create(new ConfigContext());

    return new BeanInstance(factory, value);
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> T createTransientObjectNoInit(Class<T> type)
  {
    ComponentImpl comp = (ComponentImpl) createTransient(type);

    return (T) comp.createNoInit();
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> BeanInstance<T> createTransientInstanceNoInit(Class<T> type)
  {
    ComponentImpl<T> bean = (ComponentImpl) createTransient(type);

    // server/10gn
    T value = (T) bean.createNoInit();

    return new BeanInstance<T>(bean, value);
  }

  /**
   * Returns a Bean for a class, but does not register the
   * component with webbeans.
   */
  public <T> Bean<T> createTransient(Class<T> type)
  {
    return createTransientImpl(type);
  }
  
  /**
   * Returns a Bean for a class, but does not register the
   * component with webbeans.
   */
  private <T> Bean<T> createTransientImpl(Class<T> type)
  {
    synchronized (_transientMap) {
      SoftReference<SimpleBean> compRef = _transientMap.get(type.getName());

      SimpleBean comp = compRef != null ? compRef.get() : null;

      if (comp == null) {
	if (type.isInterface())
	  throw new ConfigException(L.l("'{0}' cannot be an interface.  createTransient requires a concrete type.", type.getName()));
	else if (Modifier.isAbstract(type.getModifiers()))
	  throw new ConfigException(L.l("'{0}' cannot be an abstract.  createTransient requires a concrete type.", type.getName()));
	
	comp = new SimpleBean(this);
	comp.setTargetType(type);
	

	try {
	  Constructor nullCtor = type.getConstructor(new Class[0]);

	  if (nullCtor != null)
	    comp.setConstructor(nullCtor);
	} catch (NoSuchMethodException e) {
	  // if the type doesn't have a null-arg constructor
	}

	comp.init();

	_transientMap.put(type.getName(), new SoftReference<SimpleBean>(comp));

	// XXX: QA
	comp.bind();
      }

      return comp;
    }
  }

  public ELContext getELContext()
  {
    return new ConfigELContext();
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> T getObject(Class<T> type, Annotation ... ann)
  {
    Set<Bean<T>> beans = resolveByType(type, ann);

    if (beans == null || beans.size() == 0)
      return (T) createTransientObject(type);
    else
      return getInstanceByType(type, ann);
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> T getEnvironmentObject(Class<T> type, Annotation ... ann)
  {
    ComponentImpl comp = (ComponentImpl) createFactory(type, ann);

    Object value = comp.get();

    if (comp.isDestroyPresent())
      Environment.addClassLoaderListener(new ComponentClose(value, comp));
    
    return (T) value;
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> ComponentImpl<T> createFactory(Class<T> type, Annotation ... ann)
  {
    throw new UnsupportedOperationException();
    /*
    FactoryBinding binding = new FactoryBinding(type, ann);
    
    synchronized (_objectFactoryMap) {
      Bean<T> factory = _objectFactoryMap.get(binding);

      if (factory != null)
	return (ComponentImpl<T>) factory;

      if (ann == null)
	ann = NULL_ANN;
      
      factory = resolveByType(type, ann);

      if (factory != null) {
	_objectFactoryMap.put(binding, factory);

	return (ComponentImpl<T>) factory;
      }
      
      if (type.isInterface())
	throw new ConfigException(L.l("'{0}' cannot be an interface.  createTransient requires a concrete type.", type.getName()));
      else if (Modifier.isAbstract(type.getModifiers()))
	throw new ConfigException(L.l("'{0}' cannot be an abstract.  createTransient requires a concrete type.", type.getName()));
	
      ClassComponent comp = new ClassComponent(_wbWebBeans);
      comp.setInstanceClass(type);

      try {
	Constructor nullCtor = type.getConstructor(new Class[0]);

	if (nullCtor != null)
	  comp.setConstructor(nullCtor);
      } catch (NoSuchMethodException e) {
	// if the type doesn't have a null-arg constructor
      }

      comp.init();

      _objectFactoryMap.put(binding, comp);

      return comp;
    }
    */
  }

  //
  // javax.webbeans.Manager API
  //
  
  //
  // bean resolution and instantiation
  //

  /**
   * Adds a new bean definition to the manager
   */
  public Manager addBean(Bean<?> bean)
  {
    if (bean instanceof CauchoBean) {
      CauchoBean cauchoBean = (CauchoBean) bean;

      Iterator iter = cauchoBean.getGenericTypes().iterator();
      while (iter.hasNext()) {
	BaseType type = (BaseType) iter.next();
	
	addComponentByType(type, bean);
      }

      if (isStartupPresent(cauchoBean.getAnnotations())) {
	_pendingServiceList.add(cauchoBean);
      }

      if (isRegistrationMatch(cauchoBean))
	_pendingRegistrationList.add(cauchoBean);
    }
    else {
      for (Class type : bean.getTypes()) {
	addComponentByType(type, bean);
      }
    }

    if (bean instanceof AbstractBean) {
      AbstractBean abstractBean = (AbstractBean) bean;

      addComponentByType(abstractBean.getTargetType(), bean);
    }    

    if (bean.getName() != null)
      addComponentByName(bean.getName(), bean);

    registerJmx(bean);
    
    return this;
  }

  public void addBeanConfigFactory(BeanConfigFactory factory)
  {
    _customFactoryList.add(factory);
  }
  
  public Manager addConfigBean(CauchoBean configBean)
  {
    for (BeanConfigFactory factory : _customFactoryList) {
      if (factory.createBean(configBean))
	return this;
    }
    
    return addBean(configBean);
  }

  private void registerJmx(Bean bean)
  {
    int id = _beanId++;

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
  public Set<Bean<?>> resolveByName(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param bindings required @BindingType annotations
   */
  public <T> Set<Bean<T>> resolveByType(Class<T> type,
					Annotation... bindings)
  {
    Set<Bean<T>> set = resolve(type, bindings);

    if (set != null)
      return set;
    else
      return new HashSet<Bean<T>>();
  }

  /**
   * Returns the beans matching a generic type and annotation set
   *
   * @param type the bean's primary type
   * @param bindings required @BindingType annotations
   */
  public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> type,
					Annotation... bindings)
  {
    return resolveByType(type.getType(), bindings);
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param bindings required @BindingType annotations
   */
  public <T> Set<Bean<T>> resolveByType(Type type,
					Annotation... bindings)
  {
    Set set = resolve(type, bindings);

    if (set != null)
      return (Set<Bean<T>>) set;
    else
      return new HashSet<Bean<T>>();
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set resolve(Type type,
		     Annotation []bindings)
  {
    _wbWebBeans.init();

    if (bindings == null || bindings.length == 0) {
      if (Object.class.equals(type))
	return resolveAllBeans();
      
      bindings = CURRENT_ANN;
    }
    
    WebComponent component = getWebComponent(type);

    if (component != null) {
      Set beans = component.resolve(bindings);

      if (log.isLoggable(Level.FINER))
	log.finer(this + " bind(" + getSimpleName(type)
		  + "," + toList(bindings) + ") -> " + beans);

      if (beans != null && beans.size() > 0)
	return beans;
    }
    else if (New.class.equals(bindings[0].annotationType())) {
      // ioc/0721
      HashSet set = new HashSet();
      set.add(new NewBean(this, (Class) type));
      return set;
    }
    
    if (_parent != null) {
      return _parent.resolve(type, bindings);
    }
    
    for (Annotation ann : bindings) {
      if (! ann.annotationType().isAnnotationPresent(BindingType.class)) {
	throw new IllegalArgumentException(L.l("'{0}' is an invalid binding annotation because it does not have a @BindingType meta-annotation",
					       ann));
      }
    }

    return null;
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public Set<Bean> resolveAllByType(Class type)
  {
    _wbWebBeans.init();

    Annotation []bindings = new Annotation[0];
    
    WebComponent component = getWebComponent(type);

    if (component != null) {
      Set beans = component.resolve(bindings);

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

      for (WebComponent comp : _componentBaseTypeMap.values()) {
	Set set = comp.resolve(bindings);

	beans.addAll(set);
      }

      return beans;
    }
  }

  public WebComponent getWebComponent(Type type)
  {
    synchronized (_componentMap) {
      WebComponent comp = _componentMap.get(type);

      if (comp == null) {
	BaseType baseType = BaseType.create(type, null);

	comp = _componentBaseTypeMap.get(baseType);

	if (comp != null)
	  _componentMap.put(type, comp);
      }

      return comp;
    }
  }
  
  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  public <T> T getInstance(Bean<T> bean,
			   CreationalContext<T> createContext)
  {
    ConfigContext cxt = (ConfigContext) createContext;

    if (cxt != null) {
      T object = (T) cxt.get(bean);

      if (object != null)
	return object;
    }
    
    return getInstanceRec(bean, createContext, this);
  }
  
  private <T> T getInstanceRec(Bean<T> bean,
			       CreationalContext<T> createContext,
			       InjectManager topManager)
  {
    if (bean.getManager() != this) {
      if (getParent() == null) {
	throw new IllegalStateException(L.l("{0}: unknown bean {1} with owning manager {2}",
					    topManager,
					    bean,
					    bean.getManager()));
      }

      return getParent().getInstanceRec(bean, createContext, topManager);
    }
    else if (false && bean instanceof ComponentImpl)
      return (T) ((ComponentImpl) bean).get();
    else {
      Class scopeType = bean.getScopeType();

      if (Dependent.class.equals(scopeType)) {
	// server/4764
	return (T) bean.create(createContext);
      }

      if (scopeType == null) {
	throw new IllegalStateException("Unknown scope for " + bean);
      }
      
      Context context = getContext(scopeType);

      if (context == null)
	return null;

      if (createContext != null) {
      }
      else if ((bean instanceof ComponentImpl)
	       && (context instanceof ScopeContext)) {
	createContext = new ConfigContext((ComponentImpl) bean,
					  null,
					  (ScopeContext) context);
      }
      else {
	ConfigContext parent = ConfigContext.getCurrent();
	
	createContext = new ConfigContext(parent);
      }

      return (T) context.get(bean, createContext);
    }
  }

  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  public <T> T getInstance(Bean<T> bean)
  {
    return (T) getInstance(bean, new ConfigContext(ConfigContext.getCurrent()));
  }

  /**
   * Returns an instance of bean matching a given name
   *
   * @param name the name of the bean to match
   */
  public Object getInstanceByName(String name)
  {
    Bean bean = _namedComponentMap.get(name);

    if (bean != null)
      return getInstance(bean);
    else if (_parent != null)
      return _parent.getInstanceByName(name);
    else
      return null;
  }

  /**
   * Creates an instance for the given type.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param type the bean's class
   * @param bindings required @BindingType annotations
   */
  public <T> T getInstanceByType(Class<T> type,
				 Annotation... bindings)
  {
    Set<Bean<T>> set = resolveByType(type, bindings);

    if (set == null || set.size() == 0) {
      throw new UnsatisfiedDependencyException(L.l("'{0}' does not match any configured beans with binding {1}",
						   type.getName(),
						   toList(bindings)));
    }
    else if (set.size() == 1) {
      Iterator<Bean<T>> iter = set.iterator();

      if (iter.hasNext()) {
	Bean<T> bean = iter.next();
      
	return (T) getInstance(bean);
      }
    }
    else {
      throw new AmbiguousDependencyException(L.l("'{0}' matches too many configured beans {1}",
						   type.getName(),
						   set));
    }

    return null;
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
   * Creates an instance for the given type.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param type the bean's primary type
   * @param bindings required @BindingType annotations
   */
  public <T> T getInstanceByType(TypeLiteral<T> type,
				 Annotation... bindings)
  {
    return (T) getInstanceByType((Class) type.getType(), bindings);
  }

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public <T> T getInstanceToInject(InjectionPoint ij,
				   CreationalContext<?> cxt)
  {
    Bean bean = resolveByInjectionPoint(ij);

    if (bean instanceof SimpleBean) {
      SimpleBean simpleBean = (SimpleBean) bean;

      Object adapter = simpleBean.getScopeAdapter(cxt);

      if (adapter != null)
	return (T) adapter;
    }
	
    return (T) getInstance(bean, cxt);
  }

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public <T> T getInstanceToInject(InjectionPoint ij)
  {
    return (T) getInstanceToInject(ij, new ConfigContext());
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

    Set set = resolveByType(type, bindings);

    if (set == null || set.size() == 0) {
      throw new UnsatisfiedDependencyException(L.l("'{0}' does not match any configured beans with binding {1}",
						   type,
						   toList(bindings)));
    }
    else if (set.size() == 1) {
      Iterator iter = set.iterator();

      if (iter.hasNext()) {
	Bean bean = (Bean) iter.next();

	if (bean instanceof ComponentImpl)
 	  bean = ((ComponentImpl) bean).bindInjectionPoint(ij);

	return bean;
      }
    }
    else {
      throw new AmbiguousDependencyException(L.l("'{0}' matches too many configured beans {1}",
						 BaseType.create(type, null),
						 set));
    }

    return null;
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

  //
  // event management
  //

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
   * Registers an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public <T> void addObserver(Observer<T> observer,
			      Class<T> eventType,
			      Annotation... bindings)
  {
    checkActive();
    
    if (eventType.getTypeParameters() != null
	&& eventType.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's a parameterized type.",
					     eventType));
    }

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
   * Registers an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public <T> void addObserver(Observer<T> observer,
			      TypeLiteral<T> eventType,
			      Annotation... bindings)
  {
    addObserver(observer, (Class) eventType.getType(), bindings);
  }

  /**
   * Removes an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public <T> void removeObserver(Observer<T> observer,
				 Class<T> eventType,
				 Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public <T> void removeObserver(Observer<T> observer,
				 TypeLiteral<T> eventType,
				 Annotation... bindings)
  {
    removeObserver(observer, (Class) eventType.getType(), bindings);
  }

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
  public Manager addInterceptor(Interceptor interceptor)
  {
    _interceptorList.add(new InterceptorEntry(interceptor));

    return this;
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
  public List<Interceptor> resolveInterceptors(InterceptionType type,
					       Annotation... bindings)
  {
    if (bindings == null || bindings.length == 0)
      throw new IllegalArgumentException(L.l("resolveInterceptors requires at least one @InterceptorBindingType"));
    
    ArrayList<Interceptor> interceptorList = new ArrayList<Interceptor>();

    for (InterceptorEntry entry : _interceptorList) {
      Interceptor interceptor = entry.getInterceptor();
      
      if (interceptor.getMethod(type) == null)
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
  public Manager addDecorator(Decorator decorator)
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
  public List<Decorator> resolveDecorators(Set<Class<?>> types,
					   Annotation... bindings)
  {
    ArrayList<Decorator> decorators = new ArrayList<Decorator>();

    if (bindings == null || bindings.length == 0)
      bindings = CURRENT_ANN;

    for (DecoratorEntry entry : _decoratorList) {
      Decorator decorator = entry.getDecorator();

      if (isTypeContained(types, decorator.getDelegateType())
	  && entry.isMatch(bindings)) {
	decorators.add(decorator);
      }
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

      if (decorator.getDelegateType().isAssignableFrom(type)
	  && entry.isMatch(bindings)) {
	decorators.add(decorator);
      }
    }

    return decorators;
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
  // registration and startup
  //

  /**
   * Adds a listener for new beans matching an annotation
   */
  public void addRegistrationListener(BeanRegistrationListener listener)
  {
    _registrationListenerList.add(listener);
  }

  /**
   * Checks if the bean matches one if the registration listeners
   */
  public boolean isRegistrationMatch(CauchoBean bean)
  {
    if (_parent != null && _parent.isRegistrationMatch(bean))
      return true;

    for (BeanRegistrationListener listener : _registrationListenerList) {
      for (Annotation ann : bean.getAnnotations()) {
	if (listener.isMatch(ann))
	  return true;
      }
    }

    return false;
  }

  /**
   * Starts registrations
   */
  public void startRegistration(CauchoBean bean)
  {
    if (_parent != null)
      _parent.startRegistration(bean);

    for (BeanRegistrationListener listener : _registrationListenerList) {
      boolean isMatch = false;
      
      for (Annotation ann : bean.getAnnotations()) {
	if (listener.isMatch(ann)) {
	  isMatch = true;
	  break;
	}
      }

      if (isMatch)
	listener.start(this, bean);
    }
  }

  //
  // class loader updates
  //

  public void update()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<Path> pathList
	= new ArrayList<Path>(_pendingPathList);
      
      _pendingPathList.clear();

      for (Path path : pathList) {
	if (_webBeansMap.get(path) != null)
	  continue;
	
	if (path.canRead()) {
	  WbWebBeans webBeans = new WbWebBeans(this, path);
	  _webBeansMap.put(path, webBeans);
	  
	  path.setUserPath(path.getURL());
	    
	  new Config().configure(webBeans, path, SCHEMA);
	}
      }

      ArrayList<WebBeansRootContext> rootContextList
	= new ArrayList<WebBeansRootContext>(_pendingRootContextList);
      
      _pendingRootContextList.clear();

      for (WebBeansRootContext context : rootContextList) {
	Path root = context.getRoot();
      
	WbWebBeans webBeans = _webBeansMap.get(root);

	if (webBeans == null) {
	  webBeans = new WbWebBeans(this, root);
	  _webBeansMap.put(root, webBeans);

	  Path path = root.lookup("META-INF/beans.xml");
	  
	  if (path.canRead()) {
	    path.setUserPath(path.getURL());
	    
	    new Config().configure(webBeans, path, SCHEMA);
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
	    
	    webBeans.addScannedClass(cl);
	  } catch (ClassNotFoundException e) {
	    log.log(Level.FINER, e.toString(), e);
	  }
	}

	webBeans.update();

	webBeans.init();
      }
    
      _wbWebBeans.init();
    } catch (ConfigException e) {
      if (_configException == null)
	_configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Starts the bind phase
   */
  public void bind()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<ComponentImpl> bindList
	= new ArrayList<ComponentImpl>(_pendingBindList);
      
      _pendingBindList.clear();
      
      for (ComponentImpl comp : bindList) {
	if (_deploymentMap.get(comp.getDeploymentType()) != null)
	  comp.bind();
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
    update();
  }

  /**
   * Handles the case the environment config phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
    update();
    bind();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    update();
    bind();
    
    _wbWebBeans.init();

    fireEvent(this, new AnnotationLiteral<Initialized>() {});

    startServices();
  }

  /**
   * Initialize all the services
   */
  private void startServices()
  {
    ArrayList<CauchoBean> services;
    ArrayList<CauchoBean> registerServices;

    synchronized (_pendingServiceList) {
      services = new ArrayList<CauchoBean>(_pendingServiceList);
      _pendingServiceList.clear();
      
      registerServices = new ArrayList<CauchoBean>(_pendingRegistrationList);
      _pendingRegistrationList.clear();
    }

    for (CauchoBean bean : services) {
      registerBean(bean, bean.getAnnotations());
    }

    for (CauchoBean bean : registerServices) {
      startRegistration(bean);
    }
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

    _wbWebBeans = null;

    _webBeansMap = null;
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
    if (! root.lookup("META-INF/beans.xml").canRead())
      return false;

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

      if (className.startsWith("javax.inject"))
	return true;
      
      Class cl = Class.forName(className, false, _tempClassLoader);

      Annotation []annList = cl.getAnnotations();

      if (annList != null) {
	for (Annotation ann : annList) {
	  Class annType = ann.annotationType();
	  
	  if (annType.getName().startsWith("javax.inject"))
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
    return new SingletonHandle(Manager.class);
  }

  private void checkActive()
  {
    if (_webBeansMap == null)
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
