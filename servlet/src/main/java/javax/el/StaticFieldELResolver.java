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
 * @author Paul Cowan
 */

package javax.el;

import java.beans.FeatureDescriptor;
import java.lang.reflect.*;
import java.util.*;

public class StaticFieldELResolver extends ELResolver
{
  public Object getValue(ELContext context, Object base, Object property)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! (base instanceof ELClass && property instanceof String))
      return null;
    
    Class<?> baseClass = ((ELClass)base).getKlass();
    if (baseClass == null)
      throw new NullPointerException();

    context.setPropertyResolved(base, property);

    String fieldName = (String) property;
    Field field = null;
    try {
      field = baseClass.getField(fieldName);
    } catch (NoSuchFieldException e) {
      throw new PropertyNotFoundException(String.format("%s.%s", 
                                                        baseClass.getName(),
                                                        fieldName));
    }
    
    int fieldModifiers = field.getModifiers();
    if (! Modifier.isPublic(fieldModifiers) || 
        ! Modifier.isStatic(fieldModifiers)) {
        throw new PropertyNotFoundException(String.format("%s.%s", 
                                                          baseClass.getName(),
                                                          fieldName));
    }

    try {
      return field.get(null);
    } catch (IllegalAccessException e) {
      throw new PropertyNotFoundException(e);
    }
  }
  
  // writing to a static field is not allowed
  public void setValue(ELContext context, Object base, Object property,
                       Object value)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! (base instanceof ELClass && property instanceof String))
      return;
    
    Class<?> baseClass = ((ELClass)base).getKlass();
    if (baseClass == null)
      throw new NullPointerException();

    context.setPropertyResolved(base, property);
    
    throw new PropertyNotWritableException(
      String.format("Writing to a static field is not allowed: %s.%s", 
                    baseClass.getName(), property));
  }
  
  public Object invoke(ELContext context, 
                       Object base, 
                       Object methodObj,
                       Class<?> []paramTypes, 
                       Object []params)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! (base instanceof ELClass))
      return null;
    
    Class<?> baseClass = ((ELClass)base).getKlass();
    if (baseClass == null)
      throw new NullPointerException();

    String methodName = null;

    if (methodObj instanceof String)
      methodName = (String) methodObj;
    else if (methodObj instanceof Enum<?>)
      methodName = ((Enum<?>) methodObj).name();
    else
      methodName = String.valueOf(methodObj);

    context.setPropertyResolved(base, methodObj);

    if (methodName.equals("<init>") || 
        methodName.equals(baseClass.getSimpleName())) {
      Constructor<?> constructor = 
        ReflectUtil.getInstance().findConstructor(baseClass, 
                                                  paramTypes, 
                                                  params);
      return ReflectUtil.getInstance().invokeConstructor(context, 
                                                         constructor, 
                                                         params);
    }
    else {
      Method method = 
        ReflectUtil.getInstance().findMethod(baseClass, 
                                             methodName, 
                                             paramTypes, 
                                             params,
                                             true);
      
      return ReflectUtil.getInstance().invokeMethod(context, 
                                                    method, 
                                                    null, 
                                                    params);
     }
  }
  
  public Class<?> getType(ELContext context, Object base, Object property)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! (base instanceof ELClass && property instanceof String))
      return null;
    
    Class<?> baseClass = ((ELClass)base).getKlass();
    if (baseClass == null)
      throw new NullPointerException();

    context.setPropertyResolved(true);

    String fieldName = (String) property;
    Field field = null;
    try {
      field = baseClass.getField(fieldName);
    } catch (NoSuchFieldException e) {
      throw new PropertyNotFoundException(String.format("%s.%s", 
                                                        baseClass.getName(),
                                                        fieldName));
    }
    
    int fieldModifiers = field.getModifiers();
    if (! Modifier.isPublic(fieldModifiers) || 
        ! Modifier.isStatic(fieldModifiers)) {
        throw new PropertyNotFoundException(String.format("%s.%s", 
                                                          baseClass.getName(),
                                                          fieldName));
    }

    return field.getType();
  }
  
  // writing to a static field is not allowed
  public boolean isReadOnly(ELContext context, Object base, Object property)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! (base instanceof ELClass && property instanceof String))
      return true;
    
    Class<?> baseClass = ((ELClass)base).getKlass();
    if (baseClass == null)
      throw new NullPointerException();

    context.setPropertyResolved(true);
    return true;
  }
  
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    return null;
  }
  
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    return String.class;
  }
}
