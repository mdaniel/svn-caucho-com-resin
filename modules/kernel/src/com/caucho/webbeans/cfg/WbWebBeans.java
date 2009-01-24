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

import com.caucho.bytecode.*;
import com.caucho.config.*;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.SimpleBean;
import com.caucho.config.inject.SingletonClassComponent;
import com.caucho.config.inject.DecoratorBean;
import com.caucho.config.inject.InterceptorBean;
import com.caucho.config.manager.InjectManager;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.webbeans.Singleton;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.annotation.PostConstruct;
import javax.decorator.Decorator;
import javax.inject.DeploymentType;
import javax.inject.Standard;
import javax.inject.manager.Interceptor;

/**
 * Configuration for a classloader root containing webbeans
 */
public class WbWebBeans {
  private static final L10N L = new L10N(WbWebBeans.class);
  private static final Logger log
    = Logger.getLogger(WbWebBeans.class.getName());
  
  private InjectManager _webBeansContainer;
  private Path _root;
  
  private Path _webBeansFile;

  private HashMap<String,WbComponentType> _componentTypeMap
    = new HashMap<String,WbComponentType>();
  
  private ArrayList<WbComponentType> _componentTypeList;
  
  private ArrayList<ComponentImpl> _pendingComponentList
    = new ArrayList<ComponentImpl>();
  
  private ArrayList<ComponentImpl> _pendingBindList
    = new ArrayList<ComponentImpl>();

  private ArrayList<Interceptor> _interceptorList;
  
  private ArrayList<Class> _decoratorList
    = new ArrayList<Class>();

  private ArrayList<Class> _pendingClasses
    = new ArrayList<Class>();

  private boolean _isConfigured;

  public WbWebBeans(InjectManager webBeansContainer, Path root)
  {
    _webBeansContainer = webBeansContainer;
    
    _root = root;
    _webBeansFile = root.lookup("web-beans.xml");
    _webBeansFile.setUserPath(_webBeansFile.getURL());
  }

  public void setSchemaLocation(String schema)
  {
  }

  /**
   * returns the owning container.
   */
  public InjectManager getContainer()
  {
    return _webBeansContainer;
  }

  /**
   * Returns the owning classloader.
   */
  public ClassLoader getClassLoader()
  {
    return getContainer().getClassLoader();
  }
  
  /**
   * Gets the web beans root directory
   */
  public Path getRoot()
  {
    return _root;
  }

  /**
   * Adds a scanned class
   */
  public void addScannedClass(Class cl)
  {
    _pendingClasses.add(cl);
  }

  /**
   * True if the configuration file has been passed.
   */
  public boolean isConfigured()
  {
    return _isConfigured;
  }

  /**
   * True if the configuration file has been passed.
   */
  public void setConfigured(boolean isConfigured)
  {
    _isConfigured = isConfigured;
  }

  //
  // web-beans syntax
  //

  /**
   * Adds a component.
   */
  public WbComponentConfig createComponent()
  {
    return new WbComponentConfig(_webBeansContainer);
  }

  public void addWbComponent(ComponentImpl component)
  {
    _pendingComponentList.remove(component);
    _pendingComponentList.add(component);
  }

  /**
   * Adds a namespace bean
   */
  public void addCustomBean(CustomBeanConfig bean)
  {
  }

  /**
   * Adds a deploy
   */
  public DeployConfig createDeploy()
  {
    return new DeployConfig();
  }

  /**
   * Adds the interceptors
   */
  public Interceptors createInterceptors()
  {
    return new Interceptors();
  }

  /**
   * Adds the decorators
   */
  public Decorators createDecorators()
  {
    return new Decorators();
  }

  /**
   * Initialization and validation on parse completion.
   */
  @PostConstruct
  public void init()
  {
    if (_componentTypeList == null) {
      _componentTypeList = new ArrayList<WbComponentType>();

      WbComponentType type = createComponentType(Standard.class);
      type.setPriority(0);
      _componentTypeList.add(type);
    }

    for (Class cl : _decoratorList) {
      DecoratorBean decorator = new DecoratorBean(_webBeansContainer, cl);

      _webBeansContainer.addDecorator(decorator);
    }
    _decoratorList.clear();

    update();
    
    if (_interceptorList != null) {
      _webBeansContainer.setInterceptorList(_interceptorList);
      _interceptorList = null;
    }
  }

