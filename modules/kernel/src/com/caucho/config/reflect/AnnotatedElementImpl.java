/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.util.HashMap;
import java.util.HashSet;
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
  
  private AnnotationSet _analysisAnnSet;
  
  private Annotated _sourceAnnotated;

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
    
    _sourceAnnotated = annotated;
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
    HashMap<String,BaseType> paramMap = null;
    String paramDeclName = null;
    
    return createBaseType(declaringType, type, paramMap, paramDeclName);
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType,
                                           Type type,
                                           String paramDeclName)
  {
    HashMap<String,BaseType> paramMap = null;
    
    return createBaseType(declaringType, type, paramMap, paramDeclName);
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType,
                                           Type type,
                                           HashMap<String,BaseType> paramMap,
                                           String paramDeclName)
  {
    // ioc/0242
    BaseType baseType = createBaseType(declaringType, 
                                       type,
                                       paramMap,
                                       paramDeclName,
                                       BaseType.ClassFill.PLAIN);
    
    return baseType;
  }
  
  protected static BaseType createBaseType(AnnotatedType<?> declaringType,
                                           Type type,
                                           HashMap<String,BaseType> paramMap,
                                           String paramDeclName,
                                           BaseType.ClassFill classFill)
    {
      if (declaringType instanceof BaseTypeAnnotated) {
        BaseTypeAnnotated baseTypeAnn = (BaseTypeAnnotated) declaringType;
        
        if (paramMap == null)
          paramMap = baseTypeAnn.getBaseTypeParamMap();
      
        return BaseType.create(type, paramMap, paramDeclName, classFill);
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
  
  @Override
  public BaseType getBaseTypeImpl()
  {
    return _type;
  }
  
  @Override
  public HashMap<String,BaseType> getBaseTypeParamMap()
  {
    return getBaseTypeImpl().getParamMap();
  }
  
  @Override
  public Set<VarType<?>> getTypeVariables()
  {
    HashSet<VarType<?>> typeVariables = new HashSet<VarType<?>>();
    
    fillTypeVariables(typeVariables);
    
    return typeVariables;
  }

  protected void fillTypeVariables(Set<VarType<?>> typeVariables)
  {
    getBaseTypeImpl().fillSyntheticTypes(typeVariables);
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
    for (Annotation ann : annSet) {
      addAnnotation(ann);
    }
  }

  public void addAnnotations(Annotation []annSet)
  {
    for (Annotation ann : annSet) {
      addAnnotation(ann);
    }
  }
  
  public void addAnnotation(Annotation newAnn)
  {
    if (_annSet == null) {
      _annSet = new AnnotationSet();
    }
    
    _annSet.replace(newAnn);
  }
  
  public void addAnnotationIfAbsent(Annotation newAnn)
  {
    if (! isAnnotationPresent(newAnn.annotationType())) {
      addAnnotation(newAnn);
    }
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
  
  @Override
  public void addOverrideAnnotation(Annotation ann)
  {
    _annSet.add(ann);

    // ioc/10a0 - @NoAspect - cache that base class has no aspect
    if (_sourceAnnotated instanceof BaseTypeAnnotated) {
      BaseTypeAnnotated baseAnn = (BaseTypeAnnotated) _sourceAnnotated;
      
      baseAnn.addOverrideAnnotation(ann);
    }
  }
  
  @Override
  public void addAnalysisAnnotation(Annotation ann)
  {
    if (_analysisAnnSet == null)
      _analysisAnnSet = new AnnotationSet();
    
    _analysisAnnSet.add(ann);

    // ioc/10a0 - @NoAspect - cache that base class has no aspect
    if (_sourceAnnotated instanceof BaseTypeAnnotated) {
      BaseTypeAnnotated baseAnn = (BaseTypeAnnotated) _sourceAnnotated;
      
      baseAnn.addAnalysisAnnotation(ann);
    }
  }
  
  public <T extends Annotation> T getAnalysisAnnotation(Class<T> annType)
  {
    if (_analysisAnnSet != null) {
      T ann = (T) _analysisAnnSet.getAnnotation(annType);
      
      if (ann != null)
        return ann;
    }
    
    return getAnnotation(annType);
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
