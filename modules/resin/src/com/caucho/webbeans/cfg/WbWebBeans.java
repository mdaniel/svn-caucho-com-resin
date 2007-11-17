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

import com.caucho.bytecode.*;
import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.webbeans.SingletonScoped;
import com.caucho.webbeans.component.SingletonClassComponent;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.annotation.PostConstruct;
import javax.webbeans.*;

/**
 * Configuration for the top-level web bean
 */
public class WbWebBeans {
  private static final L10N L = new L10N(WbWebBeans.class);
  private static final Logger log
    = Logger.getLogger(WbWebBeans.class.getName());
  
  private WebBeansContainer _webBeansContainer;
  private Path _root;

  private HashMap<String,WbComponentType> _componentTypeMap
    = new HashMap<String,WbComponentType>();
  
  private ArrayList<WbComponentType> _componentTypeList;
  
  private ArrayList<WbComponent> _componentList
    = new ArrayList<WbComponent>();
  
  private ArrayList<WbComponent> _pendingComponentList
    = new ArrayList<WbComponent>();
  
  private ArrayList<WbInterceptor> _interceptorList
    = new ArrayList<WbInterceptor>();

  private ArrayList<Class> _pendingClasses
    = new ArrayList<Class>();

  private boolean _isConfigured;

  public WbWebBeans(WebBeansContainer webBeansContainer, Path root)
  {
    _webBeansContainer = webBeansContainer;
    
    _root = root;
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

  /**
   * Adds a component.
   */
  public WbComponentConfig createComponent()
  {
    return new WbComponentConfig(this);
  }

  public void addWbComponent(WbComponent component)
  {
    _componentList.remove(component);
    _componentList.add(component);
    
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

  public void update()
  {
    ArrayList<Class> pendingClasses = new ArrayList<Class>(_pendingClasses);
    _pendingClasses.clear();

    for (Class cl : pendingClasses) {
      if (_componentTypeMap.get(cl.getName()) != null)
	continue;

      WbClassComponent component;

      if (cl.isAnnotationPresent(SingletonScoped.class))
	component = new SingletonClassComponent(this);
      else
	component = new WbClassComponent(this);
	
      component.setClass(cl);
      component.setFromClass(true);
      component.introspect();
      component.init();

      _componentList.add(component);
    }
  }

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

    WebBeansContainer webBeans = _webBeansContainer;

    ArrayList<WbComponent> componentList
      = new ArrayList<WbComponent>(_pendingComponentList);
    _pendingComponentList.clear();

    for (WbComponent comp : componentList) {
      if (comp.getType().isEnabled()) {
	webBeans.addComponent(comp);
      }
    }

    for (WbComponent comp : componentList) {
      if (comp.getType().isEnabled()) {
	comp.bind();
      }
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

  public WbComponent bindParameter(Class type, Annotation []annotations)
  {
    return _webBeansContainer.bind(type, getBindList(annotations));
  }

  /**
   * Returns the binding annotations
   */
  private ArrayList<Annotation> getBindList(Annotation []annotations)
  {
    ArrayList<Annotation> bindList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindList.add(ann);
    }

    return bindList;
  }

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
}
