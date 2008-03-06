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

package com.caucho.webbeans.component;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.ejb3.gen.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.event.*;
import com.caucho.webbeans.manager.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class ClassComponent extends ComponentImpl {
  private static final L10N L = new L10N(ClassComponent.class);

  private static final Object []NULL_ARGS = new Object[0];
  
  private Class _cl;
  private boolean _isBound;

  private Constructor _ctor;
  private ComponentImpl []_ctorArgs;

  private Object _scopeAdapter;

  private HashMap<Method,ArrayList<WbInterceptor>> _interceptorMap;
  private Class _proxyClass;

  private String _mbeanName;
  private Class _mbeanInterface;

  public ClassComponent(WbWebBeans webbeans)
  {
    super(webbeans);
  }

  public void setInstanceClass(Class cl)
  {
    _cl = cl;

    if (getTargetType() == null)
      setTargetType(cl);
  }

  public Class getInstanceClass()
  {
    return _cl;
  }

  public void setConstructor(Constructor ctor)
  {
    _ctor = ctor;
  }

  public void setMBeanName(String name)
  {
    _mbeanName = name;
  }

  public Class getMBeanInterface()
  {
    return _mbeanInterface;
  }

  public void init()
  {
    introspect();

    super.init();
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect()
  {
    Class cl = getInstanceClass();
    Class scopeClass = null;

    if (getType() == null) {
      for (Annotation ann : cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	  if (getType() != null)
	    throw new ConfigException(L.l("{0}: component type annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ComponentType.",
					  cl.getName(),
					  getType().getType().getName(),
					  ann.annotationType().getName()));
	
	  setType(_webbeans.createComponentType(ann.annotationType()));
	}
      }
    }

    if (getType() == null)
      setType(_webbeans.createComponentType(Component.class));

    if (getScope() == null) {
      for (Annotation ann : cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  if (scopeClass != null)
	    throw new ConfigException(L.l("{0}: @ScopeType annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ScopeType.",
					  cl.getName(),
					  scopeClass.getName(),
					  ann.annotationType().getName()));

	  scopeClass = ann.annotationType();
	  setScope(_webbeans.getScopeContext(scopeClass));
	}
      }
    }

    if (getName() == null) {
      String name = cl.getSimpleName();

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
	
      setName(name);
    }

    introspectProduces();
    introspectConstructor();

    if (getBindingList().size() == 0)
      introspectBindings();
    
    introspectMBean();
  }

  /**
   * Introspects the methods for any @Produces
   */
  private void introspectProduces()
  {
    if (_cl == null)
      return;
    
    for (Method method : _cl.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
	continue;

      if (! method.isAnnotationPresent(Produces.class))
	continue;

      if (method.isAnnotationPresent(In.class))
	throw error(method, L.l("@Produces method may not have an @In annotation."));

      ProducesComponent comp = new ProducesComponent(_webbeans, this, method);

      _webbeans.addWbComponent(comp);
      
      comp.init();
    }
  }

  /**
   * Introspects the constructor
   */
  protected void introspectConstructor()
  {
    if (_ctor != null)
      return;
    
    try {
      Constructor best = null;
      Constructor second = null;

      for (Constructor ctor : _cl.getDeclaredConstructors()) {
	if (best == null) {
	  best = ctor;
	}
	else if (hasBindingAnnotation(ctor)) {
	  if (best != null && hasBindingAnnotation(best))
	    throw new ConfigException(L.l("WebBean {0} has two constructors with binding annotations.",
					  ctor.getDeclaringClass().getName()));
	  best = ctor;
	  second = null;
	}
	else if (ctor.getParameterTypes().length == 0) {
	  best = ctor;
	}
	else if (best.getParameterTypes().length == 0) {
	}
	else {
	  second = ctor;
	}
      }

      if (second != null)
	throw new ConfigException(L.l("{0}: WebBean does not have a unique constructor.  One constructor must be marked with @In or have a binding annotation.",
				      _cl.getName()));

      if (best == null)
	throw new ConfigException(L.l("{0}: no constructor found",
				      _cl.getName()));

      _ctor = best;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Introspects for MBeans annotation
   */
  private void introspectMBean()
  {
    if (_cl == null)
      return;
    else if (_mbeanInterface != null)
      return;
    
    for (Class iface : _cl.getInterfaces()) {
      if (iface.getName().endsWith("MBean")
	  || iface.getName().endsWith("MXBean")) {
	_mbeanInterface = iface;
	return;
      }
    }
  }

  protected String getMBeanName()
  {
    if (_mbeanName != null)
      return _mbeanName;

    if (_mbeanInterface == null)
      return null;
    
    String typeName = _mbeanInterface.getSimpleName();

    if (typeName.endsWith("MXBean"))
      typeName = typeName.substring(0, typeName.length() - "MXBean".length());
    else if (typeName.endsWith("MBean"))
      typeName = typeName.substring(0, typeName.length() - "MBean".length());

    String name = getName();

    if (name == null)
      return "type=" + typeName;
    else if (name.equals(""))
      return "type=" + typeName + ",name=default";
    else
      return "type=" + typeName + ",name=" + name;
  }

  /**
   * Creates a new instance of the component.
   */
  @Override
  public Object get(ConfigContext env)
  {
    try {
      Object value;
      boolean isNew = false;

      if (env.canInject(_scope)) {
	if (_scope != null) {
	  value = _scope.get(this, false);
	  
	  if (value != null)
	    return value;
	}
	else {
	  value = env.get(this);
	  
	  if (value != null)
	    return value;
	}
      
	value = createNew(env);
	
	if (_scope != null) {
	  _scope.put(this, value);
	  env = new ConfigContext(this, value, _scope);
	}
	else
	  env.put(this, value);
	
	init(value, env);
      }
      else {
	if (env != null) {
	  value = env.get(this);
	  if (value != null)
	    return value;
	}

	value = _scopeAdapter;
	if (value == null) {
	  ScopeAdapter scopeAdapter = ScopeAdapter.create(getInstanceClass());
	  _scopeAdapter = scopeAdapter.wrap(this);
	  value = _scopeAdapter;
	}

	env.put(this, value);
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Object createNew(ConfigContext env)
  {
    try {
      if (! _isBound)
	bind();
      
      Object []args;
      if (_ctorArgs.length > 0) {
	args = new Object[_ctorArgs.length];

	for (int i = 0; i < args.length; i++)
	  args[i] = _ctorArgs[i].create();
      }
      else
	args = NULL_ARGS;
      
      Object value = _ctor.newInstance(args);

      if (isSingleton())
	SerializationAdapter.setHandle(value, getHandle());

      if (env != null)
	env.put(this, value);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
	return;
      _isBound = true;

      ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectInject(injectList, _cl);
      _injectProgram = new ConfigProgram[injectList.size()];
      injectList.toArray(_injectProgram);
      
      ArrayList<ConfigProgram> initList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectInit(initList, _cl);
      _initProgram = new ConfigProgram[initList.size()];
      initList.toArray(_initProgram);
      
      ArrayList<ConfigProgram> destroyList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectDestroy(destroyList, _cl);
      _destroyProgram = new ConfigProgram[destroyList.size()];
      destroyList.toArray(_destroyProgram);
      
      if (_ctor == null)
	introspectConstructor();

      if (_ctor != null) {
	String loc = _ctor.getDeclaringClass().getName() + "(): ";
	Type []param = _ctor.getGenericParameterTypes();
	Annotation [][]paramAnn = _ctor.getParameterAnnotations();

	_ctorArgs = new ComponentImpl[param.length];

	for (int i = 0; i < param.length; i++) {
	  _ctorArgs[i] = _webbeans.bindParameter(loc, param[i], paramAnn[i]);

	  if (_ctorArgs[i] == null)
	    throw new ConfigException(L.l("{0} does not have valid arguments",
					  _ctor));
	}
      }

      introspectObservers();

      /*
      introspectInterceptors();

      if (_interceptorMap != null) {
	_proxyClass = InterceptorGenerator.gen(getInstanceClass(),
					       _ctor, _interceptorMap);

	Constructor proxyCtor = _proxyClass.getConstructors()[0];

	_ctor = proxyCtor;
      }
      */

      PojoBean bean = new PojoBean(_cl);
      bean.setSingleton(isSingleton());
      bean.introspect();

      Class instanceClass = bean.generateClass();

      if (instanceClass == _cl && isSingleton())
	instanceClass = SerializationAdapter.gen(_cl);

      if (instanceClass != null && instanceClass != _cl) {
	try {
	  if (_ctor != null)
	    _ctor = instanceClass.getConstructor(_ctor.getParameterTypes());
	  
	  setInstanceClass(instanceClass);
	} catch (Exception e) {
	  throw ConfigException.create(e);
	}
      }
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
    ArrayList<WbBinding> bindings = new ArrayList<WbBinding>();
    
    for (Annotation ann : getInstanceClass().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindings.add(new WbBinding(ann));

      if (ann instanceof Named)
	setName(((Named) ann).value());
    }

    if (bindings.size() > 0)
      setBindingList(bindings);
  }

  /**
   * Introspects any observers.
   */
  protected void introspectObservers()
  {
    for (Method method : getInstanceClass().getDeclaredMethods()) {
      int param = findObserverAnnotation(method);

      if (param < 0)
	continue;

      if (method.isAnnotationPresent(In.class))
	throw error(method, "@Observer may not have an @In attribute");

      ArrayList<WbBinding> bindingList = new ArrayList<WbBinding>();
      
      Annotation [][]annList = method.getParameterAnnotations();
      if (annList != null && annList[param] != null) {
	for (Annotation ann : annList[param]) {
	  if (ann.annotationType().isAnnotationPresent(EventBindingType.class))
	    bindingList.add(new WbBinding(ann));
	}
      }

      ObserverImpl observer = new ObserverImpl(this, method, param);
      observer.setBindingList(bindingList);

      _webbeans.getContainer().addObserver(observer);
    }
  }

  /**
   * Introspects any intercepted methods
   */
  protected void introspectInterceptors()
  {
    for (Method method : getInstanceClass().getMethods()) {
      if (method.getDeclaringClass().equals(Object.class))
	continue;
      
      ArrayList<Annotation> interceptorTypes = findInterceptorTypes(method);

      if (interceptorTypes == null)
	continue;

      ArrayList<WbInterceptor> interceptors
	= _webbeans.findInterceptors(interceptorTypes);

      if (interceptors != null) {
	if (_interceptorMap == null)
	  _interceptorMap = new HashMap<Method,ArrayList<WbInterceptor>>();

	_interceptorMap.put(method, interceptors);
      }
    }
  }

  private ArrayList<Annotation> findInterceptorTypes(Method method)
  {
    ArrayList<Annotation> types = null;

    for (Annotation ann : method.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBindingType.class)) {
	if (types == null)
	  types = new ArrayList<Annotation>();

	types.add(ann);
      }
    }

    return types;
  }
  
  private boolean hasBindingAnnotation(Constructor ctor)
  {
    if (ctor.isAnnotationPresent(In.class))
      return true;

    Annotation [][]paramAnn = ctor.getParameterAnnotations();

    for (Annotation []annotations : paramAnn) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  return true;
      }
    }

    return false;
  }

  private int findObserverAnnotation(Method method)
  {
    Annotation [][]paramAnn = method.getParameterAnnotations();
    int observer = -1;

    for (int i = 0; i < paramAnn.length; i++) {
      for (Annotation ann : paramAnn[i]) {
	if (ann instanceof Observes) {
	  if (observer >= 0)
	    throw WebBeansContainer.error(method, L.l("Only one param may have an @Observer"));
	  
	  observer = i;
	}
      }
    }

    return observer;
  }
}
