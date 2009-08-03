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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.NonBinding;
import javax.enterprise.inject.spi.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class WebComponent {
  private static final Logger log
    = Logger.getLogger(WebComponent.class.getName());
  private static final L10N L = new L10N(WebComponent.class);

  private static final Class []NULL_ARG = new Class[0];

  private InjectManager _beanManager;

  private Class _rawType;

  private BeanEntry _injectionPointEntry;

  private ArrayList<BeanEntry> _beanList = new ArrayList<BeanEntry>();

  public WebComponent(InjectManager beanManager, Class rawType)
  {
    _beanManager = beanManager;
    _rawType = rawType;
  }

  public void addComponent(BaseType type, Bean<?> bean)
  {
    for (BeanEntry beanEntry : _beanList) {
      if (beanEntry.getType().equals(type) && beanEntry.isMatch(bean))
        return;
    }

    if (bean instanceof ProducesBean
        && ((ProducesBean) bean).isInjectionPoint()) {
      _injectionPointEntry = new BeanEntry(type, bean);
    }

    _beanList.add(new BeanEntry(type, bean));

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

  public Set<Bean<?>> resolve(Type type, Annotation []bindings)
  {
    BaseType baseType = _beanManager.createBaseType(type);

    return resolve(baseType, bindings);
  }

  public Set<Bean<?>> resolve(BaseType type, Annotation []bindings)
  {
    LinkedHashSet<Bean<?>> beans = null;

    if (_injectionPointEntry != null) {
      beans = new LinkedHashSet<Bean<?>>();
      beans.add(_injectionPointEntry.getBean());
      return beans;
    }

    for (BeanEntry beanEntry : _beanList) {
      if (beanEntry.isMatch(type, bindings)) {
        Bean<?> bean = beanEntry.getBean();

        if (beans == null)
          beans = new LinkedHashSet<Bean<?>>();

        beans.add(beanEntry.getBean());
      }
    }

    return beans;
  }

  public ArrayList<Bean<?>> getBeanList()
  {
    ArrayList<Bean<?>> list = new ArrayList<Bean<?>>();

    for (BeanEntry beanEntry : _beanList) {
      Bean<?> bean = beanEntry.getBean();

      if (! list.contains(bean)) {
        list.add(bean);
      }
    }

    return list;
  }

  public ArrayList<Bean<?>> getEnabledBeanList()
  {
    ArrayList<Bean<?>> list = new ArrayList<Bean<?>>();

    int priority = 0;

    for (BeanEntry beanEntry : _beanList) {
      Bean<?> bean = beanEntry.getBean();

      int beanPriority = _beanManager.getDeploymentPriority(bean);

      if (priority <= beanPriority)
        list.add(bean);
    }

    return list;
  }

  static String getName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getName();
    else
      return String.valueOf(type);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _rawType + "]";
  }

  class BeanEntry {
    private Bean<?> _bean;
    private BaseType _type;
    private Binding []_bindings;

    BeanEntry(BaseType type, Bean<?> bean)
    {
      _type = type;

      _bean = bean;

      Set<Annotation> bindings = bean.getBindings();

      _bindings = new Binding[bindings.size()];

      int i = 0;
      for (Annotation binding : bindings) {
        _bindings[i++] = new Binding(binding);
      }
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    BaseType getType()
    {
      return _type;
    }

    boolean isMatch(Bean<?> bean)
    {
      // ioc/0213
      return _bean == bean;
    }

    boolean isMatch(BaseType type, Annotation []bindings)
    {
      return isMatch(type) && isMatch(bindings);
    }

    boolean isMatch(BaseType type)
    {
      return type.isAssignableFrom(_type);
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
      if (arg.annotationType() == Any.class)
        return true;

      for (Binding binding : _bindings) {
        if (binding.isMatch(arg))
          return true;
      }

      return false;
    }
  }
}
