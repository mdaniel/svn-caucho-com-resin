/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.URL;

import javax.webbeans.*;

/**
 * The web beans container for a given environment.
 */
public class WebBeansContainer
  implements ScanListener, EnvironmentListener, Container {
  private static final L10N L = new L10N(WebBeansContainer.class);
  private static final Logger log
    = Logger.getLogger(WebBeansContainer.class.getName());
  
  private static final String SCHEMA = "com/caucho/webbeans/cfg/webbeans.rnc";

  private static final EnvironmentLocal<WebBeansContainer> _localContainer
    = new EnvironmentLocal<WebBeansContainer>();

  private WebBeansContainer _parent;
  
  private EnvironmentClassLoader _classLoader;
  private ClassLoader _tempClassLoader;

  private WbWebBeans _wbWebBeans;
  
  private HashMap<Path,WbWebBeans> _webBeansMap
    = new HashMap<Path,WbWebBeans>();

  private HashMap<Class,WebComponent> _componentMap
    = new HashMap<Class,WebComponent>();

  private HashMap<String,ComponentImpl> _namedComponentMap
    = new HashMap<String,ComponentImpl>();

  private HashMap<Path,WebBeansRootContext> _rootContextMap
    = new HashMap<Path,WebBeansRootContext>();

  private ArrayList<WebBeansRootContext> _pendingRootContextList
    = new ArrayList<WebBeansRootContext>();

  private RuntimeException _configException;

  private WebBeansContainer(ClassLoader loader)
  {
    _classLoader = Environment.getEnvironmentClassLoader(loader);

    _parent = WebBeansContainer.getCurrent(_classLoader.getParent());

    _localContainer.set(this, _classLoader);

    _tempClassLoader = _classLoader.getNewTempClassLoader();
    
    _wbWebBeans = new WbWebBeans(this, null);

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

  private void init()
  {
    try {
      update();
    } catch (RuntimeException e) {
      _configException = e;
      
      throw _configException;
    } catch (Exception e) {
      _configException = new ConfigException(e);

      throw _configException;
    }

    try {
      
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
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

    if (name != null && comp.getScope() != null)
      _namedComponentMap.put(name, comp);
  }

  /**
   * Adds a component by the interface type
   *
   * @param type the interface type to expose the component
   * @param comp the component to register
   */
  public void addComponentByType(Class type, ComponentImpl comp)
  {
    if (type == null)
      return;
    
    if (log.isLoggable(Level.FINE)) {
      log.fine(comp.toDebugString() + " added to " + this);
    }

    addComponentRec(type, comp);
  }
    
  private void addComponentRec(Class type, ComponentImpl comp)
  {
    if (type == null || Object.class.equals(type))
      return;
    
    WebComponent webComponent = _componentMap.get(type);

    if (webComponent == null) {
      webComponent = new WebComponent(type);
      _componentMap.put(type, webComponent);
    }

    webComponent.addComponent(comp);

    addComponentRec(type.getSuperclass(), comp);

    for (Class subClass : type.getInterfaces()) {
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

  public ScopeContext getScopeContext(Class scope)
  {
    if (scope == null)
      throw new NullPointerException();
    else if (RequestScoped.class.equals(scope))
      return new RequestScope();
    else if (SessionScoped.class.equals(scope))
      return new SessionScope();
    else if (ApplicationScoped.class.equals(scope))
      return new ApplicationScope();
    else if (ConversationScoped.class.equals(scope))
      return new ConversationScope();
    else if (Singleton.class.equals(scope))
      return new SingletonScope();
    else if (Dependent.class.equals(scope))
      return null;
    else
      throw new IllegalArgumentException(L.l("'{0}' is an unknown scope.",
					     scope.getName()));
  }

  public void createProgram(ArrayList<Inject> injectList,
			    Field field)
    throws ConfigException
  {
    ComponentImpl component
      = bind(location(field), field.getType(), field.getAnnotations());

    if (component == null)
      throw injectError(field, L.l("Can't find a component for '{0}'",
				   field.getType().getName()));

    component.createProgram(injectList, field);
  }

  public void createProgram(ArrayList<Inject> injectList,
			    Method method)
    throws ConfigException
  {
    // XXX: lazy binding
    try {
      Class []paramTypes = method.getParameterTypes();
      Annotation[][]paramAnn = method.getParameterAnnotations();
      
      ComponentImpl []args = new ComponentImpl[paramTypes.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = bind(location(method), paramTypes[i], paramAnn[i]);
      }

      injectList.add(new InjectMethodProgram(method, args));
    } catch (Exception e) {
      String loc = location(method);

      if (e instanceof ConfigException)
	throw new ConfigException(loc + e.getMessage(), e);
      else
	throw new ConfigException(loc + e, e);
    }
  }

  /**
   * Returns the web beans component corresponding to a method
   * and a @Named value
   */
  public ComponentImpl bind(String location, Class type, String name)
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
  public ComponentImpl bind(String location, Class type)
  {
    ArrayList<Binding> bindingList = new ArrayList<Binding>();

    return bindByBindings(location, type, bindingList);
  }

  /**
   * Returns the web beans component corresponding to a method
   * parameter.
   */
  public ComponentImpl bind(String location,
			    Class type,
			    Annotation []paramAnn)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : paramAnn) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindingList.add(ann);
    }

    return bind(location, type, bindingList);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public ComponentImpl bind(String location,
			    Class type,
			    ArrayList<Annotation> bindingList)
  {
    _wbWebBeans.init();
    
    WebComponent component = _componentMap.get(type);

    if (component != null)
      return component.bind(location, bindingList);
    else if (_parent != null)
      return _parent.bind(location, type, bindingList);
    else
      return null;
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  public ComponentImpl bindByBindings(String location,
				      Class type,
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
  }
  
  public Context getContext(Class<Annotation> scopeType)
  {
    return null;
  }

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
      
      for (WebBeansRootContext context : rootContextList) {
	WbWebBeans webBeans = _webBeansMap.get(context.getRoot());
	
	webBeans.bind();
      }
      
      _wbWebBeans.bind();
    } catch (ConfigException e) {
      if (_configException == null)
	_configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
    throws Exception
  {
    update();
    
    _wbWebBeans.init();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
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
    return (method.getDeclaringClass().getName()
	    + "." + method.getName() + ": ");
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

  public String toString()
  {
    if (_classLoader.getId() != null)
      return "WebBeansContainer[" + _classLoader.getId() + "]";
    else
      return "WebBeansContainer[]";
  }
}
