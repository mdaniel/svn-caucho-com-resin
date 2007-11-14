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

package com.caucho.webbeans.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class WbComponent {
  private static final L10N L = new L10N(WbComponent.class);

  private static final Object []NULL_ARGS = new Object[0];

  protected WbWebBeans _webbeans;
  
  private Class _cl;
  private Constructor _ctor;
  private WbComponent []_ctorArgs;
  
  private WbComponentType _type;

  private Class _targetType;

  private boolean _isFromClass;

  private String _name;
  
  private boolean _hasBinding;
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Annotation _scopeAnn;
  private ScopeContext _scopeContext;

  private ArrayList<BuilderProgram> _injectProgram
    = new ArrayList<BuilderProgram>();
  
  private InitProgram _init;
  private Object _scopeAdapter;

  public WbComponent()
  {
    _webbeans = WebBeans.getLocal().getWbWebBeans();

    _webbeans.addWbComponent(this);
  }

  public WbComponent(WbWebBeans webbeans)
  {
    _webbeans = webbeans;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;

    WbBinding binding = new WbBinding();
    binding.setClass(Named.class);
    binding.addValue("value", name);

    _bindingList.add(binding);
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the component type.
   */
  public void setType(Class type)
  {
    if (! type.isAnnotationPresent(ComponentType.class))
      throw new ConfigException(L.l("'{0}' is an invalid component annotation.  Component types must be annotated by @ComponentType.",
				    type.getName()));
    
    _type = _webbeans.createComponentType(type);
  }

  /**
   * Gets the component type.
   */
  public WbComponentType getType()
  {
    return _type;
  }

  /**
   * Sets the component type.
   */
  public void setComponentType(WbComponentType type)
  {
    if (type == null)
      throw new NullPointerException();
    
    _type = type;
  }

  /**
   * Sets the component implementation class.
   */
  public void setClass(Class cl)
  {
    _cl = cl;
    _targetType = cl;

    if (_name == null) {
      Named named = (Named) cl.getAnnotation(Named.class);

      if (named != null)
	_name = named.value();

      if (_name == null || "".equals(_name)) {
	String className = cl.getName();
	int p = className.lastIndexOf('.');
      
	char ch = Character.toLowerCase(className.charAt(p + 1));
      
	_name = ch + className.substring(p + 2);
      }
    }
  }

  public Class getInstanceClass()
  {
    return _cl;
  }
  
  public String getClassName()
  {
    if (_targetType != null)
      return _targetType.getName();
    else
      return null;
  }
  
  public void setTargetType(Class type)
  {
    _targetType = type;
  }
  
  public Class getTargetType()
  {
    return _targetType;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(WbBinding binding)
  {
    _hasBinding = true;
    _bindingList.add(binding);
  }
  
  public ArrayList<WbBinding> getBindingList()
  {
    return _bindingList;
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
  }

  /**
   * Sets the scope annotation.
   */
  public void setScopeAnnotation(Annotation scopeAnn)
  {
    if (scopeAnn != null
	&& ! scopeAnn.annotationType().equals(Dependent.class)) {
      _scopeAnn = scopeAnn;

      _scopeContext = WebBeans.getLocal().getScopeContext(scopeAnn);
    }
    else {
      _scopeAnn = null;
      _scopeContext = null;
    }
  }

  /**
   * Gets the scope annotation.
   */
  public Annotation getScopeAnnotation()
  {
    return _scopeAnn;
  }

  /**
   * Sets the init program.
   */
  public void setInit(InitProgram init)
  {
    _init = init;
  }

  /**
   * True if the component was defined by class introspection.
   */
  public void setFromClass(boolean isFromClass)
  {
    _isFromClass = isFromClass;
  }

  /**
   * True if the component was defined by class introspection.
   */
  public boolean isFromClass()
  {
    return _isFromClass;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (_type == null)
      _type = _webbeans.createComponentType(Component.class);
				  
    if (_scopeAnn == null) {
      for (Annotation ann : _cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  setScopeAnnotation(ann);
	}
      }
    }

    introspectProduces();
    introspectConstructor();

    if (! _hasBinding)
      introspectBindings();
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect()
  {
    for (Annotation ann : _cl.getDeclaredAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	if (_type != null)
	  throw new ConfigException(L.l("{0}: component type annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ComponentType.",
					_cl.getName(),
					_type.getType().getName(),
					ann.annotationType().getName()));
	
	_type = _webbeans.createComponentType(ann.annotationType());
      }
    }

    if (_type == null) {
      throw new ConfigException(L.l("component '{0}' does not have a ComponentType",
				    _cl.getName()));
    }
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
    Class []param = _ctor.getParameterTypes();
    Annotation [][]paramAnn = _ctor.getParameterAnnotations();

    _ctorArgs = new WbComponent[param.length];

    for (int i = 0; i < param.length; i++) {
      _ctorArgs[i] = _webbeans.bindParameter(param[i], paramAnn[i]);
    }

    _injectProgram = InjectIntrospector.introspectNoInit(_cl);
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

      WbProducesComponent comp
	= new WbProducesComponent(_webbeans, this, method);

      comp.init();

      _webbeans.addWbComponent(comp);
    }
  }

  /**
   * Introspects the constructor
   */
  private void introspectConstructor()
  {
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
	else {
	  second = ctor;
	}
      }

      _ctor = best;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
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

  public boolean isMatch(ArrayList<Annotation> bindList)
  {
    for (int i = 0; i < bindList.size(); i++) {
      if (! isMatch(bindList.get(i)))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatch(Annotation bindAnn)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (_bindingList.get(i).isMatch(bindAnn))
	return true;
    }
    
    return false;
  }

  public Object getByName()
  {
    if (_scopeContext != null) {
      Object value = _scopeContext.get(_name);

      if (value != null)
	return value;
      else
	return get();
    }
    else
      throw new IllegalStateException();
  }

  public Object getInject()
  {
    DependentScope scope = DependentScope.getCurrent();

    if (scope == null
	|| _scopeContext == null
	|| scope.canInject(_scopeContext)) {
      return get();
    }

    if (_scopeAdapter == null)
      _scopeAdapter = ScopeAdapter.create(_cl).wrap(this);
    
    return _scopeAdapter;
  }

  public Object get()
  {
    DependentScope scope = DependentScope.getCurrent();

    if (_scopeContext != null) {
      Object value = _scopeContext.get(_name);

      if (value != null) {
	return value;
      }
    }
    else {
      if (scope != null) {
	Object value = scope.get(_name);

	if (value != null)
	  return value;
      }
    }
    
    DependentScope self = null;

    if (scope == null)
      self = DependentScope.begin(_scopeContext);

    try {
      if (_scopeContext != null) {
	return createScoped(_name, _scopeContext);
      }
      else if (_name == null)
	return create();
      else if (scope != null)
	return createScoped(_name, scope);
      else
	return create();
    } finally {
      if (self != null)
	DependentScope.end();
    }
  }

  public Object create()
  {
    try {
      Object value = createNew();

      if (_init != null)
	_init.configure(value);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Object createScoped(String name, ScopeContext scope)
  {
    try {
      Object value = createNew();

      scope.set(name, value);

      if (_injectProgram != null) {
	for (int i = 0; i < _injectProgram.size(); i++) {
	  BuilderProgram program = _injectProgram.get(i);

	  program.configure(value);
	}
      }

      if (_init != null)
	_init.configure(value);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Object createNew()
  {
    try {
      Object []args;
      if (_ctorArgs.length > 0) {
	args = new Object[_ctorArgs.length];

	for (int i = 0; i < args.length; i++)
	  args[i] = _ctorArgs[i].create();
      }
      else
	args = NULL_ARGS;
      
      return _ctor.newInstance(args);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void createProgram(ArrayList<BuilderProgram> initList,
			    AccessibleObject field,
			    String name,
			    AccessibleInject inject)
    throws ConfigException
  {
    BuilderProgram program = new ComponentProgram(this, inject);

    initList.add(program);
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof WbComponent))
      return false;

    WbComponent comp = (WbComponent) obj;

    if (! _targetType.equals(comp._targetType)) {
      return false;
    }

    int size = _bindingList.size();

    if (size != comp._bindingList.size()) {
      return false;
    }

    for (int i = size - 1; i >= 0; i--) {
      if (! comp._bindingList.contains(_bindingList.get(i))) {
	return false;
      }
    }

    return true;
  }

  public String toString()
  {
    if (_name != null) {
      return (getClass().getSimpleName() + "[" + _name
	      + ", " + _cl.getSimpleName()
	      + ", @" + _type.getType().getSimpleName() + "]");
    }
    else {
      return (getClass().getSimpleName() + "["
	      + _cl.getSimpleName()
	      + ", @" + _type.getType().getSimpleName() + "]");
    }
  }
}
