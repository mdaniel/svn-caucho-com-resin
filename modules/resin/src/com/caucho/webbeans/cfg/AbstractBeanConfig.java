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
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.naming.*;
import javax.webbeans.*;

/**
 * Convenience classes for the bean config
 */
abstract public class AbstractBeanConfig {
  private static final L10N L = new L10N(AbstractBeanConfig.class);
  
  private String _name;
  private String _jndiName;

  private Class _cl;

  private WbComponentType _type;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Class _scope;
  
  private InitProgram _init;

  protected AbstractBeanConfig()
  {
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
   * Returns the component's EL binding name.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Assigns the class
   */
  public void setClass(Class cl)
  {
    _cl = cl;
  }

  /**
   * Returns the instance class
   */
  public Class getInstanceClass()
  {
    return _cl;
  }

  /**
   * Sets the component type.
   */
  public void setComponentType(Class type)
  {
    if (! type.isAnnotationPresent(ComponentType.class))
      throw new ConfigException(L.l("'{0}' is an invalid component annotation.  Component types must be annotated by @ComponentType.",
				    type.getName()));

    WebBeansContainer webBeans = WebBeansContainer.create();
    
    _type = webBeans.createComponentType(type);
  }

  /**
   * Gets the component type.
   */
  public WbComponentType getComponentType()
  {
    return _type;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(WbBinding binding)
  {
    _bindingList.add(binding);
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
  public void setInit(InitProgram init)
  {
    _init = init;
  }

  /**
   * Sets the init program.
   */
  public InitProgram getInit()
  {
    return _init;
  }

  protected void register()
  {
    register(null, null);
  }

  protected void register(Object value)
  {
    register(value, value != null ? value.getClass() : null);
  }

  protected void register(Object value, Class api)
  {
    WebBeansContainer webBeans = WebBeansContainer.create();
    WbWebBeans wbWebBeans = webBeans.getWbWebBeans();
    
    ComponentImpl comp;

    if (value != null) {
      comp = new SingletonComponent(wbWebBeans, value);
    }
    else {
      ClassComponent classComp = new SingletonClassComponent(wbWebBeans);

      classComp.setInstanceClass(_cl);

      comp = classComp;
    }

    if (api != null)
      comp.setTargetType(api);

    if (_name != null)
      comp.setName(_name);

    comp.setBindingList(_bindingList);

    if (_type != null)
      comp.setType(_type);
    else
      comp.setType(wbWebBeans.createComponentType(ComponentType.class));

    if (_scope != null)
      comp.setScope(webBeans.getScopeContext(_scope));

    comp.init();

    webBeans.addComponent(comp);

    if (_jndiName != null) {
      try {
	Jndi.bindDeepShort(_jndiName, comp);
      } catch (NamingException e) {
	throw ConfigException.create(e);
      }
    }
  }
}
