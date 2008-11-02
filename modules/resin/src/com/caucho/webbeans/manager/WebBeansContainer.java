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
import com.caucho.config.program.FieldComponentProgram;
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
import javax.webbeans.Observer;
import javax.webbeans.manager.Bean;
import javax.webbeans.manager.Context;
import javax.webbeans.manager.Decorator;
import javax.webbeans.manager.Interceptor;
import javax.webbeans.manager.InterceptionType;
import javax.webbeans.manager.Manager;

/**
 * The web beans container for a given environment.
 */
public class WebBeansContainer
  implements Manager, ScanListener, EnvironmentListener,
	     java.io.Serializable
{
  private static final L10N L = new L10N(WebBeansContainer.class);
  private static final Logger log
    = Logger.getLogger(WebBeansContainer.class.getName());

  private static final String SCHEMA = "com/caucho/webbeans/cfg/webbeans.rnc";

  private static final EnvironmentLocal<WebBeansContainer> _localContainer
    = new EnvironmentLocal<WebBeansContainer>();
  
  private static final Annotation []NULL_ANN = new Annotation[0];

  private static final Annotation []CURRENT_ANN
    = new Annotation[] { new AnnotationLiteral<Current>() {} };

  private WebBeansContainer _parent;
  
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

  private HashMap<FactoryBinding,Bean> _objectFactoryMap
    = new HashMap<FactoryBinding,Bean>();

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

    _deploymentMap.put(Standard.class, 0);
    _deploymentMap.put(Production.class, 1);

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

      if (webBeans == null) {
	webBeans = new WebBeansContainer(loader);
      
	_localContainer.set(webBeans, loader);
      }
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

    registerJmx(comp);
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

    addComponentByType(BaseType.create(type, null), comp);
  }
    
  private void addComponentByType(BaseType type, ComponentImpl comp)
  {
    if (type == null)
      return;
    
    if (log.isLoggable(Level.FINE))
      log.fine(comp.toDebugString() + " added to " + this);

    if (comp.isSingleton()) {
      _pendingSingletonList.add(comp);
    }

    WebComponent webComponent;

    synchronized (_componentMap) {
      webComponent = _componentBaseTypeMap.get(type);

      if (webComponent == null) {
	webComponent = new WebComponent(this, type);
      
	_componentBaseTypeMap.put(type, webComponent);
      }
    }

    webComponent.addComponent(comp);
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

  public void addSingleton(Object object)
  {
    addBean(new SingletonComponent(object));
  }

  public void addSingleton(Object object, String name)
  {
    addBean(new SingletonComponent(object, name));
  }

  public void addSingleton(Object object,
			   String name,
			   Class deploymentType)
  {
    addBean(new SingletonComponent(object, name, null, deploymentType));
  }

  public void addSingleton(Object object,
			   String name,
			   Class deploymentType,
			   Type ...api)
  {
    addBean(new SingletonComponent(object, name, api, deploymentType));
  }
  
  /**
   * Adds a singleton only to the name map
   * 
   * @param object the singleton value
   * @param name the singleton's name
   */
  public void addSingletonByName(Object object, String name)
  {
    addBean(new SingletonComponent(object, name, new Type[0]));
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
    if (field.isAnnotationPresent(New.class))
      throw new IllegalStateException(L.l("can't cope with new"));
    
    Annotation []bindings = getBindings(field.getAnnotations());
      
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
    
    if (! isOptional)
      throw injectError(field, L.l("Can't find a component for '{0}' because no beans match",
				   field.getType().getName()));
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
    Bean<T> factory = createTransient(type);

    return factory.create();
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
   * Returns a Bean for a class, but does not register the
   * component with webbeans.
   */
  public <T> Bean<T> createTransient(Class<T> type)
  {
    synchronized (_transientMap) {
      ClassComponent comp = _transientMap.get(type);

      if (comp == null) {
	if (type.isInterface())
	  throw new ConfigException(L.l("'{0}' cannot be an interface.  createTransient requires a concrete type.", type.getName()));
	else if (Modifier.isAbstract(type.getModifiers()))
	  throw new ConfigException(L.l("'{0}' cannot be an abstract.  createTransient requires a concrete type.", type.getName()));
	
	comp = new ClassComponent(this);
	comp.setTargetType(type);

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
	
	addComponentByType(type, (ComponentImpl) bean);
      }
    }
    else {
      for (Class type : bean.getTypes()) {
	addComponentByType(type, (ComponentImpl) bean);
      }
    }

    if (bean.getName() != null)
      addComponentByName(bean.getName(), (ComponentImpl) bean);

    registerJmx(bean);
    
    return this;
  }

  private void registerJmx(Bean bean)
  {
    int id = _beanId++;
    WebBeanAdmin admin = new WebBeanAdmin(bean, _beanId);

    admin.register();
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
    Set<Bean<T>> set = (Set<Bean<T>>) resolve(type.getType(), bindings);

    if (set != null)
      return set;
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
	log.finer(this + " bind(" + getSimpleName(type) + ") -> " + beans);

      if (beans != null)
	return beans;
    }
    
    if (_parent != null) {
      return _parent.resolve(type, bindings);
    }
    else {
      return null;
    }
  }

  int getDeploymentPriority(Class deploymentType)
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

  private WebComponent getWebComponent(Type type)
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
  public <T> T getInstance(Bean<T> bean)
  {
    return (T) ((ComponentImpl) bean).get();
  }

  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  public <T> T getInstance(Bean<T> bean, ConfigContext env)
  {
    return (T) ((ComponentImpl) bean).get(env);
  }

  /**
   * Returns an instance of bean matching a given name
   *
   * @param name the name of the bean to match
   */
  public Object getInstanceByName(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
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

    if (set != null) {
      Iterator<Bean<T>> iter = set.iterator();

      if (iter.hasNext()) {
	Bean<T> bean = iter.next();
      
	return (T) getInstance(bean);
      }
    }

    return null;
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
    _contextMap.put(context.getScopeType(), context);
  }

  /**
   * Returns the scope context for the given type
   */
  public Context getContext(Class<Annotation> scopeType)
  {
    return _contextMap.get(scopeType);
  }

  //
  // event management
  //

  /**
   * Fires an event
   *
   * @param event the event to fire
   * @param bindings the event bindings
   */
  public void fireEvent(Object event,
			Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // interceptor support
  //

  /**
   * Adds a new interceptor
   */
  public Manager addInterceptor(Interceptor interceptor)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // decorator
  //

  /**
   * Adds a new decorator
   */
  public Manager addDecorator(Decorator decorator)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
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
    _componentBaseTypeMap = null;
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
      
      return cl.isAnnotationPresent(DeploymentType.class);
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
    return new WebBeansHandle(Manager.class);
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
