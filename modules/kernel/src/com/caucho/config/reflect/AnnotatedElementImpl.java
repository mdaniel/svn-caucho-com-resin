/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

package com.caucho.config.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

import com.caucho.inject.Module;

/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotatedElementImpl implements Annotated
{
  private static final Set<Annotation> NULL_ANN_SET
    = Collections.unmodifiableSet(new LinkedHashSet<Annotation>());
  
  private Type _type;

  private LinkedHashSet<Type> _typeSet;

  private LinkedHashSet<Annotation> _annSet;

  protected AnnotatedElementImpl(Type type,
                                 Annotated annotated,
                                 Annotation []annList)
  {
    _type = type;

    if (annList == null)
      annList = new Annotation[0];

    if (annotated != null) {
      Set<Annotation> annSet = annotated.getAnnotations();
      
      if (annSet != null && annSet.size() > 0) {
        _annSet = new LinkedHashSet<Annotation>(annSet);
      }
    }
    else if (annList != null && annList.length > 0) {
      _annSet = new LinkedHashSet<Annotation>();
      
      for (Annotation ann : annList) {
        _annSet.add(ann);
      }
    }
  }

  public AnnotatedElementImpl(Annotated annotated)
  {
    this(annotated.getBaseType(), annotated, null);
  }

  @Override
  public Type getBaseType()
  {
    return _type;
  }

  @Override
  public Set<Type> getTypeClosure()
  {
    if (_typeSet == null) {
      LinkedHashSet<Type> typeSet = new LinkedHashSet<Type>();
      typeSet.add(_type);
      
      _typeSet = typeSet;
    }

    return _typeSet;
  }

  public void addAnnotation(Annotation newAnn)
  {
    if (_annSet == null)
      _annSet = new LinkedHashSet<Annotation>();
    
    for (Annotation oldAnn : _annSet) {
      if (newAnn.annotationType().equals(oldAnn.annotationType())) {
        _annSet.remove(oldAnn);
        _annSet.add(newAnn);
        return;
      }
    }

    _annSet.add(newAnn);
  }

  public void removeAnnotation(Annotation ann)
  {
    if (_annSet == null)
      return;
    
    for (Annotation oldAnn : _annSet) {
      if (ann.annotationType().equals(oldAnn.annotationType())) {
        _annSet.remove(oldAnn);
        return;
      }
    }
  }

  public void clearAnnotations()
  {
    _annSet = null;
  }

  /**
   * Returns the declared annotations
   */
  @Override
  public Set<Annotation> getAnnotations()
  {
    if (_annSet != null)
      return _annSet;
    else
      return NULL_ANN_SET;
  }

  /**
   * Returns the matching annotation
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(Class<T> annType)
  {
    if (_annSet == null)
      return null;
    
    for (Annotation ann : _annSet) {
      if (annType.equals(ann.annotationType())) {
        return (T) ann;
      }
    }

    return null;
  }

  /**
   * Returns true if the annotation is present)
   */
  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    if (_annSet == null)
      return false;
    
    for (Annotation ann : _annSet) {
      if (annType.equals(ann.annotationType()))
        return true;
    }

    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
