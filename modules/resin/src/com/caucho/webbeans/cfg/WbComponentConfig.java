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

package com.caucho.webbeans.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.*;
import com.caucho.config.types.*;
import com.caucho.ejb3.gen.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.beans.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class WbComponentConfig {
  private static final L10N L = new L10N(WbComponentConfig.class);

  private static final Object []NULL_ARGS = new Object[0];

  private WbWebBeans _webbeans;
  
  private Class _cl;

  private WbComponentType _type;

  private String _name;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Class _scope;
  
  private ContainerProgram _init;

  protected ComponentImpl _comp;

  public WbComponentConfig()
  {
    _webbeans = WebBeansContainer.create().getWbWebBeans();
  }

  public WbComponentConfig(WbWebBeans webbeans)
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

    if (_name == null)
      _name = Introspector.decapitalize(cl.getSimpleName());
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(WbBinding binding)
  {
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
    if ("singleton".equals(scope))
      _scope = Singleton.class;
    else if ("dependent".equals(scope))
      _scope = Dependent.class;
    else if ("request".equals(scope))
      _scope = RequestScoped.class;
    else if ("session".equals(scope))
      _scope = SessionScoped.class;
    else if ("application".equals(scope))
      _scope = ApplicationScoped.class;
    else if ("conversation".equals(scope))
      _scope = ConversationScoped.class;
    else {
      Class cl = null;
      
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	cl = Class.forName(scope, false, loader);
      } catch (ClassNotFoundException e) {
      }

      if (cl == null)
	throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

      if (! Annotation.class.isAssignableFrom(cl))
	throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

      if (! cl.isAnnotationPresent(ScopeType.class))
	throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

      _scope = cl;
    }
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    if (_init != null)
      _init.addProgram(init);
    else
      _init = init;
  }

  /**
   * Adds an init property
   */
  public void addStringProperty(String name, String value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(new PropertyStringProgram(name, value));
  }

  /**
   * Adds an init property
   */
  public void addOptionalStringProperty(String name, String value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(0, new PropertyStringProgram(name, value, true));
  }

  /**
   * Returns the configured component factory.
   */
  public ComponentImpl getComponentFactory()
  {
    return _comp;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (_cl == null)
      throw new ConfigException(L.l("<component> requires a class attribute"));

    introspect();
    
    ClassComponent comp;

    if (Singleton.class.equals(_scope))
      comp = new SingletonClassComponent(_webbeans);
    else
      comp = new ClassComponent(_webbeans);

    comp.setInstanceClass(_cl);
    comp.setTargetType(_cl);

    if (_name != null) {
      comp.setName(_name);

      addOptionalStringProperty("name", _name);
    }

    comp.setBindingList(_bindingList);

    if (_type != null)
      comp.setType(_type);
    else
      comp.setType(_webbeans.createComponentType(Component.class));

    if (_scope != null)
      comp.setScope(_webbeans.getScopeContext(_scope));

    if (_init != null)
      comp.setInit(_init);

    PojoBean bean = new PojoBean(_cl);

    Class instanceClass = bean.generateClass();

    comp.setInstanceClass(instanceClass);

    comp.init();

    _comp = comp;

    deploy();
  }

  protected void deploy()
  {
    _webbeans.addWbComponent(_comp);
  }

  public Object getObject()
  {
    return _comp.get();
  }

  private void introspect()
  {
    if (_scope == null) {
      for (Annotation ann : _cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  if (_scope != null) {
	    throw new ConfigException(L.l("{0}: multiple scope annotations are forbidden ({1} and {2}).",
					  _cl.getName(),
					  _scope.getSimpleName(),
					  ann.annotationType().getSimpleName()));
	  }
	  
	  _scope = ann.annotationType();
	}
      }
    }
  }
}
