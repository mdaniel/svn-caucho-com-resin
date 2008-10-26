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
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.webbeans.Singleton;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.annotation.PostConstruct;
import javax.webbeans.*;

/**
 * Configuration for a classloader root containing webbeans
 */
public class WbWebBeans {
  private static final L10N L = new L10N(WbWebBeans.class);
  private static final Logger log
    = Logger.getLogger(WbWebBeans.class.getName());
  
  private WebBeansContainer _webBeansContainer;
  private Path _root;
  
  private Path _webBeansFile;

  private HashMap<String,WbComponentType> _componentTypeMap
    = new HashMap<String,WbComponentType>();
  
  private ArrayList<WbComponentType> _componentTypeList;
  
  private ArrayList<ComponentImpl> _pendingComponentList
    = new ArrayList<ComponentImpl>();
  
  private ArrayList<ComponentImpl> _pendingBindList
    = new ArrayList<ComponentImpl>();

  private ArrayList<WbInterceptor> _enabledInterceptors;

  private ArrayList<Class> _pendingClasses
    = new ArrayList<Class>();

  private boolean _isConfigured;

  public WbWebBeans(WebBeansContainer webBeansContainer, Path root)
  {
    _webBeansContainer = webBeansContainer;
    
    _root = root;
    _webBeansFile = root.lookup("META-INF/web-beans.xml");
    _webBeansFile.setUserPath(_webBeansFile.getURL());
  }

  /**
   * returns the owning container.
   */
  public WebBeansContainer getContainer()
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
    return new WbComponentConfig(this);
  }

  public void addWbComponent(ComponentImpl component)
  {
    _pendingComponentList.remove(component);
    _pendingComponentList.add(component);
  }

  /**
   * Adds a component.
   */
  public WbComponentTypes createComponentTypes()
  {
    return new WbComponentTypes();
  }

  /**
   * Adds a namespace bean
   */
  public void addCustomBean(CustomBeanConfig bean)
  {
  }

  /**
   * Adds the interceptors
   */
  public Interceptors createInterceptors()
  {
    return new Interceptors();
  }

  /**
   * Returns the enabled interceptors
   */
  public ArrayList<WbInterceptor> getEnabledInterceptors()
  {
    return _enabledInterceptors;
  }

  /**
   * Returns matching interceptors
   */
  public ArrayList<WbInterceptor>
    findInterceptors(ArrayList<Annotation> bindingList)
  {
    ArrayList<WbInterceptor> list = null;

    if (_enabledInterceptors != null) {
      for (WbInterceptor interceptor : _enabledInterceptors) {
	if (! interceptor.isMatch(bindingList))
	  continue;
      
	if (list == null)
	  list = new ArrayList<WbInterceptor>();

	list.add(interceptor);
      }
    }
    
    return list;
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
      type = createComponentType(Component.class);
      type.setPriority(1);
      _componentTypeList.add(type);
    }

    update();
  }

  public void update()
  {
    WebBeansContainer webBeans = _webBeansContainer;

    try {
      if (_pendingClasses.size() > 0) {
	ArrayList<Class> pendingClasses = new ArrayList<Class>(_pendingClasses);
	_pendingClasses.clear();

	for (Class cl : pendingClasses) {
	  /*
	    if (_componentTypeMap.get(cl.getName()) != null)
	    continue;
	  */

	  ClassComponent component;

	  if (cl.isAnnotationPresent(Singleton.class))
	    component = new SingletonClassComponent(this);
	  else
	    component = new ClassComponent(this);
	
	  component.setInstanceClass(cl);
	  component.setTargetType(cl);
	  component.setFromClass(true);
	  component.introspect();
	  component.init();

	  _pendingComponentList.add(component);
	}
      }

      if (_pendingComponentList.size() > 0) {
	ArrayList<ComponentImpl> componentList
	  = new ArrayList<ComponentImpl>(_pendingComponentList);
	_pendingComponentList.clear();

	for (ComponentImpl comp : componentList) {
	  /*
	  if (_deploymentTypes.contains(comp.getDeploymentType())) {
	    webBeans.addComponent(comp);
	  }
	  */
	  webBeans.addComponent(comp);
	}
      }
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

  public ComponentImpl bindParameter(String loc,
				     Type type,
				     Annotation []annotations)
  {
    return _webBeansContainer.bind(loc, type, annotations);
  }

  @Override
  public String toString()
  {
    if (_root != null)
      return "WbWebBeans[" + _root.getURL() + "]";
    else
      return "WbWebBeans[]";
  }

  public class WbComponentTypes {
    public void addComponentType(Class cl)
    {
      if (! cl.isAnnotationPresent(ComponentType.class))
	throw new ConfigException(L.l("'{0}' is missing a @ComponentType annotation.  Component annotations must be annotated with @ComponentType.",
				      cl.getName()));

      if (_componentTypeList == null)
	_componentTypeList = new ArrayList<WbComponentType>();

      int priority =  _componentTypeList.size();

      WbComponentType type = createComponentType(cl);

      type.setPriority(priority);
      
      _componentTypeList.add(type);
    }
  }

  public void addEnabledInterceptor(Class cl)
  {
    if (_enabledInterceptors == null)
      _enabledInterceptors = new ArrayList<WbInterceptor>();
    
    _enabledInterceptors.add(new WbInterceptor(cl));
  }

  public class Interceptors {
    public void addInterceptor(Class cl)
    {
      if (_enabledInterceptors == null)
	_enabledInterceptors = new ArrayList<WbInterceptor>();
    
      _enabledInterceptors.add(new WbInterceptor(cl));
    }
  }
}
