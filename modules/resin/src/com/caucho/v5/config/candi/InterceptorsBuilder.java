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
import javax.decorator.Decorator;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.L10N;

class InterceptorsBuilder
{
  private static final L10N L = new L10N(InterceptorsBuilder.class);

  private List<Class<?>> _discovered = new ArrayList<>();

  private ArrayList<Class<?>> _registered = new ArrayList<>();

  private ArrayList<InterceptorEntry<?>> _discoveredInterceptors
    = new ArrayList<>();

  private ArrayList<InterceptorEntry<?>> _registeredInterceptors
    = new ArrayList<>();

  private ArrayList<InterceptorEntry<?>> _other
    = new ArrayList<>();

  // private List<Class<?>> _discoveredList;

  private Set<Class<?>> _disabled = new HashSet<>();

  private List<InterceptorEntry<?>> _interceptorList;

  private BeanManagerBase _beanManager;
  
  public InterceptorsBuilder(BeanManagerBase beanManager)
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

  public void addInterceptor(InterceptorEntry<?> entry)
  {
    Class<?> beanClass = entry.getInterceptor().getBeanClass();
    
    if (beanClass.isAnnotationPresent(Decorator.class)) {
      throw new ConfigException(L.l("{0} may not have both a @Decorator and @Interceptor annotation.",
                                    beanClass));
    }
    
    if (_discovered.contains(beanClass))
      _discoveredInterceptors.add(entry);
    else if (_registered.contains(beanClass))
      _registeredInterceptors.add(entry);
    else
      _other.add(entry);
/*
    else
      throw new IllegalStateException(L.l("Interceptor {0} is not enabled.",
                                          entry.getInterceptor()));
*/
  }

  public boolean isEnabled(AnnotatedType<?> type)
  {
    Class<?> javaClass = type.getJavaClass();

    boolean isEnabled = isRegistered(javaClass)
                        || _discovered.contains(javaClass);

    return isEnabled;
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
        && type.isAnnotationPresent(javax.interceptor.Interceptor.class)) {
      _discovered.add(type.getJavaClass());
      Collections.sort(_discovered, new CandiManager.PriorityComparator());
    }

    /*
    if (type.isAnnotationPresent(javax.interceptor.Interceptor.class)) {
      _discovered.add(type.getJavaClass());
    }
    */
  }

  /*
  public List<Class<?>> getDiscovered()
  {
    if (_discoveredList != null)
      return _discoveredList;

    List<Class<?>> interceptors = new ArrayList<>(_discovered);

    Collections.sort(interceptors, new InjectManager.PriorityComparator());

    interceptors = Collections.unmodifiableList(interceptors);

    _discoveredList = interceptors;

    return _discoveredList;
  }
  */

  public List<Class<?>> getDiscovered()
  {
    return _discovered;
  }
  
  public void setFinalDiscovered(List<Class<?>> interceptors)
  {
    _discovered = interceptors;
    
    Collections.sort(_discovered, new CandiManager.PriorityComparator());
  }

  public <T> void disable(AnnotatedType<T> type)
  {
    _disabled.add(type.getJavaClass());
  }

  /*
  public List<Class<?>> getAdded()
  {
    List<Class<?>> diff = findAdded(_discovered, _discoveredList);

    return diff;
  }
  */

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

  public void
  getInterceptors(List<Interceptor<?>> interceptorList,
                  InterceptionType type,
                  Annotation[] qualifiers)
  {
    for (InterceptorEntry<?> entry : getInterceptorList()) {
      Interceptor<?> interceptor = entry.getInterceptor();

      if (! interceptor.intercepts(type)) {
        continue;
      }

      if (entry.isMatch(qualifiers)) {
        interceptorList.add(interceptor);
      }
    }
  }
  
  private List<InterceptorEntry<?>> getInterceptorList()
  {
    if (_interceptorList == null) {
      _interceptorList = build();
    }
    
    return _interceptorList;
  }

  public List<InterceptorEntry<?>> build()
  {
    List<InterceptorEntry<?>> list = new ArrayList<>();

    Collections.sort(_discoveredInterceptors,
                     new ListDelegatedComparator(_discovered));

    Collections.sort(_registeredInterceptors,
                     new ListDelegatedComparator(_registered));

    list.addAll(_discoveredInterceptors);
    list.addAll(_registeredInterceptors);
    list.addAll(_other);
    
    return list;
  }

  static class ListDelegatedComparator
    implements Comparator<InterceptorEntry<?>>
  {
    private List<Class<?>> _classes;

    ListDelegatedComparator(List<Class<?>> classes)
    {
      _classes = classes;
    }

    @Override
    public int compare(InterceptorEntry<?> e1, InterceptorEntry<?> e2)
    {
      Class<?> d1 = e1.getInterceptor().getBeanClass();
      Class<?> d2 = e2.getInterceptor().getBeanClass();

      int i1 = _classes.indexOf(d1);
      int i2 = _classes.indexOf(d2);

      return i1 - i2;
    }
  }
}

