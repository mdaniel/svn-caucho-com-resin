/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;
import javax.interceptor.Interceptor;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.util.L10N;

class DecoratorsBuilder
{
  private static final L10N L = new L10N(DecoratorsBuilder.class);

  private Set<Class<?>> _discovered = new HashSet<>();

  private ArrayList<Class<?>> _registered = new ArrayList<>();

  private ArrayList<DecoratorEntry<?>> _discoveredDecorators
    = new ArrayList<>();

  private ArrayList<DecoratorEntry<?>> _registeredDecorators
    = new ArrayList<>();

  private List<Class<?>> _discoveredList;

  private List<Class<?>> _finalDiscovered = new ArrayList<>();

  private Set<Class<?>> _disabled = new HashSet<>();

  private BeanManagerBase _beanManager;

  private List<DecoratorEntry<?>> _decoratorList;

  public DecoratorsBuilder(BeanManagerBase beanManager)
  {
    _beanManager = beanManager;
  }

  public void addRegistered(Class<?> type)
  {
    _registered.add(type);
  }

  public boolean isRegistered(Class<?> type)
  {
    return _registered.contains(type);
  }

  public void addDecorator(DecoratorEntry<?> entry)
  {
    Class<?> beanClass = entry.getDecorator().getBeanClass();
    
    if (beanClass.isAnnotationPresent(Interceptor.class)) {
      throw new ConfigException(L.l("{0} may not have both a @Decorator and @Interceptor annotation.",
                                    beanClass));
    }

    if (_finalDiscovered.contains(beanClass)) {
      _discoveredDecorators.add(entry);
    }
    else if (_registered.contains(beanClass)) {
      _registeredDecorators.add(entry);
    }
    else if (beanClass.isAnnotationPresent(Priority.class)) {
      // ioc/0i80
      _discoveredDecorators.add(entry);
    }
    else {
      throw new IllegalStateException(L.l("Decorator {0} is not enabled.",
                                          entry.getDecorator()));
    }
  }

  public boolean isEnabled(AnnotatedType<?> type)
  {
    Class<?> javaClass = type.getJavaClass();

    if (isRegistered(javaClass)) {
      return true;
    }
    else if (_finalDiscovered.contains(javaClass)) {
      return true;
    }
    else if (_discovered.contains(javaClass)) {
      // XXX:
      return true;
    }
    else {
      return false;
    }
  }

  public void discover(ArrayList<AnnotatedType<?>> types)
  {
    for (AnnotatedType<?> type : types) {
      discover(type);
    }
  }

  public void discover(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(Priority.class)
        && type.isAnnotationPresent(javax.decorator.Decorator.class)) {
      _discovered.add(type.getJavaClass());
    }
  }

  public List<Class<?>> getDiscovered()
  {
    if (_discoveredList != null)
      return _discoveredList;

    List<Class<?>> decorators = new ArrayList<>(_discovered);

    Collections.sort(decorators, new CandiManager.PriorityComparator());

    decorators = Collections.unmodifiableList(decorators);

    _discoveredList = decorators;

    return _discoveredList;
  }

  public void setFinalDiscovered(List<Class<?>> decorators)
  {
    _finalDiscovered = decorators;
    _decoratorList = null;
  }

  public <T> void disable(AnnotatedType<T> type)
  {
    _disabled.add(type.getJavaClass());
  }

  public List<Class<?>> getAdded()
  {
    List<Class<?>> diff = findAdded(_finalDiscovered, _discoveredList);

    return diff;
  }

  private <T> List<T> findAdded(Collection<T> original, List<T> modified)
  {
    List<T> result = new ArrayList<>();
    for (T t : modified) {
      if (original.contains(t))
        continue;
      result.add(t);
    }

    return result;
  }

  public void getDecorators(ArrayList<javax.enterprise.inject.spi.Decorator<?>> decorators,
                            ArrayList<BaseType> targetTypes,
                            Annotation[] qualifiers)
  {

    for (DecoratorEntry<?> entry : getDecoratorList()) {
      Decorator<?> decorator = entry.getDecorator();

      // XXX: delegateTypes
      if (isDelegateAssignableFrom(entry.getDelegateType(), targetTypes)
          && entry.isMatch(qualifiers)) {
        decorators.add(decorator);
      }
    }
  }

  private boolean isDelegateAssignableFrom(BaseType delegateType,
                                           ArrayList<BaseType> sourceTypes)
  {
    for (BaseType sourceType : sourceTypes) {
      if (delegateType.isAssignableFrom(sourceType)) {
        return true;
      }
    }

    return false;
  }
  
  private List<DecoratorEntry<?>> getDecoratorList()
  {
    if (_decoratorList == null) {
      _decoratorList = build();
    }
    
    return _decoratorList;
  }

  public List<Class<?>> getDecorators()
  {
    return getDiscovered();
  }

  public List<DecoratorEntry<?>> build()
  {
    List<DecoratorEntry<?>> list = new ArrayList<>();

    Collections.sort(_discoveredDecorators,
                     new ListDelegatedComparator(_finalDiscovered));

    Collections.sort(_registeredDecorators,
                     new ListDelegatedComparator(_registered));

    list.addAll(_discoveredDecorators);
    list.addAll(_registeredDecorators);
    
    /*
    for (Class<?> cl : _finalDiscovered) {
      DecoratorEntry<?> entry = findDecorator(cl, _discoveredDecorators);
      
      if (entry != null) {
        list.add(entry);
      }
    }

    Collections.sort(_registeredDecorators,
                     new ListDelegatedComparator(_registered));

    list.addAll(_registeredDecorators);
    */

    return list;
  }
  
  private DecoratorEntry<?> findDecorator(Class<?> cl, 
                                          List<DecoratorEntry<?>> list)
  {
    for (DecoratorEntry<?> entry : list) {
      if (entry.getDecorator().getBeanClass().equals(cl)) {
        return entry;
      }
    }
    
    return null;
  }

  static class ListDelegatedComparator implements Comparator<DecoratorEntry<?>>
  {
    private List<Class<?>> _classes;

    ListDelegatedComparator(List<Class<?>> classes)
    {
      _classes = classes;
    }

    @Override
    public int compare(DecoratorEntry<?> e1, DecoratorEntry<?> e2)
    {
      Class<?> d1 = e1.getDecorator().getBeanClass();
      Class<?> d2 = e2.getDecorator().getBeanClass();

      int i1 = _classes.indexOf(d1);
      int i2 = _classes.indexOf(d2);

      return i1 - i2;
    }
  }
}