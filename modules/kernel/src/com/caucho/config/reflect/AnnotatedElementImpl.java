/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.util.Collection;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotatedElementImpl implements Annotated, BaseTypeAnnotated
{
  private static final AnnotationSet NULL_ANN_SET
    = new AnnotationSet();
  
  private BaseType _type;

  private Set<Type> _typeSet;

  private AnnotationSet _annSet;

  public AnnotatedElementImpl(BaseType type,
                              Annotated annotated,
                              Annotation []annList)
  {
    _type = type;

    if (annotated != null) {
      Set<Annotation> annSet = annotated.getAnnotations();
      
      if (annSet != null && annSet.size() > 0) {
        _annSet = new AnnotationSet(annSet);
      }
    }
    
    if (annList != null && annList.length > 0) {
      if (_annSet == null)
        _annSet = new AnnotationSet();
      
      for (Annotation ann : annList) {
        _annSet.add(ann);
      }
    }
  }

  public AnnotatedElementImpl(Annotated annotated)
  {
    this(createBaseType(annotated), annotated, null);
  }

  public AnnotatedElementImpl(Class<?> cl, 
                              Annotated annotated,
                              Annotation []annotationList)
  {
    this(createBaseType(cl), annotated, annotationList);
  }
  
  protected static BaseType createBaseType(Annotated ann)
  {
    if (ann instanceof BaseTypeAnnotated)
      return ((BaseTypeAnnotated) ann).getBaseTypeImpl();
    else
      return createBaseType(ann.getBaseType());
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType)
  {
    if (declaringType instanceof BaseTypeAnnotated)
      return ((BaseTypeAnnotated) declaringType).getBaseTypeImpl();
    else
      return createBaseType(declaringType.getBaseType());
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType,
                                           Type type)
  {
    // ioc/0242
    BaseType baseType = createBaseType(declaringType, type, 
                                       BaseType.ClassFill.PLAIN);
    
    return baseType;
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType,
                                           Type type,
                                           BaseType.ClassFill classFill)
    {
    BaseType declBaseType;
    
    if (declaringType instanceof BaseTypeAnnotated) {
      declBaseType = ((BaseTypeAnnotated) declaringType).getBaseTypeImpl();

      return declBaseType.create(type, 
                                 declBaseType.getParamMap(), 
                                 classFill);
    }
    /*
    else if (declaringType instanceof ReflectionAnnotatedType<?>) {
      declBaseType = ((ReflectionAnnotatedType<?>) declaringType).getBaseTypeImpl();
      
      return declBaseType.create(type, declBaseType.getParamMap(), true);
    }
    */
    
    return createBaseType(type);
  }

  protected static BaseType createBaseType(Type type)
  {
    InjectManager cdiManager = InjectManager.getCurrent();
    
    return cdiManager.createSourceBaseType(type);
  }

  @Override
  public Type getBaseType()
  {
    return _type.toType();
  }
  
  public BaseType getBaseTypeImpl()
  {
    return _type;
  }

  @Override
  public Set<Type> getTypeClosure()
  {
    if (_typeSet == null) {
      InjectManager cdiManager = InjectManager.getCurrent();
      
      _typeSet = _type.getTypeClosure(cdiManager);
    }

    return _typeSet;
  }

  public void addAnnotations(Collection<Annotation> annSet)
  {
    for (Annotation ann : annSet)
      addAnnotation(ann);
  }

  public void addAnnotations(Annotation []annSet)
  {
    for (Annotation ann : annSet)
      addAnnotation(ann);
  }
  
  public void addAnnotation(Annotation newAnn)
  {
    if (_annSet == null)
      _annSet = new AnnotationSet();
    
    _annSet.replace(newAnn);
  }
  
  public void addAnnotationIfAbsent(Annotation newAnn)
  {
    if (! isAnnotationPresent(newAnn.annotationType()))
      addAnnotation(newAnn);
  }

  public void removeAnnotation(Annotation ann)
  {
    if (_annSet == null)
      return;

    _annSet.remove(ann);
  }

  public void clearAnnotations()
  {
    if (_annSet != null)
      _annSet.clear();
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
    
    return (T) _annSet.getAnnotation(annType);
  }

  /**
   * Returns true if the annotation is present)
   */
  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    if (_annSet == null)
      return false;
    
    return _annSet.isAnnotationPresent(annType);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
