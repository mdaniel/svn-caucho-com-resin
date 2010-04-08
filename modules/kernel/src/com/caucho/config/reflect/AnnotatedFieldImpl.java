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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;


/**
 * Abstract introspected view of a Bean
 */
public class AnnotatedFieldImpl
  extends AnnotatedElementImpl implements AnnotatedField
{
  private AnnotatedType _declaringType;
  
  private Field _field;
  
  public AnnotatedFieldImpl(AnnotatedType declaringType, Field field)
  {
    super(field.getGenericType(), null, field.getAnnotations());

    _declaringType = declaringType;
    _field = field;

    introspect(field);
  }

  public AnnotatedType getDeclaringType()
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
