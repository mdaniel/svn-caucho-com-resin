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
 * @author Scott Ferguson
 */

package com.caucho.v5.config.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.v5.inject.Module;


/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotatedFieldImpl<X>
  extends AnnotatedElementImpl implements AnnotatedField<X>
{
  private AnnotatedType<X> _declaringType;
  
  private Field _field;
  
  public AnnotatedFieldImpl(AnnotatedType<X> declaringType, Field field)
  {
    super(createBaseType(declaringType, field.getGenericType()),
          null, 
          field.getAnnotations());

    _declaringType = declaringType;
    _field = field;

    introspect(field);
  }

  public AnnotatedType<X> getDeclaringType()
  {
    return _declaringType;
  }
  
  /**
   * Returns the reflected Method
   */
  public Field getJavaMember()
  {
    return _field;
  }

  public boolean isStatic()
  {
    return Modifier.isStatic(_field.getModifiers());
  }

  private void introspect(Field field)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _field + "]";
  }
}
