/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
import java.util.List;
import java.util.Map;

public class FieldAccessor extends Accessor {
  private final Package _package;
  private final Field _field;
  private final Class _type;
  private final Type _genericType;

  public FieldAccessor(JAXBContextImpl context, Field f)
    throws JAXBException
  {
    super(context);

    Class declarer = f.getDeclaringClass();
    
    _package = declarer.getPackage();
    _field = f;
    _type = _field.getType();
    _genericType = _field.getGenericType();
    _name = _field.getName();

    init();
  }

  public Object get(Object o)
    throws JAXBException
  {
    try {
      return _field.get(o);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public void set(Object o, Object value)
    throws JAXBException
  {
    try {
      _field.set(o, value);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    return _field.getAnnotation(c);
  }

  public <A extends Annotation> A getPackageAnnotation(Class<A> c)
  {
    return _package.getAnnotation(c);
  }

  public Class getType()
  {
    return _type;
  }

  public Type getGenericType()
  {
    return _genericType;
  }

  public String getName()
  {
    return _name;
  }

  public String toString()
  {
    return "FieldAccessor[" + _field.getDeclaringClass().getName() + "." +
                              _field.getName() + "]";
  }
}
