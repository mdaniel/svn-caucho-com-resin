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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

/**
 * Abstract introspected view of a Bean
 */
public class BeanConstructorImpl
  extends AnnotatedElementImpl implements AnnotatedConstructor
{
  private final AnnotatedType _declaringType;
  
  private final Constructor _ctor;

  private final ArrayList<AnnotatedParameter> _parameterList
    = new ArrayList<AnnotatedParameter>();
  
  public BeanConstructorImpl(AnnotatedType declaringType, Constructor ctor)
  {
    super(declaringType.getType(), ctor.getAnnotations());

    _declaringType = declaringType;
    
    _ctor = ctor;

    introspect(ctor);
  }

  public AnnotatedType getDeclaringType()
  {
    return _declaringType;
  }
  
  /**
   * Returns the reflected Constructor
   */
  public Constructor getJavaMember()
  {
    return _ctor;
  }

  /**
   * Returns the constructor parameters
   */
  public List<AnnotatedParameter> getParameters()
  {
    return _parameterList;
  }

  private void introspect(Constructor ctor)
  {
    Type []paramTypes = ctor.getGenericParameterTypes();
    Annotation [][]annTypes = ctor.getParameterAnnotations();
    
    for (int i = 0; i < paramTypes.length; i++) {
      BeanParameterImpl param
	= new BeanParameterImpl(this, paramTypes[i], annTypes[i]);
	
      _parameterList.add(param);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ctor + "]";
  }
}
