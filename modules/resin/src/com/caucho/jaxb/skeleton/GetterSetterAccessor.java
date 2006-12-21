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

public class GetterSetterAccessor extends Accessor {
  private Method _get;
  private Method _set;
  private Class _type;
  private Type _genericType;
  private String _name;

  private PropertyDescriptor _propertyDescriptor;

  public GetterSetterAccessor(PropertyDescriptor propertyDescriptor, 
                              JAXBContextImpl context)
    throws JAXBException
  {
    super(context);

    _propertyDescriptor = propertyDescriptor;

    _get = _propertyDescriptor.getReadMethod();
    _set = _propertyDescriptor.getWriteMethod();
    _name = _propertyDescriptor.getName();
    _genericType = _get.getGenericReturnType();
    _property = _context.createProperty(_genericType);

    if ("clazz".equals(_name))
      _name = "class";
  }

  public Object get(Object o)
    throws JAXBException
  {
    try {
      if (_get == null)
        return null;

      return _get.invoke(o);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public void set(Object o, Object value)
    throws JAXBException
  {
    try {
      if (_set == null)
        return;

      _set.invoke(o, value);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    A a = null;

    if (_get != null)
      a = _get.getAnnotation(c);
    else if (_set != null)
      a = _set.getAnnotation(c);

    return a;
  }

  public Class getType()
  {
    return _propertyDescriptor.getPropertyType();
  }

  public Type getGenericType()
  {
    return getType();
  }

  public String getName()
  {
    return _name;
  }
}
