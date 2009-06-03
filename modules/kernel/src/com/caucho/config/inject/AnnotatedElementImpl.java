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

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * Abstract introspected view of a Bean
 */
public class AnnotatedElementImpl implements Annotated
{
  private Type _type;
  
  private LinkedHashSet<Annotation> _annSet
    = new LinkedHashSet<Annotation>();
  
  protected AnnotatedElementImpl(Type type,
				 Annotated annotated,
				 Annotation []annList)
  {
    _type = type;
    
    if (annList == null)
      annList = new Annotation[0];

    if (annotated != null) {
      _annSet.addAll(annotated.getAnnotations());
    }
    else {
      for (Annotation ann : annList) {
	_annSet.add(ann);
      }
    }
  }

  public Type getType()
  {
    return _type;
  }

  public void addAnnotation(Annotation newAnn)
  {
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
    for (Annotation oldAnn : _annSet) {
      if (ann.annotationType().equals(oldAnn.annotationType())) {
	_annSet.remove(oldAnn);
	return;
      }
    }
  }
  
  /**
   * Returns the declared annotations
   */
  public Set<Annotation> getAnnotations()
  {
    return _annSet;
  }

  /**
   * Returns the matching annotation
   */
  public <T extends Annotation> T getAnnotation(Class<T> annType)
  {
    for (Annotation ann : getAnnotations()) {
      if (annType.equals(ann.annotationType()))
	return (T) ann;
    }

    return null;
  }

  /**
   * Returns true if the annotation is present)
   */
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    for (Annotation ann : getAnnotations()) {
      if (annType.equals(ann.annotationType()))
	return true;
    }

    return false;
  }
}
