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

package com.caucho.config.inject;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.*;
import com.caucho.config.cfg.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;

import javax.annotation.NonBinding;
import javax.inject.BindingType;
import javax.inject.manager.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class WebComponent<T> {
  private static final Logger log
    = Logger.getLogger(WebComponent.class.getName());
  private static final L10N L = new L10N(WebComponent.class);

  private static final Class []NULL_ARG = new Class[0];

  private InjectManager _webBeans;
  
  private BaseType _type;

  private ArrayList<BeanEntry<T>> _beanList
    = new ArrayList<BeanEntry<T>>();

  public WebComponent(InjectManager webBeans, BaseType type)
  {
    _webBeans = webBeans;
    _type = type;
  }

  public void addComponent(Bean<T> bean)
  {
    for (BeanEntry<T> beanEntry : _beanList) {
      if (beanEntry.isMatch(bean))
	return;
    }

    _beanList.add(new BeanEntry<T>(bean));

    /*
    for (int i = _componentList.size() - 1; i >= 0; i--) {
      ComponentImpl oldComponent = _componentList.get(i);

      if (! comp.getClassName().equals(oldComponent.getClassName())) {
      }
      else if (comp.isFromClass() && ! oldComponent.isFromClass())
	return;
      else if (! comp.isFromClass() && oldComponent.isFromClass())
	_componentList.remove(i);
      else if (comp.equals(oldComponent)) {
	return;
      }
    }

    _componentList.add(comp);
    */
  }
  
  public void createProgram(ArrayList<ConfigProgram> initList,
			    Field field,
			    ArrayList<Annotation> bindList)
    throws ConfigException
  {
    /*
    ComponentImpl comp = bind(WebBeansContainer.location(field), bindList);

    comp.createProgram(initList, field);
    */
  }
  
  public Set<Bean<T>> resolve(Annotation []bindings)
  {
    LinkedHashSet<Bean<T>> beans = null;

    int priority = 0;

    for (BeanEntry<T> beanEntry : _beanList) {
      if (beanEntry.isMatch(bindings)) {
	Bean<T> bean = beanEntry.getBean();

	int beanPriority
	  = _webBeans.getDeploymentPriority(bean.getDeploymentType());

	if (beanPriority < priority)
	  continue;

	if (priority < beanPriority && bindings.length > 0) {
	  beans = null;
	  priority = beanPriority;
	}

	if (beans == null)
	  beans = new LinkedHashSet<Bean<T>>();
	
	beans.add(beanEntry.getBean());
      }
    }

    return beans;
  }

  private int getPriority(Class deploymentType)
  {
    return _webBeans.getDeploymentPriority(deploymentType);
  }

  static String getName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getName();
    else
      return String.valueOf(type);
  }

  static class BeanEntry<T> {
    private Bean<T> _bean;
    private Binding []_bindings;

    BeanEntry(Bean<T> bean)
    {
      _bean = bean;

      Set<Annotation> bindings = bean.getBindingTypes();
      
      _bindings = new Binding[bindings.size()];

      int i = 0;
      for (Annotation binding : bindings) {
	_bindings[i++] = new Binding(binding);
      }
    }

    Bean<T> getBean()
    {
      return _bean;
    }

    boolean isMatch(Bean<T> bean)
    {
      // ioc/0213
      return _bean == bean;
    }

    boolean isMatch(Annotation []bindingArgs)
    {
      for (Annotation arg : bindingArgs) {
	if (! isMatch(arg)) {
	  if (! arg.annotationType().isAnnotationPresent(BindingType.class)) {
	    throw new ConfigException(L.l("'{0}' is an invalid binding annotation because it does not have a @BindingType meta-annotation.",
					  arg));
	  }
	  
	  return false;
	}
      }

      return true;
    }

    private boolean isMatch(Annotation arg)
    {
      for (Binding binding : _bindings) {
	if (binding.isMatch(arg))
	  return true;
      }

      return false;
    }
  }
}
