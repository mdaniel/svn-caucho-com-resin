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

package com.caucho.config.cfg;

import com.caucho.config.*;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.*;
import com.caucho.config.types.*;
// import com.caucho.ejb.cfg.*;
import com.caucho.config.gen.*;
import com.caucho.util.*;

import java.beans.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.*;
import javax.ejb.*;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.spi.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class WbComponentConfig {
  private static final L10N L = new L10N(WbComponentConfig.class);

  private static final Object []NULL_ARGS = new Object[0];

  private InjectManager _beanManager;
  
  private Class _cl;

  private String _name;
  
  private ArrayList<Annotation> _bindingList
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _stereotypeList
    = new ArrayList<Annotation>();

  private Class _scope;

  private ArrayList<ConfigProgram> _newArgs;
  private ContainerProgram _init;

  protected Bean _comp;

  // XXX: temp for osgi
  private boolean _isService;

  public WbComponentConfig()
  {
    _beanManager = InjectManager.create();
  }

  public WbComponentConfig(InjectManager webbeans)
  {
    _beanManager = webbeans;
  }

  public InjectManager getBeanManager()
  {
    return _beanManager;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;

    _bindingList.add(Names.create(name));
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

  public AbstractBean getComponent()
  {
    return (AbstractBean) _comp;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(Annotation binding)
  {
    _bindingList.add(binding);
  }

  public ArrayList<Annotation> getBindingList()
  {
    return _bindingList;
  }

  public ArrayList<Annotation> getStereotypeList()
  {
    return _stereotypeList;
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
    if ("singleton".equals(scope))
      _scope = ApplicationScoped.class;
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
  public AbstractBean getComponentFactory()
  {
    return (AbstractBean) _comp;
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

    InjectManager beanManager = InjectManager.create();
    BeanFactory factory =  beanManager.createBeanFactory(_cl);

    if (_name != null) {
      factory.name(_name);

      // server/2n00
      if (! Map.class.isAssignableFrom(_cl))
	addOptionalStringProperty("name", _name);
    }

    /*
    if (getMBeanName() != null)
      comp.setMBeanName(getMBeanName());
    */

    for (Annotation binding : _bindingList) {
      factory.binding(binding);
    }

    for (Annotation stereotype : _stereotypeList) {
      factory.stereotype(stereotype);
    }

    if (_scope != null) {
      factory.scope(_scope);
      // comp.setScope(_beanManager.getScopeContext(_scope));
    }

    /*
    if (_isService) {
      comp.addAnnotation(new AnnotationLiteral<Service>() {});
    }
    */

    /*
    if (_newArgs != null)
      comp.setNewArgs(_newArgs);
    */

    /*
    if (_init != null)
      comp.setInit(_init);
    */

    _comp = factory.bean();

    introspectPostInit();

    // comp.init();

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
    /*
    if (_comp != null) {
      _beanManager.addBean(_comp);
    }
    */
  }

  public Object getObject()
  {
    if (_comp != null) {
      CreationalContext env = _beanManager.createCreationalContext();
      
      Object value = _beanManager.getReference(_comp, (Class) null, env);

      if (_init != null)
	_init.inject(value, null);
      
      return value;
    }
    else
      return null;
  }

  public Object createObjectNoInit()
  {
    if (_comp != null) {
      CreationalContext env = _beanManager.createCreationalContext();
      // XXX:
      return _beanManager.getReference(_comp, (Class) null, env);
      // return _comp.createNoInit();
    }
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl + "]";
  }
}
