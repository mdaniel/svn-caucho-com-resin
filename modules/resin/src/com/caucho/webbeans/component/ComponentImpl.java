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

package com.caucho.webbeans.component;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class ComponentImpl implements ComponentFactory {
  private static final L10N L = new L10N(ComponentImpl.class);

  private static final Object []NULL_ARGS = new Object[0];
  private static final Inject []NULL_INJECT = new Inject[0];

  protected WbWebBeans _webbeans;
  
  private WbComponentType _type;

  private Class _targetType;

  private boolean _isFromClass;

  private String _name;
  
  private boolean _hasBinding;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private ScopeContext _scope;

  protected Inject []_injectProgram = NULL_INJECT;
  
  private InitProgram _init;
  private Object _scopeAdapter;

  public ComponentImpl(WbWebBeans webbeans)
  {
    _webbeans = webbeans;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
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
  public void setType(WbComponentType type)
  {
    if (type == null)
      throw new NullPointerException();
    
    _type = type;
  }
  
  public void setTargetType(Class type)
  {
    _targetType = type;
  }
  
  public Class getTargetType()
  {
    return _targetType;
  }

  public String getClassName()
  {
    return _targetType.getName();
  }

  /**
   * Adds a component binding.
   */
  public void setBindingList(ArrayList<WbBinding> bindingList)
  {
    _bindingList = bindingList;
  }
  
  public ArrayList<WbBinding> getBindingList()
  {
    return _bindingList;
  }

  /**
   * Sets the scope annotation.
   */
  public void setScope(ScopeContext scope)
  {
    _scope = scope;
  }

  /**
   * Gets the scope annotation.
   */
  public ScopeContext getScope()
  {
    return _scope;
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
  public void init()
  {
    // _webbeans.addWbComponent(this);
    
    if (_type == null)
      _type = _webbeans.createComponentType(Component.class);

    /*
    if (_name == null) {
      Named named = (Named) _cl.getAnnotation(Named.class);

      if (named != null)
	_name = named.value();

      if (_name == null || "".equals(_name)) {
	String className = _targetType.getName();
	int p = className.lastIndexOf('.');
      
	char ch = Character.toLowerCase(className.charAt(p + 1));
      
	_name = ch + className.substring(p + 2);
      }
    }
    */
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
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

  public boolean isMatchByBinding(ArrayList<Binding> bindList)
  {
    for (int i = 0; i < bindList.size(); i++) {
      if (! isMatchByBinding(bindList.get(i)))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatchByBinding(Binding binding)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (_bindingList.get(i).isMatch(binding))
	return true;
    }
    
    return false;
  }

  public Object getByName()
  {
    if (_scope != null) {
      Object value = _scope.get(this);

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
	|| _scope == null
	|| scope.canInject(_scope)) {
      return get();
    }

    if (_scopeAdapter == null)
      _scopeAdapter = ScopeAdapter.create(getTargetType()).wrap(this);
    
    return _scopeAdapter;
  }

  /**
   * Returns the component object, creating if necessary
   */
  public Object get()
  {
    if (_scope != null) {
      Object value = _scope.get(this);

      if (value != null) {
	return value;
      }
    }
    
    return create();
  }

  public Object get(DependentScope scope)
  {
    if (_scope != null) {
      Object value = _scope.get(this);

      if (value != null) {
	return value;
      }
    }
    else {
      Object value = scope.get(this);

      if (value != null)
	return value;
    }

    return create(scope);
  }

  /**
   * Creates a new instance of the component.
   */
  public Object create()
  {
    try {
      Object value = createNew();
      
      DependentScope scope = new DependentScope(this, value, _scope);
      scope.put(this, value);

      init(value, scope);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new instance of the component.
   */
  public Object create(DependentScope scope)
  {
    try {
      Object value = createNew();

      if (_scope != null) {
	_scope.put(this, value);
	scope = new DependentScope(this, value, _scope);
      }
      else
	scope.put(this, value);

      init(value, scope);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Destroys an instance of the component
   */
  public void destroy(Object obj)
  {
  }

  protected Object createNew()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Initialize the created value
   */
  protected Object init(Object value, DependentScope scope)
  {
    for (Inject inject : _injectProgram) {
      inject.inject(value, scope);
    }

    if (_init != null)
      _init.configure(value);

    return value;
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
  }

  public void createProgram(ArrayList<Inject> initList, Field field)
    throws ConfigException
  {
    initList.add(new ComponentInject(this, field));
  }

  public String getScopeId()
  {
    return _name;
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof ComponentImpl))
      return false;

    ComponentImpl comp = (ComponentImpl) obj;

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

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(_targetType != null ? _targetType.getSimpleName() : "null");
    sb.append("[");

    for (WbBinding binding : _bindingList) {
      sb.append(binding.toDebugString());
      sb.append(",");
    }

    if (_type != null) {
      sb.append("@");
      sb.append(_type.getType().getSimpleName());
    }
    else
      sb.append("@null");
    
    if (_scope != null) {
      sb.append(", @");
      sb.append(_scope.getClass().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Class targetType = getTargetType();
    sb.append(targetType != null ? targetType.getSimpleName() : "null");
    sb.append(", ");

    if (_type != null) {
      sb.append("@");
      sb.append(_type.getType().getSimpleName());
    }
    else
      sb.append("@null");
    
    if (_name != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(_name);
    }
    
    if (_scope != null) {
      sb.append(", @");
      sb.append(_scope.getClass().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }
}
