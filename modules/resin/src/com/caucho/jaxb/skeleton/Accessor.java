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
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;
import com.caucho.jaxb.*;

import com.caucho.jaxb.*;
import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.bind.annotation.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;

/** an Accessor is either a getter/setter pair or a field */
public abstract class Accessor {

  private JAXBContextImpl _context;

  protected Accessor(JAXBContextImpl context)
  {
    _context = context;
  }

  public JAXBContextImpl getContext()
  {
    return _context;
  }

  public abstract Object     get(Object o) throws JAXBException;
  public abstract void       set(Object o, Object value) throws JAXBException;
  public abstract String     getName();
  public abstract Class      getType();
  public abstract Annotation getAnnotation(Class c);

  public XmlType getXmlType()
  {
    return (XmlType)getAnnotation(XmlType.class);
  }

  public QName getQName()
  {
    XmlType xmlType = getXmlType();

    if (xmlType == null || xmlType.name().equals("#default"))
      return new QName(getName());

    if (xmlType.namespace().equals("#default"))
      return new QName(xmlType.name());

    return new QName(xmlType.namespace(), xmlType.name());
  }

  public static class FieldAccessor extends Accessor {

    private Field _field;
    private Class _type;

    public FieldAccessor(Field f, JAXBContextImpl context)
    {
      super(context);
      _field = f;
      _type = _field.getType();
    }

    public Object     get(Object o)
      throws JAXBException
    {
      try {
        return _field.get(o);
      }
      catch (Exception e) {
        throw new JAXBException(e);
      }
    }

    public void       set(Object o, Object value)
      throws JAXBException
    {
      try {
        _field.set(o, value);
      }
      catch (Exception e) {
        throw new JAXBException(e);
      }
    }

    public Annotation getAnnotation(Class c)
    {
      return _field.getAnnotation(c);
    }

    public Class getType()
    {
      return _type;
    }

    public String getName()
    {
      return _field.getName();
    }
  }

  public static class GetterSetterAccessor extends Accessor {
    private Method _get;
    private Method _set;
    private Class _type;
    private String _name;

    public GetterSetterAccessor(Class c, String name, JAXBContextImpl context)
      throws JAXBException
    {
      super(context);
      try {
        String getName =
          "get" +
          name.substring(0, 1).toUpperCase() +
          name.substring(1);
        
        String setName =
          "set" +
          name.substring(0, 1).toUpperCase() +
          name.substring(1);
        
        _get = c.getMethod(getName, new Class[] { });
        
        _type = _get.getReturnType();
        
        _set = c.getMethod(setName, new Class[] { _type });
        
        _name = name;
      }
      catch (Exception e) {
        throw new JAXBException(e);
      }
    }

    public Object     get(Object o)
      throws JAXBException
    {
      try {
        return _get.invoke(o);
      }
      catch (Exception e) {
        throw new JAXBException(e);
      }
    }

    public void       set(Object o, Object value)
      throws JAXBException
    {
      try {
        _set.invoke(o, value);
      }
      catch (Exception e) {
        throw new JAXBException(e);
      }
    }

    public Annotation getAnnotation(Class c)
    {
      Annotation a = _get.getAnnotation(c);

      if (a != null)
        return a;

      a = _set.getAnnotation(c);
      return a;
    }

    public Class getType()
    {
      return _type;
    }

    public String getName()
    {
      return _name;
    }
  }

  public static class ArrayComponentAccessor extends Accessor {
    private Accessor _accessor;
    public ArrayComponentAccessor(Accessor a) {
      super(a.getContext());
      _accessor = a;
    }
    public Object     get(Object o) throws JAXBException
    {
      throw new JAXBException("cannot invoke ArrayComponentAccessor.get()");
    }
    public void       set(Object o, Object value) throws JAXBException
    {
      throw new JAXBException("cannot invoke ArrayComponentAccessor.set()");
    }
    public String     getName()
    {
      return _accessor.getName();
    }
    public Class      getType()
    {
      return _accessor.getType().getComponentType();
    }
    public Annotation getAnnotation(Class c)
    {
      return null;
    }
  }

  public static class CollectionComponentAccessor extends Accessor {
    private Accessor _accessor;

    public CollectionComponentAccessor(Accessor a) 
    {
      super(a.getContext());
      _accessor = a;
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
      // XXX
      return Object.class;
    }

    public Annotation getAnnotation(Class c)
    {
      return null;
    }
  }
  
  public static class WrapperListAccessor extends Accessor {
    private QName _name;
    private Class _type;

    public WrapperListAccessor(JAXBContextImpl context, QName name, Class type) 
    {
      super(context);
      _name = name;
      _type = type;
    }

    public Object get(Object o) throws JAXBException
    {
      return ((Map<QName,Object>) o).get(_name);
    }

    public void set(Object o, Object value) throws JAXBException
    {
      ((Map<QName,Object>) o).put(_name, value);
    }

    public String getName()
    {
      return _name.getLocalPart(); // XXX _name.toString()?
    }

    public Class getType()
    {
      return _type;
    }

    public Annotation getAnnotation(Class c)
    {
      return null;
    }
  }
}
