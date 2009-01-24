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
import com.caucho.config.inject.SimpleBean;
import com.caucho.config.inject.SingletonClassComponent;
import com.caucho.config.j2ee.*;
import com.caucho.config.manager.InjectManager;
import com.caucho.config.program.*;
import com.caucho.config.types.*;
// import com.caucho.ejb.cfg.*;
import com.caucho.config.gen.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;

import java.beans.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.*;
import javax.ejb.*;
import javax.context.ApplicationScoped;
import javax.context.ConversationScoped;
import javax.context.Dependent;
import javax.context.RequestScoped;
import javax.context.SessionScoped;
import javax.context.ScopeType;
import javax.inject.AnnotationLiteral;
import javax.inject.DeploymentType;
import javax.inject.Initializer;

/**
 * Configuration for the xml web bean component.
 */
public class WbComponentConfig {
  private static final L10N L = new L10N(WbComponentConfig.class);

  private static final Object []NULL_ARGS = new Object[0];

  private InjectManager _webbeans;
  
  private Class _cl;

  private Class<? extends Annotation> _deploymentType;

  private String _name;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Class _scope;

  private ArrayList<ConfigProgram> _newArgs;
  private ContainerProgram _init;

  protected ComponentImpl _comp;

  // XXX: temp for osgi
  private boolean _isService;

  public WbComponentConfig()
  {
    _webbeans = InjectManager.create();
  }

  public WbComponentConfig(InjectManager webbeans)
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
   * Returns the mbean-name
   */
  public String getMBeanName()
  {
    return null;
  }

  /**
   * Sets the component type.
   */
  public void setDeploymentType(Class type)
  {
    if (! type.isAnnotationPresent(DeploymentType.class))
      throw new ConfigException(L.l("'{0}' is an invalid deployment-type annotation because it's missing a @DeploymentType annotation.",
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
   * Sets the component implementation class.
   */
  public void setClass(Class cl)
  {
    _cl = cl;

    if (_name == null)
      _name = Introspector.decapitalize(cl.getSimpleName());
  }

  public Class getClassType()
  {
    return _cl;
  }

  public ComponentImpl getComponent()
  {
    return _comp;
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

      setScopeType(cl);
    }
  }

  public void setScopeType(Class cl)
  {
    if (cl == null)
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

    if (! Annotation.class.isAssignableFrom(cl))
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

    if (! cl.isAnnotationPresent(ScopeType.class))
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @ScopeType annotation."));

    _scope = cl;
  }

  /**
   * Sets any new values
   */
  public void addParam(ConfigProgram param)
  {
    if (_newArgs == null)
      _newArgs = new ArrayList<ConfigProgram>();

    _newArgs.add(param);
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(init);
  }

  public void addInitProgram(ConfigProgram program)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(program);
  }

  public ContainerProgram getInit()
  {
    return _init;
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
  public void addProperty(String name, Object value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(new PropertyValueProgram(name, value));
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

  // XXX: temp for OSGI
  public boolean isService()
  {
    return _isService;
  }

  public void setService(boolean isService)
  {
    _isService = isService;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (_cl == null)
      throw new ConfigException(L.l("<{0}> requires a class attribute",
				    getTagName()));

    /* XXX:
    if (_cl.isAnnotationPresent(Stateless.class)) {
      StatelessBeanConfig cfg = new StatelessBeanConfig(this);
      cfg.init();
      return;
    }
    else if (_cl.isAnnotationPresent(Stateful.class)) {
      StatefulBeanConfig cfg = new StatefulBeanConfig(this);
      cfg.init();
      return;
    }
    */

    introspect();
    
    SimpleBean comp;

    if (Singleton.class.equals(_scope))
      comp = new SingletonClassComponent(InjectManager.create());
    else
      comp = new SimpleBean(InjectManager.create());

    comp.setTargetType(_cl);

    if (_name != null) {
      comp.setName(_name);

      // server/2n00
      if (! Map.class.isAssignableFrom(_cl))
	addOptionalStringProperty("name", _name);
    }

    if (getMBeanName() != null)
      comp.setMBeanName(getMBeanName());

    for (WbBinding binding : _bindingList) {
      if (binding.getAnnotation() != null)
	comp.addBinding(binding.getAnnotation());
    }

    if (_deploymentType != null)
      comp.setDeploymentType(_deploymentType);

    if (_scope != null) {
      comp.setScopeType(_scope);
      comp.setScope(_webbeans.getScopeContext(_scope));
    }

    if (_isService) {
      comp.addAnnotation(new AnnotationLiteral<Service>() {});
    }

    if (_newArgs != null)
      comp.setNewArgs(_newArgs);

    if (_init != null)
      comp.setInit(_init);

    _comp = comp;

    introspectPostInit();

    comp.init();

    deploy();
  }

  /**
   * Returns the XML tag name for debugging.
   */
  protected String getTagName()
  {
    return "component";
  }

  /**
   * Introspection after the init has been set and before the @PostConstruct
   * for additional interception
   */
  protected void introspectPostInit()
  {
  }

  protected void deploy()
  {
    if (_comp != null) {
      _webbeans.addBean(_comp);
    }
  }

  public Object getObject()
  {
    if (_comp != null)
      return _comp.get();
    else
      return null;
  }

  public Object createObjectNoInit()
  {
    if (_comp != null)
      return _comp.createNoInit();
    else
      return null;
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
