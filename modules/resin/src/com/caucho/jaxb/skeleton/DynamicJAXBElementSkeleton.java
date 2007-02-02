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

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 *
 * Wrapper skeleton when a JAXBElement<X> is specified but no explicit
 * JAXBElementSkeleton<X> is registered.  This class is intended to be
 * instantiated once per JAXBContext.
 *
 **/
public class DynamicJAXBElementSkeleton extends ClassSkeleton {
  private static final Logger log 
    = Logger.getLogger(DynamicJAXBElementSkeleton.class.getName());
  private static final L10N L = new L10N(DynamicJAXBElementSkeleton.class);
  private static final Object[] SINGLE_NULL_ARG = new Object[] {null};

  private DynamicAccessor _accessor;

  public DynamicJAXBElementSkeleton(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);

    _accessor = new DynamicAccessor(context);
    _value = _accessor;
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
  {
    if (! (obj instanceof JAXBElement))
      throw new IllegalArgumentException(L.l("Object must be a JAXBElement"));

    JAXBElement element = (JAXBElement) obj;

    _accessor.setQName(element.getName());
    _accessor.setType(element.getDeclaredType());

    super.write(m, out, obj, fieldName);
  }

  public void write(Marshaller m, XMLEventWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
  {
    if (! (obj instanceof JAXBElement))
      throw new IllegalArgumentException(L.l("Object must be a JAXBElement"));

    JAXBElement element = (JAXBElement) obj;

    _accessor.setQName(element.getName());
    _accessor.setType(element.getDeclaredType());

    super.write(m, out, obj, fieldName);
  }

  public Object newInstance()
    throws JAXBException
  {
    // This skeleton should not be used for reading
    throw new IllegalStateException();
  }

  private class DynamicAccessor extends Accessor {
    private Class _cl;

    public DynamicAccessor(JAXBContextImpl context)
      throws JAXBException
    {
      super(context);
    }

    public void setQName(QName qname)
    {
      _qname = qname;
    }

    public QName getQName()
    {
      return _qname;
    }

    public Object get(Object o) 
      throws JAXBException
    {
      JAXBElement element = (JAXBElement) o;

      return element.getValue();
    }

    public void set(Object o, Object value) 
      throws JAXBException
    {
      JAXBElement element = (JAXBElement) o;
      element.setValue(value);
    }

    public String getName()
    {
      return null;
    }

    public void setType(Class cl)
      throws JAXBException
    {
      _cl = cl;
      _property = _context.createProperty(cl);
    }

    public Class getType()
    {
      return _cl;
    }

    public Type getGenericType()
    {
      return _cl;
    }

    public <A extends Annotation> A getAnnotation(Class<A> c)
    {
      return null;
    }
  }

  public String toString()
  {
    return "DynamicJAXBElementSkeleton";
  }
}
