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
 */

package com.caucho.v5.config.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import com.caucho.v5.config.candi.JavaReflection;
import com.caucho.v5.config.candi.QualifierBinding;
import com.caucho.v5.config.reflect.BaseType;

public class ObserverEntry<T>
{
  private ObserverMethod<T> _observer;
  private BaseType _type;
  private QualifierBinding []_qualifiers;
  private WithAnnotations _withAnnotations;

  ObserverEntry(ObserverMethod<T> observer,
                BaseType type,
                Annotation []qualifiers,
                WithAnnotations withAnnotations)
  {
    _observer = observer;
    _type = type;

    _qualifiers = new QualifierBinding[qualifiers.length];
    for (int i = 0; i < qualifiers.length; i++) {
      _qualifiers[i] = new QualifierBinding(qualifiers[i]);
    }

    _withAnnotations = withAnnotations;
  }

  ObserverMethod<T> getObserver()
  {
    return _observer;
  }

  BaseType getType()
  {
    return _type;
  }

  void notify(T event)
  {
    _observer.notify(event);
  }

  void fireEvent(T event, Annotation []qualifiers)
  {
    if (isMatch(qualifiers))
      _observer.notify(event);
  }

  void resolveObservers(Set<ObserverMethod<?>> set, Annotation []qualifiers)
  {
    if (isMatch(qualifiers))
      set.add(_observer);
  }

  boolean isAssignableFrom(BaseType type, Annotation []qualifiers, Object event)
  {
    if (! _type.isAssignableFrom(type)) {
      return false;
    }

    /*
    if (qualifiers.length < _qualifiers.length)
      return false;
      */

    if (! isMatch(qualifiers))
      return false;

    if (_withAnnotations == null)
      return true;

    return isWithAnnotationsMatch(event);
  }

  private boolean isMatch(Annotation []qualifiers)
  {
    for (QualifierBinding qualifier : _qualifiers) {
      if (qualifier.isAny()) {
      }
      else if (! qualifier.isMatch(qualifiers)) {
        return false;
      }
    }

    return true;
  }

  private boolean isWithAnnotationsMatch(Object eventObject)
  {
    if (! ProcessAnnotatedType.class.isAssignableFrom(eventObject.getClass()))
      throw new IllegalArgumentException();

    ProcessAnnotatedType event = (ProcessAnnotatedType) eventObject;

    Class type = event.getAnnotatedType().getJavaClass();

    WithAnnotationsVisitor visitor
      = new WithAnnotationsVisitor(_withAnnotations);

    JavaReflection.visitClassHierarchy(type, visitor);

    return visitor.isMatch();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _observer + "," + _type + "]";
  }
}

class WithAnnotationsVisitor implements JavaReflection.ClassVisitor
{
  private Class<? extends java.lang.annotation.Annotation> []_annotations;
  private boolean _isMatch;

  WithAnnotationsVisitor(WithAnnotations withAnnotations)
  {
    _annotations = withAnnotations.value();
  }

  @Override
  public void visit(Class type)
  {
    _isMatch = isClassAnnotatedMatch(type);

    _isMatch = _isMatch || isConstructorsAnnotated(type);

    _isMatch = _isMatch || isMethodsAnnotatedMatch(type);

    _isMatch = _isMatch || isFieldsAnnotatedMatch(type);
  }

  private boolean isClassAnnotatedMatch(Class type)
  {
    return isAnnotatedElementAnnotated(type);
  }

  private boolean isConstructorsAnnotated(Class type)
  {
    Constructor []constructors = type.getDeclaredConstructors();
    for (Constructor constructor : constructors) {
      if (isAnnotatedConstructor(constructor))
        return true;
    }

    return false;
  }

  private boolean isAnnotatedConstructor(Constructor constructor)
  {
    boolean isMatch = isAnnotatedElementAnnotated(constructor);

    isMatch = isMatch
              || isAnnotatedParametersMatch(constructor.getParameterAnnotations());

    return isMatch;
  }

  private boolean isAnnotatedParametersMatch(Annotation [][]annotations)
  {
    for (Annotation []params : annotations) {
      for (Annotation annotation : params) {
        if (isAnnotationMatch(annotation))
          return true;
      }
    }

    return false;
  }

  private boolean isMethodsAnnotatedMatch(Class type)
  {
    Method []methods = type.getDeclaredMethods();

    for (Method method : methods) {
      if (isMethodAnnotated(method))
        return true;
    }

    return false;
  }

  private boolean isMethodAnnotated(Method method)
  {
    boolean isMatch = isAnnotatedElementAnnotated(method);

    isMatch = isMatch
              || isAnnotatedParametersMatch(method.getParameterAnnotations());

    return isMatch;
  }

  private boolean isFieldsAnnotatedMatch(Class type)
  {
    Field []fields = type.getDeclaredFields();

    for (Field field : fields) {
      if (isFieldAnnotated(field))
        return true;
    }

    return false;
  }

  private boolean isFieldAnnotated(Field field)
  {
    return isAnnotatedElementAnnotated(field);
  }

  private boolean isAnnotatedElementAnnotated(AnnotatedElement element)
  {
    Annotation []annotations = element.getDeclaredAnnotations();
    for (Annotation annotation : annotations) {
      if (isAnnotationMatch(annotation))
        return true;
    }

    return false;
  }

  private boolean isAnnotationMatch(Annotation annotation)
  {
    for (Class<? extends Annotation> a : _annotations) {
      if (a.isAssignableFrom(annotation.annotationType()))
        return true;

      if (annotation.annotationType().isAnnotationPresent(a))
        return true;
    }

    return false;
  }

  @Override
  public boolean isFinished()
  {
    return _isMatch;
  }

  boolean isMatch()
  {
    return _isMatch;
  }
}