  public void update()
  {
    InjectManager webBeans = _webBeansContainer;

    try {
      if (_pendingClasses.size() > 0) {
	ArrayList<Class> pendingClasses
	  = new ArrayList<Class>(_pendingClasses);
	_pendingClasses.clear();

	for (Class cl : pendingClasses) {
	  if (webBeans.getWebComponent(cl) != null)
	    continue;

	  SimpleBean component;

	  if (cl.isAnnotationPresent(Singleton.class))
	    component = new SingletonClassComponent(cl);
	  else
	    component = new SimpleBean(cl);

	  component.setFromClass(true);
	  component.init();

	  webBeans.addBean(component);

	  //_pendingComponentList.add(component);
	}
      }

      /*
      if (_pendingComponentList.size() > 0) {
	ArrayList<ComponentImpl> componentList
	  = new ArrayList<ComponentImpl>(_pendingComponentList);
	_pendingComponentList.clear();

	for (ComponentImpl comp : componentList) {
	  if (webBeans.getWebComponent(comp.getTargetType()) == null)
	    webBeans.addBean(comp);
	}
      }
      */
    } catch (Exception e) {
      throw LineConfigException.create(_webBeansFile.getURL(), 1, e);
    }
  }

  public WbComponentType createComponentType(Class cl)
  {
    WbComponentType type = _componentTypeMap.get(cl.getName());

    if (type == null) {
      type = new WbComponentType(cl);
      _componentTypeMap.put(cl.getName(), type);
    }

    return type;
  }

  public ScopeContext getScopeContext(Class cl)
  {
    return _webBeansContainer.getScopeContext(cl);
  }

  public void addInterceptor(Class cl)
  {
    if (_interceptorList == null)
      _interceptorList = new ArrayList<Interceptor>();

    InterceptorBean bean = new InterceptorBean(_webBeansContainer, cl);
    bean.init();

    _interceptorList.add(bean);
  }

  @Override
  public String toString()
  {
    if (_root != null)
      return "WbWebBeans[" + _root.getURL() + "]";
    else
      return "WbWebBeans[]";
  }

  public class Interceptors {
    public void addCustomBean(CustomBeanConfig config)
    {
      Class cl = config.getClassType();
      
      if (cl.isInterface())
	throw new ConfigException(L.l("'{0}' is not valid because <Interceptors> can only contain interceptor implementations",
				      cl.getName()));

      if (! cl.isAnnotationPresent(javax.interceptor.Interceptor.class))
	throw new ConfigException(L.l("'{0}' must have an @Interceptor annotation because it is an interceptor implementation",
				      cl.getName()));

      addInterceptor(cl);
    }
  }

  public class Decorators {
    public void addCustomBean(CustomBeanConfig config)
    {
      Class cl = config.getClassType();
      
      if (cl.isInterface())
	throw new ConfigException(L.l("'{0}' is not valid because <Decorators> can only contain decorator implementations",
				      cl.getName()));

      if (! cl.isAnnotationPresent(Decorator.class))
	throw new ConfigException(L.l("'{0}' must have an @Decorator annotation because it is a decorator implementation",
				      cl.getName()));

      _decoratorList.add(cl);
    }
  }

  public class DeployConfig {
    private ArrayList<Class> _deployList
      = new ArrayList<Class>();

    public void addAnnotation(Annotation ann)
    {
      Class cl = ann.annotationType();

      if (! cl.isAnnotationPresent(DeploymentType.class))
	throw new ConfigException(L.l("'{0}' must have a @DeploymentType annotation because because <Deploy> can only contain @DeploymentType annotations",
				      cl.getName()));

      _deployList.add(cl);
    }
    
    @PostConstruct
    public void init()
    {
      _webBeansContainer.setDeploymentTypes(_deployList);
    }
  }
}
