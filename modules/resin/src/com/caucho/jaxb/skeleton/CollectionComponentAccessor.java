/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class CollectionComponentAccessor extends Accessor {
  private Accessor _accessor;
  private Class _type;
  private Type _genericType;

  public CollectionComponentAccessor(Accessor a) 
  {
    super(a.getContext());

    _accessor = a;

    Type collectionGenericType = a.getGenericType();

    if (collectionGenericType instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) collectionGenericType;

      Type[] parameters = pType.getActualTypeArguments();

      if (parameters.length < 1)
        throw new UnsupportedOperationException(L.l("Unsupported type {0}", pType));

      _genericType = parameters[0];

      try {
        if (_genericType instanceof ParameterizedType)
          _type = (Class) ((ParameterizedType) _genericType).getRawType();
        else
          _type = (Class) _genericType;
      }
      catch (ClassCastException e) {
        throw new UnsupportedOperationException(L.l("Unsupported type {0}", pType));
      }
    }
  }

  public Object get(Object o) throws JAXBException
  {
    throw new JAXBException("can't invoke CollectionComponentAccessor.get()");
  }

  public void set(Object o, Object value) throws JAXBException
  {
    throw new JAXBException("can't invoke CollectionComponentAccessor.set()");
  }

  public String getName()
  {
    return _accessor.getName();
  }

  public Class getType()
  {
    return _type;
  }

  public Type getGenericType()
  {
    return _genericType;
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    return null;
  }
}
