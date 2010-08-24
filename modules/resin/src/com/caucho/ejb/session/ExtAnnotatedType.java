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

package com.caucho.ejb.session;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.reflect.AnnotatedElementImpl;

/**
 * Server container for a session bean.
 */
public class ExtAnnotatedType<X> extends AnnotatedElementImpl implements AnnotatedType<X> {
  private Class<X> _cl;
  
  private Set<AnnotatedField<? super X>> _fields
    = new LinkedHashSet<AnnotatedField<? super X>>();
  
  private Set<AnnotatedMethod<? super X>> _methods
    = new LinkedHashSet<AnnotatedMethod<? super X>>();
  
  public ExtAnnotatedType(AnnotatedType<X> baseType)
  {
    super(baseType);
    
    _cl = baseType.getJavaClass();
  }

  @Override
  public Class<X> getJavaClass()
  {
    return _cl;
  }

  @Override
  public Set<AnnotatedConstructor<X>> getConstructors()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  
  void addField(AnnotatedField<? super X> field)
  {
    _fields.add(field);
  }

  @Override
  public Set<AnnotatedField<? super X>> getFields()
  {
    return _fields;
  }
  
  void addMethod(AnnotatedMethod<? super X> method)
  {
    _methods.add(method);
  }

  @Override
  public Set<AnnotatedMethod<? super X>> getMethods()
  {
    return _methods;
  }
}
