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

package com.caucho.config.gen;

import com.caucho.config.reflect.AnnotatedElementImpl;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * Represents an introspected method.
 */
public class ApiMethod extends ApiMember {
  private Method _method;
  private Class _returnType;
  private Class []_parameterTypes;
  private Class []_exceptionTypes;
  
  /**
   * Creates a new method.
   *
   * @param topClass the top class
   * @param method the introspected method
   */
  public ApiMethod(ApiClass apiClass,
		   Method method,
		   AnnotatedMethod annMethod,
		   HashMap<String,Type> typeMap)
  {
    super(apiClass,
	  method.getGenericReturnType(), annMethod,
	  method.getAnnotations());
    
    _method = method;

    introspect(method, typeMap);
  }

  /**
   * Returns the underlying method.
   */
  public Method getMethod()
  {
    return _method;
  }

  public Method getJavaMember()
  {
    return _method;
  }

  /**
   * Returns the method name.
   */
  public String getName()
  {
    return _method.getName();
  }

  /**
   * Returns true for a public method.
   */
  public boolean isPublic()
  {
    return Modifier.isPublic(_method.getModifiers());
  }

  /**
   * Returns true for a protected method.
   */
  public boolean isProtected()
  {
    return Modifier.isProtected(_method.getModifiers());
  }

  /**
   * Returns true for a static method.
   */
  public boolean isStatic()
  {
    return Modifier.isStatic(_method.getModifiers());
  }

  /**
   * Returns true for a final method.
   */
  public boolean isFinal()
  {
    return Modifier.isFinal(_method.getModifiers());
  }

  /**
   * Returns true for an abstract method.
   */
  public boolean isAbstract()
  {
    return Modifier.isAbstract(_method.getModifiers());
  }

  /**
   * Returns the method parameter types.
   */
  public Class []getParameterTypes()
  {
    return _parameterTypes;
  }

  /**
   * Returns true for var args
   */
  public boolean isVarArgs()
  {
    return _method.isVarArgs();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    return _returnType;
  }

  /**
   * Returns the method exception types.
   */
  public Class []getExceptionTypes()
  {
    return _exceptionTypes;
  }

  /**
   * Returns true if the method name matches.
   */
  public boolean isMatch(ApiMethod method)
  {
    return isMatch(method.getName(), method.getParameterTypes());
  }
  
  /**
   * Returns true if the method name matches.
   */
  public boolean isMatch(String name, Class []param)
  {
    if (! name.equals(_method.getName()))
      return false;

    if (_parameterTypes.length != param.length)
      return false;

    for (int i = 0; i < param.length; i++) {
      if (! param[i].equals(_parameterTypes[i]))
	return false;
    }

    return true;
  }

  private void introspect(Method method, HashMap<String,Type> typeMap)
  {
    Type []paramTypes = method.getGenericParameterTypes();
    
    _parameterTypes = new Class[paramTypes.length];

    for (int i = 0; i < paramTypes.length; i++)
      _parameterTypes[i] = resolve(paramTypes[i], typeMap);

    _returnType = resolve(method.getGenericReturnType(), typeMap);

    Type []exceptionTypes = method.getGenericExceptionTypes();
    
    _exceptionTypes = new Class[exceptionTypes.length];

    for (int i = 0; i < exceptionTypes.length; i++)
      _exceptionTypes[i] = resolve(exceptionTypes[i], typeMap);
  }

  private Class resolve(Type type, HashMap<String,Type> typeMap)
  {
    if (type instanceof Class)
      return (Class) type;
    else if (type instanceof TypeVariable) {
      TypeVariable var = (TypeVariable) type;

      Type value = typeMap.get(var.getName());

      if (value != null && value != type)
	return resolve(value, typeMap);

      Type []bounds = var.getBounds();

      if (bounds == null || bounds.length < 1)
	return Object.class;
      else
	return resolve(bounds[0], typeMap);
    }
    else if (type instanceof GenericArrayType) {
      GenericArrayType arrayType = (GenericArrayType) type;

      Class compType = resolve(arrayType.getGenericComponentType(), typeMap);

      try {
	return Array.newInstance(compType, 0).getClass();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      return (Class) pType.getRawType();
    }
    else
      throw new UnsupportedOperationException(type.getClass().getName());
  }

  @Override
  public int hashCode()
  {
    return _method.getName().hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof ApiMethod))
      return false;

    ApiMethod method = (ApiMethod) o;

    if (! _method.getName().equals(method._method.getName()))
      return false;

    if (_parameterTypes.length != method._parameterTypes.length)
      return false;

    for (int i = 0; i < _parameterTypes.length; i++) {
      if (! _parameterTypes[i].equals(method._parameterTypes[i]))
	return false;
    }

    return true;
  }

  public String getFullName()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("(");
    for (int i = 0; i < _parameterTypes.length; i++) {
      if (i != 0)
	sb.append(",");
      sb.append(_parameterTypes[i].getSimpleName());
    }
    sb.append(")");

    return sb.toString();
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("ApiMethod[");
    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("(");
    for (int i = 0; i < _parameterTypes.length; i++) {
      if (i != 0)
	sb.append(",");
      sb.append(_parameterTypes[i].getSimpleName());
    }
    sb.append(")");
	 
    sb.append("]");

    return sb.toString();
  }
}
