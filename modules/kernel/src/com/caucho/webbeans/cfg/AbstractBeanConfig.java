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
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SimpleBean;
import com.caucho.config.inject.SingletonBean;
import com.caucho.config.inject.SingletonClassComponent;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.context.ScopeType;
import javax.context.ApplicationScoped;
import javax.context.ConversationScoped;
import javax.context.Dependent;
import javax.context.RequestScoped;
import javax.context.SessionScoped;
import javax.inject.DeploymentType;
import javax.naming.*;

/**
 * Convenience classes for the bean config
 */
abstract public class AbstractBeanConfig {
  private static final L10N L = new L10N(AbstractBeanConfig.class);

  private String _filename;
  private int _line;
  
  private String _name;
  private String _jndiName;

  private Class _cl;

  private Class<? extends Annotation> _deploymentType;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Class _scope;
  
  private ContainerProgram _init;

  protected AbstractBeanConfig()
  {
  }

  /**
   * Sets the configuration location
   */
  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLine()
  {
    return _line;
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
    if (! type.isAnnotationPresent(DeploymentType.class))
      throw new ConfigException(L.l("'{0}' is an invalid component annotation because deployment types must be annotated by @DeploymentType.",
				    type.getName()));

    _deploymentType = type;
  }

  /**
   * Gets the component type.
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    return _deploymentType;
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
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Sets the init program.
   */
  public ContainerProgram getInit()
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
    InjectManager webBeans = InjectManager.create();
    WbWebBeans wbWebBeans = webBeans.getWbWebBeans();
    
    ComponentImpl comp;

    if (value != null) {
      comp = new SingletonBean(webBeans, value);
    }
    else {
      SimpleBean classComp = new SingletonClassComponent(webBeans);

      classComp.setTargetType(_cl);

      comp = classComp;
    }

    if (api != null)
      comp.setTargetType(api);

    if (_name != null)
      comp.setName(_name);

    for (WbBinding binding : _bindingList)
      comp.addBinding(binding.getAnnotation());

    if (_deploymentType != null)
      comp.setDeploymentType(_deploymentType);

    if (_scope != null)
      comp.setScopeType(_scope);

    comp.init();

    webBeans.addBean(comp);

    if (_jndiName != null) {
      try {
	Jndi.bindDeepShort(_jndiName, comp);
      } catch (NamingException e) {
	throw ConfigException.create(e);
      }
    }
  }
}
