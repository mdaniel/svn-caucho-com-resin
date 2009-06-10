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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

/**
 * Abstract introspected view of a Bean
 */
public class AnnotatedMethodImpl
  extends AnnotatedElementImpl implements AnnotatedMethod
{
  private AnnotatedType _declaringType;
  
  private Method _method;

  private ArrayList<AnnotatedParameter> _parameterList
    = new ArrayList<AnnotatedParameter>();
  
  public AnnotatedMethodImpl(Method method)
  {
    this(null, null, method);
  }
  
  public AnnotatedMethodImpl(AnnotatedType declaringType,
			Annotated annotated,
			Method method)
  {
    super(method.getGenericReturnType(), annotated, method.getAnnotations());

    _declaringType = declaringType;
    _method = method;

    introspect(method);
  }

  public AnnotatedType getDeclaringType()
  {
    return _declaringType;
  }
  
  /**
   * Returns the reflected Method
   */
  public Method getJavaMember()
  {
    return _method;
  }

  /**
   * Returns the constructor parameters
   */
  public List<AnnotatedParameter> getParameters()
  {
    return _parameterList;
  }

  public boolean isStatic()
  {
    return Modifier.isStatic(_method.getModifiers());
  }

  private void introspect(Method method)
  {
    Type []paramTypes = method.getGenericParameterTypes();
    Annotation [][]annTypes = method.getParameterAnnotations();
    
    for (int i = 0; i < paramTypes.length; i++) {
      AnnotatedParameterImpl param
	= new AnnotatedParameterImpl(this, paramTypes[i], annTypes[i]);
    
      _parameterList.add(param);
    }
  }

  public static boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName()))
      return false;

    Class []paramA = methodA.getParameterTypes();
    Class []paramB = methodB.getParameterTypes();

    if (paramA.length != paramB.length)
      return false;

    for (int i = 0; i < paramA.length; i++) {
      if (! paramA[i].equals(paramB[i]))
	return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    return _method.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof AnnotatedMethodImpl))
      return false;

    AnnotatedMethodImpl method = (AnnotatedMethodImpl) obj;

    return isMatch(_method, method._method);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
