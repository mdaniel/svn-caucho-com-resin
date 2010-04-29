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

package javax.enterprise.util;

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Convenience API to create runtime Annotations.
 *
 * <code><pre>
 * Annotation current = new AnnotationLiteral&lt;Current>() {}
 * </pre></code>
 *
 * <code><pre>
 * Annotation named = new AnnotationLiteral&lt;Named>() {
 *   public String name() { return "my-name"; }
 * }
 * </pre></code>
 */
public abstract class AnnotationLiteral<T extends Annotation>
  implements Annotation, Serializable
{
  private transient Class<T> _annotationType;
  private transient Method [] _methods;
  private transient int _hashCode;
  
  protected AnnotationLiteral()
  {
  }
  
  @Override
  public final Class<T> annotationType()
  {
    if (_annotationType == null) {
      fillAnnotationType(getClass());
    }
    
    return _annotationType;
  }
  
  private void fillAnnotationType(Class<?> cl)
  {
    if (cl == null)
      throw new UnsupportedOperationException(getClass().toString());
      
    Type type = cl.getGenericSuperclass();

    if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      _annotationType = (Class) pType.getActualTypeArguments()[0];
    }
    else {
      fillAnnotationType(cl.getSuperclass());
    }
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof Annotation))
      return false;

    Class<?> annTypeA = annotationType();
    Class<?> annTypeB = ((Annotation) o).annotationType();
    
    if (! annTypeA.equals(annTypeB))
      return false;
    
    for (Method annMethod : getMethods()) {
      if (annMethod.getParameterTypes().length > 0 
          || annMethod.getDeclaringClass() == Annotation.class
          || annMethod.getDeclaringClass() == Object.class) {
        continue;
      }
      
      try {
        annMethod.setAccessible(true);
        
        Object a = annMethod.invoke(this);
        Object b = annMethod.invoke(o);
        
        if (a != b && (a == null || ! a.equals(b)))
          return false;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    return true;
  }
  
  @Override
  public int hashCode()
  {
    if (_hashCode != 0)
      return _hashCode;
    
    int hash = 0;
    
    for (Method annMethod : getMethods()) {
      try {
        Method method = getClass().getMethod(annMethod.getName());
        
        method.setAccessible(true);

        hash += (127 * method.getName().hashCode()) ^ valueHashCode(method);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    _hashCode = hash;

    return hash;
  }
  
  private Method []getMethods()
  {
    if (_methods == null) {
      Class<?> annType = annotationType();
      
      _methods = annType.getDeclaredMethods();
      
      if (_methods.length > 0 && ! annType.isAssignableFrom(getClass())) {
        throw new IllegalStateException("Annotation literal '" + getClass()
                                        + "' must implement '" + annType.getName()
                                        + "' because it has member values.");
      }
    }
    
    return _methods;
  }
  
  private int valueHashCode(Method method)
  {
    try {
      Object value = method.invoke(this);

      if (value != null)
        return value.hashCode();
      else
        return 0;
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public String toString()
  {
    return "@" + annotationType().getName() + "()";
  }
}
