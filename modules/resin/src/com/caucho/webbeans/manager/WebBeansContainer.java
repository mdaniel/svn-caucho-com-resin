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

package com.caucho.webbeans.manager;

import com.caucho.config.program.MethodComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.event.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import javax.el.*;
import javax.webbeans.*;

/**
 * The web beans container for a given environment.
 */
public class WebBeansContainer
  implements ScanListener, EnvironmentListener, Container,
	     java.io.Serializable
{
  private static final L10N L = new L10N(WebBeansContainer.class);
  private static final Logger log
    = Logger.getLogger(WebBeansContainer.class.getName());

  private static final String SCHEMA = "com/caucho/webbeans/cfg/webbeans.rnc";

  private static final EnvironmentLocal<WebBeansContainer> _localContainer
    = new EnvironmentLocal<WebBeansContainer>();
  
  private static final Annotation []NULL_ANN = new Annotation[0];

  private WebBeansContainer _parent;
  
  private EnvironmentClassLoader _classLoader;
  private ClassLoader _tempClassLoader;

  private WbWebBeans _wbWebBeans;
  
  private HashMap<Path,WbWebBeans> _webBeansMap
    = new HashMap<Path,WbWebBeans>();

  private HashMap<Type,WebComponent> _componentMap
    = new HashMap<Type,WebComponent>();

  private HashMap<String,ComponentImpl> _namedComponentMap
    = new HashMap<String,ComponentImpl>();

  private HashMap<Path,WebBeansRootContext> _rootContextMap
    = new HashMap<Path,WebBeansRootContext>();

  private HashMap<Class,Context> _contextMap
    = new HashMap<Class,Context>();

  private HashMap<Class,ObserverMap> _observerMap
    = new HashMap<Class,ObserverMap>();

  private HashMap<Class,ArrayList<ObserverMap>> _observerListCache
    = new HashMap<Class,ArrayList<ObserverMap>>();

  private HashMap<Class,ClassComponent> _transientMap
    = new HashMap<Class,ClassComponent>();

  private HashMap<FactoryBinding,ComponentFactory> _objectFactoryMap
    = new HashMap<FactoryBinding,ComponentFactory>();

  private ArrayList<WebBeansRootContext> _pendingRootContextList
    = new ArrayList<WebBeansRootContext>();

  private ArrayList<ComponentImpl> _pendingBindList
    = new ArrayList<ComponentImpl>();

  private ArrayList<ComponentImpl> _pendingSingletonList
    = new ArrayList<ComponentImpl>();

  private HashMap<Class,InjectProgram> _injectMap
    = new HashMap<Class,InjectProgram>();

  private RuntimeException _configException;

  private WebBeansContainer(ClassLoader loader)
  {
    _classLoader = Environment.getEnvironmentClassLoader(loader);

    if (_classLoader != null) {
      _parent = WebBeansContainer.create(_classLoader.getParent());
    }
    
    _localContainer.set(this, _classLoader);

    if (_classLoader != null)
      _tempClassLoader = _classLoader.getNewTempClassLoader();
    else
      _tempClassLoader = new DynamicClassLoader(null);
    
    _wbWebBeans = new WbWebBeans(this, Vfs.lookup());

    _contextMap.put(RequestScoped.class, new RequestScope());
    _contextMap.put(SessionScoped.class, new SessionScope());
    _contextMap.put(ConversationScoped.class, new ConversationScope());
    _contextMap.put(ApplicationScoped.class, new ApplicationScope());
    _contextMap.put(Singleton.class, new SingletonScope());

    if (_classLoader != null)
      _classLoader.addScanListener(this);
    
    Environment.addEnvironmentListener(this, _classLoader);
  }

  /**
   * Returns the local container.
   */
  public static WebBeansContainer getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static WebBeansContainer getCurrent(ClassLoader loader)
  {
    synchronized (_localContainer) {
      return _localContainer.get(loader);
    }
  }

  /**
   * Returns the current active container.
   */
  public static WebBeansContainer create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current active container.
   */
  public static WebBeansContainer create(ClassLoader loader)
  {
    WebBeansContainer webBeans = null;

    synchronized (_localContainer) {
      webBeans = _localContainer.getLevel(loader);

      if (webBeans != null)
	return webBeans;
      
      webBeans = new WebBeansContainer(loader);
      
      _localContainer.set(webBeans);
    }

    return webBeans;
  }

  public WbWebBeans getWbWebBeans()
  {
    return _wbWebBeans;
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
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

  public void addComponent(ComponentImpl comp)
  {
    addComponentByType(comp.getTargetType(), comp);

    String name = comp.getName();

    /*
    if (name != null && comp.getScope() != null)
      _namedComponentMap.put(name, comp);
    */
    // ioc/0030
    if (name != null)
      _namedComponentMap.put(name, comp);

    _pendingBindList.add(comp);

    if (comp.isSingleton()) {
      _pendingSingletonList.add(comp);
    }
  }
  
  public void addComponentByName(String name, ComponentImpl comp)
  {
    _namedComponentMap.put(name, comp);
  }

  /**
   * Adds a component by the interface type
   *
   * @param type the interface type to expose the component
   * @param comp the component to register
   */
  public void addComponentByType(Type type, ComponentImpl comp)
  {
    if (type == null)
      return;
    
    if (log.isLoggable(Level.FINE))
      log.fine(comp.toDebugString() + " added to " + this);

    if (comp.isSingleton()) {
      _pendingSingletonList.add(comp);
    }      

    addComponentRec(type, comp);
  }

  public ArrayList<ComponentFactory> getBeansOfType(Type type)
  {
    ArrayList<ComponentFactory> beans = new ArrayList<ComponentFactory>();
    
    WebComponent webComponent = _componentMap.get(type);

    if (webComponent == null)
      return beans;

    beans.addAll(webComponent.getComponentList());

    return beans;
  }
    
  private void addComponentRec(Type type, ComponentImpl comp)
  {
    if (type == null || Object.class.equals(type))
      return;
    
    WebComponent webComponent = _componentMap.get(type);

    if (webComponent == null) {
      webComponent = new WebComponent(type);
      _componentMap.put(type, webComponent);
    }

    webComponent.addComponent(comp);

    Class cl;

    if (type instanceof Class)
      cl = (Class) type;
    else if (type instanceof ParameterizedType) {
      cl = (Class) ((ParameterizedType) type).getRawType();
      addComponentRec(cl, comp);
      return;
    }
    else {
      return;
    }

    addComponentRec(cl.getSuperclass(), comp);

    for (Class subClass : cl.getInterfaces()) {
      addComponentRec(subClass, comp);
    }
  }

  public void addSingleton(Object object)
  {
    SingletonComponent comp = new SingletonComponent(_wbWebBeans, object);

    comp.init();

    addComponent(comp);
  }

  public void addSingleton(Object object, String name)
  {
    SingletonComponent comp = new SingletonComponent(_wbWebBeans, object);

    comp.setName(name);

    WbBinding binding = new WbBinding();
    binding.setClass(Named.class);
    binding.addValue("value", name);

    ArrayList<WbBinding> bindingList = new ArrayList<WbBinding>();
    bindingList.add(binding);
    
    comp.setBindingList(bindingList);
    
    comp.init();

    addComponent(comp);
  }

  public void addSingleton(Object object,
			   String name,
			   Class componentType)
  {
    SingletonComponent comp = new SingletonComponent(_wbWebBeans, object);

    comp.setName(name);
    comp.setType(_wbWebBeans.createComponentType(componentType));

    WbBinding binding = new WbBinding();
    binding.setClass(Named.class);
    binding.addValue("value", name);

    ArrayList<WbBinding> bindingList = new ArrayList<WbBinding>();
    bindingList.add(binding);
    
    comp.setBindingList(bindingList);
    
    comp.init();

    addComponent(comp);
  }
  
  /**
   * Adds a singleton only to the name map
   * 
   * @param object the singleton value
   * @param name the singleton's name
   */
  public void addSingletonByName(Object object, String name)
  {
    SingletonComponent comp = new SingletonComponent(_wbWebBeans, object);

    comp.setName(name);
    comp.init();
    
    _namedComponentMap.put(name, comp);
  }


  public void addEnabledInterceptor(Class cl)
  {
    _wbWebBeans.addEnabledInterceptor(cl);
  }

  public ArrayList<Class> findInterceptors(ArrayList<Annotation> annList)
  {
    ArrayList<Class> list = new ArrayList<Class>();

    ArrayList<WbInterceptor> interceptors
      = _wbWebBeans.findInterceptors(annList);

    // root interceptors take priority
    if (interceptors != null) {
      addInterceptorClasses(list, interceptors);
      
      return list;
    }

    for (WbWebBeans wbWebBeans : _webBeansMap.values()) {
      interceptors = wbWebBeans.findInterceptors(annList);

      if (interceptors != null)
	addInterceptorClasses(list, interceptors);
    }

    return list;
  }

  private void addInterceptorClasses(ArrayList<Class> classes,
				     ArrayList<WbInterceptor> interceptors)
  {
    for (WbInterceptor interceptor : interceptors) {
      classes.add(interceptor.getInterceptorClass());
    }
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

    if (context != null)
      return (ScopeContext) context;
    else
      throw new IllegalArgumentException(L.l("'{0}' is an unknown scope.",
					     scope.getName()));
  }

  /**
   * Creates an injection program for the given field
   */
  public void createProgram(ArrayList<ConfigProgram> injectList,
			    Field field)
    throws ConfigException
  {
    ComponentImpl component;
      
    component = bind(location(field),
		     field.getGenericType(),
		     field.getAnnotations());

    if (component == null)
      throw injectError(field, L.l("Can't find a component for '{0}'",
				   field.getType().getName()));

    component.createProgram(injectList, field);
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
	args[i] = bind(location(method), paramTypes[i], paramAnn[i]);

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
   * Returns the web beans component corresponding to a method
   * and a @Named value
   */
  public ComponentImpl bind(String location, Type type, String name)
  {
    ArrayList<Binding> bindingList = new ArrayList<Binding>();

    Binding binding = new Binding(Named.class);
    binding.put("value", name);

    bindingList.add(binding);

    return bindByBindings(location, type, bindingList);
  }

  /**
   * Returns the web beans component corresponding to the return type.
   */
  public ComponentImpl bind(String location, Type type)
  {
    ArrayList<Binding> bindingList = new ArrayList<Binding>();

    return bindByBindings(location, type, bindingList);
  }

  /**
   * Returns the web beans component corresponding to a method
   * parameter.
   */
  public ComponentImpl bind(String location,
			    Type type,
			    Annotation []paramAnn)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    boolean isNew = false;
    for (Annotation ann : paramAnn) {
      if (ann instanceof New)
	isNew = true;
      else if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    if (isNew)
      return bindNew(location, (Class) type);
    else
      return bind(location, type, bindingList);
  }

  /**
   * Binds for the @New expression
   */
  private ComponentImpl bindNew(String location, Class type)
  {
    ComponentImpl component = bind(location, type, new Annotation[0]);

    if (component == null) {
      ClassComponent newComp = new ClassComponent(_wbWebBeans);
      newComp.setInstanceClass(type);
      newComp.setTargetType(type);
      newComp.init();

      addComponent(newComp);

      component = newComp;
    }

    return component;
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public ComponentImpl bind(String location,
			    Type type,
			    ArrayList<Annotation> bindingList)
  {
    _wbWebBeans.init();
    
    WebComponent component = _componentMap.get(type);

    if (component != null) {
      ComponentImpl comp = component.bind(location, bindingList);

      if (log.isLoggable(Level.FINER))
	log.finer(this + " bind(" + getSimpleName(type) + ") returns " + comp);
      
      return comp;
    }
    else if (_parent != null) {
      return _parent.bind(location, type, bindingList);
    }
    else {
      return null;
    }
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public ComponentImpl bindByBindings(String location,
				      Type type,
				      ArrayList<Binding> bindingList)
  {
    _wbWebBeans.init();
    
    WebComponent component = _componentMap.get(type);

    if (component != null)
      return component.bindByBindings(location, type, bindingList);
    else if (_parent != null)
      return _parent.bindByBindings(location, type, bindingList);
    else
      return null;
  }

  /**
   * Finds a component by its component name.
   */
  public ComponentImpl findByName(String name)
  {
    ComponentImpl comp = _namedComponentMap.get(name);
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
    ComponentImpl comp = _namedComponentMap.get(name);
    if (comp != null)
      return comp.get();
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
	program = InjectIntrospector.introspectProgram(cl);
	_injectMap.put(cl, program);
      }
    }

    program.configure(obj);
  }
  

  //
  // events
  //
  
  public void addObserver(ObserverImpl observer)
  {
    synchronized (_observerMap) {
      ObserverMap map = _observerMap.get(observer.getType());
      
      if (map == null) {
	map = new ObserverMap(observer.getType());
	_observerMap.put(observer.getType(), map);
      }

      map.addObserver(observer);
    }
  }

  //
  // javax.webbeans.Container
  //

  /**
   * Returns the component which matches the apiType and binding types
   */
  public <T> ComponentFactory<T> resolveByType(Class<T> apiType,
					       Annotation...bindingTypes)
  {
    return bind("", apiType, bindingTypes);
  }
  
  public void addContext(Class<Annotation> scopeType, Context context)
  {
    _contextMap.put(scopeType, context);
  }
  
  public Context getContext(Class<Annotation> scopeType)
  {
    return _contextMap.get(scopeType);
  }

  /**
   * Sends the specified event to any observer instances in the scope
   */
  public void raiseEvent(Object event, Annotation... bindings)
  {
    if (_parent != null)
      _parent.raiseEvent(event, bindings);

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
      observerList.get(i).raiseEvent(event, bindings);
    }
  }

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
    ComponentFactory<T> factory = createTransient(type);

    return factory.create();
  }

  /**
   * Returns a ComponentFactory for a class, but does not register the
   * component with webbeans.
   */
  public <T> ComponentFactory<T> createTransient(Class<T> type)
  {
    synchronized (_transientMap) {
      ClassComponent comp = _transientMap.get(type);

      if (comp == null) {
	if (type.isInterface())
	  throw new ConfigException(L.l("'{0}' cannot be an interface.  createTransient requires a concrete type.", type.getName()));
	else if (Modifier.isAbstract(type.getModifiers()))
	  throw new ConfigException(L.l("'{0}' cannot be an abstract.  createTransient requires a concrete type.", type.getName()));
	
	comp = new ClassComponent(_wbWebBeans);
	comp.setInstanceClass(type);

	try {
	  Constructor nullCtor = type.getConstructor(new Class[0]);

	  if (nullCtor != null)
	    comp.setConstructor(nullCtor);
	} catch (NoSuchMethodException e) {
	  // if the type doesn't have a null-arg constructor
	}

	comp.init();

	_transientMap.put(type, comp);

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
    return (T) createFactory(type, ann).get();
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> T getObject(Class<T> type, String name)
  {
    Annotation []ann = new Annotation[] { Names.create(name) };
    
    return (T) createFactory(type, ann).get();
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> T createFactory(Class<T> type, String name)
  {
    Annotation []ann = new Annotation[] { Names.create(name) };
    
    return (T) createFactory(type, ann);
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
  public ComponentFactory createFactory(Class type, Annotation ... ann)
  {
    FactoryBinding binding = new FactoryBinding(type, ann);
    
    synchronized (_objectFactoryMap) {
      ComponentFactory factory = _objectFactoryMap.get(binding);

      if (factory != null)
	return factory;

      if (ann == null)
	ann = NULL_ANN;
      
      factory = resolveByType(type, ann);

      if (factory != null) {
	_objectFactoryMap.put(binding, factory);

	return factory;
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

      ArrayList<WebBeansRootContext> rootContextList
	= new ArrayList<WebBeansRootContext>(_pendingRootContextList);
      
      _pendingRootContextList.clear();

      for (WebBeansRootContext context : rootContextList) {
	Path root = context.getRoot();
      
	WbWebBeans webBeans = _webBeansMap.get(root);

	if (webBeans == null) {
	  webBeans = new WbWebBeans(this, root);
	  _webBeansMap.put(root, webBeans);

	  Path path = root.lookup("META-INF/web-beans.xml");
	  
	  if (path.canRead()) {
	    path.setUserPath(path.getURL());
	    
	    new Config().configure(webBeans, path, SCHEMA);
	  }
	}

	for (String className : context.getClassNameList()) {
	  try {
	    Class cl = Class.forName(className, false, _classLoader);

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
	if (comp.getType().isEnabled())
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
  public void environmentBind(EnvironmentClassLoader loader)
  {
    update();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    update();
    bind();
    
    _wbWebBeans.init();

    startSingletons();
  }

  /**
   * Initialize all the singletons
   */
  private void startSingletons()
  {
    ArrayList<ComponentImpl> singletons;

    synchronized (_pendingSingletonList) {
      if (_pendingSingletonList.size() == 0)
	return;

      singletons = new ArrayList<ComponentImpl>();
      singletons.addAll(_pendingSingletonList);
      _pendingSingletonList.clear();
    }

    for (ComponentImpl singleton : singletons)
      singleton.get();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    _componentMap = null;
    _namedComponentMap = null;
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
   * Returns true if the root is a valid scannable root.
   */
  public boolean isRootScannable(Path root)
  {
    if (! root.lookup("META-INF/web-beans.xml").canRead())
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

  public boolean isScanMatch(CharBuffer annotationName)
  {
    try {
      String className = annotationName.toString();
      
      Class cl = Class.forName(className, false, _tempClassLoader);
      
      return cl.isAnnotationPresent(ComponentType.class);
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
    return new WebBeansHandle(Container.class);
  }

  public String toString()
  {
    if (_classLoader != null && _classLoader.getId() != null)
      return "WebBeansContainer[" + _classLoader.getId() + "]";
    else
      return "WebBeansContainer[]";
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
}
