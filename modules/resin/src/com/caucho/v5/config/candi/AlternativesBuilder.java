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

import com.caucho.v5.util.L10N;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class AlternativesBuilder implements StereotypeFilter
{
  private static final L10N L = new L10N(AlternativesBuilder.class);

  private ArrayList<Class<?>> _discovered = new ArrayList<>();

  private ArrayList<Class<?>> _registered = new ArrayList<>();

  private List<Class<?>> _discoveredList;
  
  private List<Class<?>> _alternativesList;

  private Set<Class<?>> _disabled = new HashSet<>();

  private BeanManagerBase _beanManager;
  
  public AlternativesBuilder(BeanManagerBase beanManager)
  {
    _beanManager = beanManager;
  }

  public void addRegistered(Class<?> type)
  {
    clearAlternatives();
    _registered.add(type);
  }

  public boolean isRegistered(Class<?> type)
  {
    return _registered.contains(type);
  }

  public boolean isEnabled(AnnotatedType<?> type)
  {
    Class<?> javaClass = type.getJavaClass();

    if (isRegistered(javaClass)) {
      return true;
    }
    else if (isAlternativeByStereotype(type, this)) {
      return true;
    }
    else if (_discovered.contains(javaClass)) {
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
    if (! type.isAnnotationPresent(Priority.class)) {
      return;
    }
    
    BeanManagerBase parent = _beanManager.getDelegate().getBeanManager();

    if (parent != _beanManager) {
      parent.discover(type);
      return;
    }

    if (isAlternative(type)) {
      _discovered.add(type.getJavaClass());

      clearAlternatives();
    }
  }

  private <X> boolean isAlternative(AnnotatedType<X> type)
  {
    if (isAlternative((Annotated) type))
      return true;

    Set<AnnotatedField<? super X>> fields = type.getFields();
    for (AnnotatedField<? super X> field : fields) {
      if (!field.isAnnotationPresent(Produces.class))
        continue;

      boolean isAlternative = isAlternative(field);

      if (isAlternative)
        return true;
    }

    Set<AnnotatedMethod<? super X>> methods = type.getMethods();
    for (AnnotatedMethod<? super X> method : methods) {
      if (!method.isAnnotationPresent(Produces.class))
        continue;

      boolean isAlternative = isAlternative(method);

      if (isAlternative)
        return true;
    }

    return false;
  }

  private boolean isAlternative(Annotated annotated)
  {
    if (annotated.isAnnotationPresent(Alternative.class)) {
      return true;
    }
    else if (isAlternativeByStereotype(annotated,
                                       StereotypeFilter.wildCardFilter)) {
      return true;
    }
    else {
      return false;
    }
  }

  private boolean isAlternativeByStereotype(final Annotated type,
                                            StereotypeFilter filter)
  {
    LinkedList<Annotation> stereotypes = new LinkedList<>();

    for (Annotation a : type.getAnnotations()) {
      if (a.annotationType().isAnnotationPresent(Stereotype.class)) {
        stereotypes.add(a);
      }
    }

    while (!stereotypes.isEmpty()) {
      final Annotation stereotype = stereotypes.removeFirst();
      for (Annotation a : stereotype.annotationType().getAnnotations()) {

        if (a.annotationType().isAnnotationPresent(Stereotype.class)) {
          stereotypes.add(a);
        }
        else if (Alternative.class.isAssignableFrom(a.annotationType())
                 && filter.accept(stereotype))
          return true;
      }
    }

    return false;
  }

  public List<Class<?>> getAlternatives()
  {
    if (_alternativesList == null) {
      _alternativesList = build();
    }
    
    return _alternativesList;
  }

  public List<Class<?>> getDiscovered()
  {
    return new ArrayList<>(_discovered);
  }
  
  private void clearAlternatives()
  {
    _alternativesList = null;
  }

  public void setFinalDiscovered(List<Class<?>> alternatives)
  {
    _discovered = new ArrayList<>(alternatives);

    clearAlternatives();
  }

  public <T> void disable(AnnotatedType<T> type)
  {
    _disabled.add(type.getJavaClass());
  }

  /*
  public List<Class<?>> getAdded()
  {
    List<Class<?>> diff = findAdded(_discoveredList, _finalDiscovered);

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

  public List<Class<?>> build()
  {
    List<Class<?>> list = new ArrayList<>();

    list.addAll(_registered); // XXX:
    // list.addAll(_discovered);
    
    for (Class<?> cl: _discovered) {
      if (! list.contains(cl)) {
        list.add(cl);
      }
    }
    
    BeanManagerBase parentManager = _beanManager.getDelegate().getBeanManager();
    
    if (parentManager != _beanManager) {
      for (Class<?> cl : parentManager.getAlternatives()) {
        if (! list.contains(cl)) {
          list.add(cl);
        }
      }
    }
    
    Collections.sort(list, new CandiManager.PriorityComparator());

    return list;
  }

  @Override
  public boolean accept(Annotation stereotype)
  {
    return _registered.contains(stereotype.annotationType());
  }
} 

interface StereotypeFilter
{
  boolean accept(Annotation stereotype);

  static StereotypeFilter wildCardFilter = new WildCardFilter();

  static class WildCardFilter implements StereotypeFilter
  {
    @Override
    public boolean accept(Annotation stereotype)
    {
      return true;
    }
  }
}